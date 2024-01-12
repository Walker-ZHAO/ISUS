package com.example.testapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.lamy.system.Magicbox
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.hikvision.dmb.system.InfoSystemApi
import com.hikvision.dmb.time.InfoTimeApi
import com.jakewharton.rxbinding4.view.clicks
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.trello.rxlifecycle4.android.ActivityEvent
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import com.trello.rxlifecycle4.kotlin.bindUntilEvent
import com.walker.anke.framework.doAsync
import com.walker.anke.framework.longToast
import com.walker.anke.framework.startActivity
import com.walker.anke.framework.toast
import com.ys.rkapi.MyManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.ACTION_COMMAND
import net.ischool.isus.ACTION_QUEUE_STATE_CHANGE
import net.ischool.isus.DeviceType
import net.ischool.isus.EXTRA_ARGS
import net.ischool.isus.EXTRA_CMD
import net.ischool.isus.EXTRA_CMDB_ID
import net.ischool.isus.EXTRA_VERSION
import net.ischool.isus.ISUS
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.activity.InitActivity
import net.ischool.isus.broadcast.UserSyncReceiver
import net.ischool.isus.databinding.ActivityMainBinding
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.isDh32Device
import net.ischool.isus.isHongHeDevice
import net.ischool.isus.isTouchWoDevice
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.ISUSService
import net.ischool.isus.service.UDPService
import net.ischool.isus.service.alarmIntervalDetect
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import startest.ys.com.poweronoff.PowerOnOffManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 测试页
 */
class MainActivity : RxAppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var mSpeech: TextToSpeech? = null

    private val factory by lazy { ConnectionFactory() }

    private var connection: Connection? = null
    var channel: Channel? = null

    @SuppressLint("CheckResult")
    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ISUS.init(this, DeviceType.BADGE, "202401p12", securityEnhance = false)
//        ISUS.init(this, DeviceType.VISION_PHONE, true, "huixiaoan")

        binding.init.setOnClickListener { startActivity<InitActivity>() }

        binding.config.setOnClickListener { startActivity<ConfigActivity>() }

        binding.ping.clicks()
            .debounce(500, TimeUnit.MICROSECONDS)
            .bindUntilEvent(this, ActivityEvent.DESTROY)
            .observeOn(Schedulers.io())
            .flatMap { APIService.pong().bindUntilEvent(this, ActivityEvent.DESTROY) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    Log.i(LOG_TAG, "$it")
                    Log.i(LOG_TAG, "area:${PreferenceManager.instance.getAreaId()}")
                    Log.i(
                        LOG_TAG,
                        "checkpoint: ${PreferenceManager.instance.getCheckpointId()}"
                    )
                    Log.i(LOG_TAG, "tunnel: ${PreferenceManager.instance.getTunnelId()}")
                    Log.i(
                        LOG_TAG,
                        "attend: ${PreferenceManager.instance.getAttendModel()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "peripherals: ${PreferenceManager.instance.getPeripherals()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "homepage: ${PreferenceManager.instance.getHomePage()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "refresh interval: ${PreferenceManager.instance.getRefreshInterval()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "control: ${PreferenceManager.instance.canControlWebview()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "back: ${PreferenceManager.instance.canBackWebview()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "forward: ${PreferenceManager.instance.canForwardWebview()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "refresh: ${PreferenceManager.instance.canRefreshWebview()}"
                    )
                    Log.i(
                        LOG_TAG,
                        "use x5: ${PreferenceManager.instance.useX5Core()}"
                    )
                },
                onComplete = { Log.i(LOG_TAG, "onComplete") },
                onError = { Log.e(LOG_TAG, "$it") }
            )

        binding.reset.clicks()
            .debounce(500, TimeUnit.MICROSECONDS)
            .bindUntilEvent(this, ActivityEvent.DESTROY)
            .observeOn(Schedulers.io())
            .subscribeBy {
//                    APIService.downloadAsync("http://download.i-school.net/apk/ischool_teacher_8.8.0.apk", "/sdcard", object : StringCallback {
//                        override fun onResponse(string: String) {
//                            Log.i(LOG_TAG, string)
//                        }
//
//                        override fun onFailure(request: Request, e: IOException) {
//                            Log.i(LOG_TAG, e.toString())
//                        }
//                    })
//                    val strs = Shell.SU.run("0 echo -BOC- id")
//                    Log.i(LOG_TAG, "$strs")
//                    if (Shell.SU.available())
//                        Shell.SU.run("pm install -r /sdcard/app-debug.apk")
//                    val p = execRuntimeProcess("su 0 reboot")
//                    val cn = ComponentName("com.studio.baoxu.gaofa", "com.studio.baoxu.gaofa.activity.LoginActivity");
//                    val intent = Intent()
//                    intent.component = cn
//                    startActivity(intent)

//                    reboot(null)
//                    SSLSocketFactoryProvider.getSSLSocketFactory(assets.open("test.pem"))

                APIService.getUids().subscribeBy(
                    onError = { e -> Log.e(LOG_TAG, "getUids error: $e") },
                    onNext = { result ->
                        Log.i(
                            LOG_TAG,
                            "getUids success: ${result.body()?.data?.uids?.size}"
                        )
                    }
                )
            }

        binding.btnStart.setOnClickListener {
            ISUS.instance.startService()
//            Observable.just(null)
//                    .subscribe { "$it" }
//            val p = execRuntimeProcess("sunew /system/xbin/system_reboot.sh")
//            btn_start.snack("Hello Kotlin")
            alarmIntervalDetect().subscribeBy {
                Log.i("Walker", "alarm: $it")
            }
        }

        binding.btnStop.setOnClickListener{
//            Syslog.logI("Hello syslog")
            ISUS.instance.stopService()
//            btn_stop.snack("Hello Android") {
//                action("OK") { toast("Kotlin power!") }
//            }
        }

        binding.mqTest.setOnClickListener {
            testMQ()
        }

        binding.sync.setOnClickListener {
            sendBroadcast(Intent("net.ischool.isus.sync"))
        }

        binding.syncCount.setOnClickListener {
            toast("sync count: ${PreferenceManager.instance.getSyncCount()}")
        }

        binding.findUser.setOnClickListener {
            ObjectBox.findUser("D5D69AF4A1FD")?.let {
                longToast("$it")
                Log.i(LOG_TAG, "$it")
            }
        }

        binding.powerOff.setOnClickListener {
            if (isHikDevice()) {

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val offTime = sdf.parse("2021-01-25 17:04:00")
                val onTime = sdf.parse("2021-01-25 17:06:00")
//                val offTime1 = sdf.parse("2020-07-20 09:34:00")
//                val onTime1 = sdf.parse("2020-07-20 09:36:00")
                InfoTimeApi.clearPlan()
                InfoTimeApi.setTimeSwitch(offTime?.time ?: 0, onTime?.time ?: 0)
//                InfoTimeApi.setTimeSwitch(offTime1.time, onTime1.time)
            } else if (isTouchWoDevice() || isDh32Device()){
                val manager = MyManager.getInstance(this)
                val powerManager = PowerOnOffManager.getInstance(this)
                Log.i(
                    LOG_TAG,
                    "Device info: ${manager.apiVersion}, ${manager.androidModle}, ${manager.androidVersion}, ${manager.firmwareVersion}, ${manager.kernelVersion}, ${manager.cpuType}, "
                )
                powerManager.clearPowerOnOffTime()
                Log.i(LOG_TAG, "Power on mode: ${powerManager.powerOnMode}")
                val powerOff = intArrayOf(2021, 1, 25, 17, 15)
                val powerOn = intArrayOf(2021, 1, 25, 17, 17)
                powerManager.setPowerOnOff(powerOn, powerOff)
//                val powerOff = intArrayOf(11,23)
//                val powerOn = intArrayOf(11,26)
//                val powerOff1 = intArrayOf(11,28)
//                val powerOn1 = intArrayOf(11,30)
//                val weekly = intArrayOf(1,1,1,1,1,0,0)
//                powerManager.setPowerOnOffWithWeekly(powerOn, powerOff, weekly)
//                powerManager.setPowerOnOffWithWeekly(powerOn1, powerOff1, weekly)
                Log.i(LOG_TAG, "Power on mode: ${powerManager.powerOnMode}")
                Log.i(LOG_TAG, "Power on time: ${powerManager.powerOnTime}")
                Log.i(LOG_TAG, "Power off time: ${powerManager.powerOffTime}")
            } else if (isHongHeDevice()) {
                Log.i(LOG_TAG, "honghe device")
                val timeonArray = intArrayOf(2022,9,29,13,59)
                val timeoffArray = intArrayOf(2022,9,29,13,57)
                val intent = Intent("android.intent.action.setpoweronoff").apply{
                    putExtra("timeon", timeonArray)
                    putExtra("timeoff", timeoffArray)
                    putExtra("enable", true)
                }
                sendBroadcast(intent)
            }else {
//                val intent1 = Intent("com.hra.setAutoShutdown").apply {
//                    putExtra("key", false)
//                }
//                val intent2 = Intent("com.hra.setAutoBoot").apply {
//                    putExtra("key", false)
//                }
//                val intent3 = Intent("com.hra.setShutdownDate").apply {
//                    putExtra("key", 1613717874000)
//                }
//                val intent4 = Intent("com.hra.setBootDate").apply {
//                    putExtra("key", 1613717994000)
//                }
//                sendBroadcast(intent1)
//                sendBroadcast(intent2)
//                sendBroadcast(intent3)
//                sendBroadcast(intent4)
                val result = Magicbox.enableTimmingPoweron("2021-04-16 10:24:00")
                Log.i("Walker", "set power on result: $result")
                Magicbox.shutdown(false)
            }
        }

        binding.udpStart.setOnClickListener { UDPService.start() }
        binding.udpStop.setOnClickListener { UDPService.stop() }

//        Syslog.logI("Hello syslog")

//        disableBar()

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(register, IntentFilter(ACTION_COMMAND))
        registerReceiver(syncReceiver, IntentFilter("net.ischool.isus.sync"))
        registerReceiver(stateReceiver, IntentFilter(ACTION_QUEUE_STATE_CHANGE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(register)
        unregisterReceiver(syncReceiver)
        unregisterReceiver(stateReceiver)
    }

    fun testInit(): Response {
        val url = "https://www.i-school.net/eqptapi/init"
        val formBody = FormBody.Builder()
            .add("cmdbid", "153")
            .add("sid", "1117164")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        return OkHttpClient.Builder().build().newCall(request).execute()
    }

    fun testConfig(): Response {
        val url = "https://www.i-school.net/eqptapi/getConfig"
        val formBody = FormBody.Builder()
            .add(
                "token",
                "649d88749cac57923a5f2fe3bd04d2665a5d5a58c1fab461290b02e2f64a2e7e835f930ca72763e34c883cf4f3d4a2db"
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        return OkHttpClient.Builder().build().newCall(request).execute()
    }

    fun testCommand(): Response {
        val url = "https://comet0.i-school.net:1931/sub-legacy?channel=51928d2eb388d8535dc2a41f4b8b05df"
        val request = Request.Builder()
            .url(url)
            .build()

        return OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).build().newCall(request).execute()
    }

//    fun installAPP() {
//        val file = File("${Environment.getExternalStorageDirectory().path}/app-debug.apk")
//        if (!file.exists())
//            return
//        val intent = Intent(Intent.ACTION_VIEW)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            intent.setDataAndType(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file), "application/vnd.android.package-archive");
//        } else {
//            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
//        }
//        startActivity(intent)
//    }

    fun execRuntimeProcess(cmd: String): Process? {
        val map = mutableMapOf("1" to 1, "2" to 2)
        map.getOrElse("1") { 0 }
        map += "1" to 90
        val p: Process? = null
        try {
            val process = Runtime.getRuntime().exec(cmd)
//            val process = Runtime.getRuntime().exec("su 0", null)
            val in1 = process.inputStream
//            val STDIN = DataOutputStream(process.outputStream)
//            try {
//                STDIN.write(("ls\n").toByteArray(charset("UTF-8")))
//                STDIN.flush()
//                STDIN.write("exit\n".toByteArray(charset("UTF-8")))
//                STDIN.flush()
//            } catch (e: IOException) {
//                if (e.message!!.contains("EPIPE") || e.message!!.contains("Stream closed")) {
            // Method most horrid to catch broken pipe, in which case we
            // do nothing. The command is not a shell, the shell closed
            // STDIN, the script already contained the exit command, etc.
            // these cases we want the output instead of returning null.
//                } else {
            // other issues we don't know how to handle, leads to
            // returning null
//                    throw e
//                }
//            }
            process.waitFor()

            val reader1 = BufferedReader(InputStreamReader(in1))
            var line1 = reader1.readLine()
            while ( line1 != null) {
                Log.i(LOG_TAG, "返回结果=$line1")
                line1 = reader1.readLine()
            }
            in1.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return p
    }

    private val register = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val intent = checkNotNull(p1)
            if (intent.action == ACTION_COMMAND) {
                val cmd = intent.getStringExtra(EXTRA_CMD) ?: ""
                val version = intent.getLongExtra(EXTRA_VERSION, 0L)
                val cmdbid = intent.getStringExtra(EXTRA_CMDB_ID) ?: ""
                val args = intent.getBundleExtra(EXTRA_ARGS)
                val type = args?.getString("type") ?: ""
                val content = args?.getString("content") ?: ""

                Log.i(LOG_TAG, cmd ?: "")
                Log.i(LOG_TAG, "$version")
                Log.i(LOG_TAG, cmdbid ?: "")
                Log.i(LOG_TAG, type ?: "")
                Log.i(LOG_TAG, content ?: "")
            }
        }
    }

    private val syncReceiver by lazy { UserSyncReceiver() }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_QUEUE_STATE_CHANGE) {
                longToast("RabbitMQ State: ${ISUSService.queueState}")
            }
        }
    }

    private fun testMQ() {
        setUpConnectionFactory()
        doAsync {
            // 需要再次初始化数据的时候就关闭上一个连接
            channel?.close()
            connection?.close()
            val config = factory.saslConfig
            val mechanism = config.getSaslMechanism(arrayOf("PLAIN"))
            Log.i(LOG_TAG, "name: ${mechanism.name}")
            // 创建新的连接
            connection = factory.newConnection()
            // 创建通道
            channel = connection?.createChannel()
            // 处理完一个消息，再接收下一个消息
            channel?.basicQos(1)

            // 随机命名一个队列名称
            val queueName = "tester"
            // 声明交换机类型
            channel?.exchangeDeclare("equipment", "topic", true)
            // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
            val queue = channel?.queueDeclare(queueName, true, false, false, null)
            // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
            channel?.queueBind(queue?.queue, "equipment", "equipment.#")
            // 创建消费者获取rabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery（）方法，该方法可以获取到消息数据并进行相应处理
            val consumer = object : DefaultConsumer(channel) {
                override fun handleDelivery(
                    consumerTag: String?,
                    envelope: Envelope?,
                    properties: AMQP.BasicProperties?,
                    body: ByteArray?
                ) {
                    super.handleDelivery(consumerTag, envelope, properties, body)
                    val msg = body?.toString(charset("UTF-8"))
                    Log.e(LOG_TAG, "RabbitMQ message: $msg")
                }
            }
            channel?.basicConsume(queue?.queue, true, consumer)
        }
    }

    /**
     * 服务器连接设置
     */
    private fun setUpConnectionFactory() {
        /**
         * host: cdn.schools.i-school.net
         * port: 5672
         * user: equipment
         * pass: 1835ac0a6b749651efa42dd4e09e625a
         * vhost: /
         * exchange: equipment

         */
        factory.host = "cdn.schools.i-school.net"
        factory.port = 5672
        factory.username = "equipment"
        factory.password = "1835ac0a6b749651efa42dd4e09e625a"
        factory.virtualHost = "/"
        // 设置连接恢复
        factory.isAutomaticRecoveryEnabled = true
    }

    override fun onDestroy() {
        ISUS.instance.destroy()
        super.onDestroy()
    }

    private fun isHikDevice(): Boolean {
        try {   // 通过使用海康SDK获取主板信息判断是否为海康设备
            InfoSystemApi.getMotherboardType()
            return true
        } catch (_: Exception) { }
        return false
    }
}

const val LOG_TAG = "ISUS"
