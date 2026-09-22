package com.example.papereyes.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.papereyes.data.model.Paper
import com.example.papereyes.domain.reference.ReferenceResolution
import com.example.papereyes.domain.reference.ReferenceResolutionStatus

private enum class ReferenceResultFilter {
    ALL,
    IDENTIFIED,
    NEEDS_REVIEW
}

@Composable
fun ReferenceBatchResults(
    outcomes: List<ReferenceResolution>,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    onPaperClick: ((Paper) -> Unit)? = null
) {
    if (total == 0) return

    var candidateOutcome by remember { mutableStateOf<ReferenceResolution?>(null) }
    var filter by remember { mutableStateOf(ReferenceResultFilter.ALL) }
    val identifiedCount = outcomes.count { it.status == ReferenceResolutionStatus.IDENTIFIED }
    val reviewCount = outcomes.size - identifiedCount
    val visibleOutcomes = outcomes.filter { outcome ->
        when (filter) {
            ReferenceResultFilter.ALL -> true
            ReferenceResultFilter.IDENTIFIED ->
                outcome.status == ReferenceResolutionStatus.IDENTIFIED
            ReferenceResultFilter.NEEDS_REVIEW ->
                outcome.status != ReferenceResolutionStatus.IDENTIFIED
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (scrollable) {
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (completed < total) "Searching references $completed/$total" else "$total references",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                if (completed < total) {
                    "$identifiedCount identified so far"
                } else {
                    "$identifiedCount identified · $reviewCount need review"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (completed < total) {
                LinearProgressIndicator(
                    progress = { completed.toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (completed == total && outcomes.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResultFilterChip(
                        selected = filter == ReferenceResultFilter.ALL,
                        label = "All",
                        onClick = { filter = ReferenceResultFilter.ALL }
                    )
                    ResultFilterChip(
                        selected = filter == ReferenceResultFilter.IDENTIFIED,
                        label = "Identified",
                        onClick = { filter = ReferenceResultFilter.IDENTIFIED }
                    )
                    ResultFilterChip(
                        selected = filter == ReferenceResultFilter.NEEDS_REVIEW,
                        label = "Review",
                        onClick = { filter = ReferenceResultFilter.NEEDS_REVIEW }
                    )
                }
            }

            visibleOutcomes.forEachIndexed { index, outcome ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                ReferenceResultRow(
                    outcome = outcome,
                    onClick = when {
                        outcome.paper != null && onPaperClick != null -> {
                            { onPaperClick(outcome.paper) }
                        }
                        outcome.candidates.isNotEmpty() -> {
                            { candidateOutcome = outcome }
                        }
                        else -> null
                    }
                )
            }
        }
    }

    candidateOutcome?.let { outcome ->
        CandidatePapersSheet(
            outcome = outcome,
            onDismiss = { candidateOutcome = null },
            onPaperClick = { paper ->
                candidateOutcome = null
                onPaperClick?.invoke(paper)
            }
        )
    }
}

@Composable
private fun ResultFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ReferenceResultRow(
    outcome: ReferenceResolution,
    onClick: (() -> Unit)?
) {
    val label = outcome.reference.label?.let { "[$it] " }.orEmpty()
    val identified = outcome.status == ReferenceResolutionStatus.IDENTIFIED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label + outcome.reference.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            outcome.statusLabel(),
            color = if (identified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge
        )
        outcome.paper?.let { paper ->
            Text(
                paper.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            if (onClick != null) {
                Text("Open paper ›", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (outcome.paper == null && outcome.candidates.isNotEmpty()) {
            Text(
                "Review ${outcome.candidates.size} ${if (outcome.candidates.size == 1) "candidate" else "candidates"} ›",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun ReferenceResolution.statusLabel(): String =
    when (status) {
        ReferenceResolutionStatus.IDENTIFIED -> "Identified"
        ReferenceResolutionStatus.AMBIGUOUS -> "Needs review · uncertain match"
        ReferenceResolutionStatus.NOT_FOUND -> "Not identified · no match found"
        ReferenceResolutionStatus.PROVIDERS_UNAVAILABLE -> "Not identified · services unavailable"
        ReferenceResolutionStatus.ERROR -> "Not identified · lookup failed"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidatePapersSheet(
    outcome: ReferenceResolution,
    onDismiss: () -> Unit,
    onPaperClick: (Paper) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Possible papers", style = MaterialTheme.typography.headlineSmall)
            Text(
                outcome.reference.label?.let { "Reference [$it]" } ?: "Selected reference",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                outcome.reference.text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp)
            ) {
                items(outcome.candidates, key = { it.identityKey }) { paper ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPaperClick(paper) }
                            .padding(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(paper.title, style = MaterialTheme.typography.titleSmall)
                        if (paper.authors.isNotBlank()) {
                            Text(
                                paper.authors,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        paper.year?.let {
                            Text(it.toString(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Close") }
        }
    }
}
