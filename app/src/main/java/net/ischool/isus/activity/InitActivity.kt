package net.ischool.isus.activity

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_init.*
import net.ischool.isus.R
import net.ischool.isus.network.APIService
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

/**
 * 可视电话初始化界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/12
 */
class InitActivity : RxAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
        tool_bar.setTitle(R.string.device_init_title)
        setSupportActionBar(tool_bar)

        RxView.clicks(ok_btn)
                .debounce(1, TimeUnit.SECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .subscribe { init() }
    }

    private fun init() {

        var dialog: ProgressDialog? = null
        runOnUiThread { dialog = indeterminateProgressDialog(getString(R.string.init_dialog_title)) { setCancelable(false) } }

        if (set_school_id.text.isEmpty()) {
            runOnUiThread { toast("学校ID不能为空") }
        } else if (set_cmdb_id.text.isEmpty()) {
            runOnUiThread { toast("CMDB ID不能为空") }
        } else {
            APIService.initDevice(set_cmdb_id.text.toString(), set_school_id.text.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .flatMap { APIService.getConfig() }
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
}