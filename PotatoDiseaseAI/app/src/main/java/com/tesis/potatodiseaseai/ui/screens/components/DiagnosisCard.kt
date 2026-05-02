package com.tesis.potatodiseaseai.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.potatodiseaseai.R

@Composable
fun DiagnosisCard(
    diseaseName: String,
    confidence: Float,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    val isLowConfidence = confidence < 0.70f

    val containerColor = when {
        isLowConfidence -> MaterialTheme.colorScheme.tertiaryContainer
        isHealthy -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val iconVector = when {
        isLowConfidence -> Icons.Default.Info
        isHealthy -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }

    val iconTint = when {
        isLowConfidence -> MaterialTheme.colorScheme.tertiary
        isHealthy -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isLowConfidence) {
                        stringResource(R.string.result_low_confidence_title)
                    } else {
                        diseaseName
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.result_confidence,
                        String.format("%.1f", confidence * 100)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}