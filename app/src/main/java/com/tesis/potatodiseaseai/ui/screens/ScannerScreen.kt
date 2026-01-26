package com.tesis.potatodiseaseai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.tesis.potatodiseaseai.ui.screens.components.CameraPreview
import com.tesis.potatodiseaseai.utils.FileUtils
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
        // ✅ CRÍTICO: Limpiar archivos temporales al iniciar
        FileUtils.cleanTempFiles(context)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { vm.onCaptureSuccess(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        CameraPreview(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onReady = { imageCapture, camera ->
                imageCaptureState.value = imageCapture
                cameraState.value = camera
                camera.cameraInfo.torchState.observe(lifecycleOwner) { torch ->
                    if ((torch == TorchState.ON) != uiState.flashEnabled) {
                        vm.toggleFlash()
                    }
                }
            }
        )

        // Botón de captura
        IconButton(
            onClick = {
                val imageCapture = imageCaptureState.value ?: return@IconButton
                vm.startCapture()
                
                // Guardar en archivo temporal
                val tempFile = File.createTempFile("temp_", ".jpg", context.cacheDir)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val uri = android.net.Uri.fromFile(tempFile)
                            vm.onCaptureSuccess(uri)
                            
                            // ✅ CRÍTICO: Eliminar archivo temporal después de procesarlo
                            tempFile.deleteOnExit()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            vm.onCaptureError(exception.message ?: "Error al capturar")
                            // ✅ CRÍTICO: Eliminar archivo temporal en caso de error
                            tempFile.delete()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(72.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Capturar",
                modifier = Modifier.size(48.dp)
            )
        }

        // Toggle de flash
        IconButton(
            onClick = {
                vm.toggleFlash()
                cameraState.value?.cameraControl?.enableTorch(!uiState.flashEnabled)
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(56.dp)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (uiState.flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                contentDescription = "Flash"
            )
        }

        // Abrir galería
        IconButton(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(56.dp)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "Galería"
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (uiState.isClassifying) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Vista Previa Escáner",
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun ScannerScreenPreview() {
    ScannerScreen(innerPadding = PaddingValues())
}