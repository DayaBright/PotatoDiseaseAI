package com.tesis.potatodiseaseai.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
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
import com.tesis.potatodiseaseai.BuildConfig
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.utils.FileUtils
import com.tesis.potatodiseaseai.utils.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    innerPadding: PaddingValues,
    onNavigateToDetail: (Long) -> Unit = {},
    onRepeatTutorial: () -> Unit = {},
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
            val context = LocalContext.current
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

                // ── Sección: Tutorial ──
                item {
                    Button(
                        onClick = onRepeatTutorial,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        contentPadding = PaddingValues(Dimensions.spacingMedium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ContactSupport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "¿Repetir tutorial?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                            }
                        }
                    }
                }

                // ── Sección: Actualizaciones ──
                item {
                    UpdateCard()
                }

                // ── Sección: Manual de Usuario ──
                item {
                    ManualUsuarioCard(
                        onClick = {
                            FileUtils.openPdfFromCache(context, "Manual_Usuario.pdf")
                        }
                    )
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
                    text = if (enfermedad.tipoAgente.isNotBlank() && enfermedad.tipoAgente != "N/A")
                               "${enfermedad.tipoAgente} — ${enfermedad.agenteCausal}".take(70) +
                               if ("${enfermedad.tipoAgente} — ${enfermedad.agenteCausal}".length > 70) "…" else ""
                           else enfermedad.agenteCausal.take(60) +
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
 * Obtiene el ID de un recurso dado su nombre.
 * Retorna 0 si no existe o el nombre está vacío.
 */
@SuppressLint("DiscouragedApi")
private fun getDrawableResId(context: Context, name: String): Int {
    if (name.isBlank()) return 0
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

@Composable
private fun ManualUsuarioCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = "Manual de Usuario",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.iconSizeMedium)
                )
                Column {
                    Text(
                        text = "Manual de usuario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Visualiza la guía completa en formato PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = "Abrir PDF",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.iconSizeSmall)
            )
        }
    }
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    val updateManager = remember { UpdateManager(context) }
    val currentVersion = BuildConfig.VERSION_NAME

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = "Actualizaciones",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Buscar actualizaciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Versión actual: v$currentVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.iconSizeSmall),
                        strokeWidth = 2.dp
                    )
                } else if (isDownloading) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(Dimensions.iconSizeSmall),
                        strokeWidth = 2.dp
                    )
                } else if (updateAvailable != null) {
                    IconButton(
                        onClick = {
                            updateAvailable?.let { (version, url) ->
                                isDownloading = true
                                downloadProgress = 0f
                                Toast.makeText(context, "Iniciando descarga...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val file = updateManager.downloadAndInstallUpdate(url, version) { progress ->
                                        downloadProgress = progress
                                    }
                                    isDownloading = false
                                    if (file != null) {
                                        Toast.makeText(context, "Descarga completada", Toast.LENGTH_SHORT).show()
                                        updateManager.installApk(file)
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Descargar actualización",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (!updateManager.isConnectedToWifi()) {
                                Toast.makeText(context, "No tienes conexión a Wi-Fi", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            isChecking = true
                            coroutineScope.launch {
                                val result = updateManager.checkForUpdates()
                                isChecking = false
                                if (result != null) {
                                    updateAvailable = result
                                    Toast.makeText(context, "Nueva versión disponible: ${result.first}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Ya tienes la última versión", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (updateAvailable != null) {
                Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                Text(
                    text = "¡Hay una nueva actualización (${updateAvailable?.first}) disponible! Toca el ícono de descarga para actualizar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}