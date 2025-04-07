package net.ischool.isus.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ChecksSdkIntAtLeast
import net.ischool.isus.SYSLOG_CATEGORY_IME
import net.ischool.isus.isArch64
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.silentInstallApk
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * 输入法相关工具
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/4/7
 */

/**
 * 自定义输入法 ID
 */
private const val CUSTOM_IME_ID = "org.fcitx.fcitx5.android/.input.FcitxInputMethodService"

/**
 * 设置自定义输入法
 */
fun Context.setupCustomIME() {
    // 如果不支持使用自定义输入法，直接退出
    if (!supportCustomIME()) return

    // 如果当前自定义输入法为默认输入方，代表已设置成功，直接退出
    if (isCustomIMEAsDefault()) {
        Syslog.logI("Custom IME is already set as default.", category = SYSLOG_CATEGORY_IME)
        return
    }

    if (isCustomIMEInstalled()) {
        // 如果当前自定义输入法已安装，则将其设置为默认输入法
        setCustomIMEAsDefault()
        // 设置完毕后，再次检查是否设置成功
        setupCustomIME()
    } else {
        // 输入法应用下载地址
        val imeUrl = getIMEUrl()
        // 如果未安装，则下载输入法并安装
        APIService.downloadAsync(
            imeUrl,
            Environment.getExternalStorageDirectory().path,
            callback = object :
                StringCallback {
                override fun onResponse(string: String) {
                    // 下载成功，开始安装
                    silentInstallApk(File(string)) { success, message ->
                        val doubleCheckDelay: Long = if (success) {
                            5 * 1000
                        } else {
                            30 * 1000
                        }

                        Syslog.logI("install apk: $success, message: $message", category = SYSLOG_CATEGORY_IME)

                        // 无论安装成功与否，都尝试重新配置输入法，以将其设置为默认输入法
                        Handler().postDelayed({
                            setupCustomIME()
                        }, doubleCheckDelay)

                        // 安装完成后删除下载的 APK 文件
                        File(string).delete()
                    }
                }

                override fun onFailure(request: Request, e: IOException) {
                    Syslog.logI("download ime from $imeUrl failure: ${e.message}", category = SYSLOG_CATEGORY_IME)
                    // 下载失败，延迟 60s，尝试重新配置输入法
                    Handler().postDelayed({
                        setupCustomIME()
                    }, 60 * 1000)
                }
            })
    }
}

/**
 * 是否支持自定义输入法
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
fun supportCustomIME(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}

/**
 * 获取输入法应用下载地址
 */
fun getIMEUrl(): String {
    return if (isArch64()) {
        "${PreferenceManager.instance.getCdnUrl()}/webui/asset/fcitx5/fcitx5-arm64-v8a.apk"
    } else {
        "${PreferenceManager.instance.getCdnUrl()}/webui/asset/fcitx5/fcitx5-armeabi-v7a.apk"
    }
}

/**
 * 获取系统当前默认输入法
 */
fun Context.getDefaultIME(): String {
    // 获取 InputMethodManager 实例
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // 获取默认输入法的 ID
    val defaultIme = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    ) ?: ""
    return defaultIme
}

/**
 * 系统当前输入法是否为自定义输入法
 */
fun Context.isCustomIMEAsDefault(): Boolean {
    return getDefaultIME() == CUSTOM_IME_ID
}

/**
 * 检查自定义输入法是否已安装
 */
fun Context.isCustomIMEInstalled(): Boolean {
    return isInputMethodInstalled(CUSTOM_IME_ID)
}

/**
 * 将自定义输入法设为系统默认输入法
 * 该方法需要系统签名权限
 */
fun Context.setCustomIMEAsDefault() {
    // 启用输入法
    enableInputMethod(CUSTOM_IME_ID)
    // 设置默认输入法
    setDefaultInputMethod(CUSTOM_IME_ID)
}


/**
 * 检查输入法是否已安装
 * @param imeId 输入法完整ID (格式：包名/服务类名)
 * @return Boolean
 */
private fun Context.isInputMethodInstalled(imeId: String): Boolean {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val imeList = imm.inputMethodList

    return imeList.any { it.id == imeId }
}

/**
 * 检查输入法是否已启用
 */
private fun Context.isInputMethodEnabled(imeId: String): Boolean {
    val enabledInputMethods = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    ) ?: ""

    return enabledInputMethods.split(":").contains(imeId)
}

/**
 * 启用指定输入法
 * 该方法需要系统签名权限
 * @param imeId 输入法的ID
 */
private fun Context.enableInputMethod(imeId: String) {
    try {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imeList = imm.inputMethodList
        for (ime in imeList) {
            if (ime.id == imeId) {
                if (!isInputMethodEnabled(imeId)) {
                    // 启用输入法
                    val enabledInputMethods = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ENABLED_INPUT_METHODS
                    ) ?: ""
                    val newList = if (enabledInputMethods.isEmpty()) {
                        imeId
                    } else {
                        "$enabledInputMethods:$imeId"
                    }
                    Settings.Secure.putString(
                        contentResolver,
                        Settings.Secure.ENABLED_INPUT_METHODS,
                        newList
                    )
                }
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 设置默认输入法
 * 该方法需要系统签名权限
 * @param imeId 输入法的ID（格式：包名/服务类名）
 * @return 是否设置成功
 */
private fun Context.setDefaultInputMethod(imeId: String): Boolean {
    return try {
        Settings.Secure.putString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
            imeId
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}