package com.tesis.potatodiseaseai.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tesis.potatodiseaseai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class UpdateManager(private val context: Context) {

    // Cambia esto a tu usuario/repositorio real de GitHub
    private val githubRepo = "Kevin17Vichi/PotatoDiseaseAI"
    private val apiUrl = "https://api.github.com/repos/Kevin17Vichi/PotatoDiseaseAI/releases/latest"

    /**
     * Verifica si el dispositivo está conectado a Wi-Fi
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isConnectedToWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Comprueba la última versión en GitHub Releases.
     * Retorna un par (Versión, URL de descarga) si hay una actualización,
     * o null si estamos en la última versión o hay error.
     */
    suspend fun checkForUpdates(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.getString("tag_name") // Ej. "v1.1.0"
                val assets = json.getJSONArray("assets")

                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                val currentVersion = "v${BuildConfig.VERSION_NAME}"

                // Comparación muy básica. Para producción es mejor comparar semver.
                if (tagName != currentVersion && downloadUrl.isNotEmpty()) {
                    return@withContext Pair(tagName, downloadUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * Inicia la descarga del APK y registra el receiver para la instalación.
     */
    fun downloadAndInstallUpdate(apkUrl: String, version: String) {
        Toast.makeText(context, "Iniciando descarga de $version...", Toast.LENGTH_SHORT).show()

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Actualización de PotatoDiseaseAI")
            .setDescription("Descargando versión $version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PotatoDiseaseAI_$version.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Toast.makeText(context, "Descarga completada", Toast.LENGTH_SHORT).show()
                    installApk(downloadId)
                    context.unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    /**
     * Lanza el intent para instalar el APK descargado
     */
    private fun installApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al abrir el instalador", Toast.LENGTH_SHORT).show()
        }
    }
}
