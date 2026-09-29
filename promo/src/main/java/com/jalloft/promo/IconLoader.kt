package com.jalloft.promo

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Carregador mínimo de ícones (memória + disco) para a lib não depender de
 * Coil/Glide — cada app usa uma versão diferente, ou nenhuma. Os ícones são
 * poucos, pequenos (192px) e mudam raramente.
 */
internal object IconLoader {

    private val memory = LruCache<String, ImageBitmap>(32)

    fun cached(url: String): ImageBitmap? = memory.get(url)

    suspend fun load(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        memory.get(url)?.let { return@withContext it }
        val file = File(File(context.cacheDir, "jalloft_promo/icons"), sha1(url))
        val bytes = if (file.exists()) {
            file.readBytes()
        } else {
            runCatching { download(url) }.getOrNull()?.also { data ->
                file.parentFile?.mkdirs()
                file.writeBytes(data)
            }
        } ?: return@withContext null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        if (bitmap == null) {
            file.delete() // arquivo corrompido: baixa de novo na próxima vez
            return@withContext null
        }
        memory.put(url, bitmap)
        bitmap
    }

    private fun download(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
