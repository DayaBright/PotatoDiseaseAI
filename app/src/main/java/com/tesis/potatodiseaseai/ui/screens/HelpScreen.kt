package com.tesis.potatodiseaseai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.ui.theme.PotatoDiseaseAITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(innerPadding: PaddingValues) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(innerPadding)
                .padding(Dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMedium)
        ) {
            // Sección: Cómo usar la app
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

            // Sección: Consejos de captura
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

            // Sección: Enfermedades detectables
            item {
                HelpSection(
                    title = "Enfermedades que puede detectar",
                    icon = Icons.Outlined.BugReport,
                    items = listOf(
                        "Tizón temprano (Early Blight)",
                        "Tizón tardío (Late Blight)",
                        "Marchitez bacteriana",
                        "Virus del enrollamiento",
                        "Virus del mosaico",
                        "Nematodos",
                        "Plagas generales"
                    )
                )
            }

            // Sección: Contacto
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
                                imageVector = Icons.Outlined.ContactSupport,
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

@Preview(showBackground = true)
@Composable
fun HelpScreenPreview() {
    PotatoDiseaseAITheme {
        HelpScreen(innerPadding = PaddingValues())
    }
}