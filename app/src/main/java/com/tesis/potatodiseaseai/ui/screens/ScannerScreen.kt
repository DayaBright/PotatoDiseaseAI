package com.tesis.potatodiseaseai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.tesis.potatodiseaseai.R
import com.tesis.potatodiseaseai.ui.screens.components.CameraPreview
import com.tesis.potatodiseaseai.ui.theme.Dimensions
import com.tesis.potatodiseaseai.utils.FileUtils
import com.tesis.potatodiseaseai.utils.ImageUtils
import java.io.File

@Composable
fun ScannerScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: ScannerViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraState = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        FileUtils.cleanTempFiles(context)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
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

        // Botón Flash movido más abajo (debajo de la barra de estado)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = {
                    val newFlashState = !uiState.flashEnabled
                    vm.toggleFlash()
                    cameraState.value?.cameraControl?.enableTorch(newFlashState) // ✅ Controla el hardware
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (uiState.flashEnabled) 
                        Icons.Outlined.FlashOn 
                    else 
                        Icons.Outlined.FlashOff,
                    contentDescription = stringResource(R.string.scanner_flash),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Controles inferiores más compactos con gradiente sutil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Galería (izquierda)
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(56.dp)
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

                // Botón Captura (centro)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(5.dp, Color.White, CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val imageCapture = imageCaptureState.value ?: return@IconButton
                            vm.startCapture()
                            
                            val tempFile = File.createTempFile("temp_", ".jpg", context.cacheDir)
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            
                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val originalUri = android.net.Uri.fromFile(tempFile)
                                        
                                        // ✅ Corregir rotación ANTES de procesar
                                        val correctedUri = ImageUtils.fixImageRotation(context, originalUri)
                                        
                                        vm.onCaptureSuccess(correctedUri)
                                        
                                        // Limpiar archivo temporal original si se creó uno nuevo
                                        if (correctedUri != originalUri) {
                                            tempFile.delete()
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        vm.onCaptureError(exception.message ?: context.getString(R.string.scanner_error_capture))
                                        tempFile.delete()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {}
                }

                // Espacio vacío para balance visual
                Spacer(modifier = Modifier.size(56.dp))
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
        )

        // Loading indicator
        if (uiState.isClassifying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
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