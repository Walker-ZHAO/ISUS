package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import net.ischool.isus.preference.PreferenceManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startService
import java.io.File
import java.lang.Exception

/**
 * CMDB ID设置服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-21
 */
class CMDBService : Service() {

    companion object {
        const val FILE_NAME = "cmdbid"
        const val ARG_CMDBID = "cmdb_id"
        const val ACTION_UPDATE_CMDBID = "net.ischool.isus.update_cmdbid"

        fun startService(context: Context?, cmdbid: String) {
            context?.startService<CMDBService>(ARG_CMDBID to cmdbid)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dealWithCMDB(intent?.getStringExtra(ARG_CMDBID) ?: "", startId)
        return START_NOT_STICKY
    }

    private fun dealWithCMDB(cmdbid: String, startId: Int) {
        doAsync {
            try {
                val file = File(filesDir.absolutePath, FILE_NAME)
                file.createNewFile()

                val preCMDB = file.readText()
                file.writeText(cmdbid)
                if (preCMDB.isNotEmpty() && preCMDB != cmdbid) {
                    PreferenceManager.needUpdateCMDB = true
                    baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(it)
                    }
                }
                // 发送更新cmdbid广播
                sendBroadcast(Intent(ACTION_UPDATE_CMDBID).apply { putExtra(ARG_CMDBID, cmdbid) })
            } catch (e: Exception) {
                print(e)
            } finally {
                stopSelfResult(startId)
            }
        }
    }
}