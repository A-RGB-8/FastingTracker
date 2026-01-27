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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fastingtracker.data.FastingDatabase
import com.example.fastingtracker.data.FastingRepository
import com.example.fastingtracker.data.PreferencesManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class FastingAppViewModel(
    private val repository: FastingRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    var isFasting by mutableStateOf(false)
    var startTime by mutableStateOf<LocalDateTime?>(null)
    var goalHours by mutableStateOf<Float?>(null)
    var showGoalDialog by mutableStateOf(false)
    var lastFastDuration by mutableStateOf("0")
    var allSessions by mutableStateOf<List<FastingSessionUiModel>>(emptyList())

    init {
        // Restore state from preferences
        lastFastDuration = preferencesManager.getLastFastDuration()
        val savedStartTime = preferencesManager.getCurrentSessionStartTime()
        val savedGoal = preferencesManager.getCurrentSessionGoal()

        if (savedStartTime > 0 && savedGoal > 0) {
            startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(savedStartTime),
                ZoneId.systemDefault()
            )
            goalHours = savedGoal
            isFasting = true
        }
    }

    fun startFasting(goal: Float) {
        val now = LocalDateTime.now()
        startTime = now
        goalHours = goal
        isFasting = true
        preferencesManager.setCurrentSessionStartTime(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        preferencesManager.setCurrentSessionGoal(goal)
    }

    suspend fun endFasting() {
        val start = startTime ?: return
        val goal = goalHours ?: return
        val endTime = LocalDateTime.now()

        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Save to database
        repository.insertSession(startMillis, endMillis, goal)

        // Calculate and save duration
        val durationHours = (endMillis - startMillis) / 3600000.0
        lastFastDuration = String.format("%.1f", durationHours)
        preferencesManager.setLastFastDuration(lastFastDuration)

        isFasting = false
        startTime = null
        goalHours = null
        preferencesManager.clearCurrentSession()
    }

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

    fun loadSessions() {
        // This will be called when we need to update the history
    }
}

// UI Model for display
data class FastingSessionUiModel(
    val id: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val goalHours: Float,
    val durationHours: Double
)

class FastingAppViewModelFactory(
    private val repository: FastingRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FastingAppViewModel(repository, preferencesManager) as T
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = FastingDatabase.getInstance(this)
        val repository = FastingRepository(database.fastingDao())
        val preferencesManager = PreferencesManager(this)
        val factory = FastingAppViewModelFactory(repository, preferencesManager)

        setContent {
            val viewModel: FastingAppViewModel = viewModel(factory = factory)
            FastingTrackerApp(viewModel, repository)
        }
    }
}

@Composable
fun FastingTrackerApp(
    viewModel: FastingAppViewModel,
    repository: FastingRepository
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.FastingTracker) }
    val scope = rememberCoroutineScope()
    var allSessions by remember { mutableStateOf<List<FastingSessionUiModel>>(emptyList()) }

    // Load sessions on first composition
    LaunchedEffect(Unit) {
        repository.getAllSessions().collect { sessions ->
            allSessions = sessions.map { entity ->
                FastingSessionUiModel(
                    id = entity.id,
                    startTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(entity.startTime),
                        ZoneId.systemDefault()
                    ),
                    endTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(entity.endTime),
                        ZoneId.systemDefault()
                    ),
                    goalHours = entity.goalHours,
                    durationHours = entity.durationHours
                )
            }
        }
    }

    when (currentScreen) {
        Screen.FastingTracker -> FastingScreen(
            viewModel = viewModel,
            onNavigateToHistory = { currentScreen = Screen.History },
            onEndFasting = {
                scope.launch {
                    viewModel.endFasting()
                }
            }
        )
        Screen.History -> HistoryScreen(
            sessions = allSessions,
            onNavigateBack = { currentScreen = Screen.FastingTracker },
            onDeleteSession = { session ->
                // We'll add delete functionality later
            }
        )
    }
}

sealed class Screen {
    object FastingTracker : Screen()
    object History : Screen()
}

@Composable
fun FastingScreen(
    viewModel: FastingAppViewModel,
    onNavigateToHistory: () -> Unit,
    onEndFasting: () -> Unit
) {
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
                                viewModel.startFasting(value)
                                viewModel.showGoalDialog = false 
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
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

        // Middle: Message Box
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.showGoalDialog = true
                    }, 
                    enabled = !viewModel.isFasting
                ) { Text("Start Fast") }

                Button(
                    onClick = { onEndFasting() }, 
                    enabled = viewModel.isFasting
                ) { Text("End Fast") }
            }

            Button(
                onClick = { onNavigateToHistory() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View History") }
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
