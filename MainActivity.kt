package com.etour.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val webClientId = "162957967049-dsn3n0pprcmfp84v33qfvbe70889730e.apps.googleusercontent.com"

    // নেটিভ Google লগইন (WebView তে Google পপআপ লগইন কাজ করে না)
    fun startGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId).requestEmail().build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener { startActivityForResult(client.signInIntent, 9001) }
    }

    private fun sendToJs(tok: String?, err: String?) {
        val a = if (tok == null) "null" else "'" + tok + "'"
        val b = if (err == null) "null" else "'" + err + "'"
        web.evaluateJavascript("window.onNativeGoogleToken && window.onNativeGoogleToken(" + a + "," + b + ")", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 9001) return
        try {
            val acc = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            sendToJs(acc.idToken, null)
        } catch (e: ApiException) {
            sendToJs(null, if (e.statusCode == 12501) "cancel" else "code " + e.statusCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.layoutParams = ViewGroup.LayoutParams(-1, -1)
        setContentView(web)

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url)

            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (r.url.host == "appassets.androidplatform.net") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (_: Exception) {}
                return true
            }
        }
        // window.open() লিংক বাইরের ব্রাউজারে খুলবে
        web.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(v: WebView, isDialog: Boolean, isUserGesture: Boolean, msg: android.os.Message): Boolean {
                val t = WebView(this@MainActivity)
                t.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(x: WebView, r: WebResourceRequest): Boolean {
                        try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (_: Exception) {}
                        return true
                    }
                }
                (msg.obj as WebView.WebViewTransport).webView = t
                msg.sendToTarget()
                return true
            }
        }
        web.addJavascriptInterface(OverlayBridge(this), "EtourOverlay")

        // নোটিফিকেশন পারমিশন (OneSignal নিজে থেকে এটা ধরে নেবে)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)

        if (savedInstanceState == null) web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        else web.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); web.saveState(out) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
