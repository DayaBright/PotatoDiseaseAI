package com.tesis.potatodiseaseai.ui.screens

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.tesis.potatodiseaseai.ui.screens.components.CameraPreview

@Composable
fun ScannerScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: ScannerViewModel = viewModel {
        ScannerViewModel(context)
    }
    val uiState by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraState = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Mostrar snackbar sin LaunchedEffect aquí
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Picker de galería (Photo Picker, no requiere permiso)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.onCaptureSuccess(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Vista previa de cámara
        CameraPreview(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onReady = { imageCapture, camera ->
                imageCaptureState.value = imageCapture
                cameraState.value = camera
                camera.cameraInfo.torchState.observe(lifecycleOwner) { torch ->
                    val enabled = torch == TorchState.ON
                    if (enabled != uiState.flashEnabled) {
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
                val name = "PD_${System.currentTimeMillis()}"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                }
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            vm.onCaptureSuccess(outputFileResults.savedUri)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            vm.onCaptureError(exception.message ?: "Error al capturar")
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
                cameraState.value?.cameraControl?.enableTorch(uiState.flashEnabled.not())
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

        // Abrir galería (Photo Picker)
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

        // Snackbar para feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Mostrar resultado de clasificación
        if (uiState.isClassifying) {
            Text(
                text = "Analizando...",
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (uiState.classification != null && uiState.confidence != null) {
            Text(
                text = "Enfermedad: ${uiState.classification}\nConfianza: ${String.format("%.2f", uiState.confidence!! * 100)}%",
                modifier = Modifier.align(Alignment.TopCenter)
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