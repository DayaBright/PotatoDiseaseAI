package com.tesis.potatodiseaseai.utils

import android.util.Log

object AppLogger {
    
    private const val APP_PREFIX = "PotatoAI"
    
    /**
     * Log de debug
     */
    fun debug(tag: String, message: String) {
        Log.d("$APP_PREFIX:$tag", message)
    }
    
    /**
     * Log de error
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$APP_PREFIX:$tag", message, throwable)
    }
    
    /**
     * Log de warning
     */
    fun warning(tag: String, message: String) {
        Log.w("$APP_PREFIX:$tag", message)
    }
    
    /**
     * Log de info
     */
    fun info(tag: String, message: String) {
        Log.i("$APP_PREFIX:$tag", message)
    }
}