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
import com.jakewharton.rxbinding4.view.clicks
import com.trello.rxlifecycle4.android.ActivityEvent
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import com.trello.rxlifecycle4.kotlin.bindUntilEvent
import com.walker.anke.foundation.md5
import com.walker.anke.framework.*
import com.walker.anke.gson.fromJson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_init.*
import net.ischool.isus.*
import net.ischool.isus.io.PUBLIC_KEY
import net.ischool.isus.io.decrypt
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
import java.util.concurrent.TimeUnit

/**
 * 初始化界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/12
 */
class InitActivity : RxAppCompatActivity() {

    companion object {
        private const val SAFETY_APP = "net.zxedu.safetycampus"
        private const val SCAN_REQUEST_CODE = 100
    }

    private val receiver = CMDBUpdateReceiver()

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
        tool_bar.setTitle(R.string.device_init_title)
        setSupportActionBar(tool_bar)

        ok_btn.clicks()
            .debounce(1, TimeUnit.SECONDS)
            .bindUntilEvent(this, ActivityEvent.DESTROY)
            .subscribe { init() }

        scanBtn.clicks()
            .debounce(1, TimeUnit.SECONDS)
            .bindUntilEvent(this, ActivityEvent.DESTROY)
            .subscribe {
                val intent = Intent(this, ScanActivity::class.java)
                startActivityForResult(intent, SCAN_REQUEST_CODE)
            }

        if (applicationContext.packageName == SAFETY_APP) {
            safety_logo.visiable()
            safety_title.visiable()
            safety_subtitle.visiable()
        }
        if (ISUS.instance.se) {
            set_school_id_tip.visiable()
            set_school_id.visiable()
            set_pass_code_tip.visiable()
            set_pass_code.visiable()
            set_domain_tip.visiable()
            set_domain.visiable()
            set_pem_tip.visiable()
            set_pem.visiable()
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
        // 填充数据
        set_school_id.setText("${qrInfo.sid}")
        set_cmdb_id.setText("${qrInfo.cmdbid}")
        set_pass_code.setText(qrInfo.code)
        set_domain.setText(qrInfo.server)
        qrInfo.certificate?.let {
            set_pem.setText(it)
        }
    }

    private fun init() {
        if (ISUS.instance.se)
            initSe()
        else
            initPoor()
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
                set_cmdb_id.setText(args[0])
                set_school_id.setText(args[1])
                set_pass_code.setText(args[2])
            } else {
                set_cmdb_id.setText(cmdb)
            }
            ok_btn.performClick()
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
    private fun initPoor() {
        var dialog: ProgressDialog? = null

        if (set_cmdb_id.text.isEmpty()) {
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
                        APIService.initDevice(set_cmdb_id.text.toString(), result.data.school_id.toString())
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
    private fun initSe() {
        val schoolId = set_school_id.text.toString()
        val passCode = set_pass_code.text.toString()
        val cmdbId = set_cmdb_id.text.toString()

        if (schoolId.isEmpty() || passCode.isEmpty() || cmdbId.isEmpty()) {
            runOnUiThread { toast("缺少必要参数") }
            return
        }

        var dialog: ProgressDialog? = null
        runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) {
            setCancelable(false)
            window?.attributes?.gravity = Gravity.CENTER
        } }

        val url = "http://update.${ISUS.instance.domain}/zxedu-system-images/schools/schoolcdn-$schoolId-$passCode.p12"
        APIService.downloadAsync(url, filesDir.absolutePath, callback = object : StringCallback {
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
    }

    private fun genPrivateKeyPass(pass: String) = "zxedu$pass$CONFIG_RSA_PASS".md5()
}