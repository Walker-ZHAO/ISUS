package net.ischool.isus.network.se

import android.annotation.SuppressLint
import net.ischool.isus.ISUS
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 自定义信任管理器
 *
 * 该信任管理器可信任系统CA及自签CA
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/9/21
 */
@SuppressLint("CustomX509TrustManager")
object CustomTrustManager: X509TrustManager {

    private val systemTrustManager: X509TrustManager
    private val selfTrustManager: X509TrustManager

    init {
        // 获取系统默认的X509TrustManager
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        systemTrustManager = trustManagerFactory.trustManagers[0] as X509TrustManager

        // 构建包含自签CA的的X509TrustManager
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val trustRootCAStream = ISUS.instance.certificate

        // 读取所有自签CA
        val trustRootCAs = mutableListOf<X509Certificate>()
        while (true) {
            try {
                val trustRootCA = cf.generateCertificate(trustRootCAStream) as X509Certificate
                trustRootCAs.add(trustRootCA)
            } catch (e: Exception) {
                break
            }
        }

        // Create a KeyStore containing our trusted CAs
        val keyStoreType = KeyStore.getDefaultType()
        val trustKeyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)
            trustRootCAs.forEachIndexed { index, cert ->
                setCertificateEntry("ca$index", cert)
            }
        }

        // Create a TrustManager that trusts the CAs inputStream our KeyStore
        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(trustKeyStore)
        }

        selfTrustManager = tmf.trustManagers[0] as X509TrustManager
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 使用系统默认的TrustManager
        systemTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 验证服务器证书的可信任性
        try {
            if (chain.isNullOrEmpty()) {
                throw IllegalArgumentException("checkServerTrusted: Invalid Certificate Chain")
            }

            // 先用系统默认信任链，再用自签验证
            try {
                systemTrustManager.checkServerTrusted(chain, authType)
            } catch (e: Exception) {
                selfTrustManager.checkServerTrusted(chain, authType)
            }
        } catch (e: Exception) {
            throw CertificateException("Failed to verify server certificate.", e)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        // 返回系统默认的可信任证书
        return systemTrustManager.acceptedIssuers
    }
}