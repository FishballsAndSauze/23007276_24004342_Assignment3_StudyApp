package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProfileScreen(
    progress: UserProgress,
    userName: String,
    onNameChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }

    val title = when {
        progress.level >= 10 -> "Master Scholar"
        progress.level >= 5 -> "Dedicated Learner"
        progress.level >= 3 -> "Rising Student"
        else -> "Beginner"
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(90.dp).background(Purple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(userName.take(1).uppercase(), color = Purple, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }

        if (editing) {
            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                label = { Text("Your name") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    onNameChange(tempName.trim().ifBlank { "Student" })
                    editing = false
                }) { Text("Save") }
                OutlinedButton(onClick = { editing = false; tempName = userName }) { Text("Cancel") }
            }
        } else {
            Text(userName, color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Cyan, fontSize = 15.sp)
            TextButton(onClick = { editing = true; tempName = userName }) {
                Text("Edit name", color = Cyan)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileRow("Level", "${progress.level}")
                ProfileRow("Total XP", "${(progress.level - 1) * 100 + progress.xp}")
                ProfileRow("Tasks completed", "${progress.completedTasks}")
                ProfileRow("Current streak", "${progress.streak} days")
                ProfileRow("Total study time", "${progress.totalStudyMinutes} min")
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(value, color = WhiteText, fontWeight = FontWeight.Bold)
    }
}