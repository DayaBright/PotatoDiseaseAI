package com.tesis.potatodiseaseai.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

object ImageLoaderConfig {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * ✅ ImageLoader optimizado con caché de memoria y disco
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.25) // Usa 25% de memoria disponible
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .respectCacheHeaders(false) // Ignorar headers HTTP
                .build()
                .also { imageLoader = it }
        }
    }
}