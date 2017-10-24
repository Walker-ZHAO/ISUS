package net.ischool.isus.network.interceptor

import net.ischool.isus.preference.PreferenceManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * URL 拦截器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/24
 */
class URLInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain?): Response {
        val safeChain = checkNotNull(chain)
        var request = safeChain.request()
        val host = PreferenceManager.instance.getServer()
        val scheme = PreferenceManager.instance.getProtocal()
        if (request.url().toString().contains("config") || request.url().toString().contains("pong")) {
            val urlBuilder =  request.url().newBuilder()
            if (host.isNotEmpty())
                urlBuilder.host(host)
            if (scheme.isNotEmpty())
                urlBuilder.scheme(scheme)
            val newURL = urlBuilder.build()
            request = request.newBuilder().url(newURL).build()
        }
        return safeChain.proceed(request)
    }
}