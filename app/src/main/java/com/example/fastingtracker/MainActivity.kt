package com.example.fastingtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
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
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModel() {
    var isFasting by mutableStateOf(false)
    var startTime by mutableStateOf<LocalDateTime?>(null)
    var goalHours by mutableStateOf<Float?>(null)
    var showGoalDialog by mutableStateOf(false)
    var lastFastDuration by mutableStateOf("0")
    var allSessions by mutableStateOf<List<FastingSessionUiModel>>(emptyList())
    var goalReachedNotificationShown by mutableStateOf(false)

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
        goalReachedNotificationShown = false
        preferencesManager.setCurrentSessionStartTime(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        preferencesManager.setCurrentSessionGoal(goal)
    }

    fun updateGoalMidFast(newGoal: Float) {
        goalHours = newGoal
        preferencesManager.setCurrentSessionGoal(newGoal)
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
        goalReachedNotificationShown = false
        preferencesManager.clearCurrentSession()
    }

    fun sendGoalReachedNotification() {
        if (goalReachedNotificationShown) return
        
        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Fasting Goal Reached! 🎉")
            .setContentText("Congratulations! You've reached your fasting goal.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GOAL_REACHED_NOTIFICATION_ID, notificationBuilder.build())
        goalReachedNotificationShown = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Fasting Tracker"
            val descriptionText = "Notifications for fasting milestones"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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

    companion object {
        const val CHANNEL_ID = "fasting_tracker_channel"
        const val GOAL_REACHED_NOTIFICATION_ID = 1
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
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FastingAppViewModel(repository, preferencesManager, context) as T
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = FastingDatabase.getInstance(this)
        val repository = FastingRepository(database.fastingDao())
        val preferencesManager = PreferencesManager(this)
        val factory = FastingAppViewModelFactory(repository, preferencesManager, this)

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
    var showEditGoalDialog by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val scope = rememberCoroutineScope()

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
                
                // Check if goal is reached and send notification
                val goal = viewModel.goalHours ?: 0f
                if (currentFastedHours >= goal) {
                    viewModel.sendGoalReachedNotification()
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    // UI: Goal Selection Dialog
    if (viewModel.showGoalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showGoalDialog = false },
            title = { Text("How many hours would you like to fast?", fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf("12 hours" to 12f, "14 hours" to 14f, "16 hours" to 16f)
                    options.forEach { (label, value) ->
                        Button(
                            onClick = { 
                                viewModel.startFasting(value)
                                viewModel.showGoalDialog = false 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { 
                            Text(label, fontSize = 16.sp)
                        }
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

    // Edit Goal Dialog
    if (showEditGoalDialog) {
        var newGoalText by remember { mutableStateOf(viewModel.goalHours?.toInt().toString()) }
        
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            title = { Text("Change Fasting Goal", fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current goal: ${viewModel.goalHours?.toInt()} hours")
                    OutlinedTextField(
                        value = newGoalText,
                        onValueChange = { newGoalText = it },
                        label = { Text("New goal (hours)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        newGoalText.toFloatOrNull()?.let { newGoal ->
                            viewModel.updateGoalMidFast(newGoal)
                        }
                        showEditGoalDialog = false
                    }
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // UI: Main Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Branding Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_dialog_info),
                contentDescription = "Fast Feed Icon",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Fast Feed",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Timestamps
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Start: ${viewModel.startTime?.format(formatter) ?: "--:--:--"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Now: ${LocalDateTime.now().format(formatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Progress Section
        if (viewModel.isFasting && viewModel.goalHours != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress Bar
                    val goal = viewModel.goalHours!!
                    val progress = (currentFastedHours / goal).coerceIn(0f, 1f)
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Time remaining and elapsed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Elapsed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                String.format("%.1f / %.0f h", currentFastedHours, goal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Time Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            val timeRemaining = (goal - currentFastedHours).coerceAtLeast(0f)
                            val hours = timeRemaining.toInt()
                            val minutes = ((timeRemaining - hours) * 60).toInt()
                            Text(
                                String.format("%d h %d m", hours, minutes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (timeRemaining <= 1f) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Message Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Goal and Last Duration Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val goalDisplay = viewModel.goalHours?.let { "${it.toInt()} h" } ?: "Not set"
            
            Column {
                Text(
                    "Current Goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    goalDisplay,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Last Fast",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    "${viewModel.lastFastDuration} h",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { 
                        viewModel.showGoalDialog = true
                    }, 
                    enabled = !viewModel.isFasting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) { 
                    Text("Start Fast", fontSize = 14.sp)
                }

                Button(
                    onClick = { 
                        scope.launch {
                            onEndFasting()
                        }
                    }, 
                    enabled = viewModel.isFasting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { 
                    Text("End Fast", fontSize = 14.sp)
                }
            }

            // Edit Goal Button (show only during fasting)
            if (viewModel.isFasting) {
                Button(
                    onClick = { showEditGoalDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Goal", fontSize = 14.sp)
                }
            }

            Button(
                onClick = { onNavigateToHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { 
                Text("View History", fontSize = 14.sp)
            }
        }
    }
}
