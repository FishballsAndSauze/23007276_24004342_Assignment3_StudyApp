package com.example.a23007276_24004342_assignment3_studyapp


/* Simple month calendar. Today is tinted, the selected date is purple,
* and a small cyan dot marks any date that has a task due*/

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    tasks: List<StudyTask>
) {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Study calendar", color = WhiteText, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onPreviousMonth) { Text("‹", color = Cyan, fontSize = 22.sp) }
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", color = WhiteText, fontSize = 13.sp)
                TextButton(onClick = onNextMonth) { Text("›", color = Cyan, fontSize = 22.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, color = Muted, modifier = Modifier.width(32.dp), fontSize = 12.sp)
                }
            }
            val cells = buildList<LocalDate?> {
                repeat(offset) { add(null) }
                for (day in 1..days) add(month.atDay(day))
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { date ->
                        if (date == null) {
                            Box(Modifier.size(32.dp))
                        } else {
                            val hasTask = tasks.any { it.dueDate == date.toString() }
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            Box(
                                modifier = Modifier.size(32.dp)
                                    .background(
                                        when {
                                            isSelected -> Purple
                                            isToday -> Cyan.copy(alpha = 0.22f)
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    )
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("${date.dayOfMonth}", color = if (isSelected) Color.White else WhiteText, fontSize = 12.sp)
                                    if (hasTask) Box(Modifier.size(4.dp).background(if (isSelected) Color.White else Cyan, CircleShape))
                                }
                            }
                        }
                    }
                    // Pad the last week so its row keeps the same spacing.
                    repeat(7 - week.size) { Spacer(Modifier.size(32.dp)) }
                }
            }
            Text("Today: ${LocalDate.now()}  •  Selected: $selectedDate", color = Muted, fontSize = 12.sp)
            Text("A cyan dot marks a date with an assignment.", color = Muted, fontSize = 11.sp)
        }
    }
}