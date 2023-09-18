package net.ischool.isus.network.se

import net.ischool.isus.ISUS
import net.ischool.isus.preference.PreferenceManager
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*
import java.io.*
import java.lang.Exception
import java.security.cert.X509Certificate

/**
 * HTTPS 双向认证 提供器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-30
 */
class SSLSocketFactoryProvider {
    companion object {

        private const val X509 = "X.509"
        private const val P12  = "PKCS12"

        @JvmStatic fun getSSLContext(type: String = P12): SSLContext {
            val protocol = "TLS"
            val ssContext = SSLContext.getInstance(protocol)
            ssContext.init(getKeyManagers(type), getTrustManagers(), null)
            return ssContext
        }

        @JvmStatic fun getSSLSocketFactory(): SSLSocketFactory = getSSLContext().socketFactory

        private fun emptyKeyStore(password: String)=  KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, password.toCharArray()) }

        /**
         * 获取包含服务端CA证书的TrustManager列表
         */
        fun getTrustManagers(): Array<TrustManager> {
            // 设置服务端的可信根CA证书
            val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
            val trustRootCAStream = ISUS.instance.certificate
            val trustRootCA = cf.generateCertificate(trustRootCAStream) as X509Certificate

            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val trustKeyStore = KeyStore.getInstance(keyStoreType).apply {
                load(null, null)
                setCertificateEntry("ca", trustRootCA)
            }

            // Create a TrustManager that trusts the CAs inputStream our KeyStore
            val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
            val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(trustKeyStore)
            }

            return tmf.trustManagers
        }

        /**
         * 获取包含客户端证书的KeyManager列表
         */
        fun getKeyManagers(type: String = P12): Array<KeyManager> {
            // 设置提供给服务端验证的客户端证书
            val clientCertPassword = PreferenceManager.instance.getKeyPass()
            try {
                val file = File(PreferenceManager.instance.getSePemPath())
                val keyStore = when (type) {
                    X509 -> {
                        val keyStore = emptyKeyStore(clientCertPassword)

                        // 证书
                        val certificates = file.inputStream().use { CertificateFactory.getInstance(type)?.generateCertificates(it) }
                        certificates?.forEachIndexed { index, certificate ->
                            keyStore.setCertificateEntry("$index", certificate)
                        }

                        // 私钥
                        keyStore.setKeyEntry(
                            "",
                            file.inputStream().use { KeyImport.readPrivateKey(it, clientCertPassword) },
                            clientCertPassword.toCharArray(),
                            certificates?.toTypedArray()
                        )
                        keyStore
                    }
                    P12 -> {
                        val keyStore = KeyStore.getInstance(type)
                        file.inputStream().use { keyStore.load(it, clientCertPassword.toCharArray()) }
                        keyStore
                    }
                    else -> emptyKeyStore(clientCertPassword)
                }

                val kmfAlgorithm: String = KeyManagerFactory.getDefaultAlgorithm()
                val kmf = KeyManagerFactory.getInstance(kmfAlgorithm).apply {
                    init(keyStore, clientCertPassword.toCharArray())
                }
                return kmf.keyManagers
            } catch (e: Exception) {
                return arrayOf()
            }
        }
    }
}