package com.jalloft.promo.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.jalloft.promo.JalloftPromo

/**
 * Exemplo de uso: abre direto a tela "Mais apps".
 * Com `-Pdev` o catálogo vem do `next dev` local em vez de jalloft.com.
 */
class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.LOCAL_ENDPOINT.isNotEmpty()) JalloftPromo.endpoint = BuildConfig.LOCAL_ENDPOINT
        JalloftPromo.open(this)
        finish()
    }
}
