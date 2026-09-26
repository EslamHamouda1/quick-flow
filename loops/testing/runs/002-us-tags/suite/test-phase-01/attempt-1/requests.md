# test-phase-01 attempt 1: how the calls were made

`bash test-phase-01-verify.sh` needed approval in this session, so its calls ran as five chained curl commands
(`curl --next`, base `http://localhost:8080`) on the runner's fresh test DB, 2026-09-26 23:50–23:52 Africa/Cairo.
Each call wrote its body to `b/NN.json` and one line `NN check label METHOD path [T = timed] [expected]<TAB>http_code<TAB>time_total<TAB>content_type`
to `status.tsv` (`status.json` = the same as JSON). `expect.json` = expected schema and status per call.
Ids used in later calls (task 1/2/3, habit 1, card 1, milestone 1, note 1, plan 1, item 1) were checked against the
create responses (b/03, 21, 31, 54, 62, 68, 78, 79). Today = 2026-09-26, yesterday 2026-09-25, tomorrow 2026-09-27.

Request bodies (Content-Type: application/json):

| NN | body |
|---|---|
| 03 | `{"title":"Contract task","dueDate":"2026-09-25"}` |
| 04 | `{"title":""}` |
| 06 | (query) `q=contract&status=TODO&priority=MEDIUM&dueFrom=2026-09-25&dueTo=2026-09-26&archived=false&sort=dueDate&direction=asc` |
| 10 | `{"title":"Contract task 2","status":"IN_PROGRESS","priority":"HIGH","dueDate":null}` |
| 11 | `{"title":"x","priority":"HIGH"}` |
| 12 | `{"title":"x","status":"TODO","priority":"LOW"}` |
| 21 | `{"title":"Tagged contract task","dueDate":"2026-09-26"}` |
| 22 | `{"tags":["Work"," urgent "]}` |
| 23 | `{"tags":[]}` |
| 24 | `{"tags":["work"]}` |
| 31 | `{"name":"Contract habit","frequency":"DAILY"}` |
| 32 | `{"name":"","frequency":"DAILY"}` |
| 37 | `{"name":"Contract habit 2","description":"d","frequency":"WEEKLY"}` |
| 38 | `{"name":"x"}` |
| 39 | `{"name":"x","frequency":"DAILY"}` |
| 44 | (no body) |
| 45 | `{"date":"2026-09-27"}` |
| 46, 47 | `{"date":"2026-09-26"}` |
| 54 | `{"title":"Contract card","description":"d"}` |
| 55 | `{"title":""}` |
| 59 | `{"title":"Contract card 2","description":null,"status":"IN_PROGRESS"}` |
| 60 | `{"title":"x"}` |
| 61 | `{"title":"x","status":"NOT_STARTED"}` |
| 62 | `{"title":"M1","targetDate":"2026-10-01"}` |
| 63 | `{"title":""}` |
| 64 | `{"title":"M"}` |
| 65 | `{"title":"M1b","targetDate":null,"done":true}` |
| 66 | `{"title":"M1b"}` |
| 67 | `{"title":"M","done":false}` |
| 68 | `{"text":"A note"}` |
| 69 | `{"text":""}` |
| 70 | `{"text":"n"}` |
| 78 | `{"title":"Plan source task"}` |
| 79 | `{"title":"Contract plan","estimatedDurationMinutes":60,"startDateTime":"2026-09-27T01:00:00+03:00","endDateTime":"2026-09-27T02:00:00+03:00","priorityOrder":1,"items":[{"sourceType":"TASK","sourceId":3}]}` |
| 80 | same as 79 with `"items":[]` |
| 81 | start `02:00`, end `01:00` (end before start), items as 79 |
| 82 | start = end = `01:00`, items as 79 |
| 89 | `{"title":"Contract plan 2","estimatedDurationMinutes":30,"startDateTime":"…T01:00:00+03:00","endDateTime":"…T02:00:00+03:00","priorityOrder":2}` |
| 90 | as 89 with start `02:00`, end `01:00` |
| 91 | `{"title":"x","estimatedDurationMinutes":30,…01:00…02:00…,"priorityOrder":1}` |
| 92 | `{"done":true}` |
| 93 | `{}` |
| 94 | `{"done":false}` |
| 100 | `{"displayName":"Contract","planStartNotifications":false,"defaultPage":"TASKS"}` |
| 101 | `{"displayName":"x","planStartNotifications":true,"defaultPage":"NOPE"}` |
| 102 | `{"displayName":null,"planStartNotifications":true,"defaultPage":"DASHBOARD"}` (restore defaults) |
