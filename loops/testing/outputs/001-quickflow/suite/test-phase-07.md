# test-phase-07: US6 Adjust settings

## Goal
US6's acceptance criteria (AC-US6-1/2) and FR-10.2 / NFR-3 hold: settings are read and saved through the API,
then the Settings page (`/settings`) shows them, saves changes that survive a reload and take effect (greeting,
plan-start notifications, default page), through Playwright MCP, headless.

Depends on: test-phase-01 (the "takes effect" checks use the Dashboard and Todo Plans pages). Layers: backend → frontend.

## API checks (curl)
- [ ] A1 FR-10.2: fresh DB → `displayName` null, `planStartNotifications` true, `defaultPage` DASHBOARD.
- [ ] A2 AC-US6-2 / NFR-3: PUT values saved and returned on the next GET; every one of the six defaultPage values accepted.
- [ ] A3 FR-10.2: displayName 100 chars ok, 101 → 400 naming `displayName`.
- [ ] A4 FR-10.2: invalid defaultPage or planStartNotifications → 400, settings unchanged.

## UI checks (Playwright MCP, `/settings`)
- [ ] U1 AC-US6-1: `settings-form` shows the API values; `settings-time-zone` = app-info time zone.
- [ ] U2 AC-US6-2 / NFR-3: change and `settings-save` → saved notice, API and reload keep the values.
- [ ] U3 FR-10.2 takes effect: `dash-greeting` "Hello, <name>"; `/` opens the chosen default page.
- [ ] U4 FR-10.2 takes effect: notifications off → no plan-start `notice` at start time; on → shown.
- [ ] U5 FR-10.2: 101-char name → `error-displayName`, nothing saved.
- [ ] U6 Navigation: `nav-settings` reaches `/settings` with `page-title` "Settings".
