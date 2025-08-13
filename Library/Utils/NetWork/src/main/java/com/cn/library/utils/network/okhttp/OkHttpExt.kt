package com.cn.library.utils.network.okhttp

import android.content.Context
import android.util.Base64
import com.cn.library.utils.network.config.NetworkConfig.BASE_URL
import com.cn.library.utils.network.config.NetworkConfig.context
import com.cn.library.utils.network.okhttp.response.interceptor.ResponseInterceptor
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 23:25
 * @Description:
 */
object OkHttpExt {

    val okHttpClient by lazy { OkHttpClient.Builder()
        .addInterceptor(ResponseInterceptor())
        .addInterceptor(HttpLoggingInterceptor())
//        .cookieJar(PersistentCookieJar(context))
        .build() }
}

class PersistentCookieJar(context: Context) : CookieJar {

    private val cookieStore = PersistentCookieStore(context)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.addAll(url, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.get(url)
    }
}

class PersistentCookieStore(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE)
    private val cookies = mutableMapOf<String, ConcurrentHashMap<String, Cookie>>()

    init {
        val prefsMap = sharedPreferences.all
        prefsMap.forEach { (key, value) ->
            if (key.startsWith("cookie_")) {
                val cookieNames = value.toString().split(";").toHashSet()
                cookieNames.forEach { name ->
                    val encodedCookie = sharedPreferences.getString("$key$name", null)
                    encodedCookie?.let {
                        val decodedCookie = decodeCookie(it)
                        decodedCookie?.let { cookie ->
                            if (!cookies.containsKey(key)) cookies[key] = ConcurrentHashMap()
                            cookies[key]?.put(name, cookie)
                        }
                    }
                }
            }
        }
    }

    fun addAll(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { add(url, it) }
    }

    fun add(url: HttpUrl, cookie: Cookie) {
        val key = url.host
        if (!cookies.containsKey(key)) cookies[key] = ConcurrentHashMap()
        cookies[key]?.put(cookie.name, cookie)
        saveToPrefs(url, cookie)
    }

    fun get(url: HttpUrl): List<Cookie> {
        return cookies[url.host]?.values?.toList() ?: emptyList()
    }

    private fun saveToPrefs(url: HttpUrl, cookie: Cookie) {
        val key = "cookie_${url.host}"
        val name = cookie.name
        val cookieNames = sharedPreferences.getStringSet(key, HashSet())?.toMutableSet() ?: mutableSetOf()
        cookieNames.add(name)
        sharedPreferences.edit().apply {
            putStringSet(key, cookieNames)
            putString("$key$name", encodeCookie(cookie))
            apply()
        }
    }

    private fun encodeCookie(cookie: Cookie): String =
        Base64.encodeToString("${cookie.name}=${cookie.value}".toByteArray(), Base64.DEFAULT)

    private fun decodeCookie(cookieString: String): Cookie? {
        return try {
            val decoded = Base64.decode(cookieString, Base64.DEFAULT).toString(Charsets.UTF_8)
            Cookie.parse(BASE_URL.toHttpUrl(), decoded)
        } catch (e: Exception) {
            null
        }
    }
}