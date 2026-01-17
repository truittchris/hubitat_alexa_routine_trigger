# Troubleshooting

## Alexa does not run the routine

1. Confirm the trigger device is selected in Hubitat’s Amazon Echo Skill app.
2. In the Alexa app, run Discover Devices.
3. In Hubitat, open the trigger device and confirm the device state changes (contact opens then closes).
4. In Alexa, confirm the routine trigger is configured as: When contact sensor opens.
5. Increase the Reset after (seconds) value to 2 or 3 and test again.

## The device triggers in Hubitat but Alexa still does nothing

The Hubitat-to-Alexa integration does not provide an acknowledgement callback. This project can confirm Hubitat emitted the event, but it cannot confirm whether Alexa executed the routine.

## Duplicate triggers

Increase the Minimum time between triggers (ms) (debounce/cooldown). A starting point is 250 ms to 1000 ms.

## Group triggers fire out of order or too fast

Increase the group’s time-between-triggers setting in the app.

## Collecting information for an issue

Include:

- Hubitat model and platform version
- App version and driver versions
- Whether you are using a single trigger or a group
- Steps to reproduce
- Relevant logs (redact personal device names if desired)
