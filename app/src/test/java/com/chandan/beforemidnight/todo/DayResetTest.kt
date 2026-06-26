package com.chandan.beforemidnight.todo

import com.chandan.beforemidnight.domain.usecase.AddTaskUseCase
import com.chandan.beforemidnight.fake.FakeDateProvider
import com.chandan.beforemidnight.fake.FakeTodoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies the core day-reset contract at the use-case + repository level,
 * with no ViewModel, no Room, and no real time.
 */
class DayResetTest {

    private val day1 = LocalDate.of(2026, 6, 26)
    private val day2 = day1.plusDays(1)

    private lateinit var dateProvider: FakeDateProvider
    private lateinit var repo: FakeTodoRepository
    private lateinit var addTask: AddTaskUseCase

    @Before
    fun setUp() {
        dateProvider = FakeDateProvider(day1)
        repo = FakeTodoRepository()
        addTask = AddTaskUseCase(repo, dateProvider)
    }

    @Test
    fun `tasks added today are visible on today's query`() = runTest {
        addTask("Buy groceries")
        addTask("Read a book")

        val todayTasks = repo.getTasksForDay(day1).first()
        assertEquals(2, todayTasks.size)
    }

    @Test
    fun `tasks from yesterday return zero results after the date advances`() = runTest {
        addTask("Buy groceries")
        addTask("Read a book")

        // Simulate midnight: advance the fake date to the next day
        dateProvider.currentDate = day2

        val tomorrowTasks = repo.getTasksForDay(dateProvider.today()).first()
        assertEquals(0, tomorrowTasks.size)
    }

    @Test
    fun `tasks are retained in the repository after the date advances`() = runTest {
        addTask("Buy groceries")
        addTask("Read a book")

        dateProvider.currentDate = day2

        // Old tasks must still exist in storage — they are filtered, not deleted
        assertEquals(2, repo.allTasks().size)
    }

    @Test
    fun `yesterday's tasks appear in getTasksBeforeDay on the new day`() = runTest {
        addTask("Buy groceries")
        addTask("Read a book")

        dateProvider.currentDate = day2

        val expiredTasks = repo.getTasksBeforeDay(dateProvider.today()).first()
        assertEquals(2, expiredTasks.size)
    }
}
