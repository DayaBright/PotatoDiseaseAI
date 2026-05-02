package com.tesis.potatodiseaseai.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.model.DiseaseDatabase
import com.tesis.potatodiseaseai.data.repository.AnalisisRepository
import com.tesis.potatodiseaseai.ui.screens.components.CachedImage
import com.tesis.potatodiseaseai.ui.screens.components.DiagnosisCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: String,
    disease: String,
    confidence: Float,
    detectionId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AnalisisRepository(context) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val diseaseName = DiseaseDatabase.getDiseaseName(disease)
    val recommendations = DiseaseDatabase.getRecommendations(disease)
    val isHealthy = disease.lowercase().contains("healthy")
    val isLowConfidence = confidence < 0.70f
    val isSaved = detectionId != null && detectionId != 0L

    // Tips para fotos de baja confianza
    val photoTips = listOf(
        R.string.result_tip_clean_camera,
        R.string.result_tip_center_leaf,
        R.string.result_tip_ensure_leaf,
        R.string.result_tip_good_lighting,
        R.string.result_tip_avoid_blur,
        R.string.result_tip_single_leaf
    )
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.result_delete_title)) },
            text = { Text(stringResource(R.string.result_delete_message)) },
            confirmButton = {
    TextButton(
        onClick = {
            showDeleteDialog = false
            scope.launch {
                detectionId?.let {
                    if (repository.deleteAnalisisById(it, imageUri)) {
                        onDeleted()
                    }
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
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.history_delete_cancel))
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.result_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //  Usar CachedImage
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // Imagen guardada es 1:1 cuadrada
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    CachedImage(
                        imageUri = imageUri,
                        contentDescription = stringResource(R.string.cd_analyzed_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Usar DiagnosisCard
            item {
                DiagnosisCard(
                    diseaseName = diseaseName,
                    confidence = confidence,
                    isHealthy = isHealthy
                )
            }

            if (isLowConfidence) {
                //  Confianza baja: mostrar consejos para mejor foto
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.result_low_confidence_tips_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.result_low_confidence_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                items(photoTips) { tipResId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(tipResId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                // ── Confianza suficiente: mostrar recomendaciones normales ──
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.result_recommendations_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                items(recommendations) { recommendation ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "•",
                            modifier = Modifier.padding(end = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Botón para eliminar (solo si se guardó en historial, ID > 0)
            if (isSaved) {
                item {
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_delete_button))
                    }
                }
            }
        }
    }
}