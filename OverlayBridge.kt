package com.etour.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface

class OverlayBridge(private val ctx: Context) {

    @JavascriptInterface
    fun canDraw(): Boolean = Settings.canDrawOverlays(ctx)

    @JavascriptInterface
    fun requestPermission() {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @JavascriptInterface
    fun show(json: String): Boolean {
        if (!canDraw()) { requestPermission(); return false }
        val i = Intent(ctx, OverlayService::class.java).putExtra(OverlayService.EXTRA_JSON, json)
        if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        return true
    }

    @JavascriptInterface
    fun update(json: String) {
        if (OverlayService.running && canDraw()) show(json)
    }

    @JavascriptInterface
    fun googleSignIn() {
        (ctx as? MainActivity)?.let { a -> a.runOnUiThread { a.startGoogle() } }
    }

    @JavascriptInterface
    fun hide() { ctx.stopService(Intent(ctx, OverlayService::class.java)) }
}
