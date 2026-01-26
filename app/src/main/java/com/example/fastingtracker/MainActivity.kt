package com.example.fastingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FastingAppViewModel : ViewModel() {
    var isFasting by mutableStateOf(false)
    var startTime by mutableStateOf<LocalDateTime?>(null)
    var goalHours by mutableStateOf<Float?>(null)
    var showGoalDialog by mutableStateOf(false)
    var lastFastDuration by mutableStateOf("0")

    fun getStatusMessage(currentFastedHours: Float): String {
        val goal = goalHours ?: return "No goal, no message"
        val delta = goal - currentFastedHours

        return when {
            delta >= 4 -> "Check again later. Keep going."
            delta in 1f..4f -> "You're doing Ok"
            delta in 0f..1f -> "Almost there"
            delta < -2 -> "Remind me to challenge you next time"
            delta < 0 -> "You're doing great"
            else -> ""
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: FastingAppViewModel = viewModel()
            FastingScreen(viewModel)
        }
    }
}

@Composable
fun FastingScreen(viewModel: FastingAppViewModel) {
    var currentFastedHours by remember { mutableStateOf(0f) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    // Logic: Trigger dialog on app launch if not already fasting
    LaunchedEffect(Unit) {
        if (!viewModel.isFasting && viewModel.goalHours == null) {
            viewModel.showGoalDialog = true
        }
    }

    // Logic: Timer Loop
    LaunchedEffect(viewModel.isFasting) {
        while (viewModel.isFasting) {
            val now = LocalDateTime.now()
            viewModel.startTime?.let {
                val diff = Duration.between(it, now)
                currentFastedHours = diff.toSeconds() / 3600f 
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    // UI: Goal Selection Dialog
    if (viewModel.showGoalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showGoalDialog = false },
            title = { Text("How many hours would you like to fast?") },
            text = {
                Column {
                    val options = listOf("12 hours" to 12f, "14 hours" to 14f, "16 hours" to 16f)
                    options.forEach { (label, value) ->
                        Button(
                            onClick = { 
                                viewModel.goalHours = value
                                viewModel.showGoalDialog = false 
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.goalHours = null
                    viewModel.showGoalDialog = false 
                }) { Text("Skip") }
            }
        )
    }

    // UI: Main Layout
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Timestamps
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Start: ${viewModel.startTime?.format(formatter) ?: "--:--:--"}")
            Text("Now: ${LocalDateTime.now().format(formatter)}")
        }

        // Middle: Message Box (The "Empty Space")
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val message = if (!viewModel.isFasting && viewModel.goalHours != null) {
                "The goal is set ... you only need to start"
            } else if (viewModel.isFasting) {
                viewModel.getStatusMessage(currentFastedHours)
            } else {
                "Ready to start?"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )
        }

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { 
                    viewModel.startTime = LocalDateTime.now()
                    viewModel.isFasting = true 
                }, 
                enabled = !viewModel.isFasting
            ) { Text("Start Fast") }

            Button(
                onClick = { 
                    viewModel.isFasting = false
                    viewModel.lastFastDuration = String.format("%.1f", currentFastedHours)
                    viewModel.goalHours = null // Reset for next cycle
                }, 
                enabled = viewModel.isFasting
            ) { Text("End Fast") }
        }

        // Bottom Stats
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val goalDisplay = viewModel.goalHours?.let { "${it.toInt()} h" } ?: "NaN"
            Text("Goal = $goalDisplay", style = MaterialTheme.typography.bodyLarge)
            Text("last fast = ${viewModel.lastFastDuration} h", style = MaterialTheme.typography.bodyLarge)
        }
    }
}