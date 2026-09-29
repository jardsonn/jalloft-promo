package com.jalloft.promo

import androidx.compose.runtime.Immutable

/** Um app do catálogo, já com os textos no idioma do aparelho. */
@Immutable
data class PromoApp(
    val packageName: String,
    val name: String,
    val iconUrl: String?,
    val category: String?,
    val description: String?,
    val isNew: Boolean,
    val playStoreUrl: String,
)
