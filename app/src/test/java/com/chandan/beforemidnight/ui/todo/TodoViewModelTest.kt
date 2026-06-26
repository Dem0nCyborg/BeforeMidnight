package com.chandan.beforemidnight.ui.todo

import androidx.lifecycle.viewModelScope
import com.chandan.beforemidnight.domain.usecase.AddTaskUseCase
import com.chandan.beforemidnight.fake.FakeDateProvider
import com.chandan.beforemidnight.fake.FakeTodoRepository
import com.chandan.beforemidnight.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val day1 = LocalDate.of(2026, 6, 26)
    private val day2 = day1.plusDays(1)

    private lateinit var dateProvider: FakeDateProvider
    private lateinit var repo: FakeTodoRepository
    private lateinit var vm: TodoViewModel

    @Before
    fun setUp() {
        dateProvider = FakeDateProvider(day1)
        repo = FakeTodoRepository()
        vm = TodoViewModel(
            repository = repo,
            addTaskUseCase = AddTaskUseCase(repo, dateProvider),
            dateProvider = dateProvider,
        )
    }

    // Cancels viewModelScope before runTest drains the scheduler, preventing the
    // midnight delay loop from being advanced indefinitely by advanceUntilIdle().
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Day reset (through ViewModel) ---

    @Test
    fun `onResume after date advances refreshes task list and updates currentDate`() = runViewModelTest {
        repo.addTask("Buy groceries", day1)
        repo.addTask("Read a book", day1)
        assertEquals(2, vm.uiState.value.tasks.size)

        dateProvider.currentDate = day2
        vm.onResume()

        assertEquals(0, vm.uiState.value.tasks.size)
        assertEquals(day2, vm.uiState.value.currentDate)
    }

    @Test
    fun `onResume when the date has not changed does not reset the task list`() = runViewModelTest {
        repo.addTask("Buy groceries", day1)
        assertEquals(1, vm.uiState.value.tasks.size)

        // Date unchanged — onResume should be a no-op
        vm.onResume()

        assertEquals(1, vm.uiState.value.tasks.size)
        assertEquals(day1, vm.uiState.value.currentDate)
    }

    // --- Toggle completion ---

    @Test
    fun `onToggle flips isCompleted from false to true`() = runViewModelTest {
        repo.addTask("Buy groceries", day1)
        val task = vm.uiState.value.tasks.single()
        assertFalse(task.isCompleted)

        vm.onToggle(task)

        assertTrue(vm.uiState.value.tasks.single().isCompleted)
    }

    @Test
    fun `onToggle flips isCompleted back to false on second call`() = runViewModelTest {
        repo.addTask("Buy groceries", day1)
        val task = vm.uiState.value.tasks.single()

        vm.onToggle(task)
        val toggled = vm.uiState.value.tasks.single()
        assertTrue(toggled.isCompleted)

        vm.onToggle(toggled)
        assertFalse(vm.uiState.value.tasks.single().isCompleted)
    }

    // --- onAddTask error handling ---

    @Test
    fun `onAddTask with blank title sets isInputError to true`() = runViewModelTest {
        vm.onInputChange("")
        vm.onAddTask()

        assertTrue(vm.uiState.value.isInputError)
    }

    @Test
    fun `onAddTask with whitespace-only title sets isInputError to true`() = runViewModelTest {
        vm.onInputChange("   ")
        vm.onAddTask()

        assertTrue(vm.uiState.value.isInputError)
    }

    @Test
    fun `onAddTask with valid title clears input and does not set isInputError`() = runViewModelTest {
        vm.onInputChange("Buy milk")
        vm.onAddTask()

        assertEquals("", vm.uiState.value.inputText)
        assertFalse(vm.uiState.value.isInputError)
        assertEquals(1, vm.uiState.value.tasks.size)
    }

    @Test
    fun `onInputChange after an error clears isInputError`() = runViewModelTest {
        vm.onInputChange("")
        vm.onAddTask()
        assertTrue(vm.uiState.value.isInputError)

        vm.onInputChange("B")
        assertFalse(vm.uiState.value.isInputError)
    }

    @Test
    fun `isLoading is false after a successful add`() = runViewModelTest {
        vm.onInputChange("Buy milk")
        vm.onAddTask()

        assertFalse(vm.uiState.value.isLoading)
    }
}
