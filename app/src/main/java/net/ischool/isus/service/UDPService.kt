package net.ischool.isus.service

import android.util.Log
import com.google.gson.Gson
import com.walker.anke.gson.fromJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.CommandResult
import net.ischool.isus.getDeviceID
import net.ischool.isus.model.Command
import net.ischool.isus.model.UDPMessage
import java.io.IOException
import java.lang.Exception
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
    private const val BROADCAST_IP = "255.255.255.255"
    private var socket: DatagramSocket? = null
    // UDP服务是否激活
    private var udpRunning = false
    private var udpLifeOver = true
    private const val TAG = "UDPService"

    @ExperimentalStdlibApi
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
                    val bytes = ByteArray(UDPMessage.MSG_SIZE)
                    val packet = DatagramPacket(bytes, bytes.size)
                    socket?.receive(packet)
                    val msg = UDPMessage(packet.data)
                    Log.i(TAG, "version: ${msg.version}, payload: ${msg.payload}, receiver: ${msg.receiver}, sender: ${msg.sender}, data: ${msg.data.decodeToString()}")

                    // 本地UUID与receiver比对
                    if (msg.receiver == getDeviceID()) {
                        val command = Gson().fromJson<Command>(msg.data.decodeToString())
                        CommandParser.instance.processCommand(command, msg.sender)
                    }
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

    private fun send(data: ByteArray) {
        try {
            socket?.let {
                if (it.isBound && !it.isClosed)
                    socket?.send(DatagramPacket(data, data.size,InetSocketAddress(BROADCAST_IP, PORT)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 发送基于UDP的命令回执
     */
    @ExperimentalStdlibApi
    fun sendResult(result: CommandResult, remoteUUID: String) {
        // 仅当remoteUUID存在，才向目标发送回执
        if (remoteUUID.isNotBlank()) {
            val json = Gson().toJson(result) ?: ""
            val data = json.encodeToByteArray()
            send(pack(data, remoteUUID))
        }
    }

    /**
     * 按协议格式进行数据打包
     */
    @ExperimentalStdlibApi
    private fun pack(data: ByteArray, remoteUUID: String): ByteArray {
        /**
         * message version 0x01
         * version: 1 byte
         * payload type: 1 byte
         * receiver uuid: 16 bytes
         * sender uuid: 16 bytes
         * data: 0-1024 bytes
         */

        return byteArrayOf(0x01, 0x01) + remoteUUID.encodeToByteArray() + getDeviceID().encodeToByteArray() + data
    }
}