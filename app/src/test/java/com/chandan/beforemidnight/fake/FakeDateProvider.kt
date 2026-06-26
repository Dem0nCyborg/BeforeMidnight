package com.chandan.beforemidnight.fake

import com.chandan.beforemidnight.util.DateProvider
import java.time.LocalDate
import java.time.ZoneOffset

class FakeDateProvider(initialDate: LocalDate) : DateProvider {
    var currentDate: LocalDate = initialDate
    var nowMillisValue: Long = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    override fun today(): LocalDate = currentDate
    override fun nowMillis(): Long = nowMillisValue
}
