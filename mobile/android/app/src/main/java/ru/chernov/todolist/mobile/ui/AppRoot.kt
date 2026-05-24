package ru.chernov.todolist.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun AppRoot() {
    val authenticated = remember { mutableStateOf(false) }

    if (authenticated.value) {
        CalendarScreen(
            onLogout = { authenticated.value = false }
        )
    } else {
        LoginScreen(
            onLogin = { authenticated.value = true },
            onRegister = { authenticated.value = true }
        )
    }
}
