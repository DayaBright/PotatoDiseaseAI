package com.tesis.potatodiseaseai.ui.screens.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tesis.potatodiseaseai.utils.ImageLoaderConfig

@Composable
fun CachedImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(Uri.parse(imageUri))
            .crossfade(true)
            .memoryCacheKey(imageUri)
            .diskCacheKey(imageUri)
            .build(),
        contentDescription = contentDescription,
        imageLoader = ImageLoaderConfig.getImageLoader(context),
        modifier = modifier,
        contentScale = contentScale
    )
}