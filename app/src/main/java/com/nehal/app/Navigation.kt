package com.nehal.app

import androidx.compose.runtime.Composable
import androidx.navigation3.SinglePaneScene
import androidx.navigation3.rememberNavWrapper
import com.nehal.app.ui.main.MainScreen

@Composable
fun MainNavigation() {
    val navWrapper = rememberNavWrapper(startDestination = MainScreenKey)

    SinglePaneScene(navWrapper = navWrapper) { key ->
        when (key) {
            is MainScreenKey -> MainScreen()
            else -> MainScreen()
        }
    }
}
