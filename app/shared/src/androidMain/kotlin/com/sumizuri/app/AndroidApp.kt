package com.sumizuri.app

import android.content.Context
import com.sumizuri.app.data.profile.ProfileHost

// What the parts of the app that run with no screen need from the Android app, such as the check of the library while the app is closed. The app sets both when it starts.
object AndroidApp {
    lateinit var context: Context
    lateinit var profiles: () -> ProfileHost
}
