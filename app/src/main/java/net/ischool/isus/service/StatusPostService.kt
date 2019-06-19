package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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
    private var disposable: Disposable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postStatus()
        return START_STICKY
    }

    override fun onDestroy() {
        if (disposable?.isDisposed == false)
            disposable?.dispose()
        super.onDestroy()
    }

    private fun postStatus() {
        disposable = APIService.postStatus()
            .repeatWhen { it.flatMap { Observable.timer(1, TimeUnit.MINUTES) } }
            .retryWhen { it.flatMap { Observable.timer(1, TimeUnit.MINUTES) } }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}