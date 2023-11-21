package net.ischool.isus.network.interceptor

import net.ischool.isus.ISUS
import okhttp3.Interceptor
import okhttp3.Response

/**
 * UA插入拦截器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/11/21
 */
class UserAgentInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val request =
            originalRequest.newBuilder()
                .header("User-Agent", ISUS.instance.getUserAgent())
                .build()
        return chain.proceed(request)
    }
}