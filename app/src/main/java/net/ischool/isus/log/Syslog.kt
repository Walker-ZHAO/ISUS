package net.ischool.isus.log

import android.os.Process
import com.walker.anke.framework.activityManager
import com.walker.anke.framework.doAsync
import net.ischool.isus.ISUS
import net.ischool.isus.UDP_PORT
import net.ischool.isus.preference.PreferenceManager
import java.io.IOException
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

        private val socket: DatagramSocket by lazy { DatagramSocket() }
        private val server: InetAddress by lazy {
            val host = with(PreferenceManager.instance) {
                if (getSyslog().isEmpty())
                    getServer()
                else
                    getSyslog()
            }
            InetAddress.getByName(host)
        }

        private const val PRI_ERROR = 155       // 错误事件
        private const val PRI_NOTICE = 157      // 普通但重要的事件
        private const val PRI_INFO = 158        // 有用的信息

        private fun createLog(pri: Int = PRI_INFO, message: String, tag: String): String {
            val ts = SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH).format(Date())
            return with(ISUS.instance.context) {
                val tagName = if (tag.isEmpty()) {
                    activityManager.runningAppProcesses
                        .filter { it.pid == Process.myPid() }
                        .map { it.processName }
                        .firstOrNull() ?: "null"
                } else {
                    tag
                }
                "<$pri>$ts $tagName[${Process.myPid()}]: CMDBID=${PreferenceManager.instance.getCMDB()}: $message"
            }
        }

        /**
         * 输出报警信息，如系统崩溃，异常捕获，API调用异常等
         */
        @JvmOverloads
        @JvmStatic fun logE(message: String, tag: String = "") {
            doAsync {
                val log = createLog(PRI_ERROR, message, tag).toByteArray()
                try {
                    socket.send(DatagramPacket(log, log.size, server, UDP_PORT))
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

        /**
         * 输出注意信息，如业务逻辑不合规，接口数据无效，接口的业务逻辑错误等
         */
        @JvmOverloads
        @JvmStatic fun logN(message: String, tag: String = "") {
            doAsync {
                val log = createLog(PRI_NOTICE, message, tag).toByteArray()
                try {
                    socket.send(DatagramPacket(log, log.size, server, UDP_PORT))
                }  catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 输出常规日志信息，如业务逻辑埋点等
         */
        @JvmOverloads
        @JvmStatic fun logI(message: String, tag: String = "") {
            doAsync {
                val log = createLog(PRI_INFO, message, tag).toByteArray()
                try {
                    socket.send(DatagramPacket(log, log.size, server, UDP_PORT))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}