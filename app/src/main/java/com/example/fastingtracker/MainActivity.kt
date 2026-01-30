package com.example.fastingtracker

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// --- ViewModel ---

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
    var goalReachedNotificationShown by mutableStateOf(false)

    init {
        lastFastDuration = preferencesManager.getLastFastDuration()
        val savedStartTime = preferencesManager.getCurrentSessionStartTime()
        val savedGoal = preferencesManager.getCurrentSessionGoal()

        if (savedStartTime > 0 && savedGoal > 0) {
            startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(savedStartTime), ZoneId.systemDefault())
            goalHours = savedGoal
            isFasting = true
        }
    }

    fun startFasting(goal: Float, selectedTime: LocalDateTime) {
        startTime = selectedTime
        goalHours = goal
        isFasting = true
        goalReachedNotificationShown = false
        saveCurrentState(selectedTime, goal)
    }

    fun updateStartTime(newTime: LocalDateTime) {
        startTime = newTime
        goalHours?.let { saveCurrentState(newTime, it) }
    }

    private fun saveCurrentState(time: LocalDateTime, goal: Float) {
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        preferencesManager.setCurrentSessionStartTime(millis)
        preferencesManager.setCurrentSessionGoal(goal)
    }

    suspend fun endFasting() {
        val start = startTime ?: return
        val goal = goalHours ?: return
        val endTime = LocalDateTime.now()

        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        repository.insertSession(startMillis, endMillis, goal)
        
        lastFastDuration = String.format("%.1f", (endMillis - startMillis) / 3600000.0)
        preferencesManager.setLastFastDuration(lastFastDuration)

        isFasting = false
        startTime = null
        goalHours = null
        preferencesManager.clearCurrentSession()
    }

    fun sendGoalReachedNotification() {
        if (goalReachedNotificationShown) return
        val channelId = "fasting_notifications"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Fasting Tracker", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Fasting Goal Reached!")
            .setContentText("You have hit your target. Well done.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
        goalReachedNotificationShown = true
    }

    fun getStatusMessage(hours: Float): String {
        val goal = goalHours ?: return ""
        return when {
            hours >= goal -> "Target reached! 🎉"
            goal - hours <= 1f -> "Almost there..."
            else -> "Fasting in progress"
        }
    }
}

// --- Factory & Activity ---

class FastingAppViewModelFactory(
    private val repo: FastingRepository, 
    private val pref: PreferencesManager, 
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FastingAppViewModel(repo, pref, context) as T
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FastingDatabase.getInstance(this)
        val repo = FastingRepository(db.fastingDao())
        val pref = PreferencesManager(this)
        val factory = FastingAppViewModelFactory(repo, pref, this)

        setContent {
            val viewModel: FastingAppViewModel = viewModel(factory = factory)
            FastingTrackerApp(viewModel, repo)
        }
    }
}

// --- Sequential Picker Utility ---

fun showDateTimePicker(context: Context, initial: LocalDateTime?, onSelected: (LocalDateTime) -> Unit) {
    val current = initial ?: LocalDateTime.now()
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            onSelected(LocalDateTime.of(year, month + 1, day, hour, minute))
        }, current.hour, current.minute, true).show()
    }, current.year, current.monthValue - 1, current.dayOfMonth).show()
}

// --- Navigation & Screens ---

@Composable
fun FastingTrackerApp(viewModel: FastingAppViewModel, repository: FastingRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.FastingTracker) }
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<FastingSessionUiModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getAllSessions().collect { list ->
            sessions = list.map { 
                FastingSessionUiModel(it.id, 
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it.startTime), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(it.endTime), ZoneId.systemDefault()),
                    it.goalHours, it.durationHours) 
            }
        }
    }

    when (currentScreen) {
        Screen.FastingTracker -> FastingScreen(viewModel, { currentScreen = Screen.History }, { scope.launch { viewModel.endFasting() } })
        Screen.History -> HistoryScreen(sessions, { currentScreen = Screen.FastingTracker }, {})
    }
}

@Composable
fun FastingScreen(viewModel: FastingAppViewModel, onNavigateHistory: () -> Unit, onEndFasting: () -> Unit) {
    val context = LocalContext.current
    var elapsedHours by remember { mutableStateOf(0f) }
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    // Core Timer Loop
    LaunchedEffect(viewModel.isFasting, viewModel.startTime) {
        while (viewModel.isFasting) {
            viewModel.startTime?.let {
                elapsedHours = Duration.between(it, LocalDateTime.now()).toSeconds() / 3600f
                if (elapsedHours >= (viewModel.goalHours ?: 0f)) viewModel.sendGoalReachedNotification()
            }
            delay(1000)
        }
    }

    if (viewModel.showGoalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showGoalDialog = false },
            title = { Text("Start Fasting") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your goal and starting time.")
                    listOf(12f, 16f, 18f, 20f, 24f).forEach { hours ->
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            showDateTimePicker(context, null) { picked ->
                                viewModel.startFasting(hours, picked)
                                viewModel.showGoalDialog = false
                            }
                        }) { Text("$hours Hours") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.showGoalDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Fast Feed", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)

        // START TIME CARD (Calibration Point)
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                showDateTimePicker(context, viewModel.startTime) { picked ->
                    if (viewModel.isFasting) viewModel.updateStartTime(picked) else viewModel.startTime = picked
                }
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT START TIME", style = MaterialTheme.typography.labelLarge)
                Text(viewModel.startTime?.format(formatter) ?: "Not Set", style = MaterialTheme.typography.headlineMedium)
                Text("(Tap to edit date or time)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        if (viewModel.isFasting) {
            val progress = (elapsedHours / (viewModel.goalHours ?: 1f)).coerceIn(0f, 1f)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                CircularProgressIndicator(progress = 1f, modifier = Modifier.fillMaxSize(), color = Color.LightGray, strokeWidth = 10.dp)
                CircularProgressIndicator(progress = progress, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.1f", elapsedHours), style = MaterialTheme.typography.displayLarge)
                    Text("HOURS ELAPSED", style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(viewModel.getStatusMessage(elapsedHours), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))

        // Actions
        Button(
            onClick = { viewModel.showGoalDialog = true },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            enabled = !viewModel.isFasting
        ) { Text("START NEW FAST", fontSize = 16.sp) }

        if (viewModel.isFasting) {
            Button(
                onClick = onEndFasting,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("FINISH FAST / START FEED", fontSize = 16.sp) }
        }

        OutlinedButton(onClick = onNavigateHistory, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("HISTORY", fontSize = 16.sp)
        }
    }
}

sealed class Screen { object FastingTracker : Screen(); object History : Screen() }