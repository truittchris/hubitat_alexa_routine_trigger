\# Hubitat Alexa Routine Trigger



Creates Alexa-friendly virtual trigger devices (single triggers and trigger groups) for Hubitat Elevation. These triggers are designed to be simple, reliable, and understandable for everyday Hubitat users – no custom Alexa skills, no cloud middleware, and no complex rule logic required.



Use case examples:

\- Trigger an Alexa routine from Hubitat automations

\- Use Hubitat rules to drive Alexa announcements, music, smart plugs, scenes, and more

\- Fire multiple Alexa routines in a controlled sequence (advanced)



Author: Chris Truitt  

Website: https://christruitt.com  

GitHub: https://github.com/truittchris

Tip Jar:  https://christruitt.com/tip-jar

---



\## How it works (high level)



This app creates virtual contact sensors (trigger devices) that Alexa can use as routine triggers.



\- Hubitat controls the trigger device(s) (Open then auto-close).

\- Alexa sees the contact sensor “Open” event and runs a routine.

\- If you need to trigger more than one Alexa routine, the app can create a trigger group: multiple trigger devices plus a controller switch that fires them in order.



---



\## Prerequisites



1\. Hubitat Elevation hub (local or remote access is fine).

2\. Amazon Alexa account + Alexa app.

3\. Hubitat built-in integration: Amazon Echo Skill (Hubitat app)  

&nbsp;  This is required so Alexa can “see” the trigger devices created by this app.



Important: This app does not replace Hubitat’s Amazon Echo Skill – it depends on it.



---



\## Installation



You will install two components:

1\) the Hubitat app  

2\) the virtual device drivers used by the app



\### Step 1 – Install the drivers

In Hubitat:

1\. Go to Drivers Code.

2\. Click New Driver.

3\. Paste the driver code (repeat for each driver file).

4\. Click Save.



Drivers required by this app:

\- Hubitat Alexa Routine Trigger – Virtual Contact Trigger

\- Hubitat Alexa Routine Trigger – Child Contact

\- Hubitat Alexa Routine Trigger – MultiTrigger Controller



(If you used Hubitat Package Manager for installation, this is handled for you.)



\### Step 2 – Install the app

1\. Go to Apps Code.

2\. Click New App.

3\. Paste the app code.

4\. Click Save.

5\. Go to Apps.

6\. Click Add User App.

7\. Select Hubitat Alexa Routine Trigger.



---



\## Creating triggers



\### Option A – Single trigger (recommended for most users)

This creates one virtual contact sensor that you expose to Alexa and use in a routine.



In the app:

1\. Create one routine trigger

2\. Choose:

&nbsp;  - Device name

&nbsp;  - Reset after (seconds)

&nbsp;  - Ignore repeated triggers from Hubitat within (ms)

3\. Click Create trigger device



When to use:

\- You only need to trigger one Alexa routine



\### Option B – Trigger group (advanced)

This creates:

\- multiple trigger devices (virtual contact sensors)

\- one group controller switch



In the app:

1\. Create a trigger group (advanced)

2\. Enable Show advanced group options

3\. Choose:

&nbsp;  - Group name

&nbsp;  - Number of trigger devices

&nbsp;  - Reset after (seconds)

&nbsp;  - Ignore repeated triggers from Hubitat within (ms)

&nbsp;  - Delay between trigger devices (ms)

4\. Click Create trigger group



When to use:

\- You want Hubitat to trigger multiple Alexa routines in sequence



---



\## Understanding the key settings



Reset after (seconds)

\- How long the trigger stays “Open” before it automatically closes again.

\- Default is 5 seconds.

\- Setting this to 0 disables auto-close (not recommended for most users).



Ignore repeated triggers from Hubitat within (ms)

\- Prevents accidental double-fires caused by rapid repeated events.

\- Set to 0 to disable this protection.

\- This applies to:

&nbsp; - single triggers (the device)

&nbsp; - trigger groups (the group and all trigger devices)



Delay between trigger devices (ms) (groups only)

\- Spacing between each trigger device firing when you run the group.

\- Set to 0 to fire all trigger devices at the same time.

\- If Alexa misses triggers, try 300–800 ms.



---



\## Alexa setup (required)



This is the complete Alexa setup flow, including the prerequisite Hubitat app.



\### Step 1 – Enable Hubitat’s Amazon Echo Skill app

In Hubitat:

1\. Go to Apps.

2\. Add Built-In App.

3\. Select Amazon Echo Skill.

4\. Follow the prompts to link Hubitat to Alexa.



\### Step 2 – Expose your trigger device(s) to Alexa

In the Amazon Echo Skill app (Hubitat):

1\. Select the trigger device(s) created by this app.

&nbsp;  - Single trigger: select that virtual contact sensor

&nbsp;  - Group: select each trigger device (the contact sensors), and optionally the group controller switch

2\. Save.



\### Step 3 – Discover devices in Alexa

In the Alexa app:

1\. Go to Devices.

2\. Tap + (Add Device).

3\. Choose Other.

4\. Tap Discover Devices.



After discovery, your trigger devices should appear in Alexa as contact sensors.



---



\## Creating Alexa routines



You will create a routine for each trigger device.



In the Alexa app:

1\. Go to More.

2\. Tap Routines.

3\. Tap + to create a new routine.

4\. Under When this happens, choose Smart Home.

5\. Select your trigger device (contact sensor).

6\. Choose Open (this is the trigger event).

7\. Add your desired actions.

8\. Save.



Tip for groups:

\- Each trigger device in the group should map to its own Alexa routine.

\- When Hubitat runs the group, it fires trigger device 1, then 2, then 3, etc. (based on the delay you set).



---



\## Testing



On the Edit pages:

\- Save and test applies the current on-screen settings before triggering.

\- Reset closes the contact sensor(s) immediately.



Recommended testing order:

1\. Confirm Alexa can see the trigger device (after discovery).

2\. Create an Alexa routine that triggers on Open.

3\. From Hubitat, tap Save and test.

4\. Watch the Alexa routine run.



---



\## Troubleshooting



Alexa routine does not trigger

\- Confirm the trigger device is selected in Hubitat’s Amazon Echo Skill app.

\- Re-run Alexa device discovery.

\- Confirm the routine triggers on Open (not Close).

\- Increase Delay between trigger devices (groups) to 300–800 ms.

\- Increase Reset after (seconds) slightly (for example, 7–10 seconds).



Triggers fire inconsistently in a group

\- Increase Delay between trigger devices (ms).

\- Avoid extremely low delays (under ~150 ms) unless your environment is proven stable.



Double-fires

\- Increase Ignore repeated triggers from Hubitat within (ms) (for example, 250–750 ms).



---



\## Security and privacy notes



\- This app creates local virtual devices and relies on Hubitat’s Amazon Echo Skill to share them with Alexa.

\- Alexa routine execution occurs in Amazon’s ecosystem, as expected for Alexa routines.

\- No additional cloud services are required by this app.



---



\## License



Choose your preferred license for publication (MIT is common for Hubitat community projects). If you add a LICENSE file, reference it here.



