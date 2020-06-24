package net.ischool.isus.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ischool.isus.model.UDPMessage
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * UDP服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/23
 */
object UDPService {
    private const val PORT = 8800
    private const val TIMEOUT = 30 * 1000
    private var socket: DatagramSocket? = null
    private val bytes = ByteArray(UDPMessage.MSG_SIZE)
    private val packet: DatagramPacket by lazy { DatagramPacket(bytes, bytes.size) }
    // UDP服务是否激活
    private var udpRunning = false
    private var udpLifeOver = true
    private const val TAG = "UDPService"

    fun start() {
        if (udpRunning || !udpLifeOver)
            return
        socket = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            soTimeout = TIMEOUT
            bind(InetSocketAddress(PORT))
        }
        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "UDP 监听开始")
            udpRunning = true
            udpLifeOver = false
            while (udpRunning) {
                try {
                    socket?.receive(packet)
                    val msg = UDPMessage(packet.data)
                    Log.i(TAG, "version: ${msg.version}, payload: ${msg.payload}, receiver: ${msg.receiver}, sender: ${msg.sender}, data: ${msg.data}")
                    // TODO: 本地UUID与receiver比对；data解析成Command；分析Command并执行
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            socket?.close()
            udpLifeOver = true
            Log.i(TAG, "UDP 监听结束")
        }
    }

    fun stop() {
        udpRunning = false
    }

    fun send() {

    }
}