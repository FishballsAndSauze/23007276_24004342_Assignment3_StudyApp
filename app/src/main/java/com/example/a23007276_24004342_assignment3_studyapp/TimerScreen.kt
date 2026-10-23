package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val quotes = listOf(
    "Focus is a muscle. Train it.",
    "One session at a time.",
    "You don’t have to be perfect, just consistent.",
    "Deep work compounds.",
    "Small progress is still progress."
)

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
    val isFinished = started && phases.isNotEmpty() && phaseIndex >= phases.lastIndex && remainingMs <= 0
    val background = when {
        !started || isFinished -> Ink
        currentPhase?.isBreak == true -> SoftGreen
        else -> SoftRed
    }
    val textColor = if (!started || isFinished) WhiteText else Ink

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
                    Button(onClick = onStart, enabled = total > 0 && study > 0, modifier = Modifier.fillMaxWidth()) {
                        Text("Start focus session")
                    }
                }
            }
            Text("Study phases are divided evenly around your breaks.", color = Muted, fontSize = 12.sp)
        } else if (isFinished) {
            Text("SESSION COMPLETE", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Great work!", color = WhiteText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            val totalStudy = phases.filter { !it.isBreak }.sumOf { it.durationMs } / 60000
            Text("You focused for $totalStudy minutes", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Back to planner")
            }
        } else {
            Text("FOCUS SESSION", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(currentPhase?.name ?: "Session", color = textColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            // Circular ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                val progress = if (currentPhase != null && currentPhase.durationMs > 0) {
                    1f - (remainingMs.toFloat() / currentPhase.durationMs.toFloat())
                } else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = Size(size.minDimension, size.minDimension),
                        topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)
                    )
                    drawArc(
                        color = if (currentPhase?.isBreak == true) Cyan else Color(0xFFE85A5A),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = Size(size.minDimension, size.minDimension),
                        topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)
                    )
                }
                Text(formatTime(remainingMs), color = textColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            }

            Text("Phase ${min(phaseIndex + 1, phases.size)} of ${phases.size}", color = textColor.copy(alpha = 0.8f))

            // Next phase preview
            val next = phases.getOrNull(phaseIndex + 1)
            if (next != null) {
                Text("Next: ${next.name} (${formatTime(next.durationMs)})", color = textColor.copy(alpha = 0.7f), fontSize = 13.sp)
            }

            // Quote during study
            if (currentPhase?.isBreak != true) {
                val quote = quotes[phaseIndex % quotes.size]
                Text("“$quote”", color = textColor.copy(alpha = 0.75f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onPauseResume, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "Pause timer" else "Resume timer")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset and change settings", color = textColor)
            }
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