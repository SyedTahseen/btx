package com.betterx.app

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.betterx.app.data.Reminders
import com.betterx.app.data.Store
import com.betterx.app.ui.AppRoot
import com.betterx.app.ui.BetterXTheme
import com.betterx.app.ui.LocalStore

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Privacy: hide content in the recents screen and block screenshots / screen recording.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        store = Store(applicationContext)
        Reminders.ensureChannel(this)
        Reminders.schedule(this, store.data)
        setContent {
            BetterXTheme {
                CompositionLocalProvider(LocalStore provides store) { AppRoot() }
            }
        }
    }
}
