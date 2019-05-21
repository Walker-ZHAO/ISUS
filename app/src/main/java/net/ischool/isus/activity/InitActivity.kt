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
import com.jakewharton.rxbinding2.widget.text
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_init.*
import net.ischool.isus.R
import net.ischool.isus.network.APIService
import net.ischool.isus.service.CMDBService
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit

/**
 * 可视电话初始化界面
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

        RxView.clicks(ok_btn)
                .debounce(1, TimeUnit.SECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .subscribe { init() }

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
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            onComplete = { Log.i("Walker", "onComplete") },
                            onError = {
                                dialog?.dismiss()
                                toast("设备初始化失败，请稍后重试")
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                    )
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
            set_cmdb_id.setText(cmdb)
            ok_btn.performClick()
        }
    }

    inner class CMDBUpdateReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            autoInit(intent?.getStringExtra(CMDBService.ARG_CMDBID) ?: "")
        }
    }
}