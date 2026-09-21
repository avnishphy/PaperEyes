package com.example.papereyes.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.papereyes.domain.evidence.ScanSubject

@Composable
fun ScanSubjectDialog(
    onDismiss: () -> Unit,
    onSelect: (ScanSubject) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What are you scanning?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScanSubjectButton(
                    title = "Journal paper",
                    description = "Focus on the paper title, DOI, arXiv ID, or journal header.",
                    onClick = { onSelect(ScanSubject.JOURNAL_PAPER) }
                )
                ScanSubjectButton(
                    title = "References",
                    description = "Find citations in a bibliography or reference list.",
                    onClick = { onSelect(ScanSubject.REFERENCES) }
                )
                ScanSubjectButton(
                    title = "Conference slide",
                    description = "Focus on citations or references shown on a slide.",
                    onClick = { onSelect(ScanSubject.CONFERENCE_SLIDE) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ScanSubjectButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
