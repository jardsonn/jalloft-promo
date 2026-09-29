package com.jalloft.promo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal object PlayStore {

    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Abre a ficha do app na Play Store com referrer UTM — as instalações vindas
     * de cada app aparecem em Play Console → Aquisição de usuários, por origem.
     */
    fun openListing(context: Context, packageName: String) {
        val referrer = Uri.encode(
            "utm_source=${context.packageName}&utm_medium=cross_promo&utm_campaign=more_apps"
        )
        val query = "id=$packageName&referrer=$referrer"
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?$query"))
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, market)) return
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?$query")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, web)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
