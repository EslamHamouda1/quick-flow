# phase-01: Setup
layer: frontend
story_id: none
spec_phase: 1
depends_on: []
## Goal
Scaffold check, dev-server proxy to the backend, clean start page.
## Acceptance criteria
## Tasks
- [ ] T001 Check the scaffolded frontend/package.json (`@angular/core` and `@angular/cli` at 22.2.0, `start` = `ng serve`, `build` = `ng build`) and frontend/src/app/app.config.ts (zoneless change detection, router provided); record anything that differs from research R-1 in specs/001-quickflow-frontend/research.md
- [ ] T002 Create frontend/proxy.conf.json proxying `/api` to `http://localhost:8080` (`secure: false`, `changeOrigin: true`)
- [ ] T003 Set `proxyConfig: "proxy.conf.json"` in the `serve` target options of frontend/angular.json
- [ ] T004 Replace the scaffold's welcome markup in frontend/src/app/app.html with a bare `<router-outlet />` and write base layout styles (sidebar + content, form rows, badges, buttons) in frontend/src/styles.css
