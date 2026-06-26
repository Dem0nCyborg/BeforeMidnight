package com.chandan.beforemidnight.di

import android.content.Context
import androidx.room.Room
import com.chandan.beforemidnight.data.local.TodoDatabase
import com.chandan.beforemidnight.data.repository.TodoRepositoryImpl
import com.chandan.beforemidnight.domain.repository.TodoRepository
import com.chandan.beforemidnight.domain.usecase.AddTaskUseCase
import com.chandan.beforemidnight.util.DateProvider
import com.chandan.beforemidnight.util.SystemDateProvider

class AppContainer(context: Context) {

    private val database: TodoDatabase by lazy {
        Room.databaseBuilder(context, TodoDatabase::class.java, "beforemidnight.db")
            .build()
    }

    private val dao by lazy { database.todoDao() }

    val dateProvider: DateProvider = SystemDateProvider()

    val repository: TodoRepository by lazy { TodoRepositoryImpl(dao) }

    val addTaskUseCase by lazy { AddTaskUseCase(repository, dateProvider) }
}
