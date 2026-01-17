/**
 * Hubitat Alexa Routine Trigger - Virtual Contact Trigger
 *
 * Author: Chris Truitt
 * Website: https://christruitt.com
 * GitHub: https://github.com/truittchris
 * Namespace: truittchris
 *
 * Note: Some Hubitat environments do not support paragraph() in driver preferences.
 * This driver intentionally avoids paragraph() to prevent compilation errors.
 */

import groovy.transform.Field

@Field static final String DRIVER_VERSION = "0.8.2"

metadata {
    definition(name: "Hubitat Alexa Routine Trigger - Virtual Contact Trigger", namespace: "truittchris", author: "Chris Truitt") {
        capability "ContactSensor"
        capability "Sensor"
        capability "Actuator"

        command "open"
        command "close"

        attribute "resetAfterSeconds", "number"
        attribute "minTimeBetweenTriggersMs", "number"

        attribute "lastActionAt", "string"
        attribute "lastCommand", "string"
        attribute "driverVersion", "string"
        attribute "supportUrl", "string"
    }

    preferences {
        section("Overview") {
            input name: "driverInfoVersion", type: "enum", title: "Driver version", options: [DRIVER_VERSION], defaultValue: DRIVER_VERSION, required: false
        }

        section("Options") {
            input name: "autoResetSeconds", type: "number", title: "Turn off after (seconds)", defaultValue: 5
            input name: "debounceMs", type: "number", title: "Ignore repeated triggers for (ms)", defaultValue: 250
        }

        section("Advanced") {
            input name: "logLevel", type: "enum", title: "Logging", options: ["Off", "Basic", "Debug"], defaultValue: "Off", required: true
            input name: "debugFor30", type: "bool", title: "Enable Debug logging for 30 minutes", defaultValue: false, required: false
        }

        section("Support") {
            input name: "supportInfo", type: "enum", title: "Support", options: ["https://christruitt.com/tip-jar/"], defaultValue: "https://christruitt.com/tip-jar/", required: false
        }
    }
}

def driverVersion() { return DRIVER_VERSION }

def installed() { initialize() }

def updated() {
    migrateLoggingIfNeeded()
    if (settings?.debugFor30 == true) enableDebugFor30Minutes()
    unschedule()
    initialize()
}

def initialize() {
    migrateLoggingIfNeeded()

    if (device.currentValue("contact") == null) {
        sendEvent(name: "contact", value: "closed", isStateChange: true)
    }

    sendEvent(name: "driverVersion", value: driverVersion(), isStateChange: true)
    sendEvent(name: "supportUrl", value: "https://christruitt.com", isStateChange: true)

    syncAttributesFromLocalConfig()
}

/*
 * Called by the parent app after it updates device settings, so Current States reflect app-level values.
 * Intentionally not exposed as a command.
 */

def syncFromApp(Map cfg = null) {
    syncAttributesFromLocalConfig()
}

/* Commands */

def open() {
    if (isDebounced()) return
    sendEvent(name: "contact", value: "open", isStateChange: true)
    diag("open")

    Integer sec = safeInt(settings.autoResetSeconds, 5)
    if (sec < 0) sec = 0
    if (sec > 3600) sec = 3600
    if (sec > 0) runIn(sec, "close", [overwrite: true])
}

def close() {
    if (isDebounced(forceCloseOk: true) == true) return
    sendEvent(name: "contact", value: "closed", isStateChange: true)
    diag("close")
}

/* Internals */

private void syncAttributesFromLocalConfig() {
    sendEvent(name: "resetAfterSeconds", value: safeInt(settings.autoResetSeconds, 5), isStateChange: true)
    sendEvent(name: "minTimeBetweenTriggersMs", value: safeInt(settings.debounceMs, 250), isStateChange: true)
}

private Boolean isDebounced(Map opts = [:]) {
    Boolean forceCloseOk = (opts?.forceCloseOk == true)

    Long last = state.lastActionMs as Long
    Long nowMs = now()

    Integer window = safeInt(settings.debounceMs, 250)
    if (window < 0) window = 0
    if (window > 60000) window = 60000

    if (last && (nowMs - last) < window) {
        if (forceCloseOk && device.currentValue("contact") == "open") {
            // allow scheduled close even inside debounce window
        } else {
            logDebug("Suppressed duplicate (${nowMs - last}ms < ${window}ms)")
            return true
        }
    }

    state.lastActionMs = nowMs
    return false
}

private void diag(String cmd) {
    sendEvent(name: "lastCommand", value: cmd, isStateChange: true)
    sendEvent(name: "lastActionAt", value: nowIso(), isStateChange: true)
    logBasic("${cmd}()")
}

private void migrateLoggingIfNeeded() {
    if (settings?.logLevel) return

    // Legacy booleans may exist from older versions.
    String inferred = "Off"
    if (settings?.debugLogging == true) inferred = "Debug"
    else if (settings?.infoLogging == true) inferred = "Basic"

    device.updateSetting("logLevel", [type: "enum", value: inferred])
}

private void enableDebugFor30Minutes() {
    device.updateSetting("logLevel", [type: "enum", value: "Debug"])
    device.updateSetting("debugFor30", [type: "bool", value: false])
    runIn(1800, "disableDebug")
    logWarn("Debug logging enabled for 30 minutes.")
}

def disableDebug() {
    device.updateSetting("logLevel", [type: "enum", value: "Off"])
    logWarn("Debug logging disabled.")
}

private void logDebug(String msg) {
    if (settings?.logLevel == "Debug") log.debug "${device.displayName}: ${msg}"
}

private void logBasic(String msg) {
    if (settings?.logLevel in ["Basic", "Debug"]) log.info "${device.displayName}: ${msg}"
}

private void logWarn(String msg) {
    log.warn "${device.displayName}: ${msg}"
}

private Integer safeInt(def v, Integer dflt) {
    try { return (v == null) ? dflt : Integer.parseInt(v.toString()) }
    catch (ignored) { return dflt }
}

private String nowIso() {
    def tz = location?.timeZone ?: TimeZone.getTimeZone("America/New_York")
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}
