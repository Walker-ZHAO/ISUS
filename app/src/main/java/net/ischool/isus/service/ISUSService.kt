package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import net.ischool.isus.command.CommandParser
import net.ischool.isus.network.APIService
import net.ischool.isus.network.interceptor.CacheInterceptor
import java.net.ProtocolException
import java.util.concurrent.TimeUnit

/**
 * 统一推送服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/19
 */
class ISUSService : Service() {

    companion object {
        val COMMAND_START = "net.ischool.isus.start"
        val COMMAND_STOP  = "net.ischool.isus.stop"
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, ISUSService::class.java)
            intent.action = COMMAND_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ISUSService::class.java)
            intent.action = COMMAND_STOP
            context.startService(intent)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                COMMAND_START ->  {
                    isRunning = true
                }
                COMMAND_STOP -> {
                    isRunning = false
                    APIService.cancel()
                }
                else    -> {}
            }
            if (isRunning) {
                APIService.getCommand()
                        .subscribeOn(Schedulers.io())
                        .repeatWhen {
                            it.flatMap {
                                if (isRunning)
                                    Observable.timer(0, TimeUnit.MILLISECONDS)
                                else
                                    Observable.error<Throwable>(Throwable("Finish"))
                            }
                        }
                        .retryWhen {
                            it.flatMap {
                                it.printStackTrace()
                                if (isRunning) {
                                    // 408 超过最大尝试次数，OkHttp会抛出ProtocolException，此情况应立即重试
                                    // 返回无数据的情况，也应该立即重试
                                    // 其他情况等待5s后，再进行连接（并且需要清空HTTP的缓存头）
                                    if (it is ProtocolException || it is NullPointerException)
                                        Observable.timer(0, TimeUnit.MILLISECONDS)
                                    else {
                                        CacheInterceptor.etag = ""
                                        CacheInterceptor.last_modified = ""
                                        Observable.timer(5, TimeUnit.SECONDS)
                                    }
                                } else
                                    Observable.error<Throwable>(Throwable("Finish"))
                            }
                        }
                        .subscribeBy(onNext = {
                            val command = it.body()
                            if (command != null) {
                                Log.i("Walker", "onNext: $command")
                                CommandParser.instance.processCommand(command)
                            }
                        }, onError = {
                            Log.i("Walker", "onError: $it")
                            it.printStackTrace()
                        }, onComplete = {
                            Log.i("Walker", "onComplete")
                        })
            }
        }
        return START_STICKY
    }
}