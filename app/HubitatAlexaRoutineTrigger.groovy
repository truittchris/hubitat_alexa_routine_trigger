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

def appVersion() { return "0.6.0" }

preferences {
    page(name: "mainPage")
    page(name: "managePage")
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

        section("Create one routine trigger") {
            paragraph("Creates one virtual contact sensor you can expose to Alexa and use as a routine trigger.")

            input(name: "createSingleName", type: "text", title: "Device name", defaultValue: "Alexa Routine Trigger", submitOnChange: true)
            input(name: "createSingleResetAfterSeconds", type: "number", title: "Reset after (seconds)", defaultValue: 5, submitOnChange: true)
            input(name: "createSingleMinBetweenMs", type: "number", title: "Minimum time between triggers (ms)", defaultValue: 250, submitOnChange: true)

            input(name: "btnCreateStandalone", type: "button", title: "Create trigger device")
        }

        section("Create a trigger group (advanced)") {
            paragraph("Creates multiple trigger devices and one group controller switch. Use this only if you need to trigger more than one Alexa routine in sequence.")
            input(name: "showAdvancedGroup", type: "bool", title: "Show advanced group options", defaultValue: false, submitOnChange: true)

            if (settings?.showAdvancedGroup == true) {
                input(name: "createGroupName", type: "text", title: "Group name", defaultValue: "Alexa Trigger Group", submitOnChange: true)
                input(name: "createGroupCount", type: "number", title: "Number of trigger devices", defaultValue: 2, submitOnChange: true)

                input(name: "createGroupResetAfterSeconds", type: "number", title: "Reset after (seconds)", defaultValue: 5, submitOnChange: true)
                input(name: "createGroupChildMinBetweenMs", type: "number", title: "Minimum time between triggers (ms)", defaultValue: 250, submitOnChange: true)

                input(name: "createGroupTimeBetweenMs", type: "number", title: "Time between triggers (ms)", defaultValue: 350, submitOnChange: true)
                paragraph("Set to 0 to fire all trigger devices at the same time. If Alexa misses triggers, increase this to 300–800 ms.")

                input(name: "createGroupMinBetweenGroupTriggersMs", type: "number", title: "Minimum time between group triggers (ms)", defaultValue: 250, submitOnChange: true)

                input(name: "btnCreateGroup", type: "button", title: "Create trigger group")
            }
        }

        section("How to use this in Alexa") {
            paragraph("1) Open the Alexa app.\n2) Go to Routines.\n3) Create a new routine.\n4) Under When this happens, choose Smart Home.\n5) Select your trigger device.\n6) Choose Open.\n7) Add actions and save.\n\nTip: For groups, use each trigger device for a different Alexa routine. Use the group controller switch in Hubitat to fire the sequence.")
        }

        section("Manage existing triggers") {
            href(name: "toManage", title: "View and manage your triggers", page: "managePage", description: "Edit settings, test, or delete.")
        }

        section("App options") {
            input(name: "infoLogging", type: "bool", title: "Enable app info logging", defaultValue: true, submitOnChange: true)
            input(name: "debugLogging", type: "bool", title: "Enable app debug logging", defaultValue: false, submitOnChange: true)
        }

        section("About") {
            paragraph("Author: Chris Truitt\nWebsite: christruitt.com\nGitHub: truittchris")
            href(name: "aboutLink", title: "Open christruitt.com", description: "Support and updates", required: false, url: "https://christruitt.com")
        }
    }
}

def managePage() {
    dynamicPage(name: "managePage", title: "Manage triggers", install: false, uninstall: false) {

        def kids = (getChildDevices() ?: [])

        def standalones = kids.findAll { d ->
            (d?.deviceNetworkId ?: "").toString().startsWith("heArt:standalone:")
        }.sort { (it?.displayName ?: "") }

        def controllers = kids.findAll { d ->
            (d?.deviceNetworkId ?: "").toString().startsWith("heArt:group:controller:")
        }.sort { (it?.displayName ?: "") }

        section("Single triggers") {
            if (!standalones) {
                paragraph("None yet.")
            } else {
                standalones.each { d ->
                    href(
                        name: "editStandalone_${d.deviceNetworkId}",
                        title: d.displayName,
                        description: "Reset after: ${safeInt(d?.currentValue('resetAfterSeconds'), 5)}s | Minimum between: ${safeInt(d?.currentValue('minTimeBetweenTriggersMs'), 250)}ms",
                        page: "editStandalonePage",
                        params: [dni: d.deviceNetworkId]
                    )
                }
            }
        }

        section("Trigger groups") {
            if (!controllers) {
                paragraph("None yet.")
            } else {
                controllers.each { c ->
                    def groupId = groupIdFromControllerDni(c.deviceNetworkId)
                    def cfg = getGroupCfg(groupId, c.deviceNetworkId)

                    def count = safeInt(cfg.count, safeInt(c.getDataValue("heArtChildCount"), 0))
                    def delayMs = safeInt(cfg.delayMs, safeInt(c.getDataValue("heArtDelayMs"), 350))
                    def resetSec = safeInt(cfg.childAutoResetSeconds, safeInt(c.getDataValue("heArtChildAutoResetSeconds"), 5))

                    href(
                        name: "editGroup_${c.deviceNetworkId}",
                        title: c.displayName,
                        description: "${count} trigger devices | Reset after: ${resetSec}s | Time between: ${delayMs}ms",
                        page: "editGroupPage",
                        params: [controllerDni: c.deviceNetworkId]
                    )
                }
            }
        }

        section("Danger zone") {
            paragraph("This will permanently delete all devices created by this app.")
            input(name: "btnDeleteAll", type: "button", title: "Delete all devices created by this app")
        }
    }
}

def editStandalonePage(params) {
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
            input(name: "es_resetAfterSeconds", type: "number", title: "Reset after (seconds)", defaultValue: safeInt(dev?.currentValue('resetAfterSeconds'), 5), submitOnChange: true)
            input(name: "es_minBetweenMs", type: "number", title: "Minimum time between triggers (ms)", defaultValue: safeInt(dev?.currentValue('minTimeBetweenTriggersMs'), 250), submitOnChange: true)
        }

        section("Test") {
            input(name: "btnTestStandalone", type: "button", title: "Test trigger now")
            input(name: "btnResetStandalone", type: "button", title: "Reset now")
        }

        section("Save") {
            input(name: "btnSaveStandalone", type: "button", title: "Save settings")
        }

        section("Danger zone") {
            input(name: "btnDeleteStandalone", type: "button", title: "Delete this trigger device")
        }
    }
}

def editGroupPage(params) {
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

            input(name: "eg_resetAfterSeconds", type: "number", title: "Reset after (seconds)", defaultValue: resetSec, submitOnChange: true)
            input(name: "eg_childMinBetweenMs", type: "number", title: "Minimum time between triggers (ms)", defaultValue: childMinMs, submitOnChange: true)

            input(name: "eg_timeBetweenMs", type: "number", title: "Time between triggers (ms)", defaultValue: delayMs, submitOnChange: true)
            if (safeInt(settings?.eg_timeBetweenMs, delayMs) != null && safeInt(settings?.eg_timeBetweenMs, delayMs) < 150 && safeInt(settings?.eg_timeBetweenMs, delayMs) > 0) {
                paragraph("Note: Very low delays can cause Alexa to miss triggers. If that happens, increase this to 300–800 ms.")
            } else {
                paragraph("Set to 0 to fire all trigger devices at the same time.")
            }

            input(name: "eg_groupMinBetweenMs", type: "number", title: "Minimum time between group triggers (ms)", defaultValue: groupMinMs, submitOnChange: true)
        }

        section("Test") {
            input(name: "btnTestGroup", type: "button", title: "Test group now")
            input(name: "btnResetGroup", type: "button", title: "Reset all now")
        }

        section("Save") {
            input(name: "btnSaveGroup", type: "button", title: "Save group settings")
        }

        section("Danger zone") {
            input(name: "btnDeleteGroup", type: "button", title: "Delete this group and all trigger devices")
        }
    }
}

/* Button handlers */

def appButtonHandler(String btn) {
    logDebug("appButtonHandler(${btn})")
    switch (btn) {
        case "btnCreateStandalone":
            createStandalone()
            break

        case "btnCreateGroup":
            createGroup()
            break

        case "btnDeleteAll":
            deleteAllDevices()
            break

        case "btnSaveStandalone":
            saveStandalone()
            break
        case "btnTestStandalone":
            testStandalone()
            break
        case "btnResetStandalone":
            resetStandalone()
            break
        case "btnDeleteStandalone":
            deleteStandalone()
            break

        case "btnSaveGroup":
            saveGroup()
            break
        case "btnTestGroup":
            testGroup()
            break
        case "btnResetGroup":
            resetGroup()
            break
        case "btnDeleteGroup":
            deleteGroup()
            break

        default:
            logWarn("Unknown button: ${btn}")
    }
}

/* Create devices */

private void createStandalone() {
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
        return
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
}

private void createGroup() {
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
        return
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
}

/* Save + test + delete: single */

private void saveStandalone() {
    def dni = (state._editStandaloneDni ?: "").toString()
    def dev = dni ? getChildDevice(dni) : null
    if (!dev) return

    Integer resetSec = clampInt(settings?.es_resetAfterSeconds, 5, 0, 3600)
    Integer minMs = clampInt(settings?.es_minBetweenMs, 250, 0, 60000)

    dev.updateSetting("autoResetSeconds", [type: "number", value: resetSec])
    dev.updateSetting("debounceMs",       [type: "number", value: minMs])

    safeCall(dev, "syncFromApp", [[:]])
    logInfo("Saved settings for ${dev.displayName}")
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
        return
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
        return
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
        return
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
        return
    }

    def cfg = getGroupCfg(groupId, controllerDni)
    Integer count = safeInt(cfg.count, 0)
    if (count < 1) {
        logWarn("multiTriggerSetChildAutoResetSeconds: unknown group/child count for controller dni=${controllerDni}")
        return
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
        return
    }

    if (action == "close") {
        dev.close()
        return
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