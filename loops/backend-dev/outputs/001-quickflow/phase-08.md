# phase-08: US6 Adjust settings
layer: backend
story_id: US6
spec_phase: 8
depends_on: [phase-02]
## Goal
Profile information and preferences kept between sessions (`GET/PUT /api/settings`; content per the Q3 answer).
## Acceptance criteria
- AC-US6-1: Given the Settings page, When the user opens it, Then the profile information and current preferences are shown.
- AC-US6-2: Given the Settings page, When the user changes a preference and saves, Then it is kept between sessions and takes effect (FR-10). (pending Q3)
## Tasks
- [x] T075 [P] [US6] Domain test backend/src/test/java/com/quickflow/domain/settings/SettingsServiceTest.java: `FR-10.2` first read returns defaults (recommended Q3: `displayName` null, `planStartNotifications` true, `defaultPage` DASHBOARD) and stores them; update saves; `displayName` > 100 rejected (field `displayName`); null `defaultPage` rejected
- [x] T076 [P] [US6] Persistence test backend/src/test/java/com/quickflow/persistence/SettingsRepositoryTest.java: `NFR-3` the single settings row (id 1) is saved and read back
- [x] T077 [P] [US6] Web-slice test backend/src/test/java/com/quickflow/web/settings/SettingsControllerTest.java: `getSettings` 200 with exactly the contract `Settings` fields (`displayName`, `planStartNotifications`, `defaultPage`), `updateSettings` 200, unknown `defaultPage` 400, too-long `displayName` 400
- [x] T078 [P] [US6] Create enum `DefaultPage {DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS}` and entity `AppSettings` (`id` always 1, `displayName` "≤ 100", `planStartNotifications` default true, `defaultPage` default DASHBOARD) in backend/src/main/java/com/quickflow/domain/settings/
- [x] T079 [US6] Create `SettingsRepository` in backend/src/main/java/com/quickflow/domain/settings/SettingsRepository.java
- [x] T080 [US6] Create `SettingsService` (`get()` transactional read-or-create of the default row, so it works on an empty database; `update(...)`) in backend/src/main/java/com/quickflow/domain/settings/SettingsService.java
- [x] T081 [US6] Create `SettingsDto` record and `SettingsController` (`getSettings`, `updateSettings`) in backend/src/main/java/com/quickflow/web/settings/
