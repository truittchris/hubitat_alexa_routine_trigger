/**
 * Hubitat Alexa Routine Trigger – Group Trigger Device (Contact)
 *
 * Author: Chris Truitt
 * Website: https://christruitt.com
 * GitHub: https://github.com/truittchris
 * Namespace: truittchris
 *
 * Purpose
 * - Alexa-facing trigger device created as part of a group.
 * - Alexa routines should typically use: When contact opens.
 */

metadata {
    definition(name: "Hubitat Alexa Routine Trigger - Child Contact", namespace: "truittchris", author: "Chris Truitt") {
        capability "ContactSensor"
        capability "Sensor"
        capability "Actuator"

        command "open"
        command "close"

        attribute "resetAfterSeconds", "number"
        attribute "minTimeBetweenTriggersMs", "number"
        attribute "groupName", "string"
        attribute "triggerNumber", "number"

        attribute "lastActionAt", "string"
        attribute "lastCommand", "string"
        attribute "driverVersion", "string"
        attribute "supportUrl", "string"
    }

    preferences {
        input name: "autoResetSeconds", type: "number", title: "Reset after (seconds)", defaultValue: 5
        input name: "debounceMs", type: "number", title: "Minimum time between triggers (ms)", defaultValue: 250

        input name: "infoLogging", type: "bool", title: "Enable info logging", defaultValue: true
        input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false

    }
}

def driverVersion() { return "0.8.2" }

def installed() { initialize() }
def updated() { unschedule(); initialize() }

def initialize() {
    if (device.currentValue("contact") == null) {
        sendEvent(name: "contact", value: "closed", isStateChange: true)
    }

    sendEvent(name: "driverVersion", value: driverVersion(), isStateChange: true)
    sendEvent(name: "supportUrl", value: "https://christruitt.com", isStateChange: true)

    syncAttributesFromLocalConfig()
}

/*
 * Called by the parent app to push friendly display fields into Current States.
 * Intentionally not exposed as a command.
 */
def syncFromApp(Map cfg) {
    if (cfg == null) return
    if (cfg.groupName != null) sendEvent(name: "groupName", value: cfg.groupName.toString(), isStateChange: true)
    if (cfg.triggerNumber != null) sendEvent(name: "triggerNumber", value: safeInt(cfg.triggerNumber, null), isStateChange: true)
    syncAttributesFromLocalConfig()
}

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

    // Friendly group metadata may be stored as data values for resilience.
    try {
        def g = device.getDataValue("heArtGroupName")
        def n = device.getDataValue("heArtChildNum")
        if (g) sendEvent(name: "groupName", value: g.toString(), isStateChange: true)
        if (n) sendEvent(name: "triggerNumber", value: safeInt(n, null), isStateChange: true)
    } catch (ignored) { }
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
            if (settings.debugLogging == true) log.debug "${device.displayName}: suppressed duplicate (${nowMs - last}ms < ${window}ms)"
            return true
        }
    }

    state.lastActionMs = nowMs
    return false
}

private void diag(String cmd) {
    sendEvent(name: "lastCommand", value: cmd, isStateChange: true)
    sendEvent(name: "lastActionAt", value: nowIso(), isStateChange: true)
    if (settings.infoLogging != false) log.info "${device.displayName} ${cmd}()"
}

private Integer safeInt(def v, Integer dflt) {
    try { return (v == null) ? dflt : Integer.parseInt(v.toString()) }
    catch (ignored) { return dflt }
}

private String nowIso() {
    return new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
}