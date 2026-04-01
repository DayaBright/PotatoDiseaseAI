package com.tesis.potatodiseaseai.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.ui.screens.components.CameraPreview
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.utils.FileUtils
import com.tesis.potatodiseaseai.utils.ImageUtils
import java.io.File

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ScannerScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: ScannerViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraState = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val cameraPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        FileUtils.cleanTempFiles(context)
    }

    val galleryLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { vm.onCaptureSuccess(it) }
            }

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview de cámara ocupando TODA la pantalla
        CameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onReady = { imageCapture, camera ->
                    imageCaptureState.value = imageCapture
                    cameraState.value = camera
                }
        )

        // Fracción del lado menor que se recortará
        val guideFraction = 0.85f
        
        // Animación sutil de pulso para las esquinas
        val infiniteTransition = rememberInfiniteTransition(label = "guide_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "corner_alpha"
        )

        // Overlay con recorte cuadrado centrado
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension * guideFraction
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            val cornerRadius = 24.dp.toPx()
            val bracketLen = side * 0.15f  // largo de cada brazo de esquina
            val strokeW = 3.dp.toPx()

            // 1. Fondo oscuro con hueco transparente
            val cutoutPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(Offset(left, top), Size(side, side)),
                        cornerRadius = CornerRadius(cornerRadius)
                    )
                )
            }
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.45f))
            }

            // 2. Borde sutil del recuadro
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(left, top),
                size = Size(side, side),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Esquinas animadas (tipo "visor")
            val bracketColor = Color(0xFF4CAF50).copy(alpha = pulseAlpha) // Verde suave
            val bracketStroke = Stroke(width = strokeW, cap = StrokeCap.Round)

            // ── Esquina superior-izquierda
            drawLine(bracketColor, Offset(left + cornerRadius, top), Offset(left + bracketLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top + cornerRadius), Offset(left, top + bracketLen), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawArc(bracketColor, 180f, 90f, false, topLeft = Offset(left, top), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)

            // ── Esquina superior-derecha
            drawLine(bracketColor, Offset(left + side - cornerRadius, top), Offset(left + side - bracketLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(left + side, top + cornerRadius), Offset(left + side, top + bracketLen), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawArc(bracketColor, 270f, 90f, false, topLeft = Offset(left + side - cornerRadius * 2, top), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)

            // ── Esquina inferior-izquierda
            drawLine(bracketColor, Offset(left + cornerRadius, top + side), Offset(left + bracketLen, top + side), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top + side - cornerRadius), Offset(left, top + side - bracketLen), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawArc(bracketColor, 90f, 90f, false, topLeft = Offset(left, top + side - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)

            // ── Esquina inferior-derecha
            drawLine(bracketColor, Offset(left + side - cornerRadius, top + side), Offset(left + side - bracketLen, top + side), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(left + side, top + side - cornerRadius), Offset(left + side, top + side - bracketLen), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawArc(bracketColor, 0f, 90f, false, topLeft = Offset(left + side - cornerRadius * 2, top + side - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)
        }

        // Texto guía debajo del cuadro
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val guideBottom = (maxHeight.value + maxWidth.value * guideFraction) / 2f
            Text(
                text = stringResource(R.string.scanner_guide_text),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    //.fillMaxWidth()
                    .padding(top = guideBottom.dp + 16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.35f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // Botón Flash
        Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp).align(Alignment.TopStart)) {
            IconButton(
                    onClick = {
                        val newFlashState = !uiState.flashEnabled
                        vm.toggleFlash()
                        cameraState.value?.cameraControl?.enableTorch(
                                newFlashState
                        ) // ✅ Controla el hardware
                    },
                    modifier =
                            Modifier.padding(start = 16.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                        imageVector =
                                if (uiState.flashEnabled) Icons.Outlined.FlashOn
                                else Icons.Outlined.FlashOff,
                        contentDescription = stringResource(R.string.scanner_flash),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }
        }

        // Controles inferiores más compactos con gradiente sutil
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.3f),
                                                                Color.Black.copy(alpha = 0.5f)
                                                        )
                                        )
                                )
                                .padding(
                                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                                        top = 24.dp
                                )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Galería
                IconButton(
                        onClick = {
                            galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                            )
                        },
                        modifier =
                                Modifier.size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = stringResource(R.string.scanner_gallery),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                    )
                }

                // Botón Captura
                Box(
                        modifier =
                                Modifier.size(80.dp)
                                        .clip(CircleShape)
                                        .border(5.dp, Color.White, CircleShape)
                                        .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                ) {
                    IconButton(
                            onClick = {
                                val imageCapture = imageCaptureState.value ?: return@IconButton
                                vm.startCapture()

                                val tempFile =
                                        File.createTempFile("temp_", ".jpg", context.cacheDir)
                                val outputOptions =
                                        ImageCapture.OutputFileOptions.Builder(tempFile).build()

                                imageCapture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(
                                                    outputFileResults:
                                                            ImageCapture.OutputFileResults
                                            ) {
                                                val originalUri = android.net.Uri.fromFile(tempFile)

                                                // ✅ Corregir rotación ANTES de procesar
                                                val correctedUri =
                                                        ImageUtils.fixImageRotation(
                                                                context,
                                                                originalUri
                                                        )

                                                vm.onCaptureSuccess(correctedUri)

                                                // Limpiar archivo temporal original si se creó uno
                                                // nuevo
                                                if (correctedUri != originalUri) {
                                                    tempFile.delete()
                                                }
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                vm.onCaptureError(
                                                        exception.message
                                                                ?: context.getString(
                                                                        R.string
                                                                                .scanner_error_capture
                                                                )
                                                )
                                                tempFile.delete()
                                            }
                                        }
                                )
                            },
                            modifier =
                                    Modifier.size(62.dp).clip(CircleShape).background(Color.White)
                    ) {}
                }

                // Espacio vacío para balance visual
                Spacer(modifier = Modifier.size(56.dp))
            }
        }

        // Snackbar
        SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
        )

        // Loading indicator
        if (uiState.isClassifying) {
            Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
            ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                    )
                    Text(
                            text = stringResource(R.string.common_loading),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                    )
                }
            }
        }
    }
}
