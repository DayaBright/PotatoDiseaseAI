package com.tesis.potatodiseaseai.ui.screens

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.ui.theme.PotatoDiseaseAITheme
import com.tesis.potatodiseaseai.utils.DateUtils
import com.tesis.potatodiseaseai.utils.FileUtils
import com.tesis.potatodiseaseai.utils.ImageLoaderConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    innerPadding: PaddingValues,
    onNavigateToResult: (imageUri: String, disease: String, confidence: Float, detectionId: Long) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    val detections by database.detectionDao().getAllDetections().collectAsState(initial = emptyList())
    val storageSize = remember { FileUtils.getTotalImagesSizeInMB(context) }
    
    var showDeleteDialog by remember { mutableStateOf<DetectionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                //.padding(innerPadding)
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
                            text = "${detections.size}",
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
                            text = String.format("%.2f MB", storageSize),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Lista de detecciones
            if (detections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize()
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
                    items(detections, key = { it.id }) { detection ->
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
                            onDelete = { showDeleteDialog = detection }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    showDeleteDialog?.let { detection ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { 
                Text(stringResource(R.string.history_delete_message, detection.diseaseName)) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                database.detectionDao().delete(detection)
                                FileUtils.deleteImage(Uri.parse(detection.imageUri))
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.history_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.history_delete_cancel))
                }
            }
        )
    }
}

@Composable
fun DetectionCard(
    detection: DetectionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isHealthy = detection.disease.lowercase().contains("healthy")
    val formattedDate = remember(detection.timestamp) {
        DateUtils.formatTimestamp(detection.timestamp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.cardHeight)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(detection.imageUri))
                    .crossfade(true)
                    .memoryCacheKey(detection.imageUri)
                    .diskCacheKey(detection.imageUri)
                    .build(),
                contentDescription = stringResource(R.string.cd_preview),
                imageLoader = ImageLoaderConfig.getImageLoader(context),
                modifier = Modifier
                    .width(Dimensions.cardImageSize)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = Dimensions.cornerRadiusMedium, bottomStart = Dimensions.cornerRadiusMedium)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(Dimensions.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detection.diseaseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spacingExtraSmall))
                        Text(
                            text = stringResource(
                                R.string.history_confidence,
                                String.format("%.1f", detection.confidence * 100)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isHealthy) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimensions.iconSizeSmall)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onDelete() },
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
    }
}