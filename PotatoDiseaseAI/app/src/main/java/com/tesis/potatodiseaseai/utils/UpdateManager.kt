package com.tesis.potatodiseaseai.utils


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.widget.Toast
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

class UpdateManager(private val context: Context) {

    // Cambia esto a tu usuario/repositorio real de GitHub
    private val githubRepo = "Kevin17Vichi/PotatoDiseaseAI"
    private val apiUrl = "https://api.github.com/repos/Kevin17Vichi/PotatoDiseaseAI/releases/latest"

    /**
     * Verifica si el dispositivo está conectado a Wi-Fi (Compatible desde API 21)
     */
    fun isConnectedToWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Código moderno para API 23 o superior
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            // Código clásico/legacy para API 21 y 22
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    /**
     * Comprueba la última versión en GitHub Releases.
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
                val tagName = json.getString("tag_name")
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
                    android.util.Log.d("UpdateManager", "URL DETECTADA: $downloadUrl")
                    return@withContext Pair(tagName, downloadUrl)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Dentro de checkForUpdates, antes del return de downloadUrl

        return@withContext null
    }

    /**
     * Inicia la descarga del APK sin usar DownloadManager.
     */
    suspend fun downloadAndInstallUpdate(apkUrl: String, version: String, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            var connection = URL(apkUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.addRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connect()

            if (connection.responseCode in 300..399) {
                val redirectUrl = connection.getHeaderField("Location")
                connection = URL(redirectUrl).openConnection() as HttpURLConnection
                connection.addRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Servidor devolvió error ${connection.responseCode}")
            }

            val fileLength = connection.contentLength
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PotatoDiseaseAI_$version.apk")
            
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    var lastProgress = 0
                    
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).toInt()
                            if (progress > lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) {
                                    onProgress(progress / 100f)
                                }
                            }
                        }
                    }
                }
            }
            return@withContext file

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    /**
     * Lanza el intent para instalar el APK descargado
     */
    fun installApk(file: File) {
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

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
