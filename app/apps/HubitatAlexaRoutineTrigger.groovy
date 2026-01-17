/*
 * Hubitat Alexa Routine Trigger
 *
 * Author: Chris Truitt
 * Website: https://christruitt.com
 * GitHub: https://github.com/truittchris
 * Namespace: truittchris
 */

definition(
    name: "Hubitat Alexa Routine Trigger",
    namespace: "truittchris",
    author: "Chris Truitt",
    description: "Creates Alexa-friendly virtual trigger devices (single triggers and trigger groups).",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

def appVersion() { return "0.8.2" }

preferences {
    page(name: "mainPage")
    page(name: "createStandalonePage")
    page(name: "createGroupPage")
    page(name: "managePage")
    page(name: "quickActionPage")
    page(name: "verifyPage")
    page(name: "helpPage")
    page(name: "toolsPage")
    page(name: "editStandalonePage")
    page(name: "editGroupPage")
}

def installed() {
    logInfo("Installed v${appVersion()}")
    initialize()
}

def updated() {
    logInfo("Updated v${appVersion()}")
    initialize()
}

def initialize() {
    state.groups = state.groups ?: [:]
}

/* UI */

def mainPage() {
    dynamicPage(name: "mainPage", title: "Hubitat Alexa Routine Trigger", install: true, uninstall: true) {

        renderStatusPanel()

        section("Get started") {
            paragraph("Use this app to create Alexa-friendly virtual contact sensors you can use as routine triggers.")
        }

        section("Create") {
            href(name: "goCreateStandalone", title: "Create a single trigger", page: "createStandalonePage", description: "Recommended for most users")
            href(name: "goCreateGroup", title: "Create a trigger group", page: "createGroupPage", description: "For triggering multiple Alexa routines in sequence")
        }

        section("Manage") {
            href(name: "goManage", title: "View and manage existing triggers", page: "managePage", description: "Edit settings, test, or delete")
        }

        section("Help") {
            href(name: "goHelp", title: "Setup checklist and troubleshooting", page: "helpPage", description: "Alexa setup steps and common fixes")
        }

        section("Tools") {
            href(name: "goTools", title: "Tools and maintenance", page: "toolsPage", description: "Validate, rebuild state, and logging controls")
        }

        section("App options") {
            input(name: "infoLogging", type: "bool", title: "Enable info logging", defaultValue: true, submitOnChange: true)
            input(name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false, submitOnChange: true)
            input(name: "btnDiagnostics30", type: "button", title: "Enable diagnostics for 30 minutes")
        }

        section("About") {
            paragraph("Author: Chris Truitt\nWebsite: christruitt.com\nGitHub: truittchris\nVersion: v${appVersion()}")
            href(name: "aboutLink", title: "Open christruitt.com", description: "Support and updates", required: false, url: "https://christruitt.com")
        }
    }
}

private void renderFlashIfPresent() {
    def f = state?._uiFlash
    if (!f) return

    try {
        def title = (f.title ?: "Update").toString()
        def msg = (f.msg ?: "").toString()
        section(title) {
            paragraph(msg)
        }
    } finally {
        state._uiFlash = null
    }
}

private void setFlash(String title, String msg) {
    state._uiFlash = [title: title, msg: msg]
}


// Persistent status banner (survives navigation until cleared)
private void setStatus(String title, String msg) {
    state._statusBanner = [title: title?.toString() ?: "Status", msg: msg?.toString() ?: "", when: now()]
}

private void clearStatus() {
    state._statusBanner = null
}

private void renderStatusPanel() {
    def b = state?._statusBanner
    if (!b) return

    String title = (b.title ?: "Status").toString()
    String msg = (b.msg ?: "").toString()

    section(title) {
        paragraph(msg)
    }
}

private void recordLastAction(String msg) {
    setStatus("Last action", msg)
}

private void recordLastError(String msg) {
    setStatus("Last error", msg)
}

def createStandalonePage() {
    // Post-create routing: after clicking Create, immediately land the user on Manage.
    if (state?._uiGoManage == true) {
        state._uiGoManage = false
        return managePage()
    }

    dynamicPage(name: "createStandalonePage", title: "Create a single trigger", install: false, uninstall: false) {

        section("What you get") {
            paragraph("One virtual contact sensor you can expose to Alexa and use as a routine trigger.")
        }

        section("Device") {
            input(name: "createSingleName", type: "text", title: "Device name", defaultValue: "Alexa Routine Trigger", submitOnChange: true)
        }

        section("Behavior") {
            input(name: "createSingleResetAfterSeconds", type: "number", title: "Turn off after (seconds)", defaultValue: 5, submitOnChange: true)
            paragraph("If your Alexa routine does not trigger reliably, increase the turn off delay to 5–10 seconds.")
            if (safeInt(settings?.createSingleResetAfterSeconds, 5) == 0) { paragraph("Turn off after is set to 0. This means the trigger may stay Open until you reset it manually.") }

            input(name: "createSingleMinBetweenMs", type: "number", title: "Ignore repeat triggers within (ms)", defaultValue: 250, submitOnChange: true)
            paragraph("Prevents rapid back-to-back triggers. Recommended: 250–1000 ms.")
        }

        section("Create") {
            input(name: "btnCreateStandalone", type: "button", title: "Create trigger")
            href(name: "backHome1", title: "Back to Home", page: "mainPage")
        }

        section("After you create it") {
            paragraph("After creation, you will be taken to Manage. From there, follow the Setup checklist to expose it to Alexa.")
        }
    }
}

// Group creation can feel intimidating. This page guides users through a safe default setup.
def createGroupPage() {
    if (state?._uiGoManage == true) {
        state._uiGoManage = false
        return managePage()
    }

    dynamicPage(name: "createGroupPage", title: "Create a trigger group", install: false, uninstall: false) {

        section("When to use a group") {
            paragraph("Use a group if you want multiple Alexa routines to trigger in sequence. This creates multiple trigger devices plus one group controller switch.")
        }

        section("Basics") {
            input(name: "createGroupName", type: "text", title: "Group name", defaultValue: "Alexa Trigger Group", submitOnChange: true)
            input(name: "createGroupCount", type: "number", title: "Number of trigger devices", defaultValue: 2, submitOnChange: true)
        }

        section("Recommended behavior") {
            input(name: "createGroupResetAfterSeconds", type: "number", title: "Turn off each trigger after (seconds)", defaultValue: 5, submitOnChange: true)
            input(name: "createGroupTimeBetweenMs", type: "number", title: "Delay between trigger devices (ms)", defaultValue: 350, submitOnChange: true)
            paragraph("Recommended: 300–800 ms. Set to 0 to fire all trigger devices at the same time.")
        }

        section("Advanced") {
            input(name: "showAdvancedGroup", type: "bool", title: "Show advanced options", defaultValue: false, submitOnChange: true)

            if (settings?.showAdvancedGroup == true) {
                input(name: "createGroupChildMinBetweenMs", type: "number", title: "Ignore repeat triggers on each trigger device within (ms)", defaultValue: 250, submitOnChange: true)
                input(name: "createGroupMinBetweenGroupTriggersMs", type: "number", title: "Ignore repeated group triggers within (ms)", defaultValue: 250, submitOnChange: true)
            }
        }

        section("Create") {
            input(name: "btnCreateGroup", type: "button", title: "Create group")
            href(name: "backHome2", title: "Back to Home", page: "mainPage")
        }

        section("After you create it") {
            paragraph("After creation, you will be taken to Manage. You will expose each trigger device to Alexa and use each one as a routine trigger.")
        }
    }
}



def quickActionPage(params) {
    // Lightweight action runner used by Manage page buttons (test/reset without entering edit pages).
    def kind = (params?.kind ?: "").toString()
    def action = (params?.action ?: "").toString()
    def dni = (params?.dni ?: "").toString()

    if (!kind || !action || !dni) {
        setFlash("Action", "Missing action details. Please try again.")
        return managePage()
    }

    if (kind == "standalone") {
        def dev = getChildDevice(dni)
        if (!dev) {
            setFlash("Action", "Trigger device not found. It may have been deleted.")
            return managePage()
        }

        if (action == "test") {
            try { dev.open() } catch (ignored) { }
            setFlash("Test sent", "Sent test trigger to: ${dev.displayName}\nIf Alexa does not respond, confirm the device is exposed and discovered in Alexa, and your routine uses Open.")
            return managePage()
        }

        if (action == "reset") {
            try { dev.close() } catch (ignored) { }
            setFlash("Reset", "Reset (closed) the trigger: ${dev.displayName}")
            recordLastAction("Reset single trigger: ${dev.displayName}")
            return managePage()
        }

        if (action == "clone") {
            def cloned = cloneStandalone(dni)
            if (cloned?.name) {
                setFlash("Cloned", "Created copy: ${cloned.name}")
                recordLastAction("Cloned single trigger: ${dev.displayName} –> ${cloned.name}")
            } else {
                setFlash("Clone failed", "Unable to clone the trigger. Check Logs.")
                recordLastError("Clone failed for single trigger: ${dev.displayName}")
            }
            return managePage()
        }

        setFlash("Action", "Unknown action: ${action}")
        return managePage()
    }

    if (kind == "group") {
        def controller = getChildDevice(dni)
        if (!controller) {
            setFlash("Action", "Group controller not found. It may have been deleted.")
            return managePage()
        }

        if (action == "test") {
            multiTriggerTriggerAll(dni)
            setFlash("Test sent", "Started group test: ${controller.displayName}\nThis will fire each trigger device in the group using your configured delay.")
            return managePage()
        }

        if (action == "reset") {
            multiTriggerResetAll(dni)
            setFlash("Reset", "Reset (closed) all triggers in group: ${controller.displayName}")
            recordLastAction("Reset group: ${controller.displayName}")
            return managePage()
        }

        if (action == "clone") {
            def cloned = cloneGroup(dni)
            if (cloned?.name) {
                setFlash("Cloned", "Created group copy: ${cloned.name}")
                recordLastAction("Cloned group: ${controller.displayName} –> ${cloned.name}")
            } else {
                setFlash("Clone failed", "Unable to clone the group. Check Logs.")
                recordLastError("Clone failed for group: ${controller.displayName}")
            }
            return managePage()
        }

        setFlash("Action", "Unknown action: ${action}")
        return managePage()
    }

    setFlash("Action", "Unknown kind: ${kind}")
    return managePage()
}



def verifyPage(params) {
    // Consolidated "Test and verify" view. This verifies Hubitat device activity (state changes and timestamps).
    // Alexa response still depends on exposure, discovery, and routine configuration.

    if (params?.kind) {
        state._verifyKind = params.kind.toString()
    }

    if (params?.dni) {
        state._verifyDni = params.dni.toString()
    }

    if (params?.controllerDni) {
        state._verifyDni = params.controllerDni.toString()
    }

    String kind = (state?._verifyKind ?: (params?.kind ?: "")).toString()
    String dni  = (state?._verifyDni  ?: (params?.dni ?: params?.controllerDni ?: "")).toString()

    dynamicPage(name: "verifyPage", title: "Test and verify", install: false, uninstall: false) {

        renderStatusPanel()
        renderFlashIfPresent()

        if (!kind || !dni) {
            section("") {
                paragraph("Nothing selected to verify. Return to Manage and choose a device or group.")
            }
            section("") {
                href(name: "backManage_verify0", title: "Back to Manage", page: "managePage")
            }
            return
        }

        if (kind == "standalone") {
            def dev = getChildDevice(dni)

            if (!dev) {
                section("") { paragraph("Trigger device not found. It may have been deleted.") }
                section("") { href(name: "backManage_verify1", title: "Back to Manage", page: "managePage") }
                return
            }

            section("Actions") {
                paragraph("Use Test to send an Open event now. Reset forces the device Closed.")
                input(name: "btnVerifyTestStandalone", type: "button", title: "Test now (open)")
                input(name: "btnVerifyResetStandalone", type: "button", title: "Reset now (close)")
            }

            section("Hubitat status") {
                paragraph(deviceStatusLine(dev))
            }

            section("What this verifies") {
                paragraph("If the status line updates after Test, Hubitat is generating events correctly. If Alexa still does not trigger, confirm the device is exposed to Alexa, discovered, and your routine uses Smart Home – device – Open.")
            }

            section("") {
                href(name: "backManage_verify2", title: "Back to Manage", page: "managePage")
            }

            return
        }

        if (kind == "group") {
            def controller = getChildDevice(dni)

            if (!controller) {
                section("") { paragraph("Group controller not found. It may have been deleted.") }
                section("") { href(name: "backManage_verify3", title: "Back to Manage", page: "managePage") }
                return
            }

            String groupId = groupIdFromControllerDni(dni)
            def cfg = getGroupCfg(groupId, dni)

            Integer count = safeInt(cfg.count, safeInt(controller.getDataValue("heArtChildCount"), 0))
            Integer delayMs = safeInt(cfg.delayMs, safeInt(controller.getDataValue("heArtDelayMs"), 350))

            section("Actions") {
                paragraph("Test runs the group sequence (opens each trigger). Reset forces all triggers Closed.")
                input(name: "btnVerifyTestGroup", type: "button", title: "Test group now")
                input(name: "btnVerifyResetGroup", type: "button", title: "Reset all now")
                paragraph("Configured delay between triggers: ${delayMs}ms")
            }

            section("Hubitat status") {
                paragraph(deviceStatusLine(controller, "Controller"))

                if (count < 1) {
                    paragraph("No trigger devices were found for this group.")
                } else {
                    (1..count).each { Integer i ->
                        def child = getChildDevice(childDni(groupId, i))
                        if (child) {
                            paragraph(deviceStatusLine(child, "Trigger ${i}"))
                        } else {
                            paragraph("Trigger ${i} – device not found")
                        }
                    }
                }
            }

            section("What this verifies") {
                paragraph("If the trigger devices show recent updates after Test, Hubitat is running the group sequence correctly. If Alexa routines do not run, confirm each trigger device (not the controller) is exposed to Alexa, discovered, and used as the routine trigger (Open).")
            }

            section("") {
                href(name: "backManage_verify4", title: "Back to Manage", page: "managePage")
            }

            return
        }

        section("") {
            paragraph("Unknown verify target. Return to Manage and try again.")
        }
        section("") {
            href(name: "backManage_verify5", title: "Back to Manage", page: "managePage")
        }
    }
}

private String deviceStatusLine(dev, String labelPrefix = null) {
    if (!dev) return ""

    String prefix = labelPrefix ? (labelPrefix + ": ") : ""

    def contact = null
    try {
        contact = dev.currentValue("contact")
    } catch (ignored) { }

    def sw = null
    try {
        sw = dev.currentValue("switch")
    } catch (ignored) { }

    Date lastActivity = null
    try {
        lastActivity = dev.getLastActivity()
    } catch (ignored) { }

    Date lastContactEvent = null
    try {
        def evts = dev.events(max: 10)
        def evt = evts?.find { it?.name == "contact" }
        if (evt?.date) lastContactEvent = evt.date
    } catch (ignored) { }

    Date lastUpdate = lastContactEvent ?: lastActivity

    String when = lastUpdate ? formatLocalDateTime(lastUpdate) : "Unknown"

    // Prefer showing contact if present; else show switch for controller.
    String stateStr = (contact != null) ? ("contact=" + contact) : ((sw != null) ? ("switch=" + sw) : "state=unknown")

    return "${prefix}${dev.displayName} – ${stateStr} – last update: ${when}"
}

private String formatLocalDateTime(Date d) {
    try {
        return d.format("yyyy-MM-dd h:mm:ss a", location.timeZone)
    } catch (ignored) {
        return d.toString()
    }
}
def managePage() {
    dynamicPage(name: "managePage", title: "Manage triggers", install: false, uninstall: false) {

        renderStatusPanel()
        renderFlashIfPresent()

        section("Create more") {
            href(name: "manageCreateStandalone", title: "Create a single trigger", page: "createStandalonePage")
            href(name: "manageCreateGroup", title: "Create a trigger group", page: "createGroupPage")
            href(name: "manageTools", title: "Tools and maintenance", page: "toolsPage")
        }

        def kids = (getChildDevices() ?: [])

        def standalones = kids.findAll { d ->
            (d?.deviceNetworkId ?: "").toString().startsWith("heArt:standalone:")
        }.sort { (it?.displayName ?: "") }

        def controllers = kids.findAll { d ->
            (d?.deviceNetworkId ?: "").toString().startsWith("heArt:group:controller:")
        }.sort { (it?.displayName ?: "") }

        // Single triggers
        if (!standalones) {
            section("Single triggers (${standalones?.size() ?: 0})") {
                paragraph("None yet.")
            }
        } else {
            section("Single triggers (${standalones?.size() ?: 0})") {
                paragraph("Quick actions are the fastest way to verify Hubitat-side behavior.")
            }

            standalones.each { d ->
                def key = uiKey(d.deviceNetworkId)

                Integer resetSec = safeInt(d?.currentValue('resetAfterSeconds'), 5)
                Integer minMs = safeInt(d?.currentValue('minTimeBetweenTriggersMs'), 250)

                section("${d.displayName}") {
                    paragraph("Turn off after: ${resetSec}s – Ignore repeats: ${minMs}ms")

                    href(
                        name: "editStandalone_${key}",
                        title: "Edit settings",
                        page: "editStandalonePage",
                        params: [dni: d.deviceNetworkId]
                    )

                    href(
                        name: "verifyStandalone_${key}",
                        title: "Test and verify",
                        description: "Run a test and confirm Hubitat events",
                        page: "verifyPage",
                        params: [kind: "standalone", dni: d.deviceNetworkId]
                    )

                    href(
                        name: "quickTestStandalone_${key}",
                        title: "Test (open)",
                        description: "Sends an open event now",
                        page: "quickActionPage",
                        params: [kind: "standalone", action: "test", dni: d.deviceNetworkId]
                    )

                    href(
                        name: "quickResetStandalone_${key}",
                        title: "Reset (close)",
                        description: "Forces the device closed",
                        page: "quickActionPage",
                        params: [kind: "standalone", action: "reset", dni: d.deviceNetworkId]
                    )

                    href(
                        name: "cloneStandalone_${key}",
                        title: "Clone this trigger",
                        description: "Creates a copy with the same settings",
                        page: "quickActionPage",
                        params: [kind: "standalone", action: "clone", dni: d.deviceNetworkId]
                    )
                }
            }
        }

        // Trigger groups
        if (!controllers) {
            section("Trigger groups (${controllers?.size() ?: 0})") {
                paragraph("None yet.")
            }
        } else {
            section("Trigger groups (${controllers?.size() ?: 0})") {
                paragraph("Groups create multiple trigger devices plus a controller. Use Test to run the sequence.")
            }

            controllers.each { c ->
                def key = uiKey(c.deviceNetworkId)

                def groupId = groupIdFromControllerDni(c.deviceNetworkId)
                def cfg = getGroupCfg(groupId, c.deviceNetworkId)

                Integer count = safeInt(cfg.count, safeInt(c.getDataValue("heArtChildCount"), 0))
                Integer delayMs = safeInt(cfg.delayMs, safeInt(c.getDataValue("heArtDelayMs"), 350))
                Integer resetSec = safeInt(cfg.childAutoResetSeconds, safeInt(c.getDataValue("heArtChildAutoResetSeconds"), 5))

                section("${c.displayName}") {
                    paragraph("${count} trigger devices – Turn off after: ${resetSec}s – Delay: ${delayMs}ms")

                    href(
                        name: "editGroup_${key}",
                        title: "Edit group settings",
                        page: "editGroupPage",
                        params: [controllerDni: c.deviceNetworkId]
                    )

                    href(
                        name: "verifyGroup_${key}",
                        title: "Test and verify",
                        description: "Run a test and confirm Hubitat events",
                        page: "verifyPage",
                        params: [kind: "group", controllerDni: c.deviceNetworkId]
                    )

                    href(
                        name: "quickTestGroup_${key}",
                        title: "Test group now",
                        description: "Runs the group sequence (opens each trigger)",
                        page: "quickActionPage",
                        params: [kind: "group", action: "test", dni: c.deviceNetworkId]
                    )

                    href(
                        name: "quickResetGroup_${key}",
                        title: "Reset all now",
                        description: "Closes all triggers in the group",
                        page: "quickActionPage",
                        params: [kind: "group", action: "reset", dni: c.deviceNetworkId]
                    )

                    href(
                        name: "cloneGroup_${key}",
                        title: "Clone this group",
                        description: "Creates a copy with the same settings",
                        page: "quickActionPage",
                        params: [kind: "group", action: "clone", dni: c.deviceNetworkId]
                    )
                }
            }
        }

        section("Advanced") {
            input(name: "showDeveloperInfo", type: "bool", title: "Show developer info", defaultValue: false, submitOnChange: true)
            if (settings?.showDeveloperInfo == true) {
                paragraph("""App version: v${appVersion()}
Devices: ${(kids?.size() ?: 0)}
Groups in state: ${(state?.groups?.size() ?: 0)}""")
                paragraph("State.groups snapshot: ${state?.groups}")
            }
        }

        section("Danger zone") {
            if (state?._confirmDeleteAll != true) {
                paragraph("Deletes all devices created by this app. This cannot be undone.")
                input(name: "btnArmDeleteAll", type: "button", title: "Delete all devices created by this app")
            } else {
                paragraph("Confirm delete: this will permanently remove every trigger and group created by this app.")
                input(name: "btnConfirmDeleteAll", type: "button", title: "Yes, delete everything")
                input(name: "btnCancelDeleteAll", type: "button", title: "Cancel")
            }
        }

        section("") {
            href(name: "backHome3", title: "Back to Home", page: "mainPage")
        }
    }
}

def helpPage() {
    dynamicPage(name: "helpPage", title: "Help", install: false, uninstall: false) {

        section("Quick start") {
	            paragraph("""1) Create a trigger (single) or a group (multiple triggers).
2) In Hubitat, open Amazon Echo Skill and select the trigger device(s) to expose.
3) In the Alexa app, run Discover devices.
4) Create a routine using Smart Home as the trigger.
5) Use Manage - Test and verify to confirm Hubitat events.""")
        }

        section("Alexa routine trigger mapping") {
	            paragraph("""Recommended: use the trigger device as a Contact Sensor trigger.
- Use Open as the routine trigger to fire when this app sends a test/open event.
- If you use a group, expose each trigger device (not the controller) to Alexa and create one routine per trigger device.""")
        }

        section("Diagnose: Hubitat OK, Alexa not reacting") {
	            paragraph("""A) In this app, open Manage - Test and verify.
B) Press Test.
C) If the timestamp updates in Hubitat, the Hubitat side is working.
D) If Alexa does not react, the cause is usually one of these:
- The device is not exposed in Amazon Echo Skill
- Alexa has not discovered the device
- The routine is using the wrong device or the wrong trigger (must be Open for contact)
- The routine is disabled or has conditions that are not met""")
	            paragraph("""Common fixes:
- Re-run Discover devices in Alexa
- Toggle the device exposure in Amazon Echo Skill (off then on)
- Delete the device in Alexa (if present) and rediscover
- Rebuild the routine trigger by selecting the device again""")
        }

        section("Recommended settings") {
	            paragraph("""For most users:
- Turn off after: 5-10 seconds
- Ignore repeat triggers within: 250-1000 ms
For groups:
- Delay between trigger devices: 300-800 ms (0 = all at once)""")
        }

        section("Tools") {
            href(name: "helpTools", title: "Open Tools and maintenance", page: "toolsPage", description: "Validate devices, rebuild state, and logging")
        }

        section("Support") {
            paragraph("Project updates and support links are available on christruitt.com.")
            href(name: "helpSite", title: "Open christruitt.com", required: false, url: "https://christruitt.com")
        }

        section("") {
            href(name: "backHome4", title: "Back to Home", page: "mainPage")
        }
    }
}

def toolsPage() {
    dynamicPage(name: "toolsPage", title: "Tools and maintenance", install: false, uninstall: false) {

        renderStatusPanel()
        renderFlashIfPresent()

        section("Validation") {
            paragraph("Use validation if something feels out of sync (missing devices, orphaned groups, etc.).")
            input(name: "btnValidate", type: "button", title: "Validate devices")
        }

        section("Repair") {
            paragraph("Rebuild restores state.groups from the controller devices. Use this if you migrated hubs or restored from backup and groups do not show correctly.")
            input(name: "btnRebuildState", type: "button", title: "Rebuild app state from controllers")
        }

        section("Logging") {
            paragraph("For troubleshooting, enable diagnostics for 30 minutes, then disable debug when finished.")
            input(name: "btnDiagnostics30", type: "button", title: "Enable diagnostics for 30 minutes")
            input(name: "btnDisableAllDebug", type: "button", title: "Disable debug logging everywhere")
        }

        section("Status") {
            input(name: "btnClearStatus", type: "button", title: "Clear status banner")
        }

        section("") {
            href(name: "backHome_tools", title: "Back to Home", page: "mainPage")
            href(name: "backManage_tools", title: "Back to Manage", page: "managePage")
        }
    }
}

def editStandalonePage(params) {
    if (state?._uiGoManage == true) {
        state._uiGoManage = false
        return managePage()
    }

    def dni = (params?.dni ?: "").toString()
    state._editStandaloneDni = dni

    def dev = dni ? getChildDevice(dni) : null

    dynamicPage(name: "editStandalonePage", title: "Edit single trigger", install: false, uninstall: false) {

        if (!dev) {
            section("") { paragraph("Device not found.") }
            return
        }

        section("Device") {
            paragraph("${dev.displayName}\nType: ${dev.typeName}")
        }

        section("Settings") {
            input(name: "es_deviceName", type: "text", title: "Device name", defaultValue: dev.displayName, submitOnChange: true)
            input(name: "es_resetAfterSeconds", type: "number", title: "Turn off after (seconds)", defaultValue: safeInt(dev?.currentValue('resetAfterSeconds'), 5), submitOnChange: true)
            input(name: "es_minBetweenMs", type: "number", title: "Ignore repeat triggers within (ms)", defaultValue: safeInt(dev?.currentValue('minTimeBetweenTriggersMs'), 250), submitOnChange: true)
        }

        section("Test") {
            input(name: "btnTestStandalone", type: "button", title: "Test now")
            input(name: "btnResetStandalone", type: "button", title: "Reset now")
        }

        section("Save") {
            input(name: "btnSaveStandalone", type: "button", title: "Save settings")
            input(name: "btnCloneStandalone", type: "button", title: "Clone this trigger")
        }

        section("Delete") {
            if (state?._confirmDeleteStandalone != true) {
                input(name: "btnArmDeleteStandalone", type: "button", title: "Delete this trigger")
            } else {
                paragraph("Confirm delete for: ${dev.displayName}")
                input(name: "btnConfirmDeleteStandalone", type: "button", title: "Yes, delete")
                input(name: "btnCancelDeleteStandalone", type: "button", title: "Cancel")
            }
        }

        section("") {
            href(name: "backManage1", title: "Back to Manage", page: "managePage")
        }
    }
}

def editGroupPage(params) {
    if (state?._uiGoManage == true) {
        state._uiGoManage = false
        return managePage()
    }

    def controllerDni = (params?.controllerDni ?: "").toString()
    state._editGroupControllerDni = controllerDni

    def controller = controllerDni ? getChildDevice(controllerDni) : null
    def groupId = controllerDni ? groupIdFromControllerDni(controllerDni) : null
    def cfg = getGroupCfg(groupId, controllerDni)

    Integer count = safeInt(cfg.count, safeInt(controller?.getDataValue("heArtChildCount"), 0))
    Integer delayMs = safeInt(cfg.delayMs, safeInt(controller?.getDataValue("heArtDelayMs"), 350))
    Integer resetSec = safeInt(cfg.childAutoResetSeconds, safeInt(controller?.getDataValue("heArtChildAutoResetSeconds"), 5))
    Integer childMinMs = safeInt(cfg.childDebounceMs, 250)
    Integer groupMinMs = safeInt(controller?.getDataValue("heArtSuppressMs"), 250)

    dynamicPage(name: "editGroupPage", title: "Edit trigger group", install: false, uninstall: false) {

        if (!controller) {
            section("") { paragraph("Group controller not found.") }
            return
        }

        section("Group") {
            paragraph("${controller.displayName}\nTrigger devices: ${count}")
        }

        section("Settings") {
            input(name: "eg_groupName", type: "text", title: "Group name", defaultValue: controller.displayName, submitOnChange: true)

            input(name: "eg_resetAfterSeconds", type: "number", title: "Turn off each trigger after (seconds)", defaultValue: resetSec, submitOnChange: true)
            input(name: "eg_childMinBetweenMs", type: "number", title: "Ignore repeat triggers on each trigger device within (ms)", defaultValue: childMinMs, submitOnChange: true)

            input(name: "eg_timeBetweenMs", type: "number", title: "Delay between trigger devices (ms)", defaultValue: delayMs, submitOnChange: true)
            if (safeInt(settings?.eg_timeBetweenMs, delayMs) != null && safeInt(settings?.eg_timeBetweenMs, delayMs) < 150 && safeInt(settings?.eg_timeBetweenMs, delayMs) > 0) {
                paragraph("Very low delays can cause Alexa to miss triggers. Recommended: 300–800 ms.")
            } else {
                paragraph("Set to 0 to fire all trigger devices at the same time.")
            }

            input(name: "eg_groupMinBetweenMs", type: "number", title: "Ignore repeated group triggers within (ms)", defaultValue: groupMinMs, submitOnChange: true)
        }

        section("Test") {
            input(name: "btnTestGroup", type: "button", title: "Test group now")
            input(name: "btnResetGroup", type: "button", title: "Reset all now")
        }

        section("Save") {
            input(name: "btnSaveGroup", type: "button", title: "Save group settings")
            input(name: "btnCloneGroup", type: "button", title: "Clone this group")
        }

        section("Delete") {
            if (state?._confirmDeleteGroup != true) {
                input(name: "btnArmDeleteGroup", type: "button", title: "Delete this group")
            } else {
                paragraph("Confirm delete for: ${controller.displayName}\nThis will delete the group controller and all trigger devices in the group.")
                input(name: "btnConfirmDeleteGroup", type: "button", title: "Yes, delete")
                input(name: "btnCancelDeleteGroup", type: "button", title: "Cancel")
            }
        }

        section("") {
            href(name: "backManage2", title: "Back to Manage", page: "managePage")
        }
    }
}
/* Button handlers */

def appButtonHandler(String btn) {
    logDebug("appButtonHandler(${btn})")

    switch (btn) {
        // Create
        case "btnCreateStandalone":
            def created = createStandalone()
            if (created?.name) {
                setFlash("Created", "Created trigger: ${created.name}\nNext: expose it to Alexa, run device discovery, then create a routine using Open.")
            } else {
                setFlash("Create failed", "Unable to create the trigger. Check Logs for details.")
            }
            state._uiGoManage = true
            break

        case "btnCreateGroup":
            def createdG = createGroup()
            if (createdG?.name) {
                setFlash("Created", "Created group: ${createdG.name}\nCreated ${createdG.count} trigger devices plus the group controller. Next: expose the trigger devices to Alexa and run discovery.")
            } else {
                setFlash("Create failed", "Unable to create the group. Check Logs for details.")
            }
            state._uiGoManage = true
            break

        // Diagnostics
        case "btnDiagnostics30":
            enableDiagnosticsFor30Minutes()
            setFlash("Diagnostics", "Enabled debug logging for 30 minutes.")
            recordLastAction("Enabled diagnostics for 30 minutes")
            break

        // Tools
        case "btnDisableAllDebug":
            disableAllDebugLogging()
            setFlash("Tools", "Disabled debug logging on the app and all child devices.")
            recordLastAction("Disabled all debug logging")
            break
        case "btnValidate":
            def msg = validateInstallation()
            setFlash("Validation", msg)
            recordLastAction("Ran validation")
            break
        case "btnRebuildState":
            def msg2 = rebuildStateFromControllers()
            setFlash("Rebuild", msg2)
            recordLastAction("Rebuilt app state from controllers")
            break
        case "btnClearStatus":
            clearStatus()
            setFlash("Status", "Cleared status banner.")
            break

        // Manage: delete all
        case "btnArmDeleteAll":
            state._confirmDeleteAll = true
            break
        case "btnCancelDeleteAll":
            state._confirmDeleteAll = false
            break
        case "btnConfirmDeleteAll":
            state._confirmDeleteAll = false
            deleteAllDevices()
            setFlash("Deleted", "Deleted all devices created by this app.")
            break

        // Single trigger deletion
        case "btnArmDeleteStandalone":
            state._confirmDeleteStandalone = true
            break
        case "btnCancelDeleteStandalone":
            state._confirmDeleteStandalone = false
            break
        case "btnConfirmDeleteStandalone":
            state._confirmDeleteStandalone = false
            deleteStandalone()
            setFlash("Deleted", "Trigger deleted.")
            state._uiGoManage = true
            break

        // Group deletion
        case "btnArmDeleteGroup":
            state._confirmDeleteGroup = true
            break
        case "btnCancelDeleteGroup":
            state._confirmDeleteGroup = false
            break
        case "btnConfirmDeleteGroup":
            state._confirmDeleteGroup = false
            deleteGroup()
            setFlash("Deleted", "Group deleted.")
            state._uiGoManage = true
            break

        // Edit single
        case "btnCloneStandalone":
            def dni = (state._editStandaloneDni ?: "").toString()
            def cloned = cloneStandalone(dni)
            if (cloned?.name) {
                setFlash("Cloned", "Created copy: ${cloned.name}")
                recordLastAction("Cloned single trigger from edit screen")
                state._uiGoManage = true
            } else {
                setFlash("Clone failed", "Unable to clone the trigger. Check Logs.")
                recordLastError("Clone failed from edit screen")
            }
            break

        case "btnSaveStandalone":
            saveStandalone()
            setFlash("Saved", "Saved settings for the trigger.")
            break
        case "btnTestStandalone":
            testStandalone()
            break
        case "btnResetStandalone":
            resetStandalone()
            break

        // Edit group
        case "btnCloneGroup":
            def controllerDni = (state._editGroupControllerDni ?: "").toString()
            def cloned = cloneGroup(controllerDni)
            if (cloned?.name) {
                setFlash("Cloned", "Created group copy: ${cloned.name}")
                recordLastAction("Cloned group from edit screen")
                state._uiGoManage = true
            } else {
                setFlash("Clone failed", "Unable to clone the group. Check Logs.")
                recordLastError("Clone group failed from edit screen")
            }
            break

        case "btnSaveGroup":
            saveGroup()
            setFlash("Saved", "Saved settings for the group.")
            break
        case "btnTestGroup":
            testGroup()
            break
        case "btnResetGroup":
            resetGroup()
            break

        // Verify page actions
        case "btnVerifyTestStandalone":
            verifyTestStandalone()
            break
        case "btnVerifyResetStandalone":
            verifyResetStandalone()
            break
        case "btnVerifyTestGroup":
            verifyTestGroup()
            break
        case "btnVerifyResetGroup":
            verifyResetGroup()
            break

        default:
            logWarn("Unknown button: ${btn}")
    }
}

/* Create devices */


private Map createStandalone() {
    def name = (settings?.createSingleName ?: "Alexa Routine Trigger").toString()
    def dni = "heArt:standalone:${now()}"

    def dev = addChildDevice(
        "truittchris",
        "Hubitat Alexa Routine Trigger - Virtual Contact Trigger",
        dni,
        [label: name, name: name, isComponent: false]
    )

    if (!dev) {
        logWarn("Failed to create single trigger device")
        return null
    }

    Integer resetSec = clampInt(settings?.createSingleResetAfterSeconds, 5, 0, 3600)
    Integer minMs = clampInt(settings?.createSingleMinBetweenMs, 250, 0, 60000)

    dev.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
    dev.updateSetting("debounceMs",       [type: "number", value: minMs])
    dev.updateSetting("infoLogging",      [type: "bool",   value: true])
    dev.updateSetting("debugLogging",     [type: "bool",   value: false])

    // Keep device Current States aligned with app-level values.
    safeCall(dev, "syncFromApp", [[:]])

    logInfo("Created single trigger device: ${dev.displayName}")
    return [name: dev.displayName, dni: dni]
}

private Map createGroup() {
    def baseName = (settings?.createGroupName ?: "Alexa Trigger Group").toString()
    def requestedCount = clampInt(settings?.createGroupCount, 2, 1, 25)

    def groupId = java.util.UUID.randomUUID().toString()

    // Controller
    def controllerDni = "heArt:group:controller:${groupId}"
    def controller = addChildDevice(
        "truittchris",
        "Hubitat Alexa Routine Trigger - MultiTrigger Controller",
        controllerDni,
        [label: baseName, name: baseName, isComponent: false]
    )

    if (!controller) {
        logWarn("Failed to create group controller for ${baseName}")
        return null
    }

    Integer delayMs = clampInt(settings?.createGroupTimeBetweenMs, 350, 0, 5000)
    Integer resetSec = clampInt(settings?.createGroupResetAfterSeconds, 5, 0, 3600)
    Integer childMinMs = clampInt(settings?.createGroupChildMinBetweenMs, 250, 0, 60000)
    Integer groupMinMs = clampInt(settings?.createGroupMinBetweenGroupTriggersMs, 250, 0, 60000)

    // Store metadata on controller for resilience
    try {
        controller.updateDataValue("heArtGroupId", "${groupId}")
        controller.updateDataValue("heArtGroupName", "${baseName}")
        controller.updateDataValue("heArtChildCount", "${requestedCount}")
        controller.updateDataValue("heArtDelayMs", "${delayMs}")
        controller.updateDataValue("heArtChildAutoResetSeconds", "${resetSec}")
        controller.updateDataValue("heArtSuppressMs", "${groupMinMs}")
    } catch (ignored) { }

    // Child devices
    (1..requestedCount).each { Integer i ->
        def childName = "${baseName} – ${i}"
        def dni = childDni(groupId, i)

        def child = addChildDevice(
            "truittchris",
            "Hubitat Alexa Routine Trigger - Child Contact",
            dni,
            [label: childName, name: childName, isComponent: false]
        )

        if (child) {
            child.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
            child.updateSetting("debounceMs",       [type: "number", value: childMinMs])
            child.updateSetting("infoLogging",      [type: "bool",   value: true])
            child.updateSetting("debugLogging",     [type: "bool",   value: false])

            try {
                child.updateDataValue("heArtGroupId", "${groupId}")
                child.updateDataValue("heArtGroupName", "${baseName}")
                child.updateDataValue("heArtChildNum", "${i}")
            } catch (ignored) { }

            safeCall(child, "syncFromApp", [[groupName: baseName, triggerNumber: i]])
        }
    }

    state.groups = state.groups ?: [:]
    state.groups[groupId] = [
        name: baseName,
        count: requestedCount,
        delayMs: delayMs,
        childAutoResetSeconds: resetSec,
        childDebounceMs: childMinMs,
        groupSuppressMs: groupMinMs
    ]

    // Push friendly display values into controller Current States.
    safeCall(controller, "syncFromApp", [[
        groupName: baseName,
        triggerCount: requestedCount,
        resetAfterSeconds: resetSec,
        timeBetweenTriggersMs: delayMs,
        minTimeBetweenGroupTriggersMs: groupMinMs
    ]])

    logInfo("Created group: ${baseName} (${requestedCount} triggers | reset ${resetSec}s | between ${delayMs}ms)")
    return [name: baseName, count: requestedCount, controllerDni: controllerDni, groupId: groupId]
}

/* Clone helpers */

private Map cloneStandalone(String sourceDni) {
    def src = sourceDni ? getChildDevice(sourceDni) : null
    if (!src) return null

    String baseName = (src.displayName ?: "Alexa Routine Trigger").toString()
    String newName = "${baseName} Copy"

    Integer resetSec = clampInt(src?.currentValue('resetAfterSeconds'), 5, 0, 3600)
    Integer minMs = clampInt(src?.currentValue('minTimeBetweenTriggersMs'), 250, 0, 60000)

    return createStandaloneFromCfg(newName, resetSec, minMs)
}

private Map createStandaloneFromCfg(String name, Integer resetSec, Integer minMs) {
    def dni = "heArt:standalone:${now()}"

    def dev = addChildDevice(
        "truittchris",
        "Hubitat Alexa Routine Trigger - Virtual Contact Trigger",
        dni,
        [label: name, name: name, isComponent: false]
    )

    if (!dev) return null

    dev.updateSetting("autoResetSeconds", [type: "number", value: clampInt(resetSec, 5, 0, 3600)])
    dev.updateSetting("debounceMs",       [type: "number", value: clampInt(minMs, 250, 0, 60000)])
    dev.updateSetting("infoLogging",      [type: "bool",   value: true])
    dev.updateSetting("debugLogging",     [type: "bool",   value: false])

    safeCall(dev, "syncFromApp", [[:]])

    logInfo("Cloned/created single trigger device: ${dev.displayName}")
    return [name: dev.displayName, dni: dni]
}

private Map cloneGroup(String controllerDni) {
    def controller = controllerDni ? getChildDevice(controllerDni) : null
    if (!controller) return null

    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)

    Integer count = safeInt(cfg.count, safeInt(controller.getDataValue("heArtChildCount"), 0))
    Integer delayMs = safeInt(cfg.delayMs, safeInt(controller.getDataValue("heArtDelayMs"), 350))
    Integer resetSec = safeInt(cfg.childAutoResetSeconds, safeInt(controller.getDataValue("heArtChildAutoResetSeconds"), 5))
    Integer groupMinMs = safeInt(cfg.groupSuppressMs, safeInt(controller.getDataValue("heArtSuppressMs"), 250))
    Integer childMinMs = safeInt(cfg.childDebounceMs, 250)

    String baseName = (cfg.name ?: controller.displayName ?: "Alexa Trigger Group").toString()
    String newName = "${baseName} Copy"

    return createGroupFromCfg(newName, count, delayMs, resetSec, childMinMs, groupMinMs)
}

private Map createGroupFromCfg(String baseName, Integer requestedCount, Integer delayMs, Integer resetSec, Integer childMinMs, Integer groupMinMs) {
    Integer c = clampInt(requestedCount, 2, 1, 25)
    String groupId = java.util.UUID.randomUUID().toString()

    def controllerDni = "heArt:group:controller:${groupId}"
    def controller = addChildDevice(
        "truittchris",
        "Hubitat Alexa Routine Trigger - MultiTrigger Controller",
        controllerDni,
        [label: baseName, name: baseName, isComponent: false]
    )
    if (!controller) return null

    Integer d = clampInt(delayMs, 350, 0, 5000)
    Integer r = clampInt(resetSec, 5, 0, 3600)
    Integer childDeb = clampInt(childMinMs, 250, 0, 60000)
    Integer sup = clampInt(groupMinMs, 250, 0, 60000)

    try {
        controller.updateDataValue("heArtGroupId", "${groupId}")
        controller.updateDataValue("heArtGroupName", "${baseName}")
        controller.updateDataValue("heArtChildCount", "${c}")
        controller.updateDataValue("heArtDelayMs", "${d}")
        controller.updateDataValue("heArtChildAutoResetSeconds", "${r}")
        controller.updateDataValue("heArtSuppressMs", "${sup}")
    } catch (ignored) { }

    (1..c).each { Integer i ->
        def childName = "${baseName} – ${i}"
        def dni = childDni(groupId, i)
        def child = addChildDevice(
            "truittchris",
            "Hubitat Alexa Routine Trigger - Child Contact",
            dni,
            [label: childName, name: childName, isComponent: false]
        )
        if (child) {
            child.updateSetting("autoResetSeconds", [type: "number", value: r])
            child.updateSetting("debounceMs",       [type: "number", value: childDeb])
            child.updateSetting("infoLogging",      [type: "bool",   value: true])
            child.updateSetting("debugLogging",     [type: "bool",   value: false])
            try {
                child.updateDataValue("heArtGroupId", "${groupId}")
                child.updateDataValue("heArtGroupName", "${baseName}")
                child.updateDataValue("heArtChildNum", "${i}")
            } catch (ignored) { }
            safeCall(child, "syncFromApp", [[groupName: baseName, triggerNumber: i]])
        }
    }

    state.groups = state.groups ?: [:]
    state.groups[groupId] = [
        name: baseName,
        count: c,
        delayMs: d,
        childAutoResetSeconds: r,
        childDebounceMs: childDeb,
        groupSuppressMs: sup
    ]

    safeCall(controller, "syncFromApp", [[
        groupName: baseName,
        triggerCount: c,
        resetAfterSeconds: r,
        timeBetweenTriggersMs: d,
        minTimeBetweenGroupTriggersMs: sup
    ]])

    logInfo("Cloned/created group: ${baseName} (${c} triggers)")
    return [name: baseName, count: c, controllerDni: controllerDni, groupId: groupId]
}

/* Save + test + delete: single */

private void saveStandalone() {
    def dni = (state._editStandaloneDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) return

    String newName = (settings?.es_deviceName ?: dev.displayName).toString()

    Integer resetSec = clampInt(settings?.es_resetAfterSeconds, 5, 0, 3600)
    Integer minMs = clampInt(settings?.es_minBetweenMs, 250, 0, 60000)

    dev.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
    dev.updateSetting("debounceMs",       [type: "number", value: minMs])

    if (newName && dev.label != newName) {
        try { dev.setLabel(newName) } catch (ignored) { }
    }

    safeCall(dev, "syncFromApp", [[:]])
    logInfo("Saved settings for ${dev.displayName}")
    recordLastAction("Saved single trigger settings: ${dev.displayName}")
}

private void testStandalone() {
    def dni = (state._editStandaloneDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) return
    dev.open()
}

private void resetStandalone() {
    def dni = (state._editStandaloneDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) return
    dev.close()
}

private void deleteStandalone() {
    def dni = (state._editStandaloneDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) return
    deleteChildDevice(dni)
    logInfo("Deleted trigger device: ${dev.displayName}")
}

/* Save + test + delete: group */

private void saveGroup() {
    def controllerDni = (state._editGroupControllerDni ?: "").toString()
    def controller = controllerDni ? getChildDevice(controllerDni) : null
    if (!controller) return

    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, safeInt(controller.getDataValue("heArtChildCount"), 0))

    String newName = (settings?.eg_groupName ?: controller.displayName).toString()

    Integer resetSec = clampInt(settings?.eg_resetAfterSeconds, safeInt(controller.getDataValue("heArtChildAutoResetSeconds"), 5), 0, 3600)
    Integer childMinMs = clampInt(settings?.eg_childMinBetweenMs, 250, 0, 60000)
    Integer delayMs = clampInt(settings?.eg_timeBetweenMs, safeInt(controller.getDataValue("heArtDelayMs"), 350), 0, 5000)
    Integer groupMinMs = clampInt(settings?.eg_groupMinBetweenMs, safeInt(controller.getDataValue("heArtSuppressMs"), 250), 0, 60000)

    // Persist to controller data values
    try {
        controller.updateDataValue("heArtGroupName", "${newName}")
        controller.updateDataValue("heArtChildCount", "${count}")
        controller.updateDataValue("heArtDelayMs", "${delayMs}")
        controller.updateDataValue("heArtChildAutoResetSeconds", "${resetSec}")
        controller.updateDataValue("heArtSuppressMs", "${groupMinMs}")
    } catch (ignored) { }

    // Update app state
    state.groups = state.groups ?: [:]
    state.groups[groupId] = [
        name: newName,
        count: count,
        delayMs: delayMs,
        childAutoResetSeconds: resetSec,
        childDebounceMs: childMinMs,
        groupSuppressMs: groupMinMs
    ]

    // Rename devices if group name changed
    if (newName && controller.label != newName) {
        try { controller.setLabel(newName) } catch (ignored) { }
    }

    // Apply to children
    Integer updated = 0
    (1..count).each { Integer i ->
        def child = getChildDevice(childDni(groupId, i))
        if (child) {
            try {
                child.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
                child.updateSetting("debounceMs",       [type: "number", value: childMinMs])
                child.updateDataValue("heArtGroupName", "${newName}")
                child.updateDataValue("heArtChildNum", "${i}")
                child.setLabel("${newName} – ${i}")
                safeCall(child, "syncFromApp", [[groupName: newName, triggerNumber: i]])
                updated++
            } catch (ignored) { }
        }
    }

    safeCall(controller, "syncFromApp", [[
        groupName: newName,
        triggerCount: count,
        resetAfterSeconds: resetSec,
        timeBetweenTriggersMs: delayMs,
        minTimeBetweenGroupTriggersMs: groupMinMs
    ]])

    logInfo("Saved group settings for ${newName} (${updated}/${count} trigger devices updated)")
}

private void testGroup() {
    def controllerDni = (state._editGroupControllerDni ?: "").toString()
    if (!controllerDni) return
    multiTriggerTriggerAll(controllerDni)
}

private void resetGroup() {
    def controllerDni = (state._editGroupControllerDni ?: "").toString()
    if (!controllerDni) return
    multiTriggerResetAll(controllerDni)
}

// Verify page actions (consolidated Test and verify view)

private void verifyTestStandalone() {
    String dni = (state?._verifyDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) {
        setFlash("Test", "Trigger device not found.")
        return
    }

    try { dev.open() } catch (ignored) { }
    setFlash("Test sent", "Sent Open event to: ${dev.displayName}")
}

private void verifyResetStandalone() {
    String dni = (state?._verifyDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) {
        setFlash("Reset", "Trigger device not found.")
        return
    }

    try { dev.close() } catch (ignored) { }
    setFlash("Reset", "Forced Closed for: ${dev.displayName}")
}

private void verifyTestGroup() {
    String controllerDni = (state?._verifyDni ?: "").toString()
    def controller = controllerDni ? getChildDevice(controllerDni) : null
    if (!controller) {
        setFlash("Test", "Group controller not found.")
        return
    }

    multiTriggerTriggerAll(controllerDni)
    setFlash("Test sent", "Started group test for: ${controller.displayName}")
}

private void verifyResetGroup() {
    String controllerDni = (state?._verifyDni ?: "").toString()
    def controller = controllerDni ? getChildDevice(controllerDni) : null
    if (!controller) {
        setFlash("Reset", "Group controller not found.")
        return
    }

    multiTriggerResetAll(controllerDni)
    setFlash("Reset", "Forced Closed for all triggers in: ${controller.displayName}")
}

private void deleteGroup() {
    def controllerDni = (state._editGroupControllerDni ?: "").toString()
    def controller = controllerDni ? getChildDevice(controllerDni) : null
    if (!controller) return

    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, safeInt(controller.getDataValue("heArtChildCount"), 0))

    // Delete children first
    (1..count).each { Integer i ->
        def dni = childDni(groupId, i)
        try { deleteChildDevice(dni) } catch (ignored) { }
    }

    // Delete controller
    try { deleteChildDevice(controllerDni) } catch (ignored) { }

    // Remove state record
    try { state.groups?.remove(groupId) } catch (ignored) { }

    logInfo("Deleted group ${controller.displayName}")
}

/* Parent methods called by controller driver */

void multiTriggerTriggerAll(String controllerDni) {
    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)

    Integer count = safeInt(cfg.count, 0)
    Integer delayMs = safeInt(cfg.delayMs, 350)

    if (!groupId || count < 1) {
        logWarn("multiTriggerTriggerAll: unknown group for controller dni=${controllerDni}")
        return null
    }

    (1..count).each { Integer i ->
        scheduleChildAction(groupId, i, "open", delayMs * (i - 1))
    }
}

void multiTriggerResetAll(String controllerDni) {
    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, 0)

    if (!groupId || count < 1) return

    (1..count).each { Integer i ->
        scheduleChildAction(groupId, i, "close", 0)
    }
}

/*
 * Controller refresh calls this to re-push config down to devices.
 */
void multiTriggerSync(String controllerDni) {
    def groupId = groupIdFromControllerDni(controllerDni)
    if (!groupId) return

    def controller = getChildDevice(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, safeInt(controller?.getDataValue("heArtChildCount"), 0))

    Integer delayMs = safeInt(cfg.delayMs, safeInt(controller?.getDataValue("heArtDelayMs"), 350))
    Integer resetSec = safeInt(cfg.childAutoResetSeconds, safeInt(controller?.getDataValue("heArtChildAutoResetSeconds"), 5))
    Integer childMinMs = safeInt(cfg.childDebounceMs, 250)
    Integer groupMinMs = safeInt(controller?.getDataValue("heArtSuppressMs"), 250)

    String name = (cfg.name ?: controller?.getDataValue("heArtGroupName") ?: controller?.displayName ?: "Trigger Group").toString()

    // Push to controller
    if (controller) {
        safeCall(controller, "syncFromApp", [[
            groupName: name,
            triggerCount: count,
            resetAfterSeconds: resetSec,
            timeBetweenTriggersMs: delayMs,
            minTimeBetweenGroupTriggersMs: groupMinMs
        ]])
    }

    // Push to children
    (1..count).each { Integer i ->
        def child = getChildDevice(childDni(groupId, i))
        if (child) {
            try {
                child.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
                child.updateSetting("debounceMs",       [type: "number", value: childMinMs])
                child.updateDataValue("heArtGroupName", "${name}")
                child.updateDataValue("heArtChildNum", "${i}")
            } catch (ignored) { }
            safeCall(child, "syncFromApp", [[groupName: name, triggerNumber: i]])
        }
    }
}

/*
 * Backward-compatible methods for older controller drivers.
 * These remain implemented so older installations do not break.
 */

void multiTriggerCommand(String controllerDni, String childId, String action) {
    def groupId = groupIdFromControllerDni(controllerDni)
    if (!groupId) return

    Integer childNum = safeInt(childId, null)
    if (childNum == null) {
        logWarn("multiTriggerCommand: invalid childId=${childId}")
        return null
    }

    scheduleChildAction(groupId, childNum, (action ?: "open").toString(), 0)
}

void multiTriggerTriggerRange(String controllerDni, String spec) {
    def groupId = groupIdFromControllerDni(controllerDni)
    def cfg = getGroupCfg(groupId, controllerDni)
    Integer delayMs = safeInt(cfg.delayMs, 350)
    Integer count = safeInt(cfg.count, 0)
    if (!groupId || count < 1) return

    def targets = parseRangeSpec(spec, count)
    if (!targets) return

    targets.eachWithIndex { Integer childNum, Integer idx ->
        scheduleChildAction(groupId, childNum, "open", delayMs * idx)
    }
}

void multiTriggerSetDelayMs(String controllerDni, Integer newDelayMs) {
    def groupId = groupIdFromControllerDni(controllerDni)
    if (!groupId) {
        logWarn("multiTriggerSetDelayMs: unable to resolve groupId for controller dni=${controllerDni}")
        return null
    }

    Integer d = clampInt(newDelayMs, 350, 0, 5000)

    state.groups = state.groups ?: [:]
    def cfg = (state.groups[groupId] ?: [:])
    cfg.delayMs = d
    state.groups[groupId] = cfg

    def controller = getChildDevice(controllerDni)
    if (controller) {
        try { controller.updateDataValue("heArtDelayMs", "${d}") } catch (ignored) { }
        safeCall(controller, "syncFromApp", [[timeBetweenTriggersMs: d]])
    }

    logInfo("Updated time between triggers for groupId=${groupId} to ${d}ms")
}

void multiTriggerSetChildAutoResetSeconds(String controllerDni, Integer newSeconds) {
    def groupId = groupIdFromControllerDni(controllerDni)
    if (!groupId) {
        logWarn("multiTriggerSetChildAutoResetSeconds: unable to resolve groupId for controller dni=${controllerDni}")
        return null
    }

    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, 0)
    if (count < 1) {
        logWarn("multiTriggerSetChildAutoResetSeconds: unknown group/child count for controller dni=${controllerDni}")
        return null
    }

    Integer sec = clampInt(newSeconds, 5, 0, 3600)

    Integer updated = 0
    (1..count).each { Integer i ->
        def child = getChildDevice(childDni(groupId, i))
        if (child) {
            try {
                child.updateSetting("autoResetSeconds", [type: "number", value: sec])
                safeCall(child, "syncFromApp", [[resetAfterSeconds: sec]])
                updated++
            } catch (ignored) { }
        }
    }

    state.groups = state.groups ?: [:]
    def cfg2 = (state.groups[groupId] ?: [:])
    cfg2.childAutoResetSeconds = sec
    state.groups[groupId] = cfg2

    def controller = getChildDevice(controllerDni)
    if (controller) {
        try { controller.updateDataValue("heArtChildAutoResetSeconds", "${sec}") } catch (ignored) { }
        safeCall(controller, "syncFromApp", [[resetAfterSeconds: sec]])
    }

    logInfo("Updated reset after seconds for groupId=${groupId} to ${sec}s on ${updated}/${count} trigger devices")
}


private String uiKey(String raw) {
    return (raw ?: "").toString().replaceAll(/[^A-Za-z0-9_]/, "_")
}

/* Scheduling helpers */

private void scheduleChildAction(String groupId, Integer childNum, String action, Integer delayMs) {
    def data = [groupId: groupId, childNum: childNum, action: (action ?: "open").toString()]
    if (delayMs != null && delayMs > 0) {
        runInMillis(delayMs, "doChildAction", [data: data])
    } else {
        doChildAction(data)
    }
}

void doChildAction(Map data) {
    def groupId = data?.groupId?.toString()
    Integer childNum = safeInt(data?.childNum, null)
    def action = data?.action?.toString()

    if (!groupId || childNum == null) return

    def dev = getChildDevice(childDni(groupId, childNum))
    if (!dev) {
        logWarn("Trigger device not found for groupId=${groupId} trigger=${childNum}")
        return null
    }

    if (action == "close") {
        dev.close()
        return null
    }

    dev.open()
}

/* Delete */

private void deleteAllDevices() {
    def kids = getChildDevices() ?: []
    kids.each { d ->
        try {
            deleteChildDevice(d.deviceNetworkId)
        } catch (e) {
            logWarn("Failed to delete ${d?.displayName}: ${e}")
        }
    }
    state.groups = [:]
    logInfo("Deleted all devices created by this app")
}

/* Group cfg fallback helpers */

private Map getGroupCfg(String groupId, String controllerDni) {
    state.groups = state.groups ?: [:]
    def cfg = (state.groups[groupId] ?: [:])

    // If state was lost, recover from controller data values
    if ((cfg?.count == null || cfg?.delayMs == null) && controllerDni) {
        def controller = getChildDevice(controllerDni)
        if (controller) {
            try {
                def c = safeInt(controller.getDataValue("heArtChildCount"), null)
                def d = safeInt(controller.getDataValue("heArtDelayMs"), null)
                def a = safeInt(controller.getDataValue("heArtChildAutoResetSeconds"), null)
                def n = controller.getDataValue("heArtGroupName")
                def s = safeInt(controller.getDataValue("heArtSuppressMs"), null)

                if (n) cfg.name = n.toString()
                if (c != null) cfg.count = c
                if (d != null) cfg.delayMs = d
                if (a != null) cfg.childAutoResetSeconds = a
                if (s != null) cfg.groupSuppressMs = s

                state.groups[groupId] = cfg
            } catch (ignored) { }
        }
    }

    return cfg
}

/* Parsing + utilities */

/*
 * Internal DNI format is intentionally kept stable for backward compatibility.
 * (We still use \"...:endpoint:...\" in the DNI even though UI says \"trigger devices\".)
 */
private String childDni(String groupId, Integer childNum) {
    return "heArt:group:endpoint:${groupId}:${childNum}"
}

private String groupIdFromControllerDni(String dni) {
    if (!dni) return null
    def parts = dni.toString().split(":")
    def idx = parts.findIndexOf { it == "controller" }
    if (idx >= 0 && parts.size() > idx + 1) return parts[idx + 1]
    return parts ? parts[-1] : null
}

private List<Integer> parseRangeSpec(String spec, Integer max) {
    if (!spec) return []
    def out = []
    def cleaned = spec.toString().replaceAll("\\s+", "")
    cleaned.split(",").each { token ->
        if (!token) return
        if (token.contains("-")) {
            def ab = token.split("-")
            def a = safeInt(ab[0], null)
            def b = safeInt(ab.size() > 1 ? ab[1] : null, null)
            if (a != null && b != null) {
                def lo = Math.max(1, Math.min(a, b))
                def hi = Math.min(max, Math.max(a, b))
                (lo..hi).each { out << it }
            }
        } else {
            def n = safeInt(token, null)
            if (n != null && n >= 1 && n <= max) out << n
        }
    }
    return out.unique()
}

private Integer safeInt(def v, Integer dflt) {
    try {
        if (v == null) return dflt
        return Integer.parseInt(v.toString())
    } catch (ignored) {
        return dflt
    }
}

private Integer clampInt(def v, Integer dflt, Integer min, Integer max) {
    Integer n = safeInt(v, dflt)
    if (n == null) n = dflt
    if (n < min) n = min
    if (n > max) n = max
    return n
}

private void safeCall(def deviceWrapper, String methodName, List args) {
    try {
        deviceWrapper."${methodName}"(*args)
    } catch (ignored) {
        // Intentionally ignore. Older driver versions may not implement the helper methods.
    }
}

/* Tools helpers */

private void disableAllDebugLogging() {
    try {
        app.updateSetting("debugLogging", [type: "bool", value: false])
    } catch (ignored) { }

    (getChildDevices() ?: []).each { d ->
        try {
            d.updateSetting("debugLogging", [type: "bool", value: false])
        } catch (ignored) { }
    }

    logInfo("Disabled debug logging for app and all child devices")
}

private String validateInstallation() {
    def kids = (getChildDevices() ?: [])

    def standalones = kids.findAll { (it?.deviceNetworkId ?: "").toString().startsWith("heArt:standalone:") }
    def controllers = kids.findAll { (it?.deviceNetworkId ?: "").toString().startsWith("heArt:group:controller:") }
    def endpoints = kids.findAll { (it?.deviceNetworkId ?: "").toString().startsWith("heArt:group:endpoint:") }

    int missingChildren = 0
    int orphanEndpoints = 0
    def missingLines = []

    controllers.each { c ->
        String cDni = c.deviceNetworkId
        String groupId = groupIdFromControllerDni(cDni)
        def cfg = getGroupCfg(groupId, cDni)
        Integer count = safeInt(cfg.count, safeInt(c.getDataValue("heArtChildCount"), 0))
        if (count == null) count = 0

        (1..count).each { Integer i ->
            def child = getChildDevice(childDni(groupId, i))
            if (!child) {
                missingChildren++
                missingLines << "Missing trigger device for group '${c.displayName}' – Trigger ${i}"
            }
        }

        if (!(state?.groups ?: [:]).containsKey(groupId)) {
            missingLines << "Group '${c.displayName}' is missing from app state (state.groups). Use Rebuild if needed."
        }
    }

    endpoints.each { e ->
        def dni = (e.deviceNetworkId ?: "").toString()
        def parts = dni.split(":")
        // expected: heArt:group:endpoint:<groupId>:<childNum>
        if (parts.size() >= 5) {
            def groupId = parts[3]
            def controller = getChildDevice("heArt:group:controller:${groupId}")
            if (!controller) {
                orphanEndpoints++
            }
        }
    }

    def lines = []
    lines << "App v${appVersion()}"
    lines << "Single triggers: ${standalones.size()}"
    lines << "Groups: ${controllers.size()} (trigger devices: ${endpoints.size()})"
    if (missingChildren > 0) lines << "Missing trigger devices in groups: ${missingChildren}"
    if (orphanEndpoints > 0) lines << "Orphan trigger devices without a controller: ${orphanEndpoints}"

    if (missingLines) {
        lines << ""
        lines << "Details:"
        lines.addAll(missingLines.take(25))
        if (missingLines.size() > 25) lines << "(More issues exist – see Logs or run Rebuild.)"
    } else {
        lines << ""
        lines << "No issues detected."
    }

    return lines.join("\n")
}

private String rebuildStateFromControllers() {
    def kids = (getChildDevices() ?: [])
    def controllers = kids.findAll { (it?.deviceNetworkId ?: "").toString().startsWith("heArt:group:controller:") }

    state.groups = [:]

    controllers.each { c ->
        String cDni = c.deviceNetworkId
        String groupId = groupIdFromControllerDni(cDni)

        Integer count = safeInt(c.getDataValue("heArtChildCount"), 0)
        Integer delayMs = safeInt(c.getDataValue("heArtDelayMs"), 350)
        Integer resetSec = safeInt(c.getDataValue("heArtChildAutoResetSeconds"), 5)
        Integer suppressMs = safeInt(c.getDataValue("heArtSuppressMs"), 250)
        String name = (c.getDataValue("heArtGroupName") ?: c.displayName ?: "Trigger Group").toString()

        state.groups[groupId] = [
            name: name,
            count: count,
            delayMs: delayMs,
            childAutoResetSeconds: resetSec,
            childDebounceMs: 250,
            groupSuppressMs: suppressMs
        ]

        // Push config back down so devices show friendly display values.
        try {
            multiTriggerSync(cDni)
        } catch (ignored) { }
    }

    return "Rebuilt state for ${controllers.size()} group(s)."
}

/* Diagnostics helpers */

private void enableDiagnosticsFor30Minutes() {
    try {
        app.updateSetting("debugLogging", [type: "bool", value: true])
    } catch (ignored) { }
    // Auto-disable after 30 minutes
    runIn(1800, "disableDiagnostics")
}

def disableDiagnostics() {
    try {
        app.updateSetting("debugLogging", [type: "bool", value: false])
    } catch (ignored) { }
    logInfo("Diagnostics auto-disabled")
}

/* Logging */

private void logInfo(String msg) {
    if (settings?.infoLogging != false) log.info "Hubitat Alexa Routine Trigger: ${msg}"
}

private void logDebug(String msg) {
    if (settings?.debugLogging == true) log.debug "Hubitat Alexa Routine Trigger: ${msg}"
}

private void logWarn(String msg) {
    log.warn "Hubitat Alexa Routine Trigger: ${msg}"
}
