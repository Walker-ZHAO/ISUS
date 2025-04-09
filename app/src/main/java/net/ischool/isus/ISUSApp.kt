package net.ischool.isus

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import net.ischool.isus.log.Syslog

/**
 * ISUS 应用
 *
 * 可检测应用是否在前台或后台
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/4/9
 */
open class ISUSApp: Application() {

    companion object {
        var lastTaskId: Int = -1
    }

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onForeground() {
                // 应用切换到前台
                lastTaskId = -1
                Log.i("Walker", "App is in foreground")
                Syslog.logI("App is in foreground", category = SYSLOG_CATEGORY_APP)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onBackground() {
                // 应用切换到后台
                Log.i("Walker", "App is in background")
                Syslog.logI("App is in background", category = SYSLOG_CATEGORY_APP)
                if (lastTaskId != -1) {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    intent?.let {
                        startActivity(it)
                        activityManager.moveTaskToFront(lastTaskId, ActivityManager.MOVE_TASK_WITH_HOME)
                    }
                }
            }
        })
    }
}