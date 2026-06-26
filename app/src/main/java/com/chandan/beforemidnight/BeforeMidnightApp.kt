package com.chandan.beforemidnight

import android.app.Application
import com.chandan.beforemidnight.di.AppContainer

class BeforeMidnightApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
