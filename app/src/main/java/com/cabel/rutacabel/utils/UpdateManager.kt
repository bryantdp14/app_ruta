package com.cabel.rutacabel.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.cabel.rutacabel.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private val BASE_URL = if (BuildConfig.DEBUG) BuildConfig.BASE_URL_DEV else BuildConfig.BASE_URL_PROD
        private val UPDATE_URL = "${BASE_URL}api/updates/check.json"
    }

    fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            val config = fetchUpdateConfig()
            if (config != null) {
                try {
                    val latestVersion = config.getString("version")
                    val apkUrl = config.getString("url")
                    
                    withContext(Dispatchers.Main) {
                        if (isNewVersionAvailable(latestVersion)) {
                            showUpdateDialog(apkUrl)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing update config", e)
                }
            }
        }
    }

    private fun isNewVersionAvailable(latestVersion: String): Boolean {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = pInfo.versionName
            compareVersions(latestVersion, currentVersion) > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val levels1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val levels2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(levels1.size, levels2.size)
        for (i in 0 until length) {
            val v1Level = if (i < levels1.size) levels1[i] else 0
            val v2Level = if (i < levels2.size) levels2[i] else 0
            if (v1Level > v2Level) return 1
            if (v1Level < v2Level) return -1
        }
        return 0
    }

    private fun showUpdateDialog(apkUrl: String) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Actualización Disponible")
            .setMessage("Hay una nueva versión disponible. ¿Desea descargarla ahora?")
            .setPositiveButton("Actualizar") { _, _ ->
                startDownload(apkUrl)
            }
            .setNegativeButton("Después", null)
            .show()
    }

    private fun fetchUpdateConfig(): JSONObject? {
        if (UPDATE_URL.contains("your-api-endpoint.com") || BASE_URL.isEmpty()) {
            Log.w(TAG, "Update URL not configured, skipping check.")
            return null
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(UPDATE_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Connection", "close")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use { reader -> 
                    reader.readText() 
                }
                if (content.isBlank()) {
                    Log.w(TAG, "Empty response received from update server")
                    null
                } else {
                    JSONObject(content)
                }
            } else {
                Log.w(TAG, "Server returned response code: $responseCode")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching update config: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing update config", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun startDownload(apkUrl: String) {
        Toast.makeText(context, "Descargando nueva versión...", Toast.LENGTH_SHORT).show()

        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Actualización Ruta CABEL")
            .setDescription("Descargando actualización...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting installation", e)
            Toast.makeText(context, "Error al iniciar la instalación", Toast.LENGTH_SHORT).show()
        }
    }
}
