package com.example.a23007276_24004342_assignment3_studyapp

// Stats page: four summary cards and a bar chart of the last 7 days.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
internal fun StatsScreen(
    progress: UserProgress,
    tasks: List<StudyTask>
) {
    val completedThisWeek = tasks.count { it.completed }
    // Convert total study minutes into hours and minutes for display.
    val totalMinutes = progress.totalStudyMinutes
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60

    // Counts completed tasks for each of the last 7 days, matched by due date
    val last7 = (0..6).map { dayOffset ->
        val date = LocalDate.now().minusDays(dayOffset.toLong())
        tasks.count { it.completed && it.dueDate == date.toString() }.coerceAtLeast(0)
    }.reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Your Stats", color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Study time", "${hours}h ${mins}m", Modifier.weight(1f))
                StatCard("Tasks done", "${progress.completedTasks}", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Current streak", "${progress.streak} days", Modifier.weight(1f))
                StatCard("Level", "${progress.level}", Modifier.weight(1f))
            }
        }

        // Bar chart drawn with Canvas. Bar height is scaled against the largest value.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Last 7 days (completed tasks)", color = WhiteText, fontWeight = FontWeight.Bold)
                    val maxVal = (last7.maxOrNull() ?: 1).coerceAtLeast(1)
                    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        val barWidth = size.width / 9f
                        last7.forEachIndexed { index, value ->
                            val barHeight = (value.toFloat() / maxVal) * size.height * 0.8f
                            val x = barWidth + index * (barWidth * 1.2f)
                            drawRect(
                                color = Cyan,
                                topLeft = Offset(x, size.height - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// A small card showing one statistic: a title and a big value.
@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Muted, fontSize = 13.sp)
            Text(value, color = WhiteText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}