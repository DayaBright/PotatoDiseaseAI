package com.tesis.potatodiseaseai.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.ui.theme.Dimensions

/**
 * Pantalla de detalle de una enfermedad.
 * Muestra información completa: imagen, agente causal, tipo de agente,
 * patrón visual, impacto, prevención, control químico, control biológico y fuentes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetailScreen(
    enfermedad: EnfermedadEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(enfermedad.nombre, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Dimensions.spacingMedium,
                end = Dimensions.spacingMedium,
                top = Dimensions.spacingMedium,
                bottom = Dimensions.spacingLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMedium)
        ) {
            // — Imágenes: referencia y Grad-CAM (lado a lado si existen ambas) —
            val normalResId = getDrawableResId(context, enfermedad.imagenReferencia)
            val gradcamResId = getDrawableResId(context, enfermedad.imagenGradcam)

            if (normalResId != 0 && gradcamResId != 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingMedium)
                    ) {
                        // Imagen de referencia
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Imagen de referencia",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Dimensions.spacingExtraSmall)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
                                shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
                            ) {
                                Image(
                                    painter = painterResource(id = normalResId),
                                    contentDescription = "Imagen de referencia: ${enfermedad.nombre}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimensions.imageHeightLarge)
                                        .clip(RoundedCornerShape(Dimensions.cornerRadiusMedium)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Imagen Grad-CAM
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Visualización Grad-CAM",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Dimensions.spacingExtraSmall)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
                                shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
                            ) {
                                Image(
                                    painter = painterResource(id = gradcamResId),
                                    contentDescription = "Grad-CAM: ${enfermedad.nombre}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimensions.imageHeightLarge)
                                        .clip(RoundedCornerShape(Dimensions.cornerRadiusMedium)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            } else {
                if (normalResId != 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
                            shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
                        ) {
                            Image(
                                painter = painterResource(id = normalResId),
                                contentDescription = "Imagen de referencia: ${enfermedad.nombre}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimensions.imageHeightLarge)
                                    .clip(RoundedCornerShape(Dimensions.cornerRadiusMedium)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (gradcamResId != 0) {
                    item {
                        Column {
                            Text(
                                text = "Visualización Grad-CAM",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Dimensions.spacingExtraSmall)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
                                shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
                            ) {
                                Image(
                                    painter = painterResource(id = gradcamResId),
                                    contentDescription = "Grad-CAM: ${enfermedad.nombre}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimensions.imageHeightLarge)
                                        .clip(RoundedCornerShape(Dimensions.cornerRadiusMedium)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // — Agente Causal y Tipo —
            if (enfermedad.agenteCausal.isNotBlank() && enfermedad.agenteCausal != "N/A — Planta sin patología detectada") {
                item {
                    DetailInfoCard(
                        icon = Icons.Outlined.Science,
                        title = "Agente Causal",
                        content = buildString {
                            append(enfermedad.agenteCausal)
                            if (enfermedad.tipoAgente.isNotBlank() && enfermedad.tipoAgente != "N/A") {
                                append("\nTipo: ${enfermedad.tipoAgente}")
                            }
                        }
                    )
                }
            }

            // — Patrón Visual —
            if (enfermedad.patronVisual.isNotBlank()) {
                item {
                    DetailInfoCard(
                        icon = Icons.Outlined.Visibility,
                        title = "Patrón Visual en la Hoja",
                        content = enfermedad.patronVisual
                    )
                }
            }

            // — Impacto —
            if (enfermedad.impacto.isNotBlank() && !enfermedad.impacto.startsWith("Clase de referencia")) {
                item {
                    DetailInfoCard(
                        icon = Icons.AutoMirrored.Outlined.TrendingDown,
                        title = "Impacto y Consecuencias",
                        content = enfermedad.impacto
                    )
                }
            }

            // — Prevención —
            val prevencion = enfermedad.getPrevencionList()
            if (prevencion.isNotEmpty()) {
                item {
                    DetailListCard(
                        icon = Icons.Outlined.Shield,
                        title = "Prevención",
                        items = prevencion,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // — Control Químico —
            val controlQuimico = enfermedad.getControlQuimicoList()
            if (controlQuimico.isNotEmpty() && !controlQuimico.any { it.startsWith("No requiere") }) {
                item {
                    DetailListCard(
                        icon = Icons.Outlined.Science,
                        title = "Control Químico",
                        items = controlQuimico,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // — Control Biológico —
            val controlBiologico = enfermedad.getControlBiologicoList()
            if (controlBiologico.isNotEmpty() && !controlBiologico.any { it.startsWith("No requiere") }) {
                item {
                    DetailListCard(
                        icon = Icons.Outlined.Eco,
                        title = "Control Biológico",
                        items = controlBiologico,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // — Fuentes —
            if (enfermedad.fuentes.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(Dimensions.spacingMedium)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                                )
                                Text(
                                    text = "Fuentes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimensions.spacingExtraSmall))
                            Text(
                                text = enfermedad.fuentes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card reutilizable para secciones de información del detalle (texto simple).
 */
@Composable
private fun DetailInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spacingMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Card reutilizable para secciones con listas numeradas (prevención, controles).
 */
@Composable
private fun DetailListCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>,
    containerColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spacingMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(Dimensions.iconSizeSmall)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.spacingExtraSmall),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                accentColor,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Obtiene el ID de un recurso drawable dado su nombre.
 * Retorna 0 si no existe o el nombre está vacío.
 */
@SuppressLint("DiscouragedApi")
private fun getDrawableResId(context: Context, name: String): Int {
    if (name.isBlank()) return 0
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}
