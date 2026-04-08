package com.tesis.potatodiseaseai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
        val icon: ImageVector,
        val title: String,
        val description: String,
        val highlights: List<OnboardingHighlight>,
        val accentColor: Color
)

data class OnboardingHighlight(
        val icon: ImageVector,
        val text: String,
        val isPositive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages =
            listOf(
                    OnboardingPage(
                            icon = Icons.Rounded.Eco,
                            title = "Detecta enfermedades\nen hojas de papa",
                            description = "Obtén resultados rápidos sin conexión a internet",
                            highlights =
                                    listOf(
                                            OnboardingHighlight(
                                                    Icons.Outlined.CameraAlt,
                                                    "Usa la cámara de tu celular"
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.Speed,
                                                    "Análisis instantáneo con IA"
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.WifiOff,
                                                    "Funciona sin internet"
                                            )
                                    ),
                            accentColor = Color(0xFF4CAF50)
                    ),
                    OnboardingPage(
                            icon = Icons.Rounded.PhotoCamera,
                            title = "¿Cómo tomar\nla foto correcta?",
                            description = "Sigue estos consejos para obtener resultados precisos",
                            highlights =
                                    listOf(
                                            OnboardingHighlight(
                                                    Icons.Outlined.LightMode,
                                                    "Buena iluminación",
                                                    true
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.CenterFocusStrong,
                                                    "Hoja centrada y enfocada",
                                                    true
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.Layers,
                                                    "Evita muchas hojas a la vez",
                                                    false
                                            )
                                    ),
                            accentColor = Color(0xFFFFA726)
                    ),
                    OnboardingPage(
                            icon = Icons.Rounded.Assessment,
                            title = "¿Qué obtengo\ncomo resultado?",
                            description =
                                    "Si detecta una enfermedad, podrás tomar decisiones a tiempo",
                            highlights =
                                    listOf(
                                            OnboardingHighlight(
                                                    Icons.Outlined.BugReport,
                                                    "Nombre de la enfermedad"
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.Analytics,
                                                    "Nivel de confianza del análisis"
                                            ),
                                            OnboardingHighlight(
                                                    Icons.Outlined.Checklist,
                                                    "Recomendaciones de tratamiento"
                                            )
                                    ),
                            accentColor = Color(0xFF42A5F5)
                    )
            )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Fondo decorativo animado
        val currentPage = pagerState.currentPage
        val targetColor = pages[currentPage].accentColor.copy(alpha = 0.08f)
        val animatedBgColor by
                animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(600),
                        label = "bg_color"
                )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Círculos decorativos de fondo
            drawCircle(
                    color = animatedBgColor,
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                    color = animatedBgColor.copy(alpha = animatedBgColor.alpha * 0.5f),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.1f, size.height * 0.7f)
            )
        }

        Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón "Saltar" en la esquina superior derecha
            Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                            text = "Saltar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                    )
                }
            }

            // Contenido principal con HorizontalPager
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                OnboardingPageContent(
                        page = pages[page],
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
                )
            }

            // Indicadores de página + controles
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Indicadores de página (dots)
                Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, page ->
                        val isSelected = pagerState.currentPage == index
                        val dotColor by
                                animateColorAsState(
                                        targetValue =
                                                if (isSelected) page.accentColor
                                                else
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.2f
                                                        ),
                                        animationSpec = tween(300),
                                        label = "dot_$index"
                                )
                        val dotWidth by
                                animateDpAsState(
                                        targetValue = if (isSelected) 28.dp else 10.dp,
                                        animationSpec =
                                                spring(
                                                        dampingRatio =
                                                                Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                ),
                                        label = "dot_width_$index"
                                )

                        Box(
                                modifier =
                                        Modifier.height(10.dp)
                                                .width(dotWidth)
                                                .clip(CircleShape)
                                                .background(dotColor)
                        )
                    }
                }

                // Botón principal jjj ll,l,l,,,,
                val isLastPage = pagerState.currentPage == pages.size - 1
                val buttonColor by
                        animateColorAsState(
                                targetValue = pages[pagerState.currentPage].accentColor,
                                animationSpec = tween(400),
                                label = "button_color"
                        )

                Button(
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        elevation =
                                ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                )
                ) {
                    Text(
                            text = if (isLastPage) "¡Empezar!" else "Siguiente",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    // Animación de entrada del ícono principal
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by
            infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(2000, easing = EaseInOutCubic),
                                    repeatMode = RepeatMode.Reverse
                            ),
                    label = "icon_scale"
            )

    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Ícono principal con fondo circular decorativo
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 32.dp)) {
            // Círculo exterior decorativo
            Box(
                    modifier =
                            Modifier.size((120 * iconScale).dp)
                                    .clip(CircleShape)
                                    .background(page.accentColor.copy(alpha = 0.1f))
            )
            // Círculo interior
            Box(
                    modifier =
                            Modifier.size((88 * iconScale).dp)
                                    .clip(CircleShape)
                                    .background(page.accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = page.accentColor
                )
            }
        }

        // Título
        Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Descripción
        Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Tarjetas de highlights
        Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
            page.highlights.forEach { highlight ->
                HighlightRow(highlight = highlight, accentColor = page.accentColor)
            }
        }
    }
}

@Composable
private fun HighlightRow(highlight: OnboardingHighlight, accentColor: Color) {
    val bgColor =
            if (highlight.isPositive) accentColor.copy(alpha = 0.08f)
            else Color(0xFFE53935).copy(alpha = 0.08f)

    val iconTint = if (highlight.isPositive) accentColor else Color(0xFFE53935)

    val statusIcon = if (highlight.isPositive) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ícono principal
            Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
            )

            // Texto
            Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
            )

            // Indicador de estado (check/cross)
            Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                    // ,mmmm
                    )
        }
    }
}
