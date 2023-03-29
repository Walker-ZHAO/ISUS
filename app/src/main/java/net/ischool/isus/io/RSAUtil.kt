package net.ischool.isus.io

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * RSA加解密工具
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/3/28
 */

/**
 *  将公钥的ByteArray数据还原为PublicKey
 * @param publicKeyBytes
 * @return PublicKey
 * @throws Exception
 */
fun getPublicKey(publicKeyBytes: ByteArray): PublicKey{
    val keySpec = X509EncodedKeySpec(publicKeyBytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    return keyFactory.generatePublic(keySpec)
}

/**
 * 将私钥的ByteArray数据还原为PrivateKey
 * @param privateKey
 * @return PrivateKey
 * @throws Exception
 */
fun getPrivateKey(privateKey: ByteArray): PrivateKey{
    val keySpec = PKCS8EncodedKeySpec(privateKey)
    val keyFactory = KeyFactory.getInstance("RSA")
    return keyFactory.generatePrivate(keySpec)
}

/**
 * 用私钥解密
 *
 * @param encryptedData 经过encrypt()加密返回的byte数据
 * @param privateKey 私钥
 * @param transformation 加密模式和填充方式，默认为 RSA/ECB/PKCS1Padding
 * @return
 */
fun decrypt(encryptedData: ByteArray, privateKey: PrivateKey, transformation: String = "RSA/ECB/PKCS1Padding"): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.DECRYPT_MODE, privateKey)
    return cipher.doFinal(encryptedData)
}

/**
 * 用公钥解密
 *
 * @param encryptedData 经过encrypt()加密返回的byte数据
 * @param publicKey 公钥
 * @param transformation 加密模式和填充方式，默认为 RSA/ECB/PKCS1Padding
 * @return
 */
fun decrypt(encryptedData: ByteArray, publicKey: PublicKey, transformation: String = "RSA/ECB/PKCS1Padding"): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.DECRYPT_MODE, publicKey)
    return cipher.doFinal(encryptedData)
}

/**
 * 用公钥加密
 * 每次加密的字节数，不能超过密钥的长度值减去11
 *
 * @param data  需加密数据的byte数据
 * @param publicKey 公钥
 * @param transformation 加密模式和填充方式，默认为 RSA/ECB/PKCS1Padding
 * @return 加密后的byte型数据
 */
public fun encrypt(data: ByteArray, publicKey: PublicKey, transformation: String = "RSA/ECB/PKCS1Padding"): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    // 编码前设定编码方式及密钥
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    // 传入编码数据并返回编码结果
    return cipher.doFinal(data)
}

/**
 * 用私钥加密
 * 每次加密的字节数，不能超过密钥的长度值减去11
 *
 * @param data  需加密数据的byte数据
 * @param privateKey 私钥
 * @param transformation 加密模式和填充方式，默认为 RSA/ECB/PKCS1Padding
 * @return 加密后的byte型数据
 */
public fun encrypt(data: ByteArray, privateKey: PrivateKey, transformation: String = "RSA/ECB/PKCS1Padding"): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    // 编码前设定编码方式及密钥
    cipher.init(Cipher.ENCRYPT_MODE, privateKey)
    // 传入编码数据并返回编码结果
    return cipher.doFinal(data)
}

// 公钥
const val PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4k/SHAVLm7W/vkyiqOzDwJmMd8nOw6l35bIECOUtAAcvoxbGv8JA18ZqrKoq+x6sFqZ7ztuLqop6x98MOJKgX5Q/HHHNw/rTsEBUFML+A3/tLIotExRksz85CxfyUfs/JQNpbyvtz13PQCKp/161t6/zq8WZFhiyBXL/LrcRnSwIDAQAB"