# phase-08: US6 Adjust settings
layer: frontend
story_id: US6
spec_phase: 8
depends_on: [phase-02, backend-dev:US6]
## Goal
The Settings page; preferences kept on the server and taking effect (default page here; greeting and notifications read them where used).
## Acceptance criteria
- AC-US6-1: Given the Settings page, When the user opens it, Then the profile information and current preferences are shown.
- AC-US6-2: Given the Settings page, When the user changes a preference and saves, Then it is kept between sessions and takes effect (FR-10).
## Tasks
- [ ] T040 [US6] Check that the generated `SettingsService` has `getSettings`/`updateSettings` and `Settings` has `displayName`, `planStartNotifications`, `defaultPage` (enum DASHBOARD..SETTINGS); check the Settings section of contracts/ui-contract.md covers AC-US6-1..2; raise a question for anything missing
- [ ] T041 [US6] Implement the Settings page (loads `getSettings()`; display name, plan-start notifications checkbox, default page select with the six labels, save via `updateSettings` with field errors and a notice, read-only time zone from `ClockService`) in frontend/src/app/pages/settings/settings.page.ts (FR-10.2, AC-US6-1, AC-US6-2)
- [ ] T042 [US6] Add the default-page guard on route `''` (reads `getSettings()`, maps `defaultPage` to its route, research R-8) in frontend/src/app/core/default-page.guard.ts and use it in frontend/src/app/app.routes.ts
