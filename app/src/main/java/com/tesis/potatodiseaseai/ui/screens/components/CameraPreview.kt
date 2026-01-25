package com.tesis.potatodiseaseai.ui.screens.components

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner


@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onReady: (imageCapture: ImageCapture, camera: Camera) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }

    AndroidView(factory = { previewView })

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        imageCaptureState.value = imageCapture
        cameraState.value = camera

        onReady(imageCapture, camera)
    }
}