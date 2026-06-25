package com.chandan.beforemidnight.util

import java.time.LocalDate

interface DateProvider {
    fun today(): LocalDate
    fun nowMillis(): Long
}
