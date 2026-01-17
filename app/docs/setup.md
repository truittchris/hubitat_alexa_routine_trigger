# Setup

This guide covers installation and the Alexa routine configuration required to use Hubitat Alexa Routine Trigger.

## Prerequisites

- Hubitat Elevation hub
- Amazon Alexa account and at least one Echo-capable device (or the Alexa mobile app)
- Hubitat built-in app: Amazon Echo Skill (this exposes Hubitat devices to Alexa)

## Installation options

### Option A – Hubitat Package Manager (HPM)

1. Install Hubitat Package Manager (HPM) on your hub if you do not already have it.
2. In HPM, choose Install.
3. Add a package by URL and use the repository.json in this repo:
   - https://raw.githubusercontent.com/truittchris/hubitat_alexa_routine_trigger/main/hpm/repository.json
4. Install the package.

### Option B – Manual code install

1. In Hubitat, go to Apps Code and click New App.
2. Paste the contents of:
   - apps/HubitatAlexaRoutineTrigger.groovy
3. Click Save.
4. In Hubitat, go to Drivers Code and click New Driver.
5. Paste and save each of:
   - drivers/HubitatAlexaRoutineTriggerDriverVirtualContact.groovy
   - drivers/HubitatAlexaRoutineTriggerDriverChildContact.groovy
   - drivers/HubitatAlexaRoutineTriggerDriverMultiTriggerController.groovy

## Hubitat configuration

1. Go to Apps.
2. Click Add User App.
3. Select Hubitat Alexa Routine Trigger.
4. Use the app UI to create:
   - Single trigger device(s), and/or
   - Trigger group(s) (controller + multiple child contact devices)

## Alexa configuration

### 1) Expose devices to Alexa

1. In Hubitat, open the built-in Amazon Echo Skill app.
2. Select the trigger devices you created (the contact sensors).
   - For groups: select the child contact devices (the controller is for Hubitat automations).
3. Save.

### 2) Discover devices in Alexa

1. Open the Alexa mobile app.
2. Go to Devices.
3. Tap the + icon and choose Add Device.
4. Choose Other.
5. Tap Discover Devices.

### 3) Create a routine

1. In the Alexa app, go to More → Routines.
2. Tap +.
3. For When this happens:
   - Choose Smart Home
   - Select your trigger contact sensor
   - Choose Opens
4. Add an action (for example: turn on a light, announce something, start a scene).
5. Save.

## Testing

- In Hubitat, open the trigger device.
- Use the device command (Open/On/Trigger depending on device type) and confirm:
  - Hubitat shows the contact opens, then auto-closes.
  - Alexa runs the routine.
