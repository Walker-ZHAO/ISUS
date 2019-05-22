package net.ischool.isus.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.ischool.isus.service.CMDBService

/**
 * CMDB ID设置的命令行广播监听
 *
 * adb shell am broadcast -a net.ischool.isus.cmdbid  --es cmdb_id "1234567"
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-21
 */
class CMDBReceiver : BroadcastReceiver(){
    companion object {
        private const val ARG_CMDBID = "cmdb_id"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        val cmdbid = intent?.getStringExtra(ARG_CMDBID)?:""
        if (cmdbid.isNotEmpty()) {
            CMDBService.startService(context, cmdbid)
        }
    }
}