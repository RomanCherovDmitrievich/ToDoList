package ru.chernov.todolist.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalendarScreen(onLogout: () -> Unit) {
    val selectedDate = remember { mutableStateOf("2026-03-25") }
    val dayTasks = remember {
        listOf(
            "Prepare mobile sync API",
            "Connect login to shared account",
            "Review offline queue"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Calendar", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }

        Card(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Large monthly calendar goes here")
                Text("Selected date: ${selectedDate.value}")
                Text("On Android, map this screen to a Compose grid and local Room cache.")
            }
        }

        Text("Tasks for ${selectedDate.value}", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayTasks) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(task, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
