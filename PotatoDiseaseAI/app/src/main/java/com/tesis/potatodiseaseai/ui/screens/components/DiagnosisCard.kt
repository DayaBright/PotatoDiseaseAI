package com.tesis.potatodiseaseai.ui.screens.components

import android.annotation.SuppressLint
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
import com.tesis.potatodiseaseai.ui.theme.AppTheme

@SuppressLint("DefaultLocale")
@Composable
fun DiagnosisCard(
    diseaseName: String,
    confidence: Float,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    // Usar "z no potato" normalizado (LabelNormalizer convierte "_" → " ")
    val isLowConfidence = confidence < 0.70f || diseaseName == "z no potato"

    val containerColor = when {
        isLowConfidence -> AppTheme.colors.lowConfidenceContainer
        isHealthy -> AppTheme.colors.preventionContainer
        else -> AppTheme.colors.diseaseAlertContainer
    }

    val iconVector = when {
        isLowConfidence -> Icons.Default.Info
        isHealthy -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }

    val iconTint = when {
        isLowConfidence -> AppTheme.colors.lowConfidenceAccent
        isHealthy -> AppTheme.colors.preventionAccent
        else -> AppTheme.colors.diseaseAlertAccent
    }
    
    val textColor = when {
        isLowConfidence -> AppTheme.colors.lowConfidenceText
        isHealthy -> AppTheme.colors.preventionText
        else -> AppTheme.colors.diseaseAlertText
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = textColor
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
                    text = if (isLowConfidence) {
                        val isNoPotato = diseaseName == "z no potato" || diseaseName == "z_no_potato"
                        if (isNoPotato && confidence >= 0.70f) {
                            stringResource(R.string.result_confidence, "<70")
                        } else {
                            stringResource(R.string.result_confidence, "< 70")
                        }
                    } else {
                        stringResource(
                            R.string.result_confidence,
                            String.format("%.1f", confidence * 100)
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}