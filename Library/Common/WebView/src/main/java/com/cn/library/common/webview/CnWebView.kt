package com.cn.library.common.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * @Author: CuiNing
 * @Time: 2025/6/20 22:14
 * @Description:
 */
class CnWebView: WebView {

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) { initWebView() }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        settings.run {
            javaScriptEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString()?:""
                if (url.startsWith("http://") || url.startsWith("https://"))
                    return false
                return super.shouldOverrideUrlLoading(view, request)
            }


        }
    }
}