package com.example.a23007276_24004342_assignment3_studyapp

import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsScreen(
    onReset: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = WhiteText, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Data", color = WhiteText, fontWeight = FontWeight.Bold)
                Text("Resetting will permanently delete all tasks, progress, streak and study time.", color = Muted, fontSize = 13.sp)
                Button(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB33A3A))
                ) {
                    Text("Reset all data")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About", color = WhiteText, fontWeight = FontWeight.Bold)
                Text("StudyBuddy", color = Cyan)
                Text("Stay consistent. Level up your learning.", color = Muted, fontSize = 13.sp)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Panel,
            title = { Text("Reset everything?", color = WhiteText) },
            text = { Text("This cannot be undone.", color = Muted) },
            confirmButton = {
                Button(onClick = {
                    onReset()
                    showConfirm = false
                }) { Text("Yes, reset") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}