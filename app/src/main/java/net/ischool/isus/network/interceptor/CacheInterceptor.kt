package net.ischool.isus.network.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * 缓存拦截器
 *
 * 默认的缓存策略只会带Etag/If-None-Match或Last-Modified/If-Modified-Since其中之一（Etag/If-None-Match准确度更高）
 * 此拦截器同时使用两个头
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/18
 */
class CacheInterceptor : Interceptor {

    companion object {
        private var etag: String = ""
        private var last_modified = ""
    }

    override fun intercept(chain: Interceptor.Chain?): Response {
        val safeChain = checkNotNull(chain)
        val originalRequest = safeChain.request()
        val builder: Request.Builder = originalRequest.newBuilder()
        if (originalRequest.url().toString().contains("comet")) {
            if (etag.isNotEmpty()) {
                builder.header("If-None-Match", etag)
            } else {
                builder.removeHeader("If-None-Match")
            }
            if (last_modified.isNotEmpty()) {
                builder.header("If-Modified-Since", last_modified)
            } else {
                builder.removeHeader("If-Modified-Since")
            }
        }

        val response = safeChain.proceed(builder.build())

        /**
         * 当次返回200->缓存返回的etag与modified头，作为下次访问的请求头
         * 当次返回408->不缓存的etag与modified头，下次访问继续使用之前缓存的
         * 当次返回其他->清空之前缓存的etag与modified头
         */
        if (originalRequest.url().toString().contains("comet")) {
            when (response.code()) {
                200 -> {
                    etag = response.header("Etag") ?: ""
                    last_modified = response.header("Last-Modified") ?: ""
                }
                408 -> { }
                else -> {
                    etag = ""
                    last_modified = ""
                }
            }
        }
        return response

    }
}