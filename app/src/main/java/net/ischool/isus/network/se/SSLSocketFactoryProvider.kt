package net.ischool.isus.network.se

import net.ischool.isus.preference.PreferenceManager
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*
import java.io.*
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlin.Exception

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
            return arrayOf(CustomTrustManager)
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

        /**
         * 获取客户端证书，包含私钥及对应的X509证书
         */
        fun getClientCert(type: String = P12): Pair<PrivateKey?, List<X509Certificate>> {
            var privateKey: PrivateKey? = null
            val certificates = mutableListOf<X509Certificate>()

            val clientCertPassword = PreferenceManager.instance.getKeyPass()
            try {
                val file = File(PreferenceManager.instance.getSePemPath())
                when (type) {
                    X509 -> {
                        privateKey = file.inputStream().use { KeyImport.readPrivateKey(it, clientCertPassword) }
                        file.inputStream().use {
                            CertificateFactory.getInstance(type)?.generateCertificates(it)
                        }?.forEach {
                            certificates.add(it as X509Certificate)
                        }
                    }
                    P12 -> {
                        val keyStore = KeyStore.getInstance(type)
                        file.inputStream().use { keyStore.load(it, clientCertPassword.toCharArray()) }

                        val aliases = keyStore.aliases()
                        val alias = aliases.nextElement()
                        val key = keyStore.getKey(alias, clientCertPassword.toCharArray())
                        if (key is PrivateKey) {
                            privateKey = key
                            keyStore.getCertificateChain(alias).forEach {
                                certificates.add(it as X509Certificate)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            return privateKey to certificates
        }
    }
}

/**
 * 为OkHttp构建器设置可信CA证书及客户端证书
 */
fun OkHttpClient.Builder.setCert(): OkHttpClient.Builder {
    sslSocketFactory(SSLSocketFactoryProvider.getSSLSocketFactory(), SSLSocketFactoryProvider.getTrustManagers().first() as X509TrustManager)
    return this
}