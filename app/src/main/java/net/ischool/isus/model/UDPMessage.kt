package net.ischool.isus.model

/**
 * UDP 消息封装
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/23
 */
data class UDPMessage(var bytes: ByteArray) {

    companion object {
        const val MSG_HEAD = 1 + 1 + 16 + 16
        const val MSG_SIZE = MSG_HEAD + 1024
    }

    var version: Byte = 1
    var payload: Byte = 1
    var receiver: String = ""
    var sender: String = ""
    var data: ByteArray = ByteArray(0)

    init {
        if (bytes.size >= MSG_HEAD) {
            version = bytes[0]
            payload = bytes[1]
            receiver = String(bytes, 2, 16)
            sender = String(bytes, 18, 16)
            data = bytes.drop(MSG_HEAD).toByteArray()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UDPMessage

        if (version != other.version) return false
        if (payload != other.payload) return false
        if (receiver != other.receiver) return false
        if (sender != other.sender) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result: Int = version.toInt()
        result = 31 * result + payload
        result = 31 * result + receiver.hashCode()
        result = 31 * result + sender.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}