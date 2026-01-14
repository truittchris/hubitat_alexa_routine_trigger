# Support and Contributions

This project is community-supported.

There is no formal support or service-level guarantee. The Alexa Pack is provided
as-is and is intended for users comfortable with Hubitat apps/drivers and virtual
devices.

The Alexa Pack focuses on one thing: creating Alexa-routine-friendly virtual
trigger devices with sensible defaults (auto-reset, debouncing) and basic
diagnostics.

## Getting Help

If something does not behave as expected:

1. Confirm the device(s) are selected in Hubitat’s built-in Amazon Alexa app
   and that Alexa device discovery has been run.
2. In Hubitat, open the virtual device and verify it changes state:

   - Contact sensors: contact should go open, then auto-close after the configured
     number of seconds.

3. Confirm your Alexa routine trigger is configured as:

   - “When [contact sensor] opens”

4. Enable debug logging on the device and watch Hubitat Logs to confirm the
   device is emitting events.
5. Review device attributes for quick diagnostics:

   - lastTriggeredAt
   - lastResetAt
   - triggerCount
   - lastCommand
   - lastError

Notes:

- The pack can confirm Hubitat emitted the event.
- The pack cannot confirm whether Alexa executed the routine, because the local
  Hubitat–Alexa integration does not provide an acknowledgement callback.

## Reporting Issues

Please use GitHub Issues:
https://github.com/truittchris/he_alexa_pack/issues

When reporting an issue, include:

- Hubitat platform version and model
- Alexa environment (Alexa app, Echo device models if relevant)
- Whether you are using standalone triggers or a MultiTrigger group
- Example steps to reproduce
- Relevant device attributes
- Redacted debug logs (avoid posting personal device names if desired)

## Contributions

Pull requests are welcome.

Guidelines:

- Keep changes focused and minimal
- Preserve existing behavior and defaults
- Avoid adding UI complexity unless it measurably improves usability
- Maintain reliability-first behavior (auto-reset, debouncing, safe defaults)
- Keep the project suitable for long-term maintenance

Significant feature requests should be discussed via an issue before submitting a
pull request.
