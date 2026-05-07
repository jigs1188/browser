package dev.mer.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper around Android WebView.
 *
 * WebView doesn't have native Compose support, so we use AndroidView.
 * The WebView instance is remembered to survive recomposition but
 * NOT configuration changes — that's handled by the Activity's
 * configChanges declaration in the manifest.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleChanged: (String) -> Unit
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false  // Security: no file:// access
                allowContentAccess = false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let { onPageStarted(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { onPageFinished(it) }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false // Let WebView handle all navigation
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    title?.let { onTitleChanged(it) }
                }
            }
        }
    }

    AndroidView(
        factory = {
            onWebViewCreated(webView)
            webView
        },
        modifier = modifier
    )
}
