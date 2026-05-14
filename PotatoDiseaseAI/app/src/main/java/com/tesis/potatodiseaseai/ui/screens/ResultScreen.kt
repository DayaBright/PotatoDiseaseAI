package com.tesis.potatodiseaseai.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.*
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
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.data.repository.AnalisisRepository
import com.tesis.potatodiseaseai.ui.screens.components.CachedImage
import com.tesis.potatodiseaseai.ui.screens.components.DiagnosisCard
import com.tesis.potatodiseaseai.utils.LabelNormalizer
import com.tesis.potatodiseaseai.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var expandedDetail by remember { mutableStateOf<Pair<String, String>?>(null) }

    // ── Cargar datos de la enfermedad desde Room ──
    val db = remember { AppDatabase.getDatabase(context) }
    var enfermedad by remember { mutableStateOf<EnfermedadEntity?>(null) }

    LaunchedEffect(disease) {
        enfermedad = withContext(Dispatchers.IO) {
            val normalizedLabel = LabelNormalizer.normalize(disease)
            db.enfermedadDao().getByLabel(normalizedLabel)
        }
    }
    
    val diseaseName = enfermedad?.nombre ?: disease
    val isHealthy = disease.lowercase().contains("healthy")
    val isLowConfidence = confidence < 0.70f
    val isSaved = detectionId != null && detectionId != 0L

    // Listas de recomendaciones desde la BD
    val prevencion = enfermedad?.getPrevencionList() ?: emptyList()
    val controlQuimico = enfermedad?.getControlQuimicoList() ?: emptyList()
    val controlBiologico = enfermedad?.getControlBiologicoList() ?: emptyList()

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
                            containerColor = AppTheme.colors.lowConfidenceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.result_low_confidence_tips_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.lowConfidenceText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.result_low_confidence_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.lowConfidenceText
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
                            tint = AppTheme.colors.lowConfidenceAccent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(tipResId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                // ── Confianza suficiente: mostrar recomendaciones desde BD ──

                // — Prevención —
                if (prevencion.isNotEmpty()) {
                    item {
                        RecommendationSection(
                            icon = Icons.Outlined.Shield,
                            title = stringResource(R.string.result_prevention_title),
                            items = prevencion,
                            containerColor = AppTheme.colors.preventionContainer,
                            contentColor = AppTheme.colors.preventionText,
                            accentColor = AppTheme.colors.preventionAccent
                        )
                    }
                }

                // — Control Químico —
                if (controlQuimico.isNotEmpty() && !controlQuimico.any { it.startsWith("No requiere") }) {
                    item {
                        RecommendationSection(
                            icon = Icons.Outlined.Science,
                            title = stringResource(R.string.result_chemical_control_title),
                            items = controlQuimico,
                            containerColor = AppTheme.colors.chemicalControlContainer,
                            contentColor = AppTheme.colors.chemicalControlText,
                            accentColor = AppTheme.colors.chemicalControlAccent,
                            onExpand = if (enfermedad?.detalleControlQuimico?.isNotBlank() == true) {
                                { expandedDetail = Pair("Detalle del Tratamiento Químico", enfermedad!!.detalleControlQuimico) }
                            } else null
                        )
                    }
                }

                // — Control Biológico —
                if (controlBiologico.isNotEmpty() && !controlBiologico.any { it.startsWith("No requiere") }) {
                    item {
                        RecommendationSection(
                            icon = Icons.Outlined.Eco,
                            title = stringResource(R.string.result_biological_control_title),
                            items = controlBiologico,
                            containerColor = AppTheme.colors.biologicalControlContainer,
                            contentColor = AppTheme.colors.biologicalControlText,
                            accentColor = AppTheme.colors.biologicalControlAccent,
                            onExpand = if (enfermedad?.detalleControlBiologico?.isNotBlank() == true) {
                                { expandedDetail = Pair("Detalle del Tratamiento Biológico", enfermedad!!.detalleControlBiologico) }
                            } else null
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

    expandedDetail?.let { (title, detail) ->
        AlertDialog(
            onDismissRequest = { expandedDetail = null },
            title = {
                Text(text = title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = detail, style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                TextButton(onClick = { expandedDetail = null }) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Sección reutilizable para mostrar una lista de recomendaciones con ícono y título.
 */
@Composable
private fun RecommendationSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onExpand: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onExpand != null) it.clickable(onClick = onExpand) else it },
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                if (onExpand != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Ver detalles",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}