package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun TimerScreen(
    totalHours: Int,
    totalMinutes: Int,
    breakCount: Int,
    breakMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onBreakCountChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    phases: List<StudyPhase>,
    phaseIndex: Int,
    remainingMs: Long,
    running: Boolean,
    started: Boolean,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onReset: () -> Unit
) {
    val currentPhase = phases.getOrNull(phaseIndex)
    val background = when {
        !started -> Ink
        currentPhase?.isBreak == true -> Color(0xFF143C2A)
        else -> Color(0xFF48252E)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!started) {
            Text("Plan your focus session", color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Your chosen duration includes all breaks.", color = Muted)
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Total session length", color = WhiteText, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberControl("Hours", totalHours, 0, 12, onHoursChange)
                        NumberControl("Minutes", totalMinutes, 0, 59, onMinutesChange)
                    }
                    Text("Number of breaks: $breakCount", color = WhiteText)
                    Slider(value = breakCount.toFloat(), onValueChange = { onBreakCountChange(it.toInt()) }, valueRange = 0f..5f, steps = 4)
                    Text("Length of each break: $breakMinutes min", color = WhiteText)
                    Slider(value = breakMinutes.toFloat(), onValueChange = { onBreakMinutesChange(it.toInt()) }, valueRange = 1f..30f, steps = 28)
                    val total = totalHours * 60 + totalMinutes
                    val study = total - breakCount * breakMinutes
                    Text(
                        if (study > 0) "Study time: $study min  •  Break time: ${breakCount * breakMinutes} min"
                        else "Total session must be longer than the combined breaks.",
                        color = if (study > 0) Cyan else Color(0xFFFF9292),
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onStart,
                        enabled = total > 0 && study > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start focus session") }
                }
            }
            Text("Study phases are divided evenly around your breaks.", color = Muted, fontSize = 12.sp)
        } else {
            Text("FOCUS SESSION", color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(currentPhase?.name ?: "Session complete", color = WhiteText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                formatTime(remainingMs),
                color = WhiteText,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Phase ${min(phaseIndex + 1, phases.size)} of ${phases.size}", color = WhiteText.copy(alpha = 0.8f))
            LinearProgressIndicator(
                progress = {
                    val duration = currentPhase?.durationMs ?: 1L
                    (1f - remainingMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(9.dp),
                color = if (currentPhase?.isBreak == true) Cyan else Color(0xFFFF9B9B),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onPauseResume, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Pause timer" else "Resume timer")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset and change settings", color = WhiteText)
            }
            Text(
                if (running) "Keep this screen open to watch the session." else "Timer paused or finished.",
                color = WhiteText.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun NumberControl(label: String, value: Int, minValue: Int, maxValue: Int, onChange: (Int) -> Unit) {
    Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onChange(max(minValue, value - 1)) }) { Text("−", color = Cyan, fontSize = 22.sp) }
            Text(value.toString(), color = WhiteText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onChange(min(maxValue, value + 1)) }) { Text("+", color = Cyan, fontSize = 22.sp) }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = max(0L, milliseconds / 1000L)
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val secs = seconds % 60L
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, secs)
    else "%02d:%02d".format(minutes, secs)
}