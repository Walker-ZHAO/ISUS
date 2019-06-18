package net.ischool.isus.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import net.ischool.isus.ISUS
import net.ischool.isus.activity.UserSyncActivity
import net.ischool.isus.preference.PreferenceManager
import org.jetbrains.anko.newTask

/**
 * 用户信息全量同步的命令行广播监听
 *
 * adb shell am broadcast -a net.ischool.isus.sync
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-05
 */
class UserSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!PreferenceManager.instance.getInitialized())
            Toast.makeText(ISUS.instance.context, "请先对设备进行初始化操作", Toast.LENGTH_LONG).show()
        else
            context?.startActivity(Intent(context, UserSyncActivity::class.java).newTask())
    }
}