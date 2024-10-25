package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.launchdarkly.eventsource.DefaultRetryDelayStrategy
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.HttpConnectStrategy
import com.launchdarkly.eventsource.MessageEvent
import com.launchdarkly.eventsource.background.BackgroundEventHandler
import com.launchdarkly.eventsource.background.BackgroundEventSource
import com.walker.anke.framework.doAsync
import com.walker.anke.gson.fromJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ischool.isus.LOG_TAG
import net.ischool.isus.SYSLOG_CATEGORY_SSE
import net.ischool.isus.command.CommandParser
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.Command
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import okhttp3.internal.closeQuietly
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 基于EventSource的统一推送服务
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/15
 */
class SSEService : Service() {
    companion object {
        const val COMMAND_START = "net.ischool.isus.sse.start"
        const val COMMAND_STOP = "net.ischool.isus.sse.stop"
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, SSEService::class.java)
            intent.action = COMMAND_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SSEService::class.java)
            intent.action = COMMAND_STOP
            context.startService(intent)
        }
    }

    private var eventSourceSse: BackgroundEventSource? = null

    private inner class EventHandler : BackgroundEventHandler {

        override fun onOpen() {
            val msg = "SSE onOpen"
            Log.w(LOG_TAG, msg)
            Syslog.logN(msg, category = SYSLOG_CATEGORY_SSE)

        }

        override fun onMessage(event: String, messageEvent: MessageEvent) {
            if (event == "message") {
                val msg = "SSE onMessage: $event, $messageEvent"
                Log.i(LOG_TAG, msg)
                Syslog.logI(msg, category = SYSLOG_CATEGORY_SSE)
                val command = Gson().fromJson<Command>(messageEvent.data)
                CommandParser.instance.processCommand(command)
            }
        }

        override fun onClosed() {
            val msg = "SSE onClosed"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_SSE)
            reCreate()
        }

        override fun onError(t: Throwable) {
            val msg = "SSE onError: ${t.message}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_SSE)
        }

        override fun onComment(comment: String) {
            val msg = "SSE onComment: $comment"
            Log.w(LOG_TAG, msg)
            Syslog.logN(msg, category = SYSLOG_CATEGORY_SSE)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                COMMAND_START -> {
                    isRunning = true
                }
                COMMAND_STOP -> {
                    isRunning = false
                    APIService.cancel()
                }
                else -> {
                }
            }
            if (isRunning) {
                setup()
            } else {
                disconnect()
            }
        }
        return START_STICKY
    }

    private fun setup() {
        val baseUrl = PreferenceManager.instance.getCdnUrl()
        val path = "/Cdn/Equipment/ISUSEvents"
        try {
            val uri = URI.create("$baseUrl$path")
            val connectStrategy = HttpConnectStrategy.http(uri).httpClient(APIService.client)
            val eventSourceBuilder = EventSource.Builder(connectStrategy)
                .retryDelay(10, TimeUnit.SECONDS)
                .retryDelayStrategy(
                    DefaultRetryDelayStrategy.defaultStrategy().maxDelay(10, TimeUnit.SECONDS)
                )
            eventSourceSse = BackgroundEventSource.Builder(EventHandler(), eventSourceBuilder).build()
            eventSourceSse?.start()
        } catch (e: Exception) {
            val errorMsg = "SSE setup failed: ${e.message}"
            Log.e(LOG_TAG, errorMsg)
            Syslog.logE(errorMsg, category = SYSLOG_CATEGORY_SSE)
            reCreate()
        }
    }

    private fun disconnect() {
        eventSourceSse?.closeQuietly()
    }

    private fun reCreate() {
        Thread.sleep(5000)
        disconnect()
        Thread.sleep(2000)
        setup()
    }
}