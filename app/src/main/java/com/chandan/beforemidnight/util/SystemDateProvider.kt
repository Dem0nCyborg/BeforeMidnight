package com.chandan.beforemidnight.util

import java.time.LocalDate
import java.time.ZoneId

class SystemDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    override fun nowMillis(): Long = System.currentTimeMillis()
}
