package com.chandan.beforemidnight.domain.usecase

import com.chandan.beforemidnight.fake.FakeDateProvider
import com.chandan.beforemidnight.fake.FakeTodoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddTaskUseCaseTest {

    private val today = LocalDate.of(2026, 6, 26)
    private lateinit var repo: FakeTodoRepository
    private lateinit var addTask: AddTaskUseCase

    @Before
    fun setUp() {
        repo = FakeTodoRepository()
        addTask = AddTaskUseCase(repo, FakeDateProvider(today))
    }

    @Test
    fun `blank title is rejected`() = runTest {
        try {
            addTask("")
            fail("Expected IllegalArgumentException for blank title")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `whitespace-only title is rejected`() = runTest {
        try {
            addTask("     ")
            fail("Expected IllegalArgumentException for whitespace-only title")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `title exceeding 200 characters is rejected`() = runTest {
        try {
            addTask("a".repeat(201))
            fail("Expected IllegalArgumentException for title over 200 chars")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun `title at exactly 200 characters is accepted`() = runTest {
        addTask("a".repeat(200))
        assertEquals(1, repo.getTasksForDay(today).first().size)
    }

    @Test
    fun `surrounding whitespace is trimmed before the title is saved`() = runTest {
        addTask("  Buy groceries  ")
        val saved = repo.getTasksForDay(today).first().single()
        assertEquals("Buy groceries", saved.title)
    }

    @Test
    fun `valid title is stored under today's date`() = runTest {
        addTask("Read a book")
        val tasks = repo.getTasksForDay(today).first()
        assertEquals(1, tasks.size)
        assertEquals("Read a book", tasks.single().title)
        assertEquals(today, tasks.single().createdDate)
    }
}
