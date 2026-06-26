# BeforeMidnight — Project Guide

Single-day Todo app. Tasks are created for today only, filtered by date (never deleted), and
disappear automatically at midnight. Old tasks stay in Room permanently for a future Expired view.

---

## Architecture

**Pattern:** MVVM with strict layer separation. No Hilt — manual DI only.

```
app/src/main/java/com/chandan/beforemidnight/
├── data/
│   ├── local/           # Room: TodoDatabase, TodoDao, entity/TodoEntity
│   └── repository/      # TodoRepositoryImpl (only place that touches TodoEntity)
├── domain/
│   ├── model/           # Todo.kt — pure Kotlin, zero Android/Room imports
│   ├── repository/      # TodoRepository interface
│   └── usecase/         # AddTaskUseCase (only use-case; owns all validation)
├── di/
│   └── AppContainer.kt  # Manual DI; lazily constructs the whole graph
├── util/
│   ├── DateProvider.kt  # interface: today(): LocalDate + nowMillis(): Long
│   └── SystemDateProvider.kt
├── ui/
│   ├── theme/           # M3 theme (generated, untouched)
│   └── todo/            # TodoUiState, TodoViewModel + Factory, TodoScreen
├── BeforeMidnightApp.kt # Application subclass; owns AppContainer by lazy
└── MainActivity.kt      # Wires ViewModel, LifecycleResumeEffect, TodoScreen
```

**Layer rule:** the domain layer has zero `androidx`/Room imports. Only `TodoRepositoryImpl`
crosses the entity↔domain boundary. Nothing above the repository ever sees `TodoEntity`.

---

## Key Decisions & Conventions

### Date storage — epoch day, not strings
`TodoEntity.createdAtEpochDay: Long` uses `LocalDate.toEpochDay()`. One integer, no locale
ambiguity, no TypeConverter, trivially queryable with `WHERE createdAtEpochDay = :epochDay`.
`expiresAt: Long?` stores epoch millis (it's a wall-clock instant, not a date). Both conversions
live exclusively in `TodoRepositoryImpl` — nowhere else in the codebase converts dates.

### Filter, never delete
Old tasks are never removed from Room. The Today screen queries `WHERE createdAtEpochDay = today`.
Old rows are invisible to the user but available to the future Expired view via
`TodoDao.getTasksBeforeDay()`. There are no DELETE queries anywhere.

### Injectable DateProvider
`DateProvider` has two methods: `today(): LocalDate` and `nowMillis(): Long`.
Production code uses `SystemDateProvider`. Tests use `FakeDateProvider` (mutable `currentDate`).
`nowMillis()` is on the interface now so the fake can control it when `expiresAt` filtering ships.
`LocalDateTime.now()` is used directly only inside `scheduleMidnightReset()` to compute a delay
duration — that is scheduling logic, not business logic, so it intentionally bypasses DateProvider.

### Manual DI — AppContainer
`AppContainer(context)` constructs the whole graph with `by lazy`. `database` and `dao` are
private; `repository`, `dateProvider`, and `addTaskUseCase` are public val so the ViewModel can
reach them. `BeforeMidnightApp` owns a single `AppContainer` instance. `MainActivity` casts
`application as BeforeMidnightApp` to reach it.

### Only one use-case
`GetTodayTasksUseCase` and `ToggleTaskCompletionUseCase` were dropped — they were single-line
repository forwarders with no logic. The ViewModel calls `repository.getTasksForDay()` and
`repository.toggleCompletion()` directly. `AddTaskUseCase` stays because it owns meaningful
validation: trim → blank check → 200-char cap → insert. Validation lives only here, never
duplicated in the UI.

### Success-only sheet close — Channel event
`TodoViewModel` has a `Channel<Unit>(Channel.BUFFERED)` named `_taskAdded`, exposed as
`val taskAdded = _taskAdded.receiveAsFlow()`. It is `send(Unit)` **only** on the happy path
(after `addTaskUseCase` succeeds, before `finally`). The UI (`TodoScreen`) collects it in a
`LaunchedEffect(Unit)` and sets `showAddSheet = false`. This means: validation errors keep the
sheet open so the user can fix the title; successful adds close it immediately. The Add button is
`enabled = !isLoading` (the `isLoading` guard prevents double-submission; validation stays in the
use-case, not in the button's enabled state).

### supportingText — character counter
`OutlinedTextField` in `AddTaskSheetContent` always shows `"${inputText.length}/200"` right-aligned
via `supportingText`. No conditional error-vs-counter swap; the counter is always visible.

### Midnight reset — three paths
Every scenario is covered without AlarmManager or WorkManager:

| Scenario | Mechanism |
|---|---|
| Cold start | `init {}` calls `observeTodayTasks()` which reads `dateProvider.today()` |
| App in foreground as clock crosses midnight | `scheduleMidnightReset()` — `while(true)` loop with `delay(millisUntilMidnight)` in `viewModelScope` |
| App resumed after being backgrounded past midnight | `LifecycleResumeEffect(Unit)` in `MainActivity` calls `vm.onResume()` |

`onResume()` is a plain public function on the ViewModel — no Lifecycle reference inside the VM.
`observeTodayTasks()` cancels the previous `taskObserverJob` before starting a new collect, so
there is never more than one active Room Flow collector.

### showAddSheet — rememberSaveable
`showAddSheet` uses `rememberSaveable` so the sheet survives configuration changes (rotation).

---

## Toolchain & Build Notes

| Thing | Value |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 9.2.1 |
| KSP | 2.2.10-**2.0.2** (new versioning scheme — not `1.0.x`) |
| Compose BOM | 2026.02.01 |
| Room | 2.7.1 |
| Lifecycle (VM + runtime-compose) | 2.9.1 |
| Coroutines | 1.10.2 |
| minSdk | 28 · targetSdk/compileSdk 36 |

**Step-1 fixes that must not be reverted:**

- `android.disallowKotlinSourceSets=false` in `gradle.properties` — KSP adds generated sources
  via Kotlin's `sourceSets` DSL which AGP 9's built-in Kotlin mode disallows by default.
- `ksp { arg("room.schemaLocation", ...) }` is a **top-level** block in `app/build.gradle.kts`,
  not inside `android { defaultConfig { } }` — the formatter moved it there and that's correct.
- The `kotlin-android` plugin is **not** declared — it is applied transitively by `kotlin.compose`
  and adding it explicitly causes "extension 'kotlin' already registered" error.
- `kotlinOptions { jvmTarget = "11" }` is **not** present — it requires `kotlin-android` in scope;
  AGP 9 aligns the Kotlin JVM target from `compileOptions` automatically.
- `material-icons-core` is declared separately from `material3` — it is not pulled in transitively.
- Room schema JSON is exported to `app/schemas/`. **Commit this file** — it is the baseline for
  future `MigrationTestHelper` tests. Do not gitignore `schemas/`.

---

## Test Infrastructure

```
app/src/test/java/com/chandan/beforemidnight/
├── fake/
│   ├── FakeDateProvider.kt      # mutable currentDate; nowMillis() anchored to UTC start of day
│   └── FakeTodoRepository.kt    # MutableStateFlow-backed; toggleCompletion uses !todo.isCompleted
├── util/
│   └── MainDispatcherRule.kt    # TestWatcher; swaps Dispatchers.Main ↔ UnconfinedTestDispatcher
├── domain/usecase/
│   └── AddTaskUseCaseTest.kt    # pure JVM; blank/whitespace/length/trim/boundary
├── todo/
│   └── DayResetTest.kt          # pure JVM; repo-level filter contract, getTasksBeforeDay
└── ui/todo/
    └── TodoViewModelTest.kt     # ViewModel tests; uses runViewModelTest helper
```

**`runViewModelTest` helper** (in `TodoViewModelTest`):
```kotlin
private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
    try { block() } finally { vm.viewModelScope.cancel() }
}
```
Cancels `viewModelScope` before `runTest`'s `advanceUntilIdle()` drains the scheduler.
Without this, the `scheduleMidnightReset()` `while(true)` loop causes a test hang.
**Production `scheduleMidnightReset()` is unchanged** — the fix is test-side only.

**`FakeTodoRepository.toggleCompletion`** uses `!todo.isCompleted` (from the argument), not
`!tasks[idx].isCompleted` (from storage), mirroring `TodoRepositoryImpl` exactly.

---

## What Is Done

- **Step 0** — Project scaffolding (BeforeMidnight, package `com.chandan.beforemidnight`)
- **Step 1** — Build files: KSP, Room, Lifecycle, Coroutines, material-icons-core; all Step-1 fixes applied
- **Step 2** — `DateProvider` interface + `SystemDateProvider`
- **Step 3** — Room layer: `TodoEntity` (with `expiresAt: Long?` slot), `TodoDao`, `TodoDatabase` (`exportSchema = true`)
- **Step 4** — Domain layer: `Todo` model, `TodoRepository` interface, `TodoRepositoryImpl` (private mapping extensions)
- **Step 5** — `AddTaskUseCase`, `AppContainer` (manual DI, lazy)
- **Step 6** — `BeforeMidnightApp`, `AndroidManifest.xml` (`android:name=".BeforeMidnightApp"`)
- **Step 7** — `TodoUiState`, `TodoViewModel` (Channel + midnight loop + onResume), `TodoViewModelFactory`
- **Step 8** — `TodoScreen` (stateless, M3, ModalBottomSheet, character counter), `MainActivity` (LifecycleResumeEffect)
- **Tests** — `AddTaskUseCaseTest`, `DayResetTest`, `TodoViewModelTest` (9 cases), all pure JVM

---

## What Is Left

- **Expiration feature** — `expiresAt: Long?` is already in the Room schema (no migration needed).
  Needs: UI to set a time, DAO query updated to `AND (expiresAt IS NULL OR expiresAt > :nowMillis)`,
  `DateProvider.nowMillis()` wired into the filter.
- **Expired view** — read-only screen using `TodoDao.getTasksBeforeDay()` (already on the DAO).
  Tasks grouped by date, no interaction except scroll.
- **UI polish** — animations (task completion strike-through transition), theming pass,
  accessibility labels, dark mode verification.
- **Additional tests** — Channel/sheet-close behaviour, `taskAdded` emission on success only,
  timezone-change edge case for midnight reset.
- **Release config** — enable minification/shrinking in `buildTypes.release`, signing config,
  Play Store metadata.
