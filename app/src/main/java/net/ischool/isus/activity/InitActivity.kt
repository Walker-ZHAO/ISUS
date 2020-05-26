package net.ischool.isus.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import com.walker.anke.foundation.md5
import com.walker.anke.framework.visiable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_init.*
import net.ischool.isus.CONFIG_RSA_PASS
import net.ischool.isus.ISUS
import net.ischool.isus.R
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.CMDBService
import net.ischool.isus.startZXBS
import okhttp3.Request
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit

/**
 * 初始化界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/12
 */
class InitActivity : RxAppCompatActivity() {

    private val receiver = CMDBUpdateReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
        tool_bar.setTitle(R.string.device_init_title)
        setSupportActionBar(tool_bar)

        val disposable = RxView.clicks(ok_btn)
                .debounce(1, TimeUnit.SECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .subscribe { init() }

        if (ISUS.instance.se) {
            set_school_id.visiable()
            set_pass_code.visiable()
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
            runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) { setCancelable(false) } }
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
                    onComplete = { Log.i("Walker", "onComplete") },
                    onError = {
                        dialog?.dismiss()
                        Log.e("ISUS", "${it.message}")
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
        runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) { setCancelable(false) } }

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
                            Log.e("ISUS", "${it.message}")
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