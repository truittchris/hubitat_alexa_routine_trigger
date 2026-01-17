/**
 * Hubitat Alexa Routine Trigger – MultiTrigger Controller
 *
 * Author: Chris Truitt
 * Website: https://christruitt.com
 * GitHub: https://github.com/truittchris
 * Namespace: truittchris
 *
 * Purpose
 * - Hubitat-facing controller for a trigger group.
 * - Turn the switch on to fire the group once.
 * - The switch returns to off automatically so it can be triggered again.
 *
 * Configuration
 * - All settings are managed in the Hubitat Alexa Routine Trigger app.
 * - This device shows key settings under Current States.
 */

metadata {
    definition(name: "Hubitat Alexa Routine Trigger - MultiTrigger Controller", namespace: "truittchris", author: "Chris Truitt") {
        capability "Actuator"
        capability "Refresh"
        capability "Switch"

        attribute "groupName", "string"
        attribute "triggerCount", "number"
        attribute "resetAfterSeconds", "number"
        attribute "timeBetweenTriggersMs", "number"
        attribute "minTimeBetweenGroupTriggersMs", "number"
        attribute "lastTriggeredAt", "string"
        attribute "lastError", "string"
        attribute "driverVersion", "string"
        attribute "supportUrl", "string"
    }

    preferences {
        input name: "infoLogging", type: "bool", title: "Enable info logging", defaultValue: true
        input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false

    }
}

def driverVersion() { return "0.8.2" }

def installed() { initialize() }
def updated() { unschedule(); initialize() }

def initialize() {
    if (device.currentValue("switch") == null) {
        sendEvent(name: "switch", value: "off", isStateChange: true)
    }

    // Keep a visible version + support pointer on every device.
    sendEvent(name: "driverVersion", value: driverVersion(), isStateChange: true)
    sendEvent(name: "supportUrl", value: "https://christruitt.com", isStateChange: true)

    // Populate display attributes from data values if present.
    syncAttributesFromDataValues()
}

def on() {
    if (isSuppressed()) return

    clearError()
    sendEvent(name: "switch", value: "on", isStateChange: true)

    try {
        if (!parent) {
            setError("No parent app attached. Recreate this group from the Hubitat Alexa Routine Trigger app.")
            return
        }

        parent.multiTriggerTriggerAll(device.deviceNetworkId)
        sendEvent(name: "lastTriggeredAt", value: nowIso(), isStateChange: true)

    } catch (MissingMethodException e) {
        setError("Parent app is out of date. Update the app code and try again.")
    } catch (ex) {
        setError(ex.toString())
    } finally {
        // Always behave like a momentary trigger switch.
        runIn(1, "off", [overwrite: true])
    }
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def refresh() {
    clearError()

    try {
        if (!parent) {
            setError("No parent app attached. Recreate this group from the Hubitat Alexa Routine Trigger app.")
            return
        }

        // Ask the app to push the latest settings back down to this device.
        parent.multiTriggerSync(device.deviceNetworkId)

    } catch (MissingMethodException e) {
        setError("Parent app is out of date. Update the app code and try again.")
    } catch (ex) {
        setError(ex.toString())
    }
}

/*
 * Called by the parent app to push updated values into Current States.
 * Intentionally not exposed as a command.
 */
def syncFromApp(Map cfg) {
    if (cfg == null) return

    if (cfg.groupName != null) sendEvent(name: "groupName", value: cfg.groupName.toString(), isStateChange: true)
    if (cfg.triggerCount != null) sendEvent(name: "triggerCount", value: safeInt(cfg.triggerCount, 0), isStateChange: true)
    if (cfg.resetAfterSeconds != null) sendEvent(name: "resetAfterSeconds", value: safeInt(cfg.resetAfterSeconds, 5), isStateChange: true)
    if (cfg.timeBetweenTriggersMs != null) sendEvent(name: "timeBetweenTriggersMs", value: safeInt(cfg.timeBetweenTriggersMs, 350), isStateChange: true)
    if (cfg.minTimeBetweenGroupTriggersMs != null) sendEvent(name: "minTimeBetweenGroupTriggersMs", value: safeInt(cfg.minTimeBetweenGroupTriggersMs, 250), isStateChange: true)

    // Also store as data values so a device page still shows sane values if app state is lost.
    try {
        if (cfg.triggerCount != null) device.updateDataValue("heArtChildCount", "${safeInt(cfg.triggerCount, 0)}")
        if (cfg.timeBetweenTriggersMs != null) device.updateDataValue("heArtDelayMs", "${safeInt(cfg.timeBetweenTriggersMs, 350)}")
        if (cfg.resetAfterSeconds != null) device.updateDataValue("heArtChildAutoResetSeconds", "${safeInt(cfg.resetAfterSeconds, 5)}")
        if (cfg.minTimeBetweenGroupTriggersMs != null) device.updateDataValue("heArtSuppressMs", "${safeInt(cfg.minTimeBetweenGroupTriggersMs, 250)}")
    } catch (ignored) { }

    if (settings.debugLogging == true) log.debug "${device.displayName}: syncFromApp(${cfg})"
}

/* Internals */

private void syncAttributesFromDataValues() {
    try {
        def c = safeInt(device.getDataValue("heArtChildCount"), null)
        def d = safeInt(device.getDataValue("heArtDelayMs"), null)
        def a = safeInt(device.getDataValue("heArtChildAutoResetSeconds"), null)
        def s = safeInt(device.getDataValue("heArtSuppressMs"), null)

        if (c != null) sendEvent(name: "triggerCount", value: c, isStateChange: true)
        if (d != null) sendEvent(name: "timeBetweenTriggersMs", value: d, isStateChange: true)
        if (a != null) sendEvent(name: "resetAfterSeconds", value: a, isStateChange: true)
        if (s != null) sendEvent(name: "minTimeBetweenGroupTriggersMs", value: s, isStateChange: true)
    } catch (ignored) { }
}

private Boolean isSuppressed() {
    Integer window = safeInt(device.getDataValue("heArtSuppressMs"), 250)
    if (window < 0) window = 0
    if (window > 60000) window = 60000

    Long last = state.lastOnMs as Long
    Long nowMs = now()

    if (last && (nowMs - last) < window) {
        if (settings.debugLogging == true) log.debug "${device.displayName}: suppressed duplicate (${nowMs - last}ms < ${window}ms)"
        return true
    }

    state.lastOnMs = nowMs
    return false
}

private void clearError() {
    sendEvent(name: "lastError", value: "", isStateChange: true)
}

private void setError(String msg) {
    sendEvent(name: "lastError", value: (msg ?: ""), isStateChange: true)
    if (settings.infoLogging != false && msg) log.warn "${device.displayName}: ${msg}"
}

private Integer safeInt(def v, Integer dflt) {
    try {
        if (v == null) return dflt
        return Integer.parseInt(v.toString())
    } catch (ignored) {
        return dflt
    }
}

private String nowIso() {
    return new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
}