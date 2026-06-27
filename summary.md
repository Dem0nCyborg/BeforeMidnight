# AI Usage Summary

## How I Used It

I used Claude (claude.ai) as a coding partner throughout. I wrote the prompts, reviewed every output, and made the final call on every decision. The app reflects my architectural choices — AI was used as the implementation tool.

---

## Pushback Moments

**1. AI included a 30-day DELETE/pruning job in the initial plan → Removed it entirely → It directly contradicted the filter-never-delete architecture and would have silently broken the history view.** The whole point of keeping old rows is that the history view gets them for free via `createdAtEpochDay < today`. A background delete job would have undermined that. Removed it from the prompt before implementation started.

**2. AI used GlobalScope for coroutines in an early draft → Banned it explicitly → GlobalScope leaks beyond the ViewModel lifecycle.** It's a known Android anti-pattern — coroutines launched on GlobalScope survive screen rotation and process-level cleanup. Replaced with `viewModelScope` throughout and added the constraint to subsequent prompts.

**3. AI generated GetTodayTasksUseCase and ToggleTaskCompletionUseCase → Dropped both → They were single-line pass-throughs with no logic.** A use-case that does nothing but forward one repository call adds a layer without adding value. `AddTaskUseCase` earns its place because it owns real validation: trim, blank check, 200-char cap. The other two didn't. Cutting them was a deliberate judgment call, not an oversight.

**4. AI's midnight reset plan only covered the while+delay timer → Added the `onResume()` path explicitly → The timer alone doesn't cover the "backgrounded past midnight" case.** If the user backgrounds the app at 23:59 and returns at 00:01, the timer already fired while paused and the UI is stale. I specified that the ViewModel must expose a public `onResume()` called from `LifecycleResumeEffect` in the UI — keeping the ViewModel lifecycle-unaware while still handling the resume case. This is the path that actually matters for the demo.

**5. AI used the deprecated `kotlinOptions { jvmTarget }` DSL → Removed it → Deprecated in Kotlin 2.2; AGP 9 derives JVM target from `compileOptions` automatically.** Flagged the warning, AI removed the block on the next pass. The setting was redundant and the DSL requires `kotlin-android` in scope, which itself causes a plugin conflict under AGP 9's built-in Kotlin mode.

**6. AI placed `ksp { arg("room.schemaLocation", ...) }` inside `defaultConfig {}` → Moved it to the script root manually → That scope has no `ksp` method.** The error surfaced as a "suspicious receiver type" Kotlin warning. Fixed it directly without a round-trip to AI — it was a straightforward scope issue.

**7. AI suggested `android.disallowKotlinSourceSets=false` as a fix for a stale config cache error early on → Rejected it at the time, applied it correctly later → The same flag was the wrong fix for the first error and the right fix for a different one.** The first error was unrelated; adding the flag speculatively would have masked the real issue. The second time, the actual AGP 9 / KSP source-set conflict surfaced and the error message itself called for this flag. The distinction mattered.

**8. Considered routing `LocalDateTime.now()` through `DateProvider` inside `scheduleMidnightReset()` → Kept the direct call → That function computes a delay duration, not a business logic value.** `DateProvider` exists so tests can control "what day is today" — a business concept. The milliseconds-until-midnight calculation is scheduling mechanics. Pulling it through the abstraction would be over-engineering. Noted it as deliberate in the code and in `CLAUDE.md`.

**9. When tests hung for 9+ minutes, AI investigated the wrong thing → I diagnosed the root cause myself → The `while+delay` loop in `viewModelScope` was being kept alive by `runTest`'s scheduler.** `runTest` calls `advanceUntilIdle()` after the test body, which drives the coroutine scheduler until all pending work is drained. The infinite loop never drains. Fixed test-side with a `runViewModelTest` helper that cancels `vm.viewModelScope` in a `finally` block before the scheduler runs. Production code untouched.

**10. AI's initial approach closed the bottom sheet on every button click → Designed the Channel-based signal myself → Closing on every click hides the validation error from the user.** If the title is blank and the user taps "Add", the sheet must stay open so they can see the error. The fix: `_taskAdded` is a Kotlin `Channel<Unit>` that the ViewModel sends to only on `AddTaskUseCase` success. The UI collects it in a `LaunchedEffect` and sets `showSheet = false`. Validation error = no emission = sheet stays open.

---

## Changes I Made Without AI

- Added `ZoneId.systemDefault()` explicitly to `SystemDateProvider.today()` — makes the timezone decision visible in code rather than implicit
- Created `BeforeMidnightApp.kt` and registered it in `AndroidManifest.xml` manually (faster than a round-trip to AI)
- Moved the `ksp {}` block from `defaultConfig` to the script root after catching the "suspicious receiver type" warning
- Added `import androidx.compose.runtime.getValue` to fix the `by collectAsStateWithLifecycle()` delegate error
- Changed `supportingText` from hardcoded `"Title can't be blank"` to a live character counter (`"n/200"`)
- Designed and implemented the success-only sheet close (Channel in ViewModel + LaunchedEffect in UI) without AI after understanding the design requirement
