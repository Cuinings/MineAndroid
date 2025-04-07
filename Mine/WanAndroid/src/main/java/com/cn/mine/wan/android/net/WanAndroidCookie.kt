package com.cn.mine.wan.android.net

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.nio.charset.Charset

class WanAndroidCookie(private val context: Context): CookieJar {

    companion object {
        val UTF_8 : Charset = Charset.forName("UTF-8")
    }

    private val cookieStore: PersistentCookieStore by lazy { PersistentCookieStore(context) }
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences("Cookies_Perfs", Context.MODE_PRIVATE) }

    init {
        sharedPreferences.all
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        cookies.forEach { cookie -> cookie?.let {
            cookieStore.add(url, it)
        } }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.get(url)?: emptyList()
    }

    inner class PersistentCookieStore(context: Context){
        private val cookieStore: Map<HttpUrl, List<Cookie>> = HashMap()

        fun add(httpUrl: HttpUrl, cookie: Cookie) {
            Log.d(PersistentCookieStore::class.simpleName, "add: $httpUrl, $cookie")
            sharedPreferences.edit().putString(httpUrl.toString(), cookie.toString())
        }

        fun get(httpUrl: HttpUrl) :List<Cookie>? {
            return null
        }

        fun removeAll(): Boolean {
            //清除
            return true
        }

    }
}