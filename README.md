# BeforeMidnight — Today-Only Todo

> "A task list that resets at midnight. No backlog. No overdue. Just today."

---

## Contents
- [Demo](#demo)
- [Approach](#approach)
- [Requirement Compliance](#requirement-compliance)
- [Features](#features)
- [Stack](#stack)
- [Key Decisions and Tradeoffs](#key-decisions-and-tradeoffs)
- [What I Got Stuck On](#what-i-got-stuck-on)
- [What I'd Improve With More Time](#what-id-improve-with-more-time)
- [AI Usage](#ai-usage)

---

## Demo

https://github.com/user-attachments/assets/1be31547-5ab7-4437-a26f-703ceec54f8d

[Try it in the browser](https://appetize.io/app/b_h5hbvu2buercqgyjlfyyn5vn54)

---

## Approach

The core idea: each day is a self-contained unit. Tasks belong to the day they were created, old rows are never deleted and the visibility is controlled entirely by the WHERE clause, which means the history view gets those rows for free.

Started by understanding the doc and then moved to ideation. Finalised all the features I wanted to implement and made sure everything was planned, with no changes after this point. This helped me break down the project into smaller, manageable chunks and target each layer more efficiently. Then used Claude Code to build layer by layer, making sure to review the generated code and commit after every new layer was successfully built. Tried to be consistent by giving prompts in as much detail as possible so Claude Code wouldn't misinterpret the task, hallucinate, or introduce inconsistency. Informed it about the changes I made manually as well. My overall approach was to keep the app lightweight and not over-engineer it.

---

## Requirement Compliance

| Requirement | Where it's satisfied |
|---|---|
| Add task | ModalBottomSheet → AddTaskUseCase → Room insert |
| Mark complete | Checkbox toggle → ToggleTaskCompletion → Room update |
| Persist locally | Room database, fully offline, no network calls |
| Today-only list | `WHERE createdAtEpochDay = :today` query; old rows filtered, not deleted |
| Automatic day reset | 3 paths: cold start (ViewModel init), `onResume()` via `LifecycleResumeEffect`, `scheduleMidnightReset()` while+delay loop |

---

## Features

| Area | What's built |
|---|---|
| Tasks | Add, tap-to-edit, checkbox complete, expiration time per task |
| Today view | Date header, character counter (n/200), empty state |
| Day reset | 3 paths: cold start, onResume, background midnight timer |
| Expired tasks | Grey out + disable when expiresAt passes — never disappear |
| History view | Previous days grouped by date, read-only |
| Polish | Completion animation, haptic feedback, dynamic color (API 31+), light/dark mode, app icon |
| Testing | 14 unit tests — core logic, UI event channels, day-rollover, FakeDateProvider for time-travel |

---

## Stack

Kotlin 2.2.10 · Jetpack Compose · Material 3 · Room 2.7.1 · Coroutines + StateFlow + Channel · Manual DI (AppContainer + ViewModelFactory) · minSdk 28 · fully offline

---

## Key Decisions and Tradeoffs

**1. epochDay Long for day identity** — store `LocalDate.toEpochDay()` as a `Long`. Main query: `WHERE createdAtEpochDay = :today`. Day rollover = different integer = old tasks vanish from the query automatically. No TypeConverter, no timezone ambiguity, simple integer equality. Rejected date string (locale/format risk) and full timestamp (changes every second, wrong tool for day-equality).

**2. Filter, never delete** — old rows stay in the DB forever. Visibility is controlled entirely by the `WHERE` clause. This is what makes the history view possible — those rows exist and are queryable by `createdAtEpochDay < today`. Explicitly rejected a pruning/DELETE job — it would contradict this and silently break the history view. DB growth: a today-only app generates ~KB/year; noted as an acceptable tradeoff.

**3. Injectable DateProvider** — interface with two methods: `today()` returning `LocalDate`, `nowMillis()` returning `Long`. `SystemDateProvider` uses `LocalDate.now(ZoneId.systemDefault())` — the explicit zone makes the timezone decision visible in code. Abstraction enables `FakeDateProvider` in tests with no mocking framework. Note: `LocalDateTime.now()` is called directly inside `scheduleMidnightReset()` only for computing a delay duration — a duration calc is not business logic, so it doesn't go through the provider. Deliberate decision, not an oversight.

**4. Manual DI over Hilt** — `AppContainer` holds the full dependency graph (all fields `by lazy`). `ViewModelFactory` wires the ViewModel. For a single-screen app, Hilt would triple boilerplate for no real gain. Knowing when NOT to reach for a framework is the judgment.

**5. Midnight reset — 3 paths** — cold start: ViewModel reads `today()` in `init`, always correct on launch. `onResume()`: ViewModel exposes a public `onResume()` function called from `LifecycleResumeEffect` in `MainActivity` — the ViewModel never holds a lifecycle reference. `scheduleMidnightReset()`: `viewModelScope` while+delay loop computes ms to next midnight, delays, calls `refreshObservers()`. `refreshObservers()` cleanly cancels and restarts both `observeTodayTasks()` and `observeExpiredTasks()` — this is the single source of truth for resetting all active observers. No AlarmManager, WorkManager, or BroadcastReceiver needed for a foreground-only offline app.

**6. Expiration ticker — grey out, never disappear** — tasks with a past `expiresAt` stay visible on today's screen but grey out (`onSurface` at 0.38f alpha per M3 disabled spec) and become non-interactive. A ~1-minute `viewModelScope` ticker updates `nowMillis` in `TodoUiState`; Compose reactively compares `todo.expiresAt` against `nowMillis` on each recomposition. Color transitions use `animateColorAsState` for smooth crossfades. Product reasoning: users benefit from seeing their full day's context. Expired tasks also flow into the history view.

**7. expiresAt as Long? in domain model — acknowledged leak** — `TodoEntity.expiresAt: Long?` was in the schema from day one (zero migration when the feature shipped). The domain `Todo` also holds it as `Long?` — technically a minor layer-boundary leak (the rest of the model uses proper types). Deliberate: avoided committing to a domain representation (`LocalTime?`) before the feature was built. Maps to `LocalTime?` in the repository mapping functions when expiration logic grows.

**8. Success-only sheet close via Channel** — `_taskAdded` and `_taskUpdated` are Kotlin `Channel`s exposed as `Flow`s for one-shot UI events. The ViewModel sends a `Unit` only when `AddTaskUseCase` succeeds. The UI collects this in a `LaunchedEffect` and sets `showSheet = false`. On validation error, nothing fires — the sheet stays open showing the error. Validation lives only in `AddTaskUseCase`, never duplicated in UI. This prevents the naive bug where closing on every click hides the error message.

**9. OnConflictStrategy.ABORT over REPLACE** — auto-generated IDs mean conflicts shouldn't occur in normal flow. `ABORT` fails loudly if one ever did, rather than `REPLACE` silently overwriting data.

**10. Only AddTaskUseCase; dropped trivial use-cases** — `GetTodayTasksUseCase` and `ToggleTaskCompletionUseCase` were dropped. They would have forwarded a single repository call with no validation, no transformation, no added value. `AddTaskUseCase` earns its place: it holds title validation (trim-then-validate, reject blank/whitespace, enforce 200-char cap). Applying layers only where they pay off is the judgment.

**11. Bottom sheet over separate screen** — avoids Navigation-Compose for a single extra surface. `skipPartiallyExpanded = true` so it always snaps fully open. History view is accessed via a TopAppBar toggle — FAB is hidden in history mode. History content uses `LazyColumn` with `stickyHeader`s, tasks grouped by `createdDate` via `remember { tasks.groupBy { it.createdDate } }` for efficient recomposition. Read-only: no checkboxes, no click listeners.

---

## What I Got Stuck On

**1. KSP version mismatch** — initial Gradle sync failed because KSP `2.2.10-1.0.29` didn't exist on Maven Central. Instead of guessing, queried Maven Central's KSP metadata XML directly to find the correct published version for Kotlin 2.2.10: `2.2.10-2.0.2`. Confirmed KSP's `<kotlin>-<ksp>` versioning scheme.

**2. ksp{} block in wrong scope** — the `ksp { arg("room.schemaLocation", ...) }` block was initially nested inside `defaultConfig {}`, which has no `ksp` method in scope. Flagged by a "suspicious receiver type" Kotlin warning. Moved to the script root. This also explained why the Room schema JSON wasn't being generated.

**3. android.disallowKotlinSourceSets=false — wrong fix at the right time** — this flag appeared twice: once as a speculative fix for a stale-cache error (rejected — it didn't match the actual error), and again when the real AGP 9 / KSP source-set conflict surfaced after the first real KSP codegen. Applied it correctly the second time because the error message itself prescribed it. The lesson: the same fix can be wrong or right depending on whether it matches the actual error.

**4. Midnight loop hanging the test runner** — `testDebugUnitTest` hung for 9+ minutes after 11 tests completed. Root cause: `scheduleMidnightReset()`'s while+delay loop in `viewModelScope` was kept alive by `runTest`'s scheduler, which waits for all coroutines to complete. Fixed test-side with a `runViewModelTest` helper that cancels `vm.viewModelScope` in a `finally` block. Channel emissions were tested via `backgroundScope.launch(UnconfinedTestDispatcher())` to collect one-shot events. Production code untouched.

**5. Success-only sheet close** — the naive fix (`showSheet = false` on every button click) closes the sheet even on validation errors, hiding the error message. Correct solution: ViewModel sends a one-shot `Channel` event only on `AddTaskUseCase` success; UI collects it in `LaunchedEffect` and closes the sheet. Keeps validation logic in exactly one place.

---

## What I'd Improve With More Time

- **Priority tags** — let users mark tasks as High / Medium / Low priority on creation or edit. Today screen would surface high-priority tasks at the top regardless of creation order.

- **AI task assistant** — an in-app assistant that summarizes what's left for the day and suggests task titles or descriptions based on partial input.

- **Deadline color grading** — tasks shift color as they approach their expiration time (or end of day if no expiration is set).

---

## AI Usage

See [`summary.md`](summary.md) for a curated index of key decisions, pushback moments, and manual changes. Full transcript in [`AI_TRANSCRIPT_1.md`](AI_TRANSCRIPT_1.md) and [`AI_TRANSCRIPT_2.md`](AI_TRANSCRIPT_2.md).
