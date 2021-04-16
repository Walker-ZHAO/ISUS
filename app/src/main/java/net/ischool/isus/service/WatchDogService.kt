package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.RESULT_OK
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import java.util.concurrent.TimeUnit

/**
 * 监控网络状态
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/7/15
 */
class WatchDogService: Service() {

    companion object {
        private const val WATCH_DOG_ACTION = "com.zxedu.watchdog"       // 监控校园APP广播
        const val COMMAND_START = "net.ischool.isus.watchdog.start"
        const val COMMAND_STOP = "net.ischool.isus.watchdog.stop"
        const val SYSLOG_TAG = "watchdog"       // 向Syslog输出日志的TAG
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, WatchDogService::class.java)
            intent.action = COMMAND_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WatchDogService::class.java)
            intent.action = COMMAND_STOP
            context.startService(intent)
        }
    }

    private var disposables = CompositeDisposable()

    override fun onBind(intent: Intent?): IBinder?  = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                COMMAND_START -> {
                    isRunning = true
                }
                COMMAND_STOP -> {
                    isRunning = false
                }
                else -> {
                }
            }

            disposables.dispose()
            disposables = CompositeDisposable()

            if (isRunning) {
                updateNetwork()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    private fun updateNetwork() {
        val disposable = APIService.getNetworkStatus()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = { response ->
                    val status = checkNotNull(response.body())
                    if (status.errno == RESULT_OK) {
                        if (status.data.sids.contains(PreferenceManager.instance.getSchoolId())) {
                            sendBroadcast(Intent(WATCH_DOG_ACTION))
                            Syslog.logI("Network fine", tag = SYSLOG_TAG)
                        }
                        else
                            Syslog.logN(
                                "Network status get wrong school id [${status.data.sids}], current school id: ${PreferenceManager.instance.getSchoolId()}",
                                tag = SYSLOG_TAG
                            )
                    } else {
                        Syslog.logN(
                            "Network status error[${status.errno}]: ${status.error}",
                            tag = SYSLOG_TAG
                        )
                    }
                    disposables.add(Observable.timer(1, TimeUnit.MINUTES).subscribe { updateNetwork() })
                },
                onError = {
                    // 网络访问失败，不再重新尝试
                    it.printStackTrace()
                    Syslog.logE("Network status call error: ${it.message}", tag = SYSLOG_TAG)
                }
            )
        disposables.add(disposable)
    }
}