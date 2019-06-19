package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import net.ischool.isus.network.APIService
import org.jetbrains.anko.startService
import java.util.concurrent.TimeUnit

/**
 * 状态上报服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-19
 */
class StatusPostService: Service() {
    companion object {
        fun startService(context: Context) {
            context.startService<StatusPostService>()
        }
    }

    // 用于取消网络操作
    private var disposables = CompositeDisposable()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postStatus()
        return START_STICKY
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    private fun postStatus() {
        // 每分钟访问一次，不能使用retryWhen及repeatWhen操作符，因为每次访问的ts均不一致
        val disposable = APIService.postStatus()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onComplete = { disposables.add(Observable.timer(1, TimeUnit.MINUTES).subscribe { postStatus() }) },
                onError = { disposables.add(Observable.timer(1, TimeUnit.MINUTES).subscribe { postStatus() }) }
            )
        disposables.add(disposable)
    }
}