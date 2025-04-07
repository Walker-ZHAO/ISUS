package net.ischool.isus.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ChecksSdkIntAtLeast
import net.ischool.isus.isArch64
import net.ischool.isus.preference.PreferenceManager

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