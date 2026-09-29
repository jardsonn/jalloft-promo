package com.jalloft.promo.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Paleta do design "Promo Apps" (cores de sistema do iOS). */
@Immutable
data class PromoColors(
    val sheet: Color,
    val card: Color,
    val label: Color,
    val label2: Color,
    val separator: Color,
    val fill: Color,
    val accent: Color,
    val toast: Color,
    val toastText: Color,
    val bannerStart: Color,
    val bannerEnd: Color,
    val bannerStripe: Color,
) {
    companion object {
        val Light = PromoColors(
            sheet = Color(0xFFF2F2F7),
            card = Color(0xFFFFFFFF),
            label = Color(0xFF000000),
            label2 = Color(0x9E3C3C43),      // rgba(60,60,67,.62)
            separator = Color(0x293C3C43),   // rgba(60,60,67,.16)
            fill = Color(0x1F767680),        // rgba(118,118,128,.12)
            accent = Color(0xFFFF2D55),
            toast = Color(0xEB1E1E20),       // rgba(30,30,32,.92)
            toastText = Color(0xFFFFFFFF),
            bannerStart = Color(0xFFF9E3E7),
            bannerEnd = Color(0xFFF1EFEA),
            bannerStripe = Color(0x38FF2D55), // accent .22
        )

        val Dark = PromoColors(
            sheet = Color(0xFF1C1C1E),
            card = Color(0xFF2C2C2E),
            label = Color(0xFFFFFFFF),
            label2 = Color(0x9EEBEBF5),      // rgba(235,235,245,.62)
            separator = Color(0x8C545458),   // rgba(84,84,88,.55)
            fill = Color(0x3D767680),        // rgba(118,118,128,.24)
            accent = Color(0xFFFF375F),
            toast = Color(0xF2F2F2F7),       // rgba(242,242,247,.95)
            toastText = Color(0xFF000000),
            bannerStart = Color(0xFF3B2127),
            bannerEnd = Color(0xFF262628),
            bannerStripe = Color(0x47FF375F), // accent .28
        )
    }
}
