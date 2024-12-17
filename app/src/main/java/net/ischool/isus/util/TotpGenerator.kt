package net.ischool.isus.util

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import net.ischool.isus.preference.PreferenceManager
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

/**
 * TOTP 生成器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/12/17
 */
object TotpGenerator {
    // 有效期，600秒，10分钟
    private const val TIME_STEP = 600L

    private val generator by lazy {
        val base32EncodedSecret = PreferenceManager.instance.totpKeyWithBase32()
        val config = TimeBasedOneTimePasswordConfig(codeDigits = 6,
            hmacAlgorithm = HmacAlgorithm.SHA512,
            timeStep = TIME_STEP,
            timeStepUnit = TimeUnit.SECONDS)
        TimeBasedOneTimePasswordGenerator(Base32().decode(base32EncodedSecret), config)
    }

    /**
     * 验证TOTP码有效性
     */
    fun isValid(code: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val availableCodeList = mutableListOf<String>()
        availableCodeList.add(generator.generate(timestamp = currentTime))
        // 前后1小时内的TOTP码都认为有效
        (1 .. 6).forEach {
            availableCodeList.add(generator.generate(timestamp = currentTime + TIME_STEP * 1000 * it))
            availableCodeList.add(generator.generate(timestamp = currentTime - TIME_STEP * 1000 * it))
        }
        return availableCodeList.contains(code)
    }

    /**
     * 生成TOTP验证码
     */
    fun genCode(): String {
        return generator.generate(timestamp = System.currentTimeMillis())
    }
}