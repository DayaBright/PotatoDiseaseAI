package com.tesis.potatodiseaseai.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.ui.theme.PotatoDiseaseAITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    innerPadding: PaddingValues,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: HelpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimensions.spacingMedium,
                    end = Dimensions.spacingMedium,
                    top = padding.calculateTopPadding() + Dimensions.spacingMedium,
                    bottom = innerPadding.calculateBottomPadding() + Dimensions.spacingMedium
                ),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMedium)
            ) {
                // ── Sección: Cómo usar la app ──
                item {
                    HelpSection(
                        title = "¿Cómo usar la aplicación?",
                        icon = Icons.Outlined.Info,
                        items = listOf(
                            "Captura una foto de la hoja de papa desde la pantalla de escaneo",
                            "Espera el análisis automático con IA",
                            "Revisa el diagnóstico y las recomendaciones",
                            "Consulta el historial de análisis previos"
                        )
                    )
                }

                // ── Sección: Consejos de captura ──
                item {
                    HelpSection(
                        title = "Consejos para mejores resultados",
                        icon = Icons.Outlined.PhotoCamera,
                        items = listOf(
                            "Usa buena iluminación natural",
                            "Enfoca bien la hoja afectada",
                            "Evita sombras sobre la hoja",
                            "Captura hojas con síntomas visibles"
                        )
                    )
                }

                // ── Sección: Guía de Patologías ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimensions.spacingSmall),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimensions.iconSizeSmall)
                        )
                        Text(
                            text = "Guía de Patologías",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    Text(
                        text = "Toca una enfermedad para ver información detallada, síntomas y recomendaciones de manejo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Cards de enfermedades ──
                items(
                    uiState.enfermedades,
                    key = { it.id }
                ) { enfermedad ->
                    DiseaseCard(
                        enfermedad = enfermedad,
                        onClick = { onNavigateToDetail(enfermedad.id) }
                    )
                }

                // ── Sección: Contacto ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.spacingMedium)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ContactSupport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "¿Necesitas ayuda?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                                    Text(
                                        text = "Para soporte técnico o consultas agronómicas, contacta con nuestro equipo.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card de enfermedad con imagen normal y nombre.
 * Al hacer clic navega al detalle de la enfermedad.
 */
@Composable
private fun DiseaseCard(
    enfermedad: EnfermedadEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isHealthy = enfermedad.labelCnn.lowercase().contains("healthy")
    val imageResId = getDrawableResId(context, enfermedad.imagenReferencia)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.cardImageSize)
                .padding(Dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de la enfermedad
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Imagen: ${enfermedad.nombre}",
                    modifier = Modifier
                        .size(Dimensions.cardImageSize - Dimensions.spacingMedium)
                        .clip(RoundedCornerShape(Dimensions.cornerRadius)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder si no hay imagen
                Box(
                    modifier = Modifier
                        .size(Dimensions.cardImageSize - Dimensions.spacingMedium)
                        .clip(RoundedCornerShape(Dimensions.cornerRadius))
                        .then(
                            Modifier.fillMaxSize()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimensions.iconSizeMedium)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimensions.spacingMedium))

            // Nombre y label
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = enfermedad.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingExtraSmall))
                Text(
                    text = enfermedad.agenteCausal.take(60) +
                            if (enfermedad.agenteCausal.length > 60) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indicador sano/enfermo
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle
                              else Icons.Default.Warning,
                contentDescription = if (isHealthy) "Sana" else "Enfermedad",
                tint = if (isHealthy) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(Dimensions.iconSizeSmall)
                    .padding(end = Dimensions.spacingExtraSmall)
            )
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.spacingExtraSmall)
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = Dimensions.spacingSmall),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium
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
private fun getDrawableResId(context: Context, name: String): Int {
    if (name.isBlank()) return 0
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}