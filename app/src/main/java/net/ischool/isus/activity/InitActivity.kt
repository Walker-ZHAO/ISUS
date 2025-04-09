package net.ischool.isus.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import com.google.gson.Gson
import com.walker.anke.foundation.md5
import com.walker.anke.framework.*
import com.walker.anke.gson.fromJson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.*
import net.ischool.isus.databinding.ActivityInitBinding
import net.ischool.isus.io.PUBLIC_KEY
import net.ischool.isus.io.decryptSpilt
import net.ischool.isus.io.getPublicKey
import net.ischool.isus.model.QRInfo
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.scheme.isFunction
import net.ischool.isus.service.CMDBService
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.nio.charset.Charset

/**
 * 初始化界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/12
 */
class InitActivity : ISUSActivity() {

    companion object {
        private const val SAFETY_APP = "net.zxedu.safetycampus"
        private const val SCAN_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityInitBinding
    private val receiver = CMDBUpdateReceiver()

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolBar.setTitle(R.string.device_init_title)
        setSupportActionBar(binding.toolBar)

        binding.okBtn.setOnClickListener { init() }
        binding.scanBtn.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, SCAN_REQUEST_CODE)
        }

        if (applicationContext.packageName == SAFETY_APP) {
            binding.safetyLogo.visiable()
            binding.safetyTitle.visiable()
            binding.safetySubtitle.visiable()
        }
        if (ISUS.instance.se) {
            binding.apply {
                setSchoolIdTip.visiable()
                setSchoolId.visiable()
                setPassCodeTip.visiable()
                setPassCode.visiable()
                setPemTip.visiable()
                setPem.visiable()
                setPem.setText(DEFAULT_PEM_DOWNLOAD_HOST)
                setDomain.setText(DEFAULT_SE_API_HOST)
            }
        } else {
            binding.setDomain.setText(DEFAULT_API_HOST)
        }

        autoInit(getCMDB())
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter(CMDBService.ACTION_UPDATE_CMDBID))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SCAN_REQUEST_CODE || resultCode != RESULT_OK) return
        val result = data?.getStringExtra(ScanActivity.SCAN_RESULT) ?: ""
        if (result.isEmpty()) return
        parseScanResult(result)
    }

    /**
     * 对扫码结果进行解析：
     *
     * 处理 URL 转义
     * Base64 Decode
     * 对Base64 Decode之后的数据切分成 128 字节的片段
     * 使用 RAS 公钥对 128 字节的片段进行解密，片段长 128 字节时无需 Padding， 片段长小于 128 字节时使用 PKCS1Padding
     * 将所有片段解密出来的数据连接成一个字符串
     * 对解密后的字符串进行 JSON Decode
     */
    private fun parseScanResult(result: String) {
        if (!result.isFunction()) return
        val uri = Uri.parse(result)
        if (uri.path != "/cmdb-device-bind") return
        val encodeParam = uri.getQueryParameter("s") ?: ""
        if (encodeParam.isEmpty()) return
        // 加密数据
        val encryptedData = Base64.decode(encodeParam, Base64.DEFAULT)
        // 公钥
        val pubKey = getPublicKey(Base64.decode(PUBLIC_KEY, Base64.DEFAULT))
        // 使用公钥对加密数据进行分片解密
        val decryptData = decryptSpilt(encryptedData, pubKey)
        // 解密数据转编码成UTF-8字符串
        val json = String(decryptData, Charset.defaultCharset())
        // 解析JSON实体
        val qrInfo = Gson().fromJson<QRInfo>(json)
        binding.apply {
            // 填充数据
            setSchoolId.setText("${qrInfo.sid}")
            setCmdbId.setText("${qrInfo.cmdbid}")
            setPassCode.setText(qrInfo.code)
            setDomain.setText(qrInfo.server)
            // 如果没有证书地址，代表无需要配置证书，清空输入框的默认地址
            setPem.setText(qrInfo.certificate ?: "")
        }
    }

    private fun init() {
        val host = binding.setDomain.text
        if (host.isEmpty()) {
            runOnUiThread { toast("请输入服务器地址") }
            return
        }
        // 过滤最后的反斜杠
        if (host.last() == '/')
            host.delete(host.length - 1, host.length)
        if (ISUS.instance.se) {
            PreferenceManager.instance.setPlatformApi("$host$SE_API_PATH")
            initSe()
        } else {
            PreferenceManager.instance.setPlatformApi("$host$API_PATH")
            initPoor()
        }
    }

    private fun getCMDB(): String {
        try {
            val file = File(filesDir.absolutePath, CMDBService.FILE_NAME)
            return if (!file.exists()) "" else file.readText()
        } catch (e: Exception) {
            print(e)
        }
        return ""
    }

    private fun autoInit(cmdb: String) {
        if (cmdb.isNotEmpty()) {
            // 根据使用的是否是增强模式，完成cmdbid的不同解析逻辑
            if (ISUS.instance.se) {
                val args = cmdb.split('_')
                if (args.size != 3) {
                    longToast("命令行参数不正确！无法自动完成初始化！")
                    return
                }
                binding.apply {
                    setCmdbId.setText(args[0])
                    setSchoolId.setText(args[1])
                    setPassCode.setText(args[2])
                }
            } else {
                binding.setCmdbId.setText(cmdb)
            }
            binding.okBtn.performClick()
        }
    }

    inner class CMDBUpdateReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            autoInit(intent?.getStringExtra(CMDBService.ARG_CMDBID) ?: "")
        }
    }

    /**
     * 普通模式初始化
     */
    @SuppressLint("CheckResult")
    private fun initPoor() {
        var dialog: ProgressDialog? = null

        if (binding.setCmdbId.text.isEmpty()) {
            runOnUiThread { toast("CMDB ID不能为空") }
        } else {
            runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) {
                setCancelable(false)
                window?.attributes?.gravity = Gravity.CENTER
            } }
            APIService.getSchoolId()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap {
                    val result = checkNotNull(it.body())
                    if (result.errno == net.ischool.isus.RESULT_OK) {
                        APIService.initDevice(binding.setCmdbId.text.toString(), result.data.schoolId.toString())
                    } else {
                        Observable.error(Throwable(result.error))
                    }

                }.flatMap { APIService.getConfig() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        dialog?.dismiss()
                        toast("设备初始化成功")
                        // 启动ZXBS服务
                        startZXBS()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onError = {
                        dialog?.dismiss()
                        Log.e(LOG_TAG, "${it.message}")
                        longToast("设备初始化失败，请稍后重试!（${it.message}）")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
        }
    }

    /**
     * 安全增强版初始化
     */
    @SuppressLint("CheckResult")
    private fun initSe() {
        val schoolId = binding.setSchoolId.text.toString()
        val passCode = binding.setPassCode.text.toString()
        val cmdbId = binding.setCmdbId.text.toString()
        val pemHost = binding.setPem.text.toString()
        val pemDownloadUrl = if (pemHost.isNotEmpty()) "$pemHost${PEM_DOWNLOAD_PATH}schoolcdn-$schoolId-$passCode.p12" else ""

        if (schoolId.isEmpty() || passCode.isEmpty() || cmdbId.isEmpty()) {
            runOnUiThread { toast("缺少必要参数") }
            return
        }

        var dialog: ProgressDialog? = null
        runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) {
            setCancelable(false)
            window?.attributes?.gravity = Gravity.CENTER
        } }

        if (pemDownloadUrl.isNotEmpty()) {  // 如果配置了证书下载地址，则先下载证书，再初始化
            APIService.downloadAsync(pemDownloadUrl, filesDir.absolutePath, callback = object : StringCallback {
                override fun onResponse(string: String) {
                    PreferenceManager.instance.setSePemPath(string)
                    PreferenceManager.instance.setKeyPass(genPrivateKeyPass(passCode))
                    APIService.initDevice(cmdbId, schoolId)
                        .subscribeOn(Schedulers.io())
                        .flatMap { APIService.getConfig() }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onNext = {
                                dialog?.dismiss()
                                toast("设备初始化成功")
                                // 启动ZXBS服务
                                startZXBS()
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            onError = {
                                dialog?.dismiss()
                                longToast("设备初始化失败，请稍后重试!（${it.message}）")
                                Log.e(LOG_TAG, "${it.message}")
                                toast("${it.message}")
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        )
                }

                override fun onFailure(request: Request, e: IOException) {
                    runOnUiThread {
                        dialog?.dismiss()
                        toast("证书下载失败，请尝试重新初始化！")
                        longToast("Error: ${e.message}")
                    }
                }
            })
        } else {    // 如果未配置证书下载地址，则代表不需要证书，直接初始化
            APIService.initDevice(cmdbId, schoolId)
                .subscribeOn(Schedulers.io())
                .flatMap { APIService.getConfig() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        dialog?.dismiss()
                        toast("设备初始化成功")
                        // 启动ZXBS服务
                        startZXBS()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onError = {
                        dialog?.dismiss()
                        longToast("设备初始化失败，请稍后重试!（${it.message}）")
                        Log.e(LOG_TAG, "${it.message}")
                        toast("${it.message}")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
        }
    }

    private fun genPrivateKeyPass(pass: String) = "zxedu$pass$CONFIG_RSA_PASS".md5()
}