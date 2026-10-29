package com.rowan.abyssfiles.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class LauncherActivity : ComponentActivity() {

    // Bumped on every resume so newly installed/removed apps show up in the app row.
    private var refresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbyssTheme {
                // A home screen has nowhere to go "back" to.
                BackHandler(enabled = true) {}
                Home(refresh)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
    }
}
