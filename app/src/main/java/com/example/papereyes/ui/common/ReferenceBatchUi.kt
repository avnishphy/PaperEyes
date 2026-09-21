package com.example.papereyes.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.papereyes.domain.evidence.ReferenceEvidence
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.reference.ReferenceResolution
import com.example.papereyes.domain.reference.ReferenceResolutionStatus

@Composable
fun ReferenceSelectionDialog(
    references: List<ReferenceEvidence>,
    onDismiss: () -> Unit,
    onSearch: (List<ReferenceEvidence>) -> Unit
) {
    var selectedIndexes by remember(references) {
        mutableStateOf(references.indices.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${references.size} references found") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Search all references, or clear the ones you don't want to look up.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectedIndexes = references.indices.toSet() }) {
                        Text("Select all")
                    }
                    TextButton(onClick = { selectedIndexes = emptySet() }) {
                        Text("Clear")
                    }
                }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(references) { index, reference ->
                        val selected = index in selectedIndexes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIndexes = if (selected) {
                                        selectedIndexes - index
                                    } else {
                                        selectedIndexes + index
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    selectedIndexes = if (checked) {
                                        selectedIndexes + index
                                    } else {
                                        selectedIndexes - index
                                    }
                                }
                            )
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                reference.label?.let { label ->
                                    Text("Reference [$label]", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    reference.text,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedIndexes.isNotEmpty(),
                onClick = {
                    onSearch(references.filterIndexed { index, _ -> index in selectedIndexes })
                }
            ) {
                Text(if (selectedIndexes.size == references.size) "Search all" else "Search selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReferenceBatchSummary(
    outcomes: List<ReferenceResolution>,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    onPaperClick: ((Paper) -> Unit)? = null
) {
    if (total == 0) return

    val failures = outcomes.filter { it.status != ReferenceResolutionStatus.IDENTIFIED }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .then(
                    if (scrollable) {
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (completed < total) "Searching references $completed/$total"
                else "Reference search complete",
                style = MaterialTheme.typography.titleMedium
            )
            if (completed < total) {
                LinearProgressIndicator(
                    progress = { completed.toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (completed == total && failures.isNotEmpty()) {
                Text(
                    "Couldn't identify ${failures.size} selected ${if (failures.size == 1) "reference" else "references"}:",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            outcomes.forEach { outcome ->
                val prefix = outcome.reference.label?.let { "[$it] " }.orEmpty()
                val status = when (outcome.status) {
                    ReferenceResolutionStatus.IDENTIFIED -> "Identified: ${outcome.paper?.title.orEmpty()}"
                    ReferenceResolutionStatus.AMBIGUOUS -> "Not identified — multiple or uncertain matches"
                    ReferenceResolutionStatus.NOT_FOUND -> "Not identified — no match found"
                    ReferenceResolutionStatus.PROVIDERS_UNAVAILABLE -> "Not identified — services unavailable"
                    ReferenceResolutionStatus.ERROR -> "Not identified — lookup failed"
                }
                Column(
                    modifier = Modifier.then(
                        if (outcome.paper != null && onPaperClick != null) {
                            Modifier.clickable { onPaperClick(outcome.paper) }
                        } else {
                            Modifier
                        }
                    )
                ) {
                    Text(
                        prefix + outcome.reference.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        status,
                        color = if (outcome.status == ReferenceResolutionStatus.IDENTIFIED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (outcome.paper != null && onPaperClick != null) {
                        Text("Tap to open", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
