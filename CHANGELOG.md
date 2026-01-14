# Changelog
All notable changes to Hubitat Alexa Routine Trigger will be documented in this file.

## 0.6.2
- Improved UX for testing
  - Test actions now automatically apply the current on-screen settings before triggering (Save and test behavior).
  - Group test button label updated to “Save and test group”.
  - Single test button label updated to “Save and test trigger”.
- Simplified cooldown configuration for groups
  - Reduced from two cooldown fields to a single field on the group page:
    - “Ignore repeated triggers from Hubitat within (ms)”
  - That single value now applies to both:
    - group-level suppression (controller)
    - child device debounce
- Allowed 0 values where appropriate
  - 0 is now valid for cooldown fields and means “disabled”.
- Improved labeling and help text
  - Replaced “Minimum time between…” language with clearer, user-facing explanations.
  - Renamed group delay field to “Delay between trigger devices (ms)”.
- Minor resilience improvements
  - Improved fallback logic for recovering group cooldown settings from controller data values when app state is missing.

## 0.6.1
- Prior version baseline (pre-UX improvements for Save and test and group cooldown simplification).
