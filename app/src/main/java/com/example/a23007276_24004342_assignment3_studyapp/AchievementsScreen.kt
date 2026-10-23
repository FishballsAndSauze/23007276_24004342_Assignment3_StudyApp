package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AchievementsScreen(progress: UserProgress, tasks: List<StudyTask>) {
    val achievements = listOf(
        Achievement("first", "First Task", "Complete your first assignment", progress.completedTasks >= 1),
        Achievement("five", "Getting Started", "Complete 5 tasks", progress.completedTasks >= 5),
        Achievement("ten", "Consistent", "Complete 10 tasks", progress.completedTasks >= 10),
        Achievement("level3", "Rising Star", "Reach level 3", progress.level >= 3),
        Achievement("level5", "Scholar", "Reach level 5", progress.level >= 5),
        Achievement("streak3", "On Fire", "3-day streak", progress.streak >= 3),
        Achievement("streak7", "Unstoppable", "7-day streak", progress.streak >= 7),
        Achievement("study60", "Deep Focus", "Study 60+ minutes total", progress.totalStudyMinutes >= 60)
    )

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Achievements", color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${achievements.count { it.unlocked }} of ${achievements.size} unlocked", color = Muted)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(achievements) { ach ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ach.unlocked) Panel else Color(0xFF151A2A)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(
                                    if (ach.unlocked) Purple.copy(alpha = 0.25f) else Color(0xFF2A3148),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (ach.unlocked) "★" else "☆", color = if (ach.unlocked) Purple else Muted, fontSize = 22.sp)
                        }
                        Text(ach.title, color = if (ach.unlocked) WhiteText else Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(ach.description, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}