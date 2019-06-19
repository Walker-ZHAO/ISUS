package net.ischool.isus.network.se

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.DSAPrivateKeySpec
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList

/**
 * PEM 私钥证书解码
 *
 * https://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art050
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-31
 */
class KeyImport {
    companion object {
        private val invCodes = arrayOf(
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, 64, -1, -1,
            -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1)

        private val OID_RSA_FORMAT = arrayOf(
            0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01
        )

        private val OID_DSA_FORMAT = arrayOf(
            0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x02
        )

        fun readPrivateKey(inputStream: InputStream, password: String): PrivateKey? {
            val bufferReader = BufferedReader(InputStreamReader(inputStream))
            bufferReader.use {
                var encrypted = false
                var readingKey = false
                var pkcs8Format = false
                var rsaFormat = false
                var dsaFormat = false
                val base64EncodedKey = StringBuffer()
                var line = bufferReader.readLine()
                while (line != null) {
                    when {
                        readingKey -> {
                            when (line.trim()) {
                                "-----END RSA PRIVATE KEY-----" -> readingKey = false   // PKCS #1
                                "-----END DSA PRIVATE KEY-----" -> readingKey = false
                                "-----END PRIVATE KEY-----" -> readingKey = false       // PKCS #8
                                "-----END ENCRYPTED PRIVATE KEY-----" -> readingKey = false
                                else -> base64EncodedKey.append(line.trim())
                            }
                        }
                        else -> {
                            when (line.trim()) {
                                "-----BEGIN RSA PRIVATE KEY-----" -> {
                                    readingKey = true
                                    rsaFormat = true
                                }
                                "-----BEGIN DSA PRIVATE KEY-----" -> {
                                    readingKey = true
                                    dsaFormat = true
                                }
                                "-----BEGIN PRIVATE KEY-----" -> {
                                    readingKey = true
                                    pkcs8Format = true
                                }
                                "-----BEGIN ENCRYPTED PRIVATE KEY-----" -> {
                                    readingKey = true
                                    encrypted = true
                                }
                            }
                        }
                    }
                    line = bufferReader.readLine()
                }
                if (base64EncodedKey.isEmpty()) {
                    throw KeyImportException("File did not contain an unencrypted private key")
                }

                var bytes = base64Decode(base64EncodedKey.toString())

                if (encrypted) {
                    var pkcs5Integers = ArrayList<BigInteger>()
                    var oids = ArrayList<ByteArray>()
                    var byteStrings = ArrayList<ByteArray>()
                    ASN1Parse(bytes, pkcs5Integers, oids, byteStrings)
                    val salt = byteStrings[0]
                    val iterationCount = pkcs5Integers[0].toInt()

                    // XXX I should be verifying the key-stretching algorithm OID here
                    val key = stretchKey(password, salt, iterationCount)
                    val encryptedBytes = byteStrings[2]
                    val iv = byteStrings[1]
                    // XXX I should be verifying the encryption algorithm OID here
                    bytes = decrypt(key, iv, encryptedBytes)

                    // Parse the decrypted output to determine its type (RSA or DSA)
                    pkcs5Integers = ArrayList()
                    oids = ArrayList()
                    byteStrings = ArrayList()
                    ASN1Parse(bytes, pkcs5Integers, oids, byteStrings)

                    when {
                        Arrays.equals(oids[0].toTypedArray(), OID_RSA_FORMAT) -> {
                            bytes = byteStrings[0]
                            rsaFormat = true
                        }
                        Arrays.equals(oids[0].toTypedArray(), OID_DSA_FORMAT) -> {
                            bytes = byteStrings[0]
                            dsaFormat = true
                        }
                        else -> throw KeyImportException("Unrecognized key format")
                    }
                }

                // PKCS #8 as in: http://www.agentbob.info/agentbob/79-AB.html
                var keyFactory: KeyFactory? = null
                var spec: KeySpec? = null
                when {
                    pkcs8Format -> {
                        keyFactory = KeyFactory.getInstance("RSA")
                        spec = PKCS8EncodedKeySpec(bytes)
                    }
                    rsaFormat -> {
                        keyFactory = KeyFactory.getInstance("RSA")
                        val rsaIntegers = arrayListOf<BigInteger>()
                        ASN1Parse(bytes, rsaIntegers, null, null)
                        if (rsaIntegers.size < 8) {
                            throw KeyImportException("Pem file does not appear to be a properly formatted RSA key")
                        }
                        val publicExponent = rsaIntegers[2]
                        val privateExponent = rsaIntegers[3]
                        val modulus = rsaIntegers[1]
                        val primeP = rsaIntegers[4]
                        val primeQ = rsaIntegers[5]
                        val primeExponentP = rsaIntegers[6]
                        val primeExponentQ = rsaIntegers[7]
                        val crtCoefficient = rsaIntegers[8]
                        spec = RSAPrivateCrtKeySpec(
                            modulus,
                            publicExponent,
                            privateExponent,
                            primeP,
                            primeQ,
                            primeExponentP,
                            primeExponentQ,
                            crtCoefficient
                        )
                    }
                    dsaFormat -> {
                        keyFactory = KeyFactory.getInstance("DSA")
                        val dsaIntegers = arrayListOf<BigInteger>()
                        ASN1Parse(bytes, dsaIntegers, null, null)
                        if (dsaIntegers.size < 5) {
                            throw KeyImportException("Pem file does not appear to be a properly formatted DSA key")
                        }
                        val privateExponent = dsaIntegers[1]
                        val publicExponent = dsaIntegers[2]
                        val P = dsaIntegers[3]
                        val Q = dsaIntegers[4]
                        val G = dsaIntegers[5]
                        spec = DSAPrivateKeySpec(privateExponent, P, Q, G)
                    }
                }
                return keyFactory?.generatePrivate(spec)
            }
        }

        private fun base64Decode(input: String): ByteArray {
            if (input.length % 4 != 0) {
                throw IllegalArgumentException("Invalid base64 input")
            }
            val decoded = ByteArray(input.length * 3 / 4 - if (input.indexOf('=') > 0) input.length - input.indexOf('=') else 0)
            val inChars = input.toCharArray()
            var j = 0
            val b = IntArray(4)
            var i = 0
            while (i < inChars.size) {
                b[0] = invCodes[inChars[i].toInt() and 0xFF]
                b[1] = invCodes[inChars[i + 1].toInt() and 0xFF]
                b[2] = invCodes[inChars[i + 2].toInt() and 0xFF]
                b[3] = invCodes[inChars[i + 3].toInt() and 0xFF]
                decoded[j++] = (b[0].shl(2) or (b[1].shr(4))).toByte()
                if (b[2] < 64) {
                    decoded[j++] = (b[1].shl(4) or (b[2].shr(2))).toByte()
                    if (b[3] < 64) {
                        decoded[j++] = (b[2].shl(6) or b[3]).toByte()
                    }
                }
                i += 4
            }
            return decoded
        }

        /**
         * Bare-bones ASN.1 parser that can only deal with a structure that contains integers
         * (as I expect for the RSA private key format given in PKCS #1 and RFC 3447).
         * @param b the bytes to be parsed as ASN.1 DER
         * @param integers an output array to which all integers encountered during the parse
         *   will be appended in the order they're encountered.  It's up to the caller to determine
         *   which is which.
         * @param oids an output array of all 0x06 tags
         * @param byteStrings an output array of all 0x04 tags
         */
        private fun ASN1Parse(b: ByteArray, integers: ArrayList<BigInteger>, oids: ArrayList<ByteArray>?, byteStrings: ArrayList<ByteArray>?) {
            var pos = 0
            while (pos < b.size) {
                val tag = b[pos++].toInt() and 0xFF
                var length = b[pos++].toInt()

                if ((length and 0x80) != 0) {
                    var extLen = 0
                    for (i in 0 until (length and 0x7F)) {
                        extLen = (extLen.shl(8)) or (b[pos++].toInt() and 0xFF)
                    }
                    length = extLen
                }

                val contents = ByteArray(length)
                System.arraycopy(b, pos, contents, 0, length)
                pos += length

                when (tag) {
                    0x30 -> ASN1Parse(contents, integers, oids, byteStrings)      // sequence
                    0x02 -> integers.add(BigInteger(contents))  // Integer
                    0x04 -> byteStrings?.add(contents)  // byte string
                    0x06 -> oids?.add(contents) // OID
                    0x05 -> { } // Ignore this.  It comes up in the RSA format, but only as a placeholder.
                    else -> throw KeyImportException("Unsupported ASN.1 tag $tag encountered.  Is this a valid RSA key?")

                }
            }
        }

        private fun stretchKey(password: String, salt: ByteArray, iterationCount: Int): ByteArray {
            val pbeKeySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, 192) // length of a DES3 key
            val fact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") // SHA256 不支持Android 8以下平台；若需要兼容老平台，需要改用SHA1算法
            return fact.generateSecret(pbeKeySpec).encoded
        }

        private fun decrypt(key: ByteArray, iv: ByteArray, encrypted: ByteArray): ByteArray {
            val desKeySpec = DESedeKeySpec(key)
            val desKey = SecretKeySpec(desKeySpec.key, "DESede")
            // 如果不指定使用BC作为Provider，会存在问题（https://github.com/jeroentrappers/flutter_keychain/issues/4）
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding", "BC")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, desKey, ivSpec)
            return cipher.doFinal(encrypted)
        }
    }

    class KeyImportException(msg: String) : Exception(msg)
}