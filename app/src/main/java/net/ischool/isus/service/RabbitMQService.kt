package net.ischool.isus.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.rabbitmq.client.*
import com.rabbitmq.client.impl.DefaultExceptionHandler
import com.walker.anke.framework.doAsync
import com.walker.anke.gson.fromJson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.*
import net.ischool.isus.command.CommandParser
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.Command
import net.ischool.isus.model.SECommand
import net.ischool.isus.model.SEUserSync
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.network.se.SSLSocketFactoryProvider
import net.ischool.isus.preference.PreferenceManager
import okhttp3.Request
import java.io.IOException

/**
 * 基于RabbitMQ的统一推送服务
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/19
 */
class RabbitMQService : Service() {

    /** RabbitMQ Start **/
    private val factory by lazy { ConnectionFactory() }
    private var connection: Connection? = null
    private var channel: Channel? = null
    private val shutdownListener = { cause: ShutdownSignalException ->
        val type = if (cause.isHardError) "Connection" else "Channel"
        val errorMsg = "RabbitMQ $type shutdown: ${cause.reason}"
        Log.e(LOG_TAG, errorMsg)
        Syslog.logE(errorMsg, category = SYSLOG_CATEGORY_RABBITMQ)
        changeState(QueueState.STATE_BLOCK)
    }
    private val recoveryListener = object : RecoveryListener {
        override fun handleRecovery(recoverable: Recoverable?) {
            val msg = "RabbitMQ connect recovery completed"
            Log.w(LOG_TAG, msg)
            Syslog.logN(msg, category = SYSLOG_CATEGORY_RABBITMQ)
            changeState(QueueState.STATE_STANDBY)
        }

        override fun handleRecoveryStarted(recoverable: Recoverable?) {
            val msg = "RabbitMQ connect recovery Started"
            Log.w(LOG_TAG, msg)
            Syslog.logN(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }
    }

    // 普通模式下的消费者，连接校园服务器
    inner class PoorConsumer(channel: Channel?): DefaultConsumer(channel) {
        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray?
        ) {
            super.handleDelivery(consumerTag, envelope, properties, body)
            val msg = body?.toString(charset("UTF-8"))
            Log.i(LOG_TAG, "RabbitMQ message: $msg")
            Syslog.logI("getCommand Info: $msg", category = SYSLOG_CATEGORY_RABBITMQ)
            msg?.let {
                val command = Gson().fromJson<Command>(it)
                CommandParser.instance.processCommand(command)
            }
        }

        override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
            super.handleShutdownSignal(consumerTag, sig)
            val errorMsg = "RabbitMQ consume($consumerTag) get ShutdownSignalException, ${sig?.reason}"
            Log.e(LOG_TAG, errorMsg)
            Syslog.logE(errorMsg, category = SYSLOG_CATEGORY_RABBITMQ)
        }
    }

    // 安全增强模式下的消费者，连接公网服务器
    inner class SeConsumer(channel: Channel?): DefaultConsumer(channel) {
        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray?
        ) {
            super.handleDelivery(consumerTag, envelope, properties, body)
            val msg = body?.toString(charset("UTF-8"))

            Log.e(LOG_TAG, "RabbitMQ SE message: $msg")
            Log.e(LOG_TAG, "RabbitMQ SE routing key: ${envelope?.routingKey}")

            msg?.let {
                when (envelope?.routingKey) {
                    MQ_ROUTING_KEY_COMET -> {   // 普通命令
                        Syslog.logI("getCommand SE Info: $it", category = SYSLOG_CATEGORY_RABBITMQ)
                        // 直接ack
                        channel?.basicAck(envelope.deliveryTag, false)
                        Gson().fromJson<SECommand>(it).payload.payload.apply {
                            if (cmdbid == PreferenceManager.instance.getCMDB())
                                CommandParser.instance.processCommand(this)
                        }
                    }
                    MQ_ROUTING_KEY_USER -> {    // 增量同步用户信息
                        // 非IC刷卡考勤项目，忽略用户信息同步消息
                        if (PreferenceManager.instance.getDeviceType() != DeviceType.SECURITY) {
                            return@let
                        }
                        Syslog.logI("syncUser SE Info: $msg", category = SYSLOG_CATEGORY_RABBITMQ)
                        Gson().fromJson<SEUserSync>(it).payload.uid.apply {
                            syncUserInfo(
                                toLong(),
                                success = { doAsync { channel?.basicAck(envelope.deliveryTag, false) } },
                                fail = { doAsync { channel?.basicReject(envelope.deliveryTag, true) } })
                        }
                    }
                    else -> {
                        // 其他路由消息，拒收
                        channel?.basicReject(envelope?.deliveryTag!!, false)
                    }
                }
            }
        }

        override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
            super.handleShutdownSignal(consumerTag, sig)
            val errorMsg = "RabbitMQ SE consume($consumerTag) get ShutdownSignalException, ${sig?.reason}"
            Log.e(LOG_TAG, errorMsg)
            Syslog.logE(errorMsg, category = SYSLOG_CATEGORY_RABBITMQ)
        }
    }

    private val exceptionHandler = object : DefaultExceptionHandler() {
        override fun handleBlockedListenerException(connection: Connection?, exception: Throwable?) {
            super.handleBlockedListenerException(connection, exception)
            val msg = "RabbitMQ Blocked Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleTopologyRecoveryException(
            conn: Connection?,
            ch: Channel?,
            exception: TopologyRecoveryException?
        ) {
            super.handleTopologyRecoveryException(conn, ch, exception)
            val msg = "RabbitMQ Topology Recovery Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleConsumerException(
            channel: Channel?,
            exception: Throwable?,
            consumer: Consumer?,
            consumerTag: String?,
            methodName: String?
        ) {
            super.handleConsumerException(channel, exception, consumer, consumerTag, methodName)
            val msg = "RabbitMQ Consumer($consumerTag) Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
            // 消费事件时产生异常，会导致Channel关闭，需要重新设置连接
            Thread.sleep(5000)
            subscribe()
        }

        override fun handleConnectionRecoveryException(conn: Connection?, exception: Throwable?) {
            super.handleConnectionRecoveryException(conn, exception)
            val msg = "RabbitMQ Connection Recovery Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleUnexpectedConnectionDriverException(conn: Connection?, exception: Throwable?) {
            super.handleUnexpectedConnectionDriverException(conn, exception)
            val msg = "RabbitMQ Connection Driver Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleChannelRecoveryException(ch: Channel?, exception: Throwable?) {
            super.handleChannelRecoveryException(ch, exception)
            val msg = "RabbitMQ Channel Recovery Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleReturnListenerException(channel: Channel?, exception: Throwable?) {
            super.handleReturnListenerException(channel, exception)
            val msg = "RabbitMQ Return Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }

        override fun handleConfirmListenerException(channel: Channel?, exception: Throwable?) {
            super.handleConfirmListenerException(channel, exception)
            val msg = "RabbitMQ Confirm Exception: ${exception?.cause}"
            Log.e(LOG_TAG, msg)
            Syslog.logE(msg, category = SYSLOG_CATEGORY_RABBITMQ)
        }
    }

    /** RabbitMQ End **/

    companion object {
        const val COMMAND_START = "net.ischool.isus.rabbitmq.start"
        const val COMMAND_STOP = "net.ischool.isus.rabbitmq.stop"
        var isRunning = false
        @JvmField var queueState = QueueState.STATE_BLOCK

        fun start(context: Context) {
            val intent = Intent(context, RabbitMQService::class.java)
            intent.action = COMMAND_START
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RabbitMQService::class.java)
            intent.action = COMMAND_STOP
            context.startService(intent)
        }

        /**
         * 同步用户信息
         *
         * @param uid       用户ID
         * @param success   同步成功回调，UI线程
         * @param fail      同步失败回调，UI线程
         */
        @SuppressLint("CheckResult")
        fun syncUserInfo(uid: Long, success: () -> Unit, fail: () -> Unit) {
            APIService.getUserInfo(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        val result = checkNotNull(it.body())
                        when (result.errno) {
                            0 -> {  // 更新用户信息，ack
                                val user = result.data
                                // 头像无效，仅保存用户信息
                                if (user.avatar.isBlank()) {
                                    user.cacheAvatar = ""
                                    ObjectBox.updateUser(user)
                                    success()
                                    return@subscribeBy
                                }
                                APIService.downloadAsync(
                                    user.avatar,
                                    AVATAR_CACHE_DIR,
                                    "${user.uid}.jpg",
                                    object : StringCallback {
                                        override fun onResponse(string: String) {
                                            user.cacheAvatar = string
                                            ObjectBox.updateUser(user)
                                            success()
                                        }

                                        override fun onFailure(request: Request, e: IOException) {
                                            Syslog.logN("同步头像($uid)下载失败: ${e.message}", category = SYSLOG_CATEGORY_RABBITMQ)
                                            user.cacheAvatar = ""
                                            ObjectBox.updateUser(user)
                                            success()
                                        }
                                    })
                            }
                            5 -> {  // 删除用户信息，ack
                                if (uid != 0L)   // 增加健壮性，后台如果传0，删除会抛出异常，因为uid为主键，不能为0
                                    ObjectBox.removeUser(uid)
                                success()
                            }
                            else -> {   // 其他错误，拒收消息
                                Syslog.logN("同步用户信息失败，服务器错误(${result.error}) uid: $uid", category = SYSLOG_CATEGORY_RABBITMQ)
                                fail()
                            }
                        }
                    },
                    onError = {
                        Syslog.logE("同步用户信息失败，内部错误(${it.message}) uid: $uid", category = SYSLOG_CATEGORY_RABBITMQ)
                        fail()
                    }
                )
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                COMMAND_START -> {
                    isRunning = true
                }
                COMMAND_STOP -> {
                    isRunning = false
                    APIService.cancel()
                }
                else -> {
                }
            }
            if (isRunning) {
                setUpConnectionFactory()
                subscribe()
            } else {
                doAsync {
                    channel?.close()
                    connection?.close()
                }
            }
        }
        return START_STICKY
    }

    /**
     * 服务器连接设置
     */
    private fun setUpConnectionFactory() {
        if (MQ_NEED_PEM) {
            factory.useSslProtocol(SSLSocketFactoryProvider.getSSLContext())
            factory.saslConfig = DefaultSaslConfig.EXTERNAL
        } else {
            factory.username = MQ_USERNAME
            factory.password = MQ_PASSWORD
        }
        // 通用设置
        factory.host = MQ_DOMAIN
        factory.port = MQ_PORT
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

                if (ISUS.instance.se) { // 安全增强，直连公网
                    // 队列名称规则：s'学校ID'.cmdb'CMDBID'
                    val queueNameSe = PreferenceManager.instance.let { "s${it.getSchoolId()}.cmdb${it.getCMDB()}" }
                    // 交换机名称规则: schools.s'学校ID'
                    val exchangeNameSe = PreferenceManager.instance.let { "schools.s${it.getSchoolId()}" }
                    // 声明交换机类型
                    channel?.exchangeDeclare(exchangeNameSe, MQ_EXCHANGE_TYPE, true)
                    // 声明队列（持久的、独占的、连接断开后队列会自动删除）
                    val queueSe = channel?.queueDeclare(queueNameSe, true, false, false, null)
                    // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
                    if (PreferenceManager.instance.getDeviceType() == DeviceType.SECURITY) {
                        // IC刷卡考勤使用的设备类型，绑定所有消息
                        channel?.queueBind(queueSe?.queue, exchangeNameSe, MQ_ROUTING_KEY_SE)
                    } else {
                        // 其他设备类型，只绑定控制消息，不绑定用户信息同步消息
                        channel?.queueBind(queueSe?.queue, exchangeNameSe, MQ_ROUTING_KEY_COMET)
                    }
                    // 创建消费者获取RabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery方法，该方法可以获取到消息数据并进行相应处理
                    channel?.basicConsume(queueSe?.queue, false, SeConsumer(channel))
                    // 未抛出异常，认为连接建立成功
                    changeState(QueueState.STATE_STANDBY)
                } else {    // 普通模式，连接内网校园服务器
                    // 队列名称规则：学校ID.设备类型.CMDBID
                    val queueName =
                        PreferenceManager.instance.let { "${it.getSchoolId()}.${it.getDeviceType()}.${it.getCMDB()}" }
                    // 声明交换机类型
                    channel?.exchangeDeclare(MQ_EXCHANGE_NAME, MQ_EXCHANGE_TYPE, true)
                    // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
                    val queue = channel?.queueDeclare(queueName, true, false, false, null)
                    // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
                    channel?.queueBind(
                        queue?.queue,
                        MQ_EXCHANGE_NAME,
                        "$MQ_ROUTING_KEY_PREFIX${PreferenceManager.instance.getCMDB()}"
                    )
                    // 创建消费者获取RabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery方法，该方法可以获取到消息数据并进行相应处理
                    channel?.basicConsume(queue?.queue, true, PoorConsumer(channel))
                    // 未抛出异常，认为连接建立成功
                    changeState(QueueState.STATE_STANDBY)
                }
            } catch (e: Exception) { // 初次创建连接及相关Topology失败时，不会自动修复连接，需要手动处理
                val errorMsg = "RabbitMQ initial connect and topology failed: ${e.message}"
                Log.e(LOG_TAG, errorMsg)
                Syslog.logE(errorMsg, category = SYSLOG_CATEGORY_RABBITMQ)
                Thread.sleep(5000)
                subscribe()
            }
        }
    }

    /**
     * RabbitMQ连接状态改变
     */
    private fun changeState(state: QueueState) {
        queueState = state
        sendBroadcast(Intent(ACTION_QUEUE_STATE_CHANGE))
    }
}