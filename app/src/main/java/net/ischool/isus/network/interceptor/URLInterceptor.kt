package net.ischool.isus.network.interceptor

import android.net.Uri
import android.util.Log
import net.ischool.isus.ISUS
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

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val platformApi = PreferenceManager.instance.getPlatformApi()
        val uri = Uri.parse(platformApi)
        val host = uri.host
        val scheme = uri.scheme
        val port = uri.port

        val urlBuilder =  request.url().newBuilder()
        if (!host.isNullOrEmpty())
            urlBuilder.host(host)
        if (!scheme.isNullOrEmpty())
            urlBuilder.scheme(scheme)
        if (port > 0)
            urlBuilder.port(port)
        if (ISUS.instance.se)
            urlBuilder.setPathSegment(0, "www")
        val newURL = urlBuilder.build()
        request = request.newBuilder().url(newURL).build()

        return chain.proceed(request)
    }
}