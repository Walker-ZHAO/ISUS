package net.ischool.isus.network.se

import net.ischool.isus.preference.PreferenceManager
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*
import java.io.*
import java.lang.Exception

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
            val password = PreferenceManager.instance.getKeyPass()
            val protocol = "TLS"
            try {
                val file = File(PreferenceManager.instance.getSePemPath())
                val keyStore = when (type) {
                    X509 -> {
                        val keyStore = emptyKeyStore(password)

                        // 证书
                        val certificates = file.inputStream().use { CertificateFactory.getInstance(type)?.generateCertificates(it) }
                        certificates?.forEachIndexed { index, certificate ->
                            keyStore.setCertificateEntry("$index", certificate)
                        }

                        // 私钥
                        keyStore.setKeyEntry(
                            "",
                            file.inputStream().use { KeyImport.readPrivateKey(it, password) },
                            password.toCharArray(),
                            certificates?.toTypedArray()
                        )
                        keyStore
                    }
                    P12 -> {
                        val keyStore = KeyStore.getInstance(type)
                        file.inputStream().use { keyStore.load(it, password.toCharArray()) }
                        keyStore
                    }
                    else -> emptyKeyStore(password)
                }
                val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(keyStore, password.toCharArray())

                val ssContext = SSLContext.getInstance(protocol)
                ssContext.init(keyManagerFactory.keyManagers, arrayOf(NullX509TrustManager()), null)
                return ssContext
            } catch (e: Exception) {
                val ssContext = SSLContext.getInstance(protocol)
                ssContext.init(arrayOf(), arrayOf(NullX509TrustManager()), null)
                return ssContext
            }
        }

        @JvmStatic fun getSSLSocketFactory(): SSLSocketFactory = getSSLContext().socketFactory

        private fun emptyKeyStore(password: String)=  KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, password.toCharArray()) }
    }
}