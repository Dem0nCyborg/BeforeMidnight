╭─── Claude Code v2.1.191 ─────────────────────────────────────────────────────╮
│                                                    │ Tips for getting        │
│                Welcome back Chandan!               │ started                 │
│                                                    │ ✔ Run /init to create … │
│                       ▐▛███▜▌                      │ ─────────────────────── │
│                      ▝▜█████▛▘                     │ What's new              │
│                        ▘▘ ▝▝                       │ Added `/rewind` suppor… │
│      Sonnet 4.6 · Claude Pro ·                     │ Fixed scroll position … │
│      chandanbhopi1607@gmail.com's Organization     │ Fixed background agent… │
│                  ~/AndroidProjects                 │ /release-notes for more │
╰──────────────────────────────────────────────────────────────────────────────╯


 ▎ [✻] [✻] [✻] · Share Claude Code and earn $10 in usage credits · /passes

✻ Conversation compacted (ctrl+o for history)


⏺ Done. The CLAUDE.md captures everything a fresh context needs to pick up
  without re-deriving it:

  - The full package structure and the layer-boundary rule (no Room imports
  above the repository)
  - Every non-obvious decision with the why: epoch day, filter-not-delete, one
  use-case, Channel for sheet close, rememberSaveable for sheet state
  - The exact Step-1 Gradle fixes that must not be reverted (the four things
  that look wrong but aren't)
  - The KSP version format change (2.0.x not 1.0.x) which will trip up anyone
  who looks it up naively
  - The runViewModelTest / viewModelScope.cancel() test pattern and why it
  exists
  - A clear done-vs-left table so the next session can jump straight to the next
  step

✻ Brewed for 1m 37s

❯ /compact                                                                      
  ⎿  Compacted (ctrl+o to see full summary)
  ⎿  Read app/src/main/java/com/chandan/beforemidnight/MainActivity.kt (47 
     lines)
  ⎿  Read app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt 
     (232 lines)
  ⎿  Read app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt 
     (110 lines)
  ⎿  Read app/build.gradle.kts (71 lines)
  ⎿  Read gradle/libs.versions.toml (47 lines)
  ⎿  Plan file referenced 
     (~/.claude/plans/i-m-building-a-small-expressive-eclipse.md)
  ⎿  Skills restored (statusline)

❯ We will divide step 9 in two parts, the logic and the UI. Now lets start with 
  9a for the expiration filtering. I don't want the task to be dropped or       
  filtered out when the time passes. It's better to keep them visible on the    
  screen so the user can see the full day history, but we will make  blured it  
  out and unclickable. Which we will handle in step 9b. Instead of filtering    
  them out, update the ViewModel to run a lightweight ~1 min ticker that        
  exposes a currentTickerTimer or something similar to the TodoUiState. Map     
  expiresAt properly through the repo. We will use this ticking time in the     
  next step so the Compose UI can reactively compare expiresAt against the      
  ticker and grey out the expired tasks. Build this ViewModel ticker logic and  
  update the tests to verify the ticker state updates, then STOP for review     
  before we touch the UI.                                                       

⏺ I need to read a few files before finalizing the plan. Let me check the
  current state of the relevant pieces.

  Read 5 files (ctrl+o to expand)

⏺ Good. I have everything I need. The expiresAt field is already correctly
  mapped through the full stack — TodoEntity → toDomain() → Todo. All that's
  missing is the ticker in the ViewModel and nowMillis in the UI state.

  Let me update the plan file now.

  Read 1 file (ctrl+o to expand)

⏺ Updated plan
  ⎿  /plan to preview

⏺ Plan is ready. Here's the summary before I ask for approval:

  What's already correct (no changes needed):
  - Todo.expiresAt: Long?, TodoEntity.expiresAt: Long?, and the
  toDomain()/toEntity() mapping in TodoRepositoryImpl — the full stack is
  already wired correctly.

  What changes in 9a:

  ┌──────────────────────┬───────────────────────────────────────────────────┐
  │         File         │                       What                        │
  ├──────────────────────┼───────────────────────────────────────────────────┤
  │ TodoUiState.kt       │ Add nowMillis: Long = 0L                          │
  ├──────────────────────┼───────────────────────────────────────────────────┤
  │                      │ Init state with nowMillis =                       │
  │ TodoViewModel.kt     │ dateProvider.nowMillis(); add                     │
  │                      │ startExpirationTicker() (60s while-loop, same     │
  │                      │ pattern as midnight reset)                        │
  ├──────────────────────┼───────────────────────────────────────────────────┤
  │                      │ Replace computed nowMillis() with a mutable       │
  │ FakeDateProvider.kt  │ nowMillisValue field — tests can set it           │
  │                      │ independently from currentDate                    │
  ├──────────────────────┼───────────────────────────────────────────────────┤
  │                      │ Two new tests: initial value matches              │
  │ TodoViewModelTest.kt │ dateProvider, and after advanceTimeBy(60_001)     │
  │                      │ state is updated                                  │
  └──────────────────────┴───────────────────────────────────────────────────┘

  The ticker fires after the first 60s delay (not immediately), because
  nowMillis is already set to the correct value in the constructor. The
  runViewModelTest/viewModelScope.cancel() pattern that guards the midnight loop
  automatically protects the ticker loop too.

⏺ User approved Claude's plan
  ⎿  Plan saved to: ~/.claude/plans/i-m-building-a-small-expressive-eclipse.md ·
     /plan to edit
     BeforeMidnight — Step 9a: Expiration Ticker

     Context

     Steps 0–8 are complete. The expiresAt: Long? field already flows correctly
     through every layer:
     TodoEntity.expiresAt → toDomain() in TodoRepositoryImpl → Todo.expiresAt.
     No repository
     changes are needed.

     Step 9 is split into two parts:
     - 9a (this step): Add a ~60s ViewModel ticker that exposes current
     wall-clock time in TodoUiState as nowMillis: Long. No filtering; expired
     tasks stay visible. Tests verify initial state and ticker update.
     - 9b (next step): Use nowMillis in the Compose UI to grey out and disable
     tasks whose expiresAt has passed.

     ---
     What Changes

     1. ui/todo/TodoUiState.kt

     Add one field:
     val nowMillis: Long = 0L,
     The 0L default is safe — the ViewModel immediately overwrites it with
     dateProvider.nowMillis() in its init.

     2. ui/todo/TodoViewModel.kt

     Init block — pass nowMillis to the initial state:
     private val _uiState = MutableStateFlow(
         TodoUiState(currentDate = dateProvider.today(), nowMillis =
     dateProvider.nowMillis())
     )

     New private function called from init {} after scheduleMidnightReset():
     private fun startExpirationTicker() {
         viewModelScope.launch {
             while (true) {
                 delay(60_000L)
                 _uiState.update { it.copy(nowMillis = dateProvider.nowMillis())
     }
             }
         }
     }

     The delay comes first so the init-time value (set via the constructor) is
     used for the first render. The ticker is a while(true) loop in
     viewModelScope — the same pattern as scheduleMidnightReset(). It is a fixed
     60s interval, not clock-aligned (acceptable for expiry greying purposes).

     3. test/fake/FakeDateProvider.kt

     Add a mutable nowMillisValue field so tests can control the exact
     millisecond independently of the date:
     class FakeDateProvider(initialDate: LocalDate) : DateProvider {
         var currentDate: LocalDate = initialDate
         var nowMillisValue: Long =
     initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
         override fun today(): LocalDate = currentDate
         override fun nowMillis(): Long = nowMillisValue
     }
     Existing tests that only mutate currentDate and never assert on nowMillis
     are unaffected.

     4. test/ui/todo/TodoViewModelTest.kt

     Add two tests in a new // --- Expiration ticker --- section:

     Test 1 — initial value:
     @Test
     fun `nowMillis is initialised from dateProvider on start`() =
     runViewModelTest {
         assertEquals(dateProvider.nowMillis(), vm.uiState.value.nowMillis)
     }

     Test 2 — ticker fires after 60s:
     @Test
     fun `ticker updates nowMillis after 60 seconds`() = runViewModelTest {
         val laterMillis = dateProvider.nowMillisValue + 90_000L
         dateProvider.nowMillisValue = laterMillis

         advanceTimeBy(60_001L)

         assertEquals(laterMillis, vm.uiState.value.nowMillis)
     }
     The runViewModelTest wrapper (which cancels viewModelScope in finally)
     already prevents the ticker's while(true) loop from hanging the test
     scheduler — same mechanism as the midnight reset loop.

     ---
     Files to Modify

     ┌───────────────────────────────────┬──────────────────────────────────────
     ┐
     │               File                │                Change
     │
     ├───────────────────────────────────┼──────────────────────────────────────
     ┤
     │ ui/todo/TodoUiState.kt            │ Add val nowMillis: Long = 0L
     │
     ├───────────────────────────────────┼──────────────────────────────────────
     ┤
     │ ui/todo/TodoViewModel.kt          │ Init nowMillis in state; add
     │
     │                                   │ startExpirationTicker()
     │
     ├───────────────────────────────────┼──────────────────────────────────────
     ┤
     │ test/fake/FakeDateProvider.kt     │ Replace computed nowMillis() with
     │
     │                                   │ mutable nowMillisValue field
     │
     ├───────────────────────────────────┼──────────────────────────────────────
     ┤
     │ test/ui/todo/TodoViewModelTest.kt │ Add 2 ticker tests
     │
     └───────────────────────────────────┴──────────────────────────────────────
     ┘

     No changes to: TodoRepositoryImpl, Todo, TodoEntity, TodoDao — expiresAt is
     already wired end-to-end.

     ---
     Verification

     - ./gradlew test — all 11 tests pass (9 existing + 2 new ticker tests)
     - Confirm nowMillis appears in TodoUiState and is non-zero when ViewModel
     starts
     - Confirm FakeDateProvider change doesn't break DayResetTest or
     AddTaskUseCaseTest

⏺ Making all four changes now.

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoUiState.kt)
  ⎿  Added 1 line
       9      val isInputError: Boolean = false,
      10      val isLoading: Boolean = false,
      11      val currentDate: LocalDate = LocalDate.now(),
      12 +    val nowMillis: Long = 0L,                                  
      13  )

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 3 lines, removed 1 line
      26      private val dateProvider: DateProvider,
      27  ) : ViewModel() {
      28  
      29 -    private val _uiState = MutableStateFlow(TodoUiState(current
         -Date = dateProvider.today()))                                  
      29 +    private val _uiState = MutableStateFlow(                   
      30 +        TodoUiState(currentDate = dateProvider.today(), nowMill
         +is = dateProvider.nowMillis())                                 
      31 +    )                                                          
      32      val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow(
          )
      33  
      34      private var taskObserverJob: Job? = null

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 1 line
      40      init {
      41          observeTodayTasks()
      42          scheduleMidnightReset()
      43 +        startExpirationTicker()                                
      44      }
      45  
      46      // Called by the UI on every ON_RESUME. Refreshes the task 
          list if the date has

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 9 lines
       89          }
       90      }
       91  
       92 +    private fun startExpirationTicker() {                     
       93 +        viewModelScope.launch {                               
       94 +            while (true) {                                    
       95 +                delay(60_000L)                                
       96 +                _uiState.update { it.copy(nowMillis = dateProv
          +ider.nowMillis()) }                                           
       97 +            }                                                 
       98 +        }                                                     
       99 +    }                                                         
      100 +                                                              
      101      private fun scheduleMidnightReset() {
      102          viewModelScope.launch {
      103              while (true) {

⏺ Now update FakeDateProvider to use the mutable field:

⏺ Write(app/src/test/java/com/chandan/beforemidnight/fake/FakeDateProvider.kt)
  ⎿  Added 2 lines, removed 2 lines
       6  
       7  class FakeDateProvider(initialDate: LocalDate) : DateProvider {
       8      var currentDate: LocalDate = initialDate
       9 +    var nowMillisValue: Long = initialDate.atStartOfDay(ZoneOff
         +set.UTC).toInstant().toEpochMilli()                            
      10      override fun today(): LocalDate = currentDate
      10 -    override fun nowMillis(): Long =                           
      11 -        currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().to
         -EpochMilli()                                                   
      11 +    override fun nowMillis(): Long = nowMillisValue            
      12  }
⏺ Now add the two ticker tests to TodoViewModelTest.kt:

⏺ Update(app/src/test/java/com/chandan/beforemidnight/ui/todo/TodoViewModelTest.
        kt)
  ⎿  Added 1 line
       8  import kotlinx.coroutines.ExperimentalCoroutinesApi
       9  import kotlinx.coroutines.cancel
      10  import kotlinx.coroutines.test.TestScope
      11 +import kotlinx.coroutines.test.advanceTimeBy                   
      12  import kotlinx.coroutines.test.runTest
      13  import org.junit.Assert.assertEquals
      14  import org.junit.Assert.assertFalse

⏺ Update(app/src/test/java/com/chandan/beforemidnight/ui/todo/TodoViewModelTest.
        kt)
  ⎿  Added 17 lines
      150  
      151          assertFalse(vm.uiState.value.isLoading)
      152      }
      153 +                                                              
      154 +    // --- Expiration ticker ---                              
      155 +                                                              
      156 +    @Test                                                     
      157 +    fun `nowMillis is initialised from dateProvider on start`(
          +) = runViewModelTest {                                        
      158 +        assertEquals(dateProvider.nowMillis(), vm.uiState.valu
          +e.nowMillis)                                                  
      159 +    }                                                         
      160 +                                                              
      161 +    @Test                                                     
      162 +    fun `ticker updates nowMillis after 60 seconds`() = runVie
          +wModelTest {                                                  
      163 +        val laterMillis = dateProvider.nowMillisValue + 90_000
          +L                                                             
      164 +        dateProvider.nowMillisValue = laterMillis             
      165 +                                                              
      166 +        advanceTimeBy(60_001L)                                
      167 +                                                              
      168 +        assertEquals(laterMillis, vm.uiState.value.nowMillis) 
      169 +    }                                                         
      170  }

⏺ All four files are edited. Let me run the tests to confirm everything passes:

⏺ Bash(./gradlew test 2>&1 | tail -40)
  ⎿  > Configure project :app
     WARNING: The option setting 'android.disallowKotlinSourceSets=false' i
     s experimental.
     … +37 lines (ctrl+o to expand)
  ⎿  (timeout 2m)

⏺ All 11 tests pass. Let me do a quick check to verify the final state of the
  modified files looks correct:

  Read 1 file (ctrl+o to expand)

⏺ All 11 tests pass (BUILD SUCCESSFUL). Here's what was delivered:

  - TodoUiState — nowMillis: Long = 0L added as the last field
  - TodoViewModel — initial state now seeded with dateProvider.nowMillis();
  startExpirationTicker() added and called from init {} (delay-first, so the
  init-time value serves the first render)
  - FakeDateProvider — nowMillis() now backed by a mutable nowMillisValue field;
  existing tests are unaffected since they never assert on it
  - TodoViewModelTest — two new tests: initial value check, and a 60s
  advanceTimeBy check confirming the ticker fires and updates the state

  Ready for 9b whenever you are.

✻ Worked for 3m 1s

❯ Now lets work on the UI side of this feature in step 9b. First, update the    
  task rows in TodoScreen. Compare each task's expiresAt against the            
  uiState.nowMillis ticker. If the task is expired, render it visibly greyed    
  out and disable its checkbox, but keep it in the list. Second, implement the  
  tap-to-edit behavior. Tapping a task should open the existing bottom sheet in 
  an 'edit' mode. Inside that sheet, add a Material 3 TimePicker (or a clean    
  time selection UI) to let the user set, edit, or clear the same-day           
  expiration time. Wire the save action through the repository cleanly.         
  Maintain our success only sheet close pattern. Write the UI code and then     
  STOP so I can review.                                                         

⏺ Let me read the remaining files I need before writing.

  Read 2 files (ctrl+o to expand)

⏺ Good. I have everything I need. Implementing all pieces now — repository
  interface → impl → fake → ViewModel → UI — then MainActivity.

⏺ Update(app/src/main/java/com/chandan/beforemidnight/domain/repository/TodoRepo
        sitory.kt)
  ⎿  User rejected     app/src/main/java/com/chandan/beforemidnight/domain/repos
     update to         itory/TodoRepository.kt

❯ Rejecting this specific edit. Instead of a narrow updateExpiresAt function, I 
  want a generic suspend fun updateTask(todo: Todo) in the repository. Since    
  the bottom sheet will let the user edit both the title and the time at once,  
  we should just pass the modified domain model down to the DAO's existing      
  @Update method. Update the interface and the implementation.                  

⏺ Understood — a generic updateTask(todo: Todo) is cleaner and sets up the edit
  sheet to handle both title and expiration together. Let me update the plan to
  reflect this and the full 9b scope before we write anything.

⏺ Updated plan
  ⎿  /plan to preview

⏺ User approved Claude's plan
  ⎿  Plan saved to: ~/.claude/plans/i-m-building-a-small-expressive-eclipse.md ·
     /plan to edit
     BeforeMidnight — Step 9b: Expiration UI + Tap-to-Edit

     Context

     Step 9a added nowMillis: Long to TodoUiState via a 60s ticker. Step 9b
     wires the UI:
     expired tasks are visually greyed out and their checkboxes are disabled;
     tapping any task opens
     a bottom sheet to edit both the title and the same-day expiration time;
     saving passes the
     modified Todo domain model through a new generic updateTask repository
     method.

     ---
     Repository Layer (no new use-case)

     domain/repository/TodoRepository.kt

     Add one method below toggleCompletion:
     suspend fun updateTask(todo: Todo)
     Generic — callers pass the full modified domain model; the repository owns
     the entity mapping.

     data/repository/TodoRepositoryImpl.kt

     Implement using the existing toEntity() private extension and the DAO's
     existing @Update:
     override suspend fun updateTask(todo: Todo) =
         dao.update(todo.toEntity())
     No new DAO queries needed — @Update matches on primary key.

     test/fake/FakeTodoRepository.kt

     Replace the stored entry with the incoming Todo wholesale:
     override suspend fun updateTask(todo: Todo) {
         val idx = tasks.indexOfFirst { it.id == todo.id }
         if (idx != -1) {
             tasks[idx] = todo
             _flow.value = tasks.toList()
         }
     }

     ---
     ViewModel Layer

     ui/todo/TodoViewModel.kt

     Add alongside the existing _taskAdded channel:
     private val _taskUpdated = Channel<Unit>(Channel.BUFFERED)
     val taskUpdated = _taskUpdated.receiveAsFlow()

     Add function (success-only close, same pattern as onAddTask):
     fun onUpdateTask(todo: Todo) {
         viewModelScope.launch {
             try {
                 repository.updateTask(todo)
                 _taskUpdated.send(Unit)
             } catch (e: Exception) { /* sheet stays open on failure */ }
         }
     }
     No validation in the ViewModel — the save button is disabled in the UI when
     the title is blank
     or over 200 chars (a UI gate, not business validation).

     ---
     UI Layer

     ui/todo/TodoScreen.kt — signature change

     fun TodoScreen(
         uiState: TodoUiState,
         taskAdded: Flow<Unit>,
         taskUpdated: Flow<Unit>,          // ← new
         onInputChange: (String) -> Unit,
         onAddTask: () -> Unit,
         onToggle: (Todo) -> Unit,
         onUpdateTask: (Todo) -> Unit,     // ← new
         modifier: Modifier = Modifier,
     )

     Edit-sheet state (rotation-safe)

     var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
     val editingTask: Todo? = editingTaskId?.let { id -> uiState.tasks.find {
     it.id == id } }
     Long? is natively saveable. If the task disappears (date rolls over while
     sheet is open),
     editingTask becomes null and the sheet auto-closes — correct behaviour.

     Second LaunchedEffect to close edit sheet on success

     LaunchedEffect(Unit) {
         taskUpdated.collect { editingTaskId = null }
     }

     ModalBottomSheet for edit (shown when editingTask != null)

     if (editingTask != null) {
         ModalBottomSheet(
             onDismissRequest = { editingTaskId = null },
             sheetState = rememberModalBottomSheetState(skipPartiallyExpanded =
     true),
         ) {
             EditTaskSheetContent(
                 todo = editingTask,
                 currentDate = uiState.currentDate,
                 onUpdateTask = { updated -> onUpdateTask(updated) },
             )
         }
     }

     TodoItem changes

     - Signature: add nowMillis: Long, onEdit: () -> Unit; remove implicit
     onToggle from row click
     - isExpired = todo.expiresAt != null && todo.expiresAt <= nowMillis
     - Row clickable(onClick = onEdit) for all tasks (including expired — to
     clear/change expiry)
     - Checkbox onCheckedChange = if (isExpired) null else { _ -> onToggle() }
     (null = M3 disabled state)
     - Text color: onSurface.copy(alpha = 0.38f) when expired; onSurfaceVariant
     when completed; onSurface otherwise

     New EditTaskSheetContent composable

     Local state:
     var editTitle by remember(todo.id) { mutableStateOf(todo.title) }
     var hasExpiration by remember(todo.id) { mutableStateOf(todo.expiresAt !=
     null) }
     val (initHour, initMinute) = remember(todo.id) {
         todo.expiresAt?.let {
             val t =
     Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
             t.hour to t.minute
         } ?: (23 to 59)
     }
     val timePickerState = rememberTimePickerState(initialHour = initHour,
     initialMinute = initMinute)

     Layout (top-to-bottom in a scrollable Column):
     1. "Edit task" — titleMedium
     2. OutlinedTextField for title with "${editTitle.length}/200" supporting
     text right-aligned
     3. "Set expiration" row — label + Switch(checked = hasExpiration, ...)
     4. AnimatedVisibility(hasExpiration) containing TimePicker(state = 
     timePickerState)
     5. Button("Save", enabled = editTitle.trim().isNotBlank() && 
     editTitle.length <= 200) — full width

     On Save:
     val expiresAt = if (hasExpiration) {
         currentDate.atTime(timePickerState.hour, timePickerState.minute)
             .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
     } else null
     onUpdateTask(todo.copy(title = editTitle.trim(), expiresAt = expiresAt))

     No separate "Clear" button — turning off the Switch sets expiresAt = null
     on save.

     MainActivity.kt

     Add two params to TodoScreen(...):
     taskUpdated = vm.taskUpdated,
     onUpdateTask = vm::onUpdateTask,

     ---
     New Imports Needed in TodoScreen.kt

     - java.time.Instant, java.time.LocalDate, java.time.ZoneId
     - androidx.compose.material3.TimePicker, rememberTimePickerState
     - androidx.compose.material3.Switch
     - androidx.compose.animation.AnimatedVisibility
     - androidx.compose.foundation.layout.height
     - androidx.compose.foundation.rememberScrollState, verticalScroll

     AnimatedVisibility requires implementation(libs.androidx.compose.animation)
     — check if it's
     already in the BOM or needs an explicit dep. The Compose BOM 2026.02.01
     includes it; no new dep needed.

     ---
     Files to Modify

     ┌───────────────────────────────────────┬──────────────────────────────────
     ┐
     │                 File                  │              Change
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ domain/repository/TodoRepository.kt   │ Add suspend fun updateTask(todo:
     │
     │                                       │  Todo)
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ data/repository/TodoRepositoryImpl.kt │ Implement updateTask via
     │
     │                                       │ dao.update(todo.toEntity())
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ test/fake/FakeTodoRepository.kt       │ Implement updateTask
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ ui/todo/TodoViewModel.kt              │ Add _taskUpdated channel +
     │
     │                                       │ onUpdateTask
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ ui/todo/TodoScreen.kt                 │ TodoItem expiry rendering; edit
     │
     │                                       │ sheet; new callbacks
     │
     ├───────────────────────────────────────┼──────────────────────────────────
     ┤
     │ MainActivity.kt                       │ Wire taskUpdated + onUpdateTask
     │
     └───────────────────────────────────────┴──────────────────────────────────
     ┘

     ---
     Verification

     - ./gradlew test — existing 11 tests still pass (no new tests this step;
     UI-only)
     - Manually: tap a task → edit sheet opens with current title pre-filled,
     Switch off if no expiry
     - Set a near-future time, wait for ticker → task greys out, checkbox
     becomes untappable
     - Tap greyed-out task → sheet opens, turn Switch off → Save → expiry
     cleared, task re-activates
     - Rotation with edit sheet open → sheet survives, task still selected

⏺ Implementing all six files in order.

⏺ Update(app/src/main/java/com/chandan/beforemidnight/domain/repository/TodoRepo
        sitory.kt)
  ⎿  Added 1 line
       9      fun getTasksBeforeDay(date: LocalDate): Flow<List<Todo>>
      10      suspend fun addTask(title: String, date: LocalDate): Long
      11      suspend fun toggleCompletion(todo: Todo)
      12 +    suspend fun updateTask(todo: Todo)                         
      13  }

⏺ Update(app/src/main/java/com/chandan/beforemidnight/data/repository/TodoReposi
        toryImpl.kt)
  ⎿  Added 3 lines
      23  
      24      override suspend fun toggleCompletion(todo: Todo) =
      25          dao.update(todo.toEntity().copy(isCompleted = !todo.isC
          ompleted))
      26 +                                                               
      27 +    override suspend fun updateTask(todo: Todo) =              
      28 +        dao.update(todo.toEntity())                            
      29  }
      30  
      31  private fun TodoEntity.toDomain(): Todo = Todo(

⏺ Update(app/src/test/java/com/chandan/beforemidnight/fake/FakeTodoRepository.kt
        )
  ⎿  Added 8 lines
      35          }
      36      }
      37  
      38 +    override suspend fun updateTask(todo: Todo) {              
      39 +        val idx = tasks.indexOfFirst { it.id == todo.id }      
      40 +        if (idx != -1) {                                       
      41 +            tasks[idx] = todo                                  
      42 +            _flow.value = tasks.toList()                       
      43 +        }                                                      
      44 +    }                                                          
      45 +                                                               
      46      fun allTasks(): List<Todo> = tasks.toList()
      47  }

⏺ Now the ViewModel — add _taskUpdated channel and onUpdateTask:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 3 lines, removed 1 line
      34      private var taskObserverJob: Job? = null
      35  
      36      private val _taskAdded = Channel<Unit>(Channel.BUFFERED)
      37 -                                                               
      37      val taskAdded = _taskAdded.receiveAsFlow()
      38  
      39 +    private val _taskUpdated = Channel<Unit>(Channel.BUFFERED) 
      40 +    val taskUpdated = _taskUpdated.receiveAsFlow()             
      41 +                                                               
      42      init {
      43          observeTodayTasks()
      44          scheduleMidnightReset()

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 11 lines
      75          }
      76      }
      77  
      78 +    fun onUpdateTask(todo: Todo) {                             
      79 +        viewModelScope.launch {                                
      80 +            try {                                              
      81 +                repository.updateTask(todo)                    
      82 +                _taskUpdated.send(Unit)                        
      83 +            } catch (e: Exception) {                           
      84 +                // Sheet stays open on failure; user can retry.
      85 +            }                                                  
      86 +        }                                                      
      87 +    }                                                          
      88 +                                                               
      89      fun onToggle(todo: Todo) {
      90          viewModelScope.launch {
      91              repository.toggleCompletion(todo)
⏺ Now the big one — TodoScreen.kt. Writing it completely:

⏺ Write(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 147 lines, removed 15 lines
      1  package com.chandan.beforemidnight.ui.todo
      2  
      3 +import androidx.compose.animation.AnimatedVisibility            
      4  import androidx.compose.foundation.clickable
      5  import androidx.compose.foundation.layout.Arrangement
      6  import androidx.compose.foundation.layout.Box
     ...
      10  import androidx.compose.foundation.layout.Spacer
      11  import androidx.compose.foundation.layout.fillMaxSize
      12  import androidx.compose.foundation.layout.fillMaxWidth
      13 +import androidx.compose.foundation.layout.height               
      14  import androidx.compose.foundation.layout.imePadding
      15  import androidx.compose.foundation.layout.padding
      16  import androidx.compose.foundation.layout.width
      17  import androidx.compose.foundation.lazy.LazyColumn
      18  import androidx.compose.foundation.lazy.items
      19 +import androidx.compose.foundation.rememberScrollState         
      20  import androidx.compose.foundation.text.KeyboardActions
      21  import androidx.compose.foundation.text.KeyboardOptions
      22 +import androidx.compose.foundation.verticalScroll              
      23  import androidx.compose.material.icons.Icons
      24  import androidx.compose.material.icons.filled.Add
      25  import androidx.compose.material3.Button
     ...
      32  import androidx.compose.material3.ModalBottomSheet
      33  import androidx.compose.material3.OutlinedTextField
      34  import androidx.compose.material3.Scaffold
      35 +import androidx.compose.material3.Switch                       
      36  import androidx.compose.material3.Text
      37 +import androidx.compose.material3.TimePicker                   
      38  import androidx.compose.material3.TopAppBar
      39  import androidx.compose.material3.rememberModalBottomSheetState
      40 +import androidx.compose.material3.rememberTimePickerState      
      41  import androidx.compose.runtime.Composable
      42  import androidx.compose.runtime.LaunchedEffect
      43  import androidx.compose.runtime.getValue
     ...
      54  import androidx.compose.ui.unit.dp
      55  import com.chandan.beforemidnight.domain.model.Todo
      56  import kotlinx.coroutines.flow.Flow
      57 +import java.time.Instant                                       
      58 +import java.time.LocalDate                                     
      59 +import java.time.ZoneId                                        
      60  import java.time.format.DateTimeFormatter
      61  
      62  @OptIn(ExperimentalMaterial3Api::class)
     ...
      64  fun TodoScreen(
      65      uiState: TodoUiState,
      66      taskAdded: Flow<Unit>,
      67 +    taskUpdated: Flow<Unit>,                                   
      68      onInputChange: (String) -> Unit,
      69      onAddTask: () -> Unit,
      70      onToggle: (Todo) -> Unit,
      71 +    onUpdateTask: (Todo) -> Unit,                              
      72      modifier: Modifier = Modifier,
      73  ) {
      62 -                                                               
      63 -                                                               
      74      var showAddSheet by rememberSaveable { mutableStateOf(false
          ) }
      65 -    val sheetState = rememberModalBottomSheetState(skipPartiall
         -yExpanded = true)                                              
      75 +    var editingTaskId by rememberSaveable { mutableStateOf<Long
         +?>(null) }                                                     
      76 +    val editingTask: Todo? = editingTaskId?.let { id -> uiState
         +.tasks.find { it.id == id } }                                  
      77 +    val addSheetState = rememberModalBottomSheetState(skipParti
         +allyExpanded = true)                                           
      78      val dateFormatter = remember { DateTimeFormatter.ofPattern(
          "EEEE, MMMM d") }
      79  
      80      LaunchedEffect(Unit) {
      69 -        taskAdded.collect {                                    
      70 -            showAddSheet = false                               
      71 -        }                                                      
      81 +        taskAdded.collect { showAddSheet = false }             
      82      }
      83  
      84 +    LaunchedEffect(Unit) {                                     
      85 +        taskUpdated.collect { editingTaskId = null }           
      86 +    }                                                          
      87 +                                                               
      88      Scaffold(
      89          modifier = modifier,
      90          topBar = {
     ...
      116                      contentPadding = PaddingValues(vertical = 
           8.dp),
      117                  ) {
      118                      items(uiState.tasks, key = { it.id }) { to
           do ->
      105 -                        TodoItem(todo = todo, onToggle = { onT
          -oggle(todo) })                                                
      119 +                        TodoItem(                             
      120 +                            todo = todo,                      
      121 +                            nowMillis = uiState.nowMillis,    
      122 +                            onToggle = { onToggle(todo) },    
      123 +                            onEdit = { editingTaskId = todo.id
          + },                                                           
      124 +                        )                                     
      125                          HorizontalDivider(modifier = Modifier.
           padding(start = 56.dp))
      126                      }
      127                  }
     ...
      135                  showAddSheet = false
      136                  onInputChange("")
      137              },
      119 -            sheetState = sheetState,                          
      138 +            sheetState = addSheetState,                       
      139          ) {
      140              AddTaskSheetContent(
      141                  inputText = uiState.inputText,
     ...
      146              )
      147          }
      148      }
      149 +                                                              
      150 +    if (editingTask != null) {                                
      151 +        ModalBottomSheet(                                     
      152 +            onDismissRequest = { editingTaskId = null },      
      153 +            sheetState = rememberModalBottomSheetState(skipPar
          +tiallyExpanded = true),                                       
      154 +        ) {                                                   
      155 +            EditTaskSheetContent(                             
      156 +                todo = editingTask,                           
      157 +                currentDate = uiState.currentDate,            
      158 +                onUpdateTask = onUpdateTask,                  
      159 +            )                                                 
      160 +        }                                                     
      161 +    }                                                         
      162  }
      163  
      164  @Composable
      165  private fun TodoItem(
      166      todo: Todo,
      167 +    nowMillis: Long,                                          
      168      onToggle: () -> Unit,
      169 +    onEdit: () -> Unit,                                       
      170  ) {
      171 +    val isExpired = todo.expiresAt != null && todo.expiresAt <
          += nowMillis                                                   
      172 +    val textColor = when {                                    
      173 +        isExpired -> MaterialTheme.colorScheme.onSurface.copy(
          +alpha = 0.38f)                                                
      174 +        todo.isCompleted -> MaterialTheme.colorScheme.onSurfac
          +eVariant                                                      
      175 +        else -> MaterialTheme.colorScheme.onSurface           
      176 +    }                                                         
      177 +                                                              
      178      Row(
      179          modifier = Modifier
      180              .fillMaxWidth()
      140 -            .clickable(onClick = onToggle)                    
      181 +            .clickable(onClick = onEdit)                      
      182              .padding(horizontal = 16.dp, vertical = 4.dp),
      183          verticalAlignment = Alignment.CenterVertically,
      184      ) {
      185          Checkbox(
      186              checked = todo.isCompleted,
      146 -            onCheckedChange = { onToggle() },                 
      187 +            onCheckedChange = if (isExpired) null else { _ -> 
          +onToggle() },                                                 
      188          )
      189          Spacer(modifier = Modifier.width(8.dp))
      190          Text(
      191              text = todo.title,
      192              style = MaterialTheme.typography.bodyLarge,
      193              textDecoration = if (todo.isCompleted) TextDecorat
           ion.LineThrough else null,
      153 -            color = if (todo.isCompleted) {                   
      154 -                MaterialTheme.colorScheme.onSurfaceVariant    
      155 -            } else {                                          
      156 -                MaterialTheme.colorScheme.onSurface           
      157 -            },                                                
      194 +            color = textColor,                                
      195          )
      196      }
      197  }
     ...
      266          }
      267      }
      268  }
      269 +                                                              
      270 +@OptIn(ExperimentalMaterial3Api::class)                       
      271 +@Composable                                                   
      272 +private fun EditTaskSheetContent(                             
      273 +    todo: Todo,                                               
      274 +    currentDate: LocalDate,                                   
      275 +    onUpdateTask: (Todo) -> Unit,                             
      276 +    modifier: Modifier = Modifier,                            
      277 +) {                                                           
      278 +    var editTitle by remember(todo.id) { mutableStateOf(todo.t
          +itle) }                                                       
      279 +    var hasExpiration by remember(todo.id) { mutableStateOf(to
          +do.expiresAt != null) }                                       
      280 +                                                              
      281 +    val (initHour, initMinute) = remember(todo.id) {          
      282 +        todo.expiresAt?.let { millis ->                       
      283 +            val t = Instant.ofEpochMilli(millis).atZone(ZoneId
          +.systemDefault()).toLocalTime()                               
      284 +            t.hour to t.minute                                
      285 +        } ?: (23 to 59)                                       
      286 +    }                                                         
      287 +    val timePickerState = rememberTimePickerState(            
      288 +        initialHour = initHour,                               
      289 +        initialMinute = initMinute,                           
      290 +    )                                                         
      291 +                                                              
      292 +    val isSaveEnabled = editTitle.trim().isNotBlank() && editT
          +itle.length <= 200                                            
      293 +                                                              
      294 +    Column(                                                   
      295 +        modifier = modifier                                   
      296 +            .fillMaxWidth()                                   
      297 +            .verticalScroll(rememberScrollState())            
      298 +            .padding(horizontal = 24.dp)                      
      299 +            .padding(bottom = 32.dp),                         
      300 +        verticalArrangement = Arrangement.spacedBy(12.dp),    
      301 +    ) {                                                       
      302 +        Text(                                                 
      303 +            text = "Edit task",                               
      304 +            style = MaterialTheme.typography.titleMedium,     
      305 +        )                                                     
      306 +        OutlinedTextField(                                    
      307 +            value = editTitle,                                
      308 +            onValueChange = { editTitle = it },               
      309 +            modifier = Modifier.fillMaxWidth(),               
      310 +            label = { Text("Task title") },                   
      311 +            supportingText = {                                
      312 +                Text(                                         
      313 +                    text = "${editTitle.length}/200",         
      314 +                    modifier = Modifier.fillMaxWidth(),       
      315 +                    textAlign = TextAlign.End,                
      316 +                )                                             
      317 +            },                                                
      318 +            singleLine = true,                                
      319 +            keyboardOptions = KeyboardOptions(                
      320 +                capitalization = KeyboardCapitalization.Senten
          +ces,                                                          
      321 +                imeAction = ImeAction.Done,                   
      322 +            ),                                                
      323 +        )                                                     
      324 +        Row(                                                  
      325 +            modifier = Modifier.fillMaxWidth(),               
      326 +            verticalAlignment = Alignment.CenterVertically,   
      327 +        ) {                                                   
      328 +            Text(                                             
      329 +                text = "Set expiration",                      
      330 +                style = MaterialTheme.typography.bodyLarge,   
      331 +                modifier = Modifier.weight(1f),               
      332 +            )                                                 
      333 +            Switch(                                           
      334 +                checked = hasExpiration,                      
      335 +                onCheckedChange = { hasExpiration = it },     
      336 +            )                                                 
      337 +        }                                                     
      338 +        AnimatedVisibility(visible = hasExpiration) {         
      339 +            Column(horizontalAlignment = Alignment.CenterHoriz
          +ontally) {                                                    
      340 +                Spacer(modifier = Modifier.height(4.dp))      
      341 +                TimePicker(state = timePickerState)           
      342 +            }                                                 
      343 +        }                                                     
      344 +        Button(                                               
      345 +            onClick = {                                       
      346 +                val expiresAt = if (hasExpiration) {          
      347 +                    currentDate                               
      348 +                        .atTime(timePickerState.hour, timePick
          +erState.minute)                                               
      349 +                        .atZone(ZoneId.systemDefault())       
      350 +                        .toInstant()                          
      351 +                        .toEpochMilli()                       
      352 +                } else {                                      
      353 +                    null                                      
      354 +                }                                             
      355 +                onUpdateTask(todo.copy(title = editTitle.trim(
          +), expiresAt = expiresAt))                                    
      356 +            },                                                
      357 +            enabled = isSaveEnabled,                          
      358 +            modifier = Modifier.fillMaxWidth(),               
      359 +        ) {                                                   
      360 +            Text("Save")                                      
      361 +        }                                                     
      362 +    }                                                         
      363 +}                                                             

⏺ Now wire the two new params in MainActivity:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/MainActivity.kt)
  ⎿  Error editing file

  Read 1 file (ctrl+o to expand)

⏺ Update(app/src/main/java/com/chandan/beforemidnight/MainActivity.kt)
  ⎿  Added 3 lines, removed 1 line
      35  
      36                  TodoScreen(
      37                      uiState = uiState,
      38 -                    onInputChange = vm::onInputChange,         
      38                      taskAdded = vm.taskAdded,
      39 +                    taskUpdated = vm.taskUpdated,              
      40 +                    onInputChange = vm::onInputChange,         
      41                      onAddTask = vm::onAddTask,
      42                      onToggle = vm::onToggle,
      43 +                    onUpdateTask = vm::onUpdateTask,           
      44                  )
      45              }
      46          }

⏺ Now run the tests to confirm nothing is broken:

⏺ Bash(./gradlew test 2>&1 | tail -20)
  ⎿  > Task :app:processDebugMainManifest UP-TO-DATE
     > Task :app:processDebugManifest UP-TO-DATE
     > Task :app:processDebugManifestForPackage UP-TO-DATE
     … +17 lines (ctrl+o to expand)
  ⎿  (timeout 2m)

⏺ All 11 tests pass. Here's what was built:

  Repository layer:
  - TodoRepository — suspend fun updateTask(todo: Todo) added
  - TodoRepositoryImpl — delegates to dao.update(todo.toEntity()) using the
  existing private extension
  - FakeTodoRepository — replaces the stored entry wholesale with the incoming
  Todo

  ViewModel:
  - _taskUpdated / taskUpdated channel — same pattern as _taskAdded, fires only
  on success
  - onUpdateTask(todo: Todo) — calls repository.updateTask, sends to channel on
  success; sheet stays open on failure

  TodoScreen:
  - TodoItem — row tap opens edit sheet; isExpired flag greys out text
  (onSurface @38% alpha) and passes null to Checkbox.onCheckedChange (Material
  3's built-in disabled state)
  - Edit sheet shows with the task's current title pre-filled and the expiration
  Switch pre-set from expiresAt. Turning the Switch off and saving sets
  expiresAt = null. AnimatedVisibility wraps the TimePicker so it slides in/out
  when the Switch is toggled
  - Save button disabled when editTitle is blank or over 200 chars
  - editingTaskId: Long? in rememberSaveable — survives rotation; if the task
  disappears (date rolls), the derived editingTask becomes null and the sheet
  closes automatically

✻ Churned for 3m 11s

❯ Awesome, that works perfectly. Let's move to Step 10: The Expired View. To    
  keep navigation lightweight, let's just add a 'History' icon action to the    
  TopAppBar in TodoScreen. Tapping it should toggle the UI between the 'Today'  
  view and an 'Expired' view.First, update the ViewModel to also collect        
  repository.getTasksBeforeDay(dateProvider.today()) and expose it to the UI    
  state.Second, build the Expired UI. It must be strictly read-only: no         
  checkboxes, no tap-to-edit, and hide the Add FAB when viewing it. Group the   
  expired tasks by their createdDate using sticky headers or dividers so it     
  acts like a clean logbook. Write the ViewModel updates and the Compose UI     
  changes, then STOP so I can review. Let me know if there can be any           
  improvements that I may have missed.                                          

⏺ Let me read the current ViewModel and think through the date-reset
  implications before writing the plan.

  Read 1 file (ctrl+o to expand)

⏺ Good. I can see the three places observeTodayTasks() is called — all three
  need to also restart observeExpiredTasks() when the date changes, because
  getTasksBeforeDay(today) uses a date parameter that changes at midnight.
  Writing the plan now.

⏺ Updated plan
  ⎿  /plan to preview
  ⎿  Error: You are not in plan mode. To enter plan mode, call the EnterPlanMode
     tool first. If your plan was already approved, continue with 
     implementation.

⏺ I wrote the plan to the file but wasn't in plan mode, so ExitPlanMode failed.
  Let me present the plan directly.

  ---
  Here's the Step 10 implementation plan. The key pieces:

  Dependency: material-icons-extended needed for Icons.Filled.History — adds one
  line to libs.versions.toml and app/build.gradle.kts.

  ViewModel changes:
  - Add expiredTasks: List<Todo> to TodoUiState
  - Add observeExpiredTasks() / expiredTasksJob (same cancel-and-restart pattern
  as observeTodayTasks())
  - Extract a refreshObservers() helper that calls both, replacing the three
  direct observeTodayTasks() calls in init, onResume(), and
  scheduleMidnightReset()

  UI changes in TodoScreen:
  - var showHistory by rememberSaveable { false } — toggle state
  - TopAppBar gains an actions block: History icon (→ open) / Close icon (→
  return); title switches to "History" when active
  - FAB is hidden when showHistory = true
  - A new HistoryContent composable using LazyColumn with stickyHeader blocks —
  groups expiredTasks by createdDate using remember(expiredTasks) { groupBy 
  {...} } so regrouping only happens when data changes
  - DateSectionHeader uses a Surface(surfaceVariant) background so scrolling
  items don't bleed through the sticky header
  - HistoryItem — plain text, no Checkbox, not clickable

  Improvements you may have missed:

  1. refreshObservers() is load-bearing, not just convenient. Without restarting
  observeExpiredTasks() at midnight, the boundary stays at yesterday's epoch
  day and newly-expired tasks won't appear in History until a cold restart.
  2. History ≠ same-day expired tasks. Tasks created today that expire by time
  (greyed out) stay in the Today view until midnight — only then do they appear
  in History. Worth confirming this is the UX you want.
  3. No expiry time shown in History items. A task with "expiresAt = 14:00"
  shows no trace of that in the logbook. A secondary line like "Expired 2:00 PM"
  could be useful — flag for a polish pass.
  4. Eager subscription. observeExpiredTasks() starts in init even if History is
  never opened. Fine for a personal app; could be deferred to first open if
  needed.
  5. Extended icons APK size. R8 shrinks it to only referenced icons in release
  builds — negligible with shrinking on, meaningful without it.

  Shall I proceed with the implementation?

✻ Worked for 3m 39s

❯ This plan is excellent, especially the refreshObservers() catch for the       
  midnight reset. Proceed with implementation, but make two small adjustments.  
  First is skip adding the material-icons-extended dependency. Just use an      
  existing standard icon like Icons.Default.DateRange or Icons.Default.List to  
  keep the build clean. And then another is Implement your suggestion #3: If a  
  task had an expiresAt time, display it as a secondary line in the HistoryItem 
  so the logbook has complete context.                                          

⏺ Implementing directly. The two adjustments — Icons.Default.DateRange from core
  (no new dep), and expiry secondary line in HistoryItem — are baked into
  everything below.

  Starting with the simpler files, then the ViewModel refactor, then the screen.

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoUiState.kt)
  ⎿  Added 1 line
       5  
       6  data class TodoUiState(
       7      val tasks: List<Todo> = emptyList(),
       8 +    val expiredTasks: List<Todo> = emptyList(),                
       9      val inputText: String = "",
      10      val isInputError: Boolean = false,
      11      val isLoading: Boolean = false,

⏺ Now the ViewModel — add the job, the two new private functions,
  refreshObservers(), and swap all three call sites:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 1 line
      32      val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow(
          )
      33  
      34      private var taskObserverJob: Job? = null
      35 +    private var expiredTasksJob: Job? = null                   
      36  
      37      private val _taskAdded = Channel<Unit>(Channel.BUFFERED)
      38      val taskAdded = _taskAdded.receiveAsFlow()

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 1 line, removed 1 line
      41      val taskUpdated = _taskUpdated.receiveAsFlow()
      42  
      43      init {
      44 -        observeTodayTasks()                                    
      44 +        refreshObservers()                                     
      45          scheduleMidnightReset()
      46          startExpirationTicker()
      47      }

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 1 line, removed 1 line
      51      // holding a Lifecycle reference.
      52      fun onResume() {
      53          if (dateProvider.today() != _uiState.value.currentDate)
           {
      54 -            observeTodayTasks()                                
      54 +            refreshObservers()                                 
      55          }
      56      }
      57  

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)  ⎿  Added 15 lines
       93          }
       94      }
       95  
       96 +    private fun refreshObservers() {                          
       97 +        observeTodayTasks()                                   
       98 +        observeExpiredTasks()                                 
       99 +    }                                                         
      100 +                                                              
      101      private fun observeTodayTasks() {
      102          taskObserverJob?.cancel()
      103          taskObserverJob = viewModelScope.launch {
     ...
      108          }
      109      }
      110  
      111 +    private fun observeExpiredTasks() {                       
      112 +        expiredTasksJob?.cancel()                             
      113 +        expiredTasksJob = viewModelScope.launch {             
      114 +            repository.getTasksBeforeDay(dateProvider.today())
      115 +                .collect { tasks ->                           
      116 +                    _uiState.update { it.copy(expiredTasks = t
          +asks) }                                                       
      117 +                }                                             
      118 +        }                                                     
      119 +    }                                                         
      120 +                                                              
      121      private fun startExpirationTicker() {
      122          viewModelScope.launch {
      123              while (true) {

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoViewModel.kt)
  ⎿  Added 1 line, removed 1 line
      133                  val now = LocalDateTime.now()
      134                  val nextMidnight = now.toLocalDate().plusDays(
           1).atStartOfDay()
      135                  delay(ChronoUnit.MILLIS.between(now, nextMidni
           ght))
      136 -                observeTodayTasks()                           
      136 +                refreshObservers()                            
      137              }
      138          }
      139      }

⏺ Now TodoScreen.kt — rewriting it completely to add the History toggle,
  composables, and expiry line:

⏺ Write(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 151 lines, removed 23 lines
      22  import androidx.compose.foundation.verticalScroll
      23  import androidx.compose.material.icons.Icons
      24  import androidx.compose.material.icons.filled.Add
      25 +import androidx.compose.material.icons.filled.Close            
      26 +import androidx.compose.material.icons.filled.DateRange        
      27  import androidx.compose.material3.Button
      28  import androidx.compose.material3.Checkbox
      29  import androidx.compose.material3.ExperimentalMaterial3Api
      30  import androidx.compose.material3.FloatingActionButton
      31  import androidx.compose.material3.HorizontalDivider
      32  import androidx.compose.material3.Icon
      33 +import androidx.compose.material3.IconButton                   
      34  import androidx.compose.material3.MaterialTheme
      35  import androidx.compose.material3.ModalBottomSheet
      36  import androidx.compose.material3.OutlinedTextField
      37  import androidx.compose.material3.Scaffold
      38 +import androidx.compose.material3.Surface                      
      39  import androidx.compose.material3.Switch
      40  import androidx.compose.material3.Text
      41  import androidx.compose.material3.TimePicker
     ...
      62  import java.time.LocalDate
      63  import java.time.ZoneId
      64  import java.time.format.DateTimeFormatter
      65 +import java.time.format.FormatStyle                            
      66  
      67  @OptIn(ExperimentalMaterial3Api::class)
      68  @Composable
     ...
      77      modifier: Modifier = Modifier,
      78  ) {
      79      var showAddSheet by rememberSaveable { mutableStateOf(false
          ) }
      80 +    var showHistory by rememberSaveable { mutableStateOf(false)
         + }                                                             
      81      var editingTaskId by rememberSaveable { mutableStateOf<Long
          ?>(null) }
      82      val editingTask: Todo? = editingTaskId?.let { id -> uiState
          .tasks.find { it.id == id } }
      83      val addSheetState = rememberModalBottomSheetState(skipParti
          allyExpanded = true)
     ...
       97              TopAppBar(
       98                  title = {
       99                      Text(
       94 -                        text = uiState.currentDate.format(date
          -Formatter),                                                   
      100 +                        text = if (showHistory) "History" else
          + uiState.currentDate.format(dateFormatter),                   
      101                          style = MaterialTheme.typography.title
           Large,
      102                      )
      103                  },
      104 +                actions = {                                   
      105 +                    IconButton(onClick = { showHistory = !show
          +History }) {                                                  
      106 +                        Icon(                                 
      107 +                            imageVector = if (showHistory) Ico
          +ns.Default.Close else Icons.Default.DateRange,                
      108 +                            contentDescription = if (showHisto
          +ry) "Back to today" else "View history",                      
      109 +                        )                                     
      110 +                    }                                         
      111 +                },                                            
      112              )
      113          },
      114          floatingActionButton = {
      101 -            FloatingActionButton(onClick = { showAddSheet = tr
          -ue }) {                                                       
      102 -                Icon(imageVector = Icons.Default.Add, contentD
          -escription = "Add task")                                      
      115 +            if (!showHistory) {                               
      116 +                FloatingActionButton(onClick = { showAddSheet 
          += true }) {                                                   
      117 +                    Icon(imageVector = Icons.Default.Add, cont
          +entDescription = "Add task")                                  
      118 +                }                                             
      119              }
      120          },
      121      ) { innerPadding ->
      106 -        Box(                                                  
      107 -            modifier = Modifier                               
      108 -                .fillMaxSize()                                
      109 -                .padding(innerPadding),                       
      110 -        ) {                                                   
      111 -            if (uiState.tasks.isEmpty()) {                    
      112 -                EmptyState(modifier = Modifier.align(Alignment
          -.Center))                                                     
      113 -            } else {                                          
      114 -                LazyColumn(                                   
      115 -                    modifier = Modifier.fillMaxSize(),        
      116 -                    contentPadding = PaddingValues(vertical = 
          -8.dp),                                                        
      117 -                ) {                                           
      118 -                    items(uiState.tasks, key = { it.id }) { to
          -do ->                                                         
      119 -                        TodoItem(                             
      120 -                            todo = todo,                      
      121 -                            nowMillis = uiState.nowMillis,    
      122 -                            onToggle = { onToggle(todo) },    
      123 -                            onEdit = { editingTaskId = todo.id
          - },                                                           
      124 -                        )                                     
      125 -                        HorizontalDivider(modifier = Modifier.
          -padding(start = 56.dp))                                       
      122 +        if (showHistory) {                                    
      123 +            HistoryContent(                                   
      124 +                expiredTasks = uiState.expiredTasks,          
      125 +                modifier = Modifier                           
      126 +                    .fillMaxSize()                            
      127 +                    .padding(innerPadding),                   
      128 +            )                                                 
      129 +        } else {                                              
      130 +            Box(                                              
      131 +                modifier = Modifier                           
      132 +                    .fillMaxSize()                            
      133 +                    .padding(innerPadding),                   
      134 +            ) {                                               
      135 +                if (uiState.tasks.isEmpty()) {                
      136 +                    EmptyState(modifier = Modifier.align(Align
          +ment.Center))                                                 
      137 +                } else {                                      
      138 +                    LazyColumn(                               
      139 +                        modifier = Modifier.fillMaxSize(),    
      140 +                        contentPadding = PaddingValues(vertica
          +l = 8.dp),                                                    
      141 +                    ) {                                       
      142 +                        items(uiState.tasks, key = { it.id }) 
          +{ todo ->                                                     
      143 +                            TodoItem(                         
      144 +                                todo = todo,                  
      145 +                                nowMillis = uiState.nowMillis,
      146 +                                onToggle = { onToggle(todo) },
      147 +                                onEdit = { editingTaskId = tod
          +o.id },                                                       
      148 +                            )                                 
      149 +                            HorizontalDivider(modifier = Modif
          +ier.padding(start = 56.dp))                                   
      150 +                        }                                     
      151                      }
      152                  }
      153              }
     ...
      186      }
      187  }
      188  
      189 +// ----- Today view -----                                     
      190 +                                                              
      191  @Composable
      192  private fun TodoItem(
      193      todo: Todo,
     ...
      243      }
      244  }
      245  
      246 +// ----- History view -----                                   
      247 +                                                              
      248  @Composable
      249 +private fun HistoryContent(                                   
      250 +    expiredTasks: List<Todo>,                                 
      251 +    modifier: Modifier = Modifier,                            
      252 +) {                                                           
      253 +    if (expiredTasks.isEmpty()) {                             
      254 +        Box(modifier = modifier, contentAlignment = Alignment.
          +Center) {                                                     
      255 +            Column(                                           
      256 +                modifier = Modifier.padding(32.dp),           
      257 +                horizontalAlignment = Alignment.CenterHorizont
          +ally,                                                         
      258 +                verticalArrangement = Arrangement.spacedBy(8.d
          +p),                                                           
      259 +            ) {                                               
      260 +                Text(                                         
      261 +                    text = "No past tasks yet",               
      262 +                    style = MaterialTheme.typography.titleMedi
          +um,                                                           
      263 +                    color = MaterialTheme.colorScheme.onSurfac
          +eVariant,                                                     
      264 +                )                                             
      265 +                Text(                                         
      266 +                    text = "Completed days will appear here", 
      267 +                    style = MaterialTheme.typography.bodyMediu
          +m,                                                            
      268 +                    color = MaterialTheme.colorScheme.onSurfac
          +eVariant,                                                     
      269 +                )                                             
      270 +            }                                                 
      271 +        }                                                     
      272 +    } else {                                                  
      273 +        // groupBy preserves insertion order (LinkedHashMap); 
          +DAO orders DESC by date so                                    
      274 +        // most-recent past day is at the top of the logbook. 
      275 +        val grouped = remember(expiredTasks) { expiredTasks.gr
          +oupBy { it.createdDate } }                                    
      276 +        LazyColumn(                                           
      277 +            modifier = modifier,                              
      278 +            contentPadding = PaddingValues(bottom = 16.dp),   
      279 +        ) {                                                   
      280 +            grouped.forEach { (date, tasks) ->                
      281 +                stickyHeader(key = "header_$date") {          
      282 +                    DateSectionHeader(date = date)            
      283 +                }                                             
      284 +                items(tasks, key = { it.id }) { todo ->       
      285 +                    HistoryItem(todo = todo)                  
      286 +                    HorizontalDivider(modifier = Modifier.padd
          +ing(start = 16.dp))                                           
      287 +                }                                             
      288 +            }                                                 
      289 +        }                                                     
      290 +    }                                                         
      291 +}                                                             
      292 +                                                              
      293 +@Composable                                                   
      294 +private fun DateSectionHeader(date: LocalDate) {              
      295 +    val formatter = remember { DateTimeFormatter.ofPattern("EE
          +EE, MMMM d") }                                                
      296 +    // Surface gives the header an opaque background so scroll
          +ing items don't bleed through.                                
      297 +    Surface(color = MaterialTheme.colorScheme.surfaceVariant) 
          +{                                                             
      298 +        Text(                                                 
      299 +            text = date.format(formatter),                    
      300 +            style = MaterialTheme.typography.labelLarge,      
      301 +            color = MaterialTheme.colorScheme.onSurfaceVariant
          +,                                                             
      302 +            modifier = Modifier                               
      303 +                .fillMaxWidth()                               
      304 +                .padding(horizontal = 16.dp, vertical = 8.dp),
      305 +        )                                                     
      306 +    }                                                         
      307 +}                                                             
      308 +                                                              
      309 +@Composable                                                   
      310 +private fun HistoryItem(todo: Todo) {                         
      311 +    val expiryLabel = remember(todo.expiresAt) {              
      312 +        todo.expiresAt?.let { millis ->                       
      313 +            val time = Instant.ofEpochMilli(millis)           
      314 +                .atZone(ZoneId.systemDefault())               
      315 +                .toLocalTime()                                
      316 +            "Expired ${time.format(DateTimeFormatter.ofLocaliz
          +edTime(FormatStyle.SHORT))}"                                  
      317 +        }                                                     
      318 +    }                                                         
      319 +                                                              
      320 +    Column(                                                   
      321 +        modifier = Modifier                                   
      322 +            .fillMaxWidth()                                   
      323 +            .padding(horizontal = 16.dp, vertical = 12.dp),   
      324 +        verticalArrangement = Arrangement.spacedBy(2.dp),     
      325 +    ) {                                                       
      326 +        Text(                                                 
      327 +            text = todo.title,                                
      328 +            style = MaterialTheme.typography.bodyLarge,       
      329 +            textDecoration = if (todo.isCompleted) TextDecorat
          +ion.LineThrough else null,                                    
      330 +            color = if (todo.isCompleted) MaterialTheme.colorS
          +cheme.onSurfaceVariant                                        
      331 +                    else MaterialTheme.colorScheme.onSurface, 
      332 +        )                                                     
      333 +        if (expiryLabel != null) {                            
      334 +            Text(                                             
      335 +                text = expiryLabel,                           
      336 +                style = MaterialTheme.typography.bodySmall,   
      337 +                color = MaterialTheme.colorScheme.onSurfaceVar
          +iant,                                                         
      338 +            )                                                 
      339 +        }                                                     
      340 +    }                                                         
      341 +}                                                             
      342 +                                                              
      343 +// ----- Add sheet -----                                      
      344 +                                                              
      345 +@Composable                                                   
      346  private fun AddTaskSheetContent(
      347      inputText: String,
      348      isInputError: Boolean,
     ...
      393      }
      394  }
      395  
      396 +// ----- Edit sheet -----                                     
      397 +                                                              
      398  @OptIn(ExperimentalMaterial3Api::class)
      399  @Composable
      400  private fun EditTaskSheetContent(

⏺ Bash(./gradlew test 2>&1 | tail -25)
  ⎿  > Task :app:mergeDebugResources UP-TO-DATE
     > Task :app:processDebugNavigationResources UP-TO-DATE
     > Task :app:compileDebugNavigationResources UP-TO-DATE
     … +22 lines (ctrl+o to expand)
  ⎿  (timeout 2m)

⏺ All 11 tests pass. Here's what was delivered:

  ViewModel:
  - expiredTasksJob alongside taskObserverJob
  - observeExpiredTasks() — same cancel-and-restart pattern, queries
  getTasksBeforeDay(today)
  - refreshObservers() — calls both; replaces the three direct
  observeTodayTasks() calls in init, onResume(), and scheduleMidnightReset()

  TodoUiState: expiredTasks: List<Todo> = emptyList()

  TodoScreen:
  - showHistory: Boolean in rememberSaveable — toggles between views, survives
  rotation
  - TopAppBar: DateRange icon → enter History; Close icon → exit; title switches
  to "History"
  - FAB: hidden when showHistory = true
  - HistoryContent: uses remember(expiredTasks) { groupBy { createdDate } } then
  stickyHeader + items blocks per day. The remember key means regrouping only
  happens on actual data change, not every recomposition
  - DateSectionHeader: wrapped in Surface(surfaceVariant) so scrolling items
  don't bleed through the sticky header
  - HistoryItem: no Checkbox, not clickable. Shows expiry secondary line
  ("Expired 2:00 PM") using DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
  — respects the device's 12/24h locale setting. The remember(todo.expiresAt)
  key means the format call only runs when the expiry actually changes

✻ Worked for 2m 50s

❯ All the feature implementation is done, now lets polisht the UI. We are going 
  to enhance the existing UI without changing any logic or navigation. In       
  TodoItem, use animateColorAsState for the text color transitions (Normal ->   
  Completed and Normal -> Expired). Use LocalHapticFeedback to trigger a subtle 
  vibration (HapticFeedbackType.LongPress or TextHandleMove) when the checkbox  
  is toggled and when the 'Save' button in the bottom sheets is clicked.        
  Verify that our Material 3 surface colors and the grayed out color for        
  expired tasks remain legible in both Light and Dark mode. Write the polish    
  code and then STOP so I can review.                                           

⏺ All purely UI-side changes, no logic touched. I'll do four targeted edits to
  TodoScreen.kt: add imports, animate TodoItem's text color, add haptic to the
  checkbox, and add haptic to both Save buttons.

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 1 line
      1  package com.chandan.beforemidnight.ui.todo
      2  
      3  import androidx.compose.animation.AnimatedVisibility
      4 +import androidx.compose.animation.animateColorAsState           
      5  import androidx.compose.foundation.clickable
      6  import androidx.compose.foundation.layout.Arrangement
      7  import androidx.compose.foundation.layout.Box

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 2 lines
      52  import androidx.compose.runtime.setValue
      53  import androidx.compose.ui.Alignment
      54  import androidx.compose.ui.Modifier
      55 +import androidx.compose.ui.hapticfeedback.HapticFeedbackType   
      56 +import androidx.compose.ui.platform.LocalHapticFeedback        
      57  import androidx.compose.ui.text.input.ImeAction
      58  import androidx.compose.ui.text.input.KeyboardCapitalization
      59  import androidx.compose.ui.text.style.TextAlign

⏺ Now TodoItem — animated color + haptic on the checkbox:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 10 lines, removed 2 lines
      198      onToggle: () -> Unit,
      199      onEdit: () -> Unit,
      200  ) {
      201 +    val haptic = LocalHapticFeedback.current                  
      202      val isExpired = todo.expiresAt != null && todo.expiresAt <
           = nowMillis
      202 -    val textColor = when {                                    
      203 +    val targetTextColor = when {                              
      204          isExpired -> MaterialTheme.colorScheme.onSurface.copy(
           alpha = 0.38f)
      205          todo.isCompleted -> MaterialTheme.colorScheme.onSurfac
           eVariant
      206          else -> MaterialTheme.colorScheme.onSurface
      207      }
      208 +    val textColor by animateColorAsState(                     
      209 +        targetValue = targetTextColor,                        
      210 +        label = "todoItemTextColor",                          
      211 +    )                                                         
      212  
      213      Row(
      214          modifier = Modifier
     ...
      219      ) {
      220          Checkbox(
      221              checked = todo.isCompleted,
      217 -            onCheckedChange = if (isExpired) null else { _ -> 
          -onToggle() },                                                 
      222 +            onCheckedChange = if (isExpired) null else { _ -> 
      223 +                haptic.performHapticFeedback(HapticFeedbackTyp
          +e.LongPress)                                                  
      224 +                onToggle()                                    
      225 +            },                                                
      226          )
      227          Spacer(modifier = Modifier.width(8.dp))
      228          Text(

⏺ Haptic on the Add task button:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 1 line
      362      onAddTask: () -> Unit,
      363      modifier: Modifier = Modifier,
      364  ) {
      365 +    val haptic = LocalHapticFeedback.current                  
      366      Column(
      367          modifier = modifier
      368              .fillMaxWidth()

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 4 lines, removed 1 line
      396              keyboardActions = KeyboardActions(onDone = { onAdd
           Task() }),
      397          )
      398          Button(
      399 -            onClick = onAddTask,                              
      399 +            onClick = {                                       
      400 +                haptic.performHapticFeedback(HapticFeedbackTyp
          +e.LongPress)                                                  
      401 +                onAddTask()                                   
      402 +            },                                                
      403              enabled = !isLoading,
      404              modifier = Modifier.fillMaxWidth(),
      405          ) {

⏺ Haptic on the Edit sheet Save button:

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 1 line
      418      onUpdateTask: (Todo) -> Unit,
      419      modifier: Modifier = Modifier,
      420  ) {
      421 +    val haptic = LocalHapticFeedback.current                  
      422      var editTitle by remember(todo.id) { mutableStateOf(todo.t
           itle) }
      423      var hasExpiration by remember(todo.id) { mutableStateOf(to
           do.expiresAt != null) }
      424  

⏺ Update(app/src/main/java/com/chandan/beforemidnight/ui/todo/TodoScreen.kt)
  ⎿  Added 1 line
      487          }
      488          Button(
      489              onClick = {
      490 +                haptic.performHapticFeedback(HapticFeedbackTyp
          +e.LongPress)                                                  
      491                  val expiresAt = if (hasExpiration) {
      492                      currentDate
      493                          .atTime(timePickerState.hour, timePick
           erState.minute)

⏺ Bash(./gradlew test 2>&1 | tail -10)
  ⎿  > Task :app:kspDebugUnitTestKotlin
     > Task :app:compileDebugUnitTestKotlin
     > Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
     … +7 lines (ctrl+o to expand)
  ⎿  (timeout 2m)

⏺ All 11 tests pass. Here's what changed:

  animateColorAsState in TodoItem:
  - targetTextColor holds the three-way conditional (onSurface /
  onSurfaceVariant / onSurface@38%)
  - val textColor by animateColorAsState(targetValue = targetTextColor, label = 
  "todoItemTextColor") wraps it — Compose's animation system crossfades between
  colors whenever isCompleted or isExpired flips
  - The label parameter feeds the Animation Inspector in Android Studio

  LocalHapticFeedback placements:
  - TodoItem: captured once at the top of the composable; fired in the
  onCheckedChange lambda just before onToggle() — only reachable when isExpired 
  = false, so expired tasks never vibrate
  - AddTaskSheetContent: fires on "Add task" click before delegating to
  onAddTask()
  - EditTaskSheetContent: fires on "Save" click before computing expiresAt and
  calling onUpdateTask() — the button is enabled = isSaveEnabled so the haptic
  only fires on a valid, actionable save

  Light / Dark mode color analysis:

  Color usage: onSurface (normal task)
  Light: Near-black on white
  Dark: Near-white on near-black
  Verdict: ✅ MD3 guaranteed
  ────────────────────────────────────────
  Color usage: onSurfaceVariant (completed task)
  Light: Muted dark on light
  Dark: Muted light on dark
  Verdict: ✅ MD3 guaranteed pair
  ────────────────────────────────────────
  Color usage: onSurface.copy(0.38f) (expired task)
  Light: ~2.5:1 contrast
  Dark: ~2.5:1 contrast
  Verdict: ⚠️ intentionally below AA — this is the MD3 disabled-state opacity,
    signalling "inactive" by design
  ────────────────────────────────────────
  Color usage: surfaceVariant bg + onSurfaceVariant text (history headers)
  Light: ✅ MD3 paired
  Dark: ✅ MD3 paired
  Verdict: ✅

  The 0.38f alpha is the Material 3 spec value for disabled content — it
  intentionally trades legibility for the visual signal that the element is
  inactive. If you'd prefer a higher floor (e.g. 0.50f or 0.60f) to keep expired
  tasks more readable, that's a one-line change in TodoItem.

✻ Worked for 2m 26s