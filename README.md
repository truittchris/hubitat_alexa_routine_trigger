# Hubitat Alexa Routine Trigger

Creates Alexa-friendly virtual trigger devices (single triggers and trigger groups) so you can start Alexa routines from Hubitat automations.

Repository: https://github.com/truittchris/hubitat_alexa_routine_trigger
Author: Chris Truitt
Website: https://christruitt.com

## What it does

- Single triggers: virtual contact sensors you expose to Alexa and use as routine triggers
- Trigger groups: a Hubitat-facing controller that fires multiple Alexa-facing child triggers
- Reliability defaults: auto-reset and debounce/cooldown controls
- Basic diagnostics: last-trigger timestamps and error messages on device pages

## Installation

- Hubitat Package Manager (recommended): see docs/setup.md
- Manual: install the app and drivers from /apps and /drivers, then add the app in Hubitat

## Alexa setup (required)

1. In Hubitat, open the built-in Amazon Echo Skill app and select the trigger devices you want Alexa to see.
2. In the Alexa app, run Discover Devices.
3. Build routines using: When this happens – Contact sensor – Opens.

Full steps: docs/setup.md

## Support and tips

- Troubleshooting: docs/troubleshooting.md
- Tip jar: https://christruitt.com/tip-jar/

## License

Apache 2.0. See LICENSE.
