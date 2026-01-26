package com.tesis.potatodiseaseai.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
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
                title = { Text("Historial de Análisis") },
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
                .padding(innerPadding)
        ) {
            // Info del almacenamiento
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total de análisis",
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
                            text = "Almacenamiento",
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay análisis guardados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Captura una foto desde el escáner",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(detections, key = { it.id }) { detection ->
                        DetectionCard(
                            detection = detection,
                            onClick = {
                                // Navegar a la pantalla de resultados
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
            title = { Text("Eliminar análisis") },
            text = { 
                Text("¿Estás seguro de que deseas eliminar este análisis de ${detection.diseaseName}?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Eliminar de Room
                                database.detectionDao().delete(detection)
                                // Eliminar imagen
                                FileUtils.deleteImage(Uri.parse(detection.imageUri))
                                showDeleteDialog = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
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
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // ✅ CAMBIADO: Usar AsyncImage con caché en lugar de rememberAsyncImagePainter
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(detection.imageUri))
                    .crossfade(true)
                    .memoryCacheKey(detection.imageUri) // Key única para caché
                    .diskCacheKey(detection.imageUri)
                    .build(),
                contentDescription = "Preview",
                imageLoader = ImageLoaderConfig.getImageLoader(context),
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                contentScale = ContentScale.Crop
            )

            // Información
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Encabezado con enfermedad e ícono
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detection.diseaseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Confianza: ${String.format("%.1f", detection.confidence * 100)}%",
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
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Footer con fecha y botón eliminar
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
                        onClick = { 
                            onDelete()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// PREVIEWS
// ============================================

@Preview(showBackground = true)
@Composable
fun DetectionCardPreview() {
    PotatoDiseaseAITheme {
        DetectionCard(
            detection = DetectionEntity(
                id = 1,
                imageUri = "",
                disease = "late blight",
                diseaseName = "Tizón tardío",
                confidence = 0.95f,
                timestamp = System.currentTimeMillis()
            ),
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetectionCardHealthyPreview() {
    PotatoDiseaseAITheme {
        DetectionCard(
            detection = DetectionEntity(
                id = 2,
                imageUri = "",
                disease = "healthy",
                diseaseName = "Planta sana",
                confidence = 0.98f,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            onDelete = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun HistoryScreenEmptyPreview() {
    PotatoDiseaseAITheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No hay análisis guardados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Captura una foto desde el escáner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
fun HistoryScreenWithDataPreview() {
    PotatoDiseaseAITheme {
        Surface {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) { index ->
                    DetectionCard(
                        detection = when (index) {
                            0 -> DetectionEntity(
                                id = 1,
                                imageUri = "",
                                disease = "late blight",
                                diseaseName = "Tizón tardío",
                                confidence = 0.95f,
                                timestamp = System.currentTimeMillis()
                            )
                            1 -> DetectionEntity(
                                id = 2,
                                imageUri = "",
                                disease = "healthy",
                                diseaseName = "Planta sana",
                                confidence = 0.98f,
                                timestamp = System.currentTimeMillis() - 7200000
                            )
                            else -> DetectionEntity(
                                id = 3,
                                imageUri = "",
                                disease = "early blight",
                                diseaseName = "Tizón temprano",
                                confidence = 0.87f,
                                timestamp = System.currentTimeMillis() - 86400000
                            )
                        },
                        onClick = {},
                        onDelete = {}
                    )
                }
            }
        }
    }
}

