package net.ischool.isus.log

import android.app.ActivityManager
import android.os.Process
import android.util.TimeUtils
import com.walker.anke.framework.processName
import net.ischool.isus.ISUS
import net.ischool.isus.preference.PreferenceManager
import org.jetbrains.anko.activityManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

/**
 * Syslog 工具
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/13
 */
class Syslog {
    companion object {

        private val socket =  DatagramSocket()
        private val server = InetAddress.getByName("192.168.0.10")

        private val PRI_ERROR = 155
        private val PRI_NOTICE = 157
        private val PRI_INFO = 158

        private fun createLog(rpi: Int = PRI_INFO, message: String): String {
            val ts = SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH).format(Date())
            return with(ISUS.instance.context) {
                val processName = activityManager.runningAppProcesses
                        .filter { it.pid ==  Process.myPid() }
                        .map { it.processName }
                        .firstOrNull()?:"null"
                "<$rpi>$ts $processName[${Process.myPid()}]: CMDBID=${PreferenceManager.instance.getCMDB()}: $message"
            }
        }

        @JvmStatic fun logE(message: String) {
            doAsync {
                val log = createLog(PRI_ERROR, message).toByteArray()
                socket.send(DatagramPacket(log, log.size, server, 514))
            }
        }

        @JvmStatic fun logN(message: String) {
            doAsync {
                val log = createLog(PRI_NOTICE, message).toByteArray()
                socket.send(DatagramPacket(log, log.size, server, 514))
            }
        }

        @JvmStatic fun logI(message: String) {
            doAsync {
                val log = createLog(PRI_INFO, message).toByteArray()
                socket.send(DatagramPacket(log, log.size, server, 514))
            }
        }
    }
}