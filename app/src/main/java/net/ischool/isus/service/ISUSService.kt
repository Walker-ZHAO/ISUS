package net.ischool.isus.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.rabbitmq.client.*
import com.rabbitmq.client.impl.DefaultExceptionHandler
import com.walker.anke.gson.fromJson
import net.ischool.isus.*
import net.ischool.isus.command.CommandParser
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.Command
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import org.jetbrains.anko.doAsync

/**
 * 统一推送服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/19
 */
class ISUSService : Service() {

    /** RabbitMQ Start **/
    private val factory by lazy { ConnectionFactory() }
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val shutdownListener =  { cause: ShutdownSignalException ->
        val type = if (cause.isHardError) "Connection" else "Channel"
        val errorMsg = "RabbitMQ $type shutdown: ${cause.reason}"
        Log.e("ISIS", errorMsg)
        Syslog.logE(errorMsg)
    }
    private val recoveryListener = object : RecoveryListener {
        override fun handleRecovery(recoverable: Recoverable?) {
            val msg = "RabbitMQ connect recovery completed"
            Log.i("ISUS", msg)
            Syslog.logI(msg)
        }

        override fun handleRecoveryStarted(recoverable: Recoverable?) {
            val msg = "RabbitMQ connect recovery Started"
            Log.i("ISUS", msg)
            Syslog.logI(msg)
        }
    }
    private val exceptionHandler = object : DefaultExceptionHandler() {
        override fun handleFlowListenerException(channel: Channel?, exception: Throwable?) {
            super.handleFlowListenerException(channel, exception)
            val msg = "RabbitMQ Flow Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleBlockedListenerException(connection: Connection?, exception: Throwable?) {
            super.handleBlockedListenerException(connection, exception)
            val msg = "RabbitMQ Blocked Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleTopologyRecoveryException(conn: Connection?, ch: Channel?, exception: TopologyRecoveryException?) {
            super.handleTopologyRecoveryException(conn, ch, exception)
            val msg = "RabbitMQ Topology Recovery Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleConsumerException(channel: Channel?, exception: Throwable?, consumer: Consumer?, consumerTag: String?, methodName: String?) {
            super.handleConsumerException(channel, exception, consumer, consumerTag, methodName)
            val msg = "RabbitMQ Consumer($consumerTag) Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
            // 消费事件时产生异常，会导致Channel关闭，需要重新设置连接
            Thread.sleep(5000)
            subscribe()
        }

        override fun handleConnectionRecoveryException(conn: Connection?, exception: Throwable?) {
            super.handleConnectionRecoveryException(conn, exception)
            val msg = "RabbitMQ Connection Recovery Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleUnexpectedConnectionDriverException(conn: Connection?, exception: Throwable?) {
            super.handleUnexpectedConnectionDriverException(conn, exception)
            val msg = "RabbitMQ Connection Driver Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleChannelRecoveryException(ch: Channel?, exception: Throwable?) {
            super.handleChannelRecoveryException(ch, exception)
            val msg = "RabbitMQ Channel Recovery Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleReturnListenerException(channel: Channel?, exception: Throwable?) {
            super.handleReturnListenerException(channel, exception)
            val msg = "RabbitMQ Return Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }

        override fun handleConfirmListenerException(channel: Channel?, exception: Throwable?) {
            super.handleConfirmListenerException(channel, exception)
            val msg = "RabbitMQ Confirm Exception: ${exception?.cause}"
            Log.e("ISUS", msg)
            Syslog.logE(msg)
        }
    }
    /** RabbitMQ End **/

    companion object {
        const val COMMAND_START = "net.ischool.isus.start"
        const val COMMAND_STOP  = "net.ischool.isus.stop"
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, ISUSService::class.java)
            intent.action = COMMAND_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ISUSService::class.java)
            intent.action = COMMAND_STOP
            context.startService(intent)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                COMMAND_START ->  {
                    isRunning = true
                }
                COMMAND_STOP -> {
                    isRunning = false
                    APIService.cancel()
                }
                else    -> {}
            }
            if (isRunning) {
                setUpConnectionFactory()
                subscribe()
            } else {
                channel?.close()
                connection?.close()
            }
        }
        return START_STICKY
    }

    /**
     * 服务器连接设置
     */
    private fun setUpConnectionFactory() {
        factory.host = MQ_DOMAIN
        factory.port = MQ_PORT
        factory.username = MQ_USERNAME
        factory.password = MQ_PASSWORD
        factory.virtualHost = MQ_VHOST
        // 设置连接恢复（4.0+默认开启）
        factory.isAutomaticRecoveryEnabled = true
        // 设置重试间隔，默认5s
        factory.networkRecoveryInterval = 5000
        // 设置异常处理
        factory.exceptionHandler = exceptionHandler
    }

    /**
     * 连接RabbitMQ服务器并订阅消息
     */
    private fun subscribe() {
        doAsync {
            try {
                // 需要再次初始化数据的时候就关闭上一个连接
                channel?.close()
                connection?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                // 创建新的连接
                connection = factory.newConnection()

                // 创建通道
                channel = connection?.createChannel()
                // 处理完一个消息，再接收下一个消息
                channel?.basicQos(1)

                // 设置断开监听器
                connection?.addShutdownListener(shutdownListener)
                channel?.addShutdownListener(shutdownListener)

                // 设置恢复监听
                (connection as Recoverable).addRecoveryListener(recoveryListener)
                (channel as Recoverable).addRecoveryListener(recoveryListener)


                // 队列名称规则：学校ID.设备类型.CMDBID
                val queueName = PreferenceManager.instance.let { "${it.getSchoolId()}.${it.getDeviceType()}.${it.getCMDB()}" }
                // 声明交换机类型
                channel?.exchangeDeclare(MQ_EXCHANGE_NAME, MQ_EXCHANGE_TYPE, true)
                // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
                val queue = channel?.queueDeclare(queueName, true, false, false, null)
                // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
                channel?.queueBind(queue?.queue, MQ_EXCHANGE_NAME, "$MQ_ROUTING_KEY_PREFIX${PreferenceManager.instance.getCMDB()}")
                // 创建消费者获取RabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery方法，该方法可以获取到消息数据并进行相应处理
                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
                        super.handleDelivery(consumerTag, envelope, properties, body)
                        val msg = body?.toString(charset("UTF-8"))
                        Log.e("ISUS", "RabbitMQ message: $msg")
                        Syslog.logI("getCommand Info: $msg")
                        if (msg != null) {
                            val command = Gson().fromJson<Command>(msg)
                            CommandParser.instance.processCommand(command)
                        }
                    }

                    override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
                        super.handleShutdownSignal(consumerTag, sig)
                        val errorMsg = "RabbitMQ consume($consumerTag) get ShutdownSignalException, ${sig?.reason}"
                        Log.e("ISIS", errorMsg)
                        Syslog.logE(errorMsg)
                    }
                }
                channel?.basicConsume(queue?.queue, true, consumer)
            } catch (e: Exception) { // 初次创建连接及相关Topology失败时，不会自动修复连接，需要手动处理
                val errorMsg = "RabbitMQ initial connect and topology failed: ${e.cause}"
                Log.e("ISUS", errorMsg)
                Syslog.logE(errorMsg)
                Thread.sleep(5000)
                subscribe()
            }
        }
    }
}