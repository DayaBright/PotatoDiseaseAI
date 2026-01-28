package com.tesis.potatodiseaseai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.ui.screens.components.CachedImage
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    innerPadding: PaddingValues,
    onNavigateToResult: (imageUri: String, disease: String, confidence: Float, detectionId: Long) -> Unit = { _, _, _, _ -> },
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar errores con Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // ✅ Indicador de carga
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Info del almacenamiento
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.spacingMedium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.spacingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.history_total),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${uiState.detections.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.history_storage),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = String.format("%.2f MB", uiState.storageSize),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Lista de detecciones
                if (uiState.detections.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.history_empty_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                            Text(
                                text = stringResource(R.string.history_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimensions.spacingMedium,
                            end = Dimensions.spacingMedium,
                            top = 0.dp,
                            bottom = innerPadding.calculateBottomPadding() + Dimensions.spacingMedium
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.cardSpacing)
                    ) {
                        items(uiState.detections, key = { it.id }) { detection ->
                            DetectionCard(
                                detection = detection,
                                onClick = {
                                    onNavigateToResult(
                                        detection.imageUri,
                                        detection.disease,
                                        detection.confidence,
                                        detection.id
                                    )
                                },
                                onDelete = { viewModel.showDeleteDialog(detection) }  // ✅ Llamar ViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    uiState.showDeleteDialog?.let { detection ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { 
                Text(stringResource(R.string.history_delete_message, detection.diseaseName)) 
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteDetection(detection) }  // ✅ Llamar ViewModel
                ) {
                    Text(
                        stringResource(R.string.history_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.history_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetectionCard(
    detection: DetectionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isHealthy = detection.disease.lowercase().contains("healthy")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CachedImage(
                imageUri = detection.imageUri,
                contentDescription = stringResource(R.string.cd_detection_preview),
                modifier = Modifier
                    .size(Dimensions.thumbnailSize)
                    .clip(RoundedCornerShape(Dimensions.cornerRadius))
            )

            Spacer(modifier = Modifier.width(Dimensions.spacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detection.diseaseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.history_confidence,
                                String.format("%.1f", detection.confidence * 100)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateUtils.formatTimestamp(detection.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimensions.iconSizeSmall)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(Dimensions.iconSizeSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
            }
        }
    }
}
