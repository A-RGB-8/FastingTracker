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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FastingApp()
        }
    }
}

@Composable
fun FastingApp(viewModel: FastingViewModel = viewModel()) {
    val context = LocalContext.current
    val isFasting by viewModel.isFasting.collectAsState()
    val timerDisplay by viewModel.timerDisplay.collectAsState()

    // Two separate state holders for the two text fields
    val defaultTime = "${viewModel.getDefaultHour()}:00"
    var startManualTime by remember { mutableStateOf(defaultTime) }
    var endManualTime by remember { mutableStateOf(defaultTime) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isFasting) "$timerDisplay fasting" else "00:00:00 feeding",
            color = if (isFasting) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        // --- SECTION 1: START FASTING ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fasting Begin", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = startManualTime,
                    onValueChange = { startManualTime = it },
                    label = { Text("Start Time (HH:mm)") },
                    enabled = !isFasting, // Gray out if already fasting
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { viewModel.startFast() },
                        enabled = !isFasting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Start Now") }

                    Button(
                        onClick = { viewModel.startFastAtTime(startManualTime) },
                        enabled = !isFasting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) { Text("Start at $startManualTime") }
                }
            }
        }

        // --- SECTION 2: END FASTING ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Fasting Ending", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = endManualTime,
                    onValueChange = { endManualTime = it },
                    label = { Text("End Time (HH:mm)") },
                    enabled = isFasting, // Gray out if not fasting
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = {
                            val duration = viewModel.endFast()
                            Toast.makeText(context, "Fast ended: $duration", Toast.LENGTH_LONG).show()
                        },
                        enabled = isFasting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) { Text("End Now") }

                    Button(
                        onClick = {
                            val duration = viewModel.endFastAtTime(endManualTime)
                            Toast.makeText(context, "Fast ended: $duration", Toast.LENGTH_LONG).show()
                        },
                        enabled = isFasting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) { Text("End at $endManualTime") }
                }
            }
        }

        Text(text = viewModel.getLastResult(), color = Color.Gray)
    }
}