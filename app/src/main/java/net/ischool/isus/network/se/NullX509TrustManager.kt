package net.ischool.isus.network.se

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Description
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-30
 */
class NullX509TrustManager: X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) { }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}