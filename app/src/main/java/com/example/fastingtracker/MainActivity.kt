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
    val context = LocalContext.current // Needed for the Toast
    val isFasting by viewModel.isFasting.collectAsState()
    val timerDisplay by viewModel.timerDisplay.collectAsState()

    // State for the manual time input (defaults to current hour:00)
    val defaultTime = "${viewModel.getDefaultHour()}:00"
    var manualTime by remember { mutableStateOf(defaultTime) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isFasting) "$timerDisplay fasting" else "00:00:00 feeding",
            color = if (isFasting) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineLarge
        )

        // Manual Time Input Field
        OutlinedTextField(
            value = manualTime,
            onValueChange = { manualTime = it },
            label = { Text("Time (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )

        // 1. Start Fasting NOW
        Button(
            onClick = { if (!isFasting) viewModel.startFast() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Start Fasting NOW")
        }

        // 2. Start Fasting at GIVEN TIME
        Button(
            onClick = { if (!isFasting) viewModel.startFastAtTime(manualTime) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)) // Blue for custom
        ) {
            Text("Start Fasting at $manualTime")
        }

        // 3. Finish Fasting
        Button(
            onClick = {
                if (isFasting) {
                    val duration = viewModel.endFast()
                    Toast.makeText(context, "Fast ended after $duration hours", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Text("Finish Fasting")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display the last result permanently at the bottom
        Text(
            text = viewModel.getLastResult(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}