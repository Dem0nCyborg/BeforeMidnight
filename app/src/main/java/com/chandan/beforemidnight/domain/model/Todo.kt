package com.chandan.beforemidnight.domain.model

import java.time.LocalDate

data class Todo(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val createdDate: LocalDate,
    val expiresAt: Long?,
)
