package com.jalloft.promo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Catálogo de apps da Jalloft, vindo de jalloft.com/api/apps (cadastrado no
 * Sanity). O JSON bruto fica em cache no aparelho: a tela abre na hora com a
 * última lista conhecida e atualiza quando a rede responder.
 */
object JalloftPromo {

    /** Endpoint do catálogo. Troque só para testar contra um servidor local. */
    @Volatile
    var endpoint: String = "https://jalloft.com/api/apps"

    /** Fonte dos rótulos em caixa-alta da [MoreAppsActivity] (o design usa JetBrains Mono). */
    @Volatile
    var monoFontFamily: FontFamily = FontFamily.Monospace

    /**
     * Toque em Baixar/Abrir na [MoreAppsActivity] — ponto para logar no Analytics
     * do app: `packageName` tocado e se já estava instalado.
     */
    @Volatile
    var onAppClick: ((packageName: String, installed: Boolean) -> Unit)? = null

    /** Abre a tela "Mais apps". Funciona em apps XML ou Compose. */
    fun open(context: Context) {
        val intent = Intent(context, MoreAppsActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private const val CACHE_DIR = "jalloft_promo"
    private const val CACHE_FILE = "apps.json"
    private const val TIMEOUT_MS = 10_000

    /** Última lista salva, sem rede. `null` se nunca carregou. */
    suspend fun cachedApps(context: Context): List<PromoApp>? = withContext(Dispatchers.IO) {
        val file = cacheFile(context)
        if (!file.exists()) return@withContext null
        runCatching { parse(file.readText(), context) }.getOrNull()
    }

    /** Baixa o catálogo, atualiza o cache e devolve a lista (sem o app atual). */
    suspend fun fetchApps(context: Context): List<PromoApp> = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val apps = parse(body, context) // valida antes de sobrescrever o cache
            cacheFile(context).apply { parentFile?.mkdirs() }.writeText(body)
            apps
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context) = File(File(context.cacheDir, CACHE_DIR), CACHE_FILE)

    private fun parse(json: String, context: Context): List<PromoApp> {
        val locale = siteLocale(context)
        val self = context.packageName
        val array: JSONArray = JSONObject(json).getJSONArray("apps")
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val packageName = item.optString("packageName")
                if (packageName.isEmpty() || packageName == self) continue
                add(
                    PromoApp(
                        packageName = packageName,
                        name = item.optString("name"),
                        iconUrl = item.optStringOrNull("icon"),
                        category = item.optJSONObject("categoryLabel")?.localized(locale)
                            ?: item.optStringOrNull("category"),
                        description = item.optJSONObject("description")?.localized(locale),
                        isNew = item.optBoolean("isNew"),
                        playStoreUrl = item.optStringOrNull("playStoreUrl")
                            ?: "https://play.google.com/store/apps/details?id=$packageName",
                    )
                )
            }
        }
    }

    // Os textos do site existem em en, pt-br e es
    private fun siteLocale(context: Context): String {
        val locale: Locale = context.resources.configuration.locales[0]
        return when (locale.language) {
            "pt" -> "pt-br"
            "es" -> "es"
            else -> "en"
        }
    }

    private fun JSONObject.localized(locale: String): String? =
        optStringOrNull(locale) ?: optStringOrNull("en")

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
