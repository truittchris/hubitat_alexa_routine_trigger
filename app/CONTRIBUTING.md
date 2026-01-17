# Contributing

Thanks for your interest in improving Hubitat Alexa Routine Trigger.

## Support expectations

This project is community-supported and provided as-is. There is no formal SLA.

## Getting help

Before filing an issue:

1. Confirm the device(s) are selected in Hubitat’s built-in Amazon Echo Skill app and Alexa device discovery has been run.
2. In Hubitat, open the virtual device and verify it changes state (contact opens, then auto-closes).
3. Confirm the Alexa routine trigger is configured as: When [contact sensor] opens.
4. Enable debug logging and capture relevant Hubitat log lines.

## Reporting issues

Use GitHub Issues for bugs and feature requests.

Include:

- Hubitat platform version and model
- Whether you are using a single trigger or a trigger group
- Steps to reproduce
- Relevant device attributes
- Redacted debug logs (avoid posting private device names if desired)

## Pull requests

Guidelines:

- Keep changes focused and minimal
- Preserve existing behavior and defaults unless there is a clear UX or reliability gain
- Avoid adding UI complexity unless it materially improves usability
- Maintain reliability-first behavior (auto-reset, debounce/cooldown, safe defaults)

For significant feature work, please open an issue first so the approach can be discussed.
