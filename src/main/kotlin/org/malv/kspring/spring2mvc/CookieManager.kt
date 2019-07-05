package org.malv.kspring.spring2mvc

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl



class CookieManager : CookieJar {

    private var cookies = emptyList<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies

    }
}
