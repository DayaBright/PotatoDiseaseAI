package com.tesis.potatodiseaseai.ui.screens.components

import android.content.Context
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onReady: (imageCapture: ImageCapture, camera: Camera) -> Unit
) {
    val previewView = remember { 
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider
        
        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        provider.unbindAll()
        
        val camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        onReady(imageCapture, camera)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}