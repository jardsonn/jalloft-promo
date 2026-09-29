package com.jalloft.promo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jalloft.promo.ui.MoreAppsScreen

/**
 * A tela "Mais apps" como Activity pronta — abre de qualquer app, Compose ou
 * XML, com [JalloftPromo.open]. Apps em Compose podem preferir chamar
 * [MoreAppsScreen] direto dentro da própria navegação.
 */
class MoreAppsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MoreAppsScreen(
                onBack = ::finish,
                monoFontFamily = JalloftPromo.monoFontFamily,
                onAppClick = { packageName, installed ->
                    JalloftPromo.onAppClick?.invoke(packageName, installed)
                },
            )
        }
    }
}
