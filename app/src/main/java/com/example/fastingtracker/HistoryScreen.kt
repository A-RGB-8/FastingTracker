package com.example.fastingtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.fastingtracker.data.FastingSessionUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<FastingSessionUiModel>,
    onNavigateBack: () -> Unit,
    onDeleteSession: (FastingSessionUiModel) -> Unit,
    onUpdateSession: (FastingSessionUiModel) -> Unit
) {
    val context = LocalContext.current
    var sessionToDelete by remember { mutableStateOf<FastingSessionUiModel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Fasting History", fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No fasting sessions yet.\nStart your first fast!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            SummaryCard(sessions = sessions)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onDelete = { sessionToDelete = session },
                        onEdit = {
                            // Step 1: Pick new Start Time
                            showDateTimePicker(context, session.startTime) { newStart ->
                                // Step 2: Pick new End Time
                                showDateTimePicker(context, session.endTime) { newEnd ->
                                    // Step 3: Calculate new duration and update
                                    val newDuration = Duration.between(newStart, newEnd).toSeconds() / 3600.0
                                    onUpdateSession(
                                        session.copy(
                                            startTime = newStart,
                                            endTime = newEnd,
                                            durationHours = newDuration
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("This will permanently remove this record from your history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { onDeleteSession(it) }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ... SummaryCard and StatItem remain the same as your code ...

@Composable
fun SummaryCard(sessions: List<FastingSessionUiModel>) {
    val totalSessions = sessions.size
    val averageDuration = if (sessions.isNotEmpty()) sessions.map { it.durationHours }.average() else 0.0
    val longestSession = sessions.maxOfOrNull { it.durationHours } ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Total Fasts", "$totalSessions")
                StatItem("Average", String.format("%.1f h", averageDuration))
                StatItem("Longest", String.format("%.1f h", longestSession))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun SessionCard(
    session: FastingSessionUiModel,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.startTime.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    "${session.startTime.format(timeFormatter)} - ${session.endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Goal: ${session.goalHours.toInt()}h", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Actual: ${String.format("%.1f", session.durationHours)}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (session.durationHours >= session.goalHours) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}