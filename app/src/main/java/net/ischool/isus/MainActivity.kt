package net.ischool.isus

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.support.v4.content.FileProvider
import android.util.Log
import com.jakewharton.rxbinding2.view.RxView
import com.rabbitmq.client.*
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.activity.InitActivity
import net.ischool.isus.network.APIService
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import java.io.*
import java.util.concurrent.TimeUnit

/**
 * 测试页
 */
class MainActivity : RxAppCompatActivity() {

    var mSpeech: TextToSpeech? = null

    val factory by lazy { ConnectionFactory() }

    var connection: Connection? = null
    var channel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ISUS.init(this, DeviceType.SECURITY)

        init.setOnClickListener { startActivity<InitActivity>() }

        config.setOnClickListener { startActivity<ConfigActivity>() }

        RxView.clicks(ping)
                .debounce(500, TimeUnit.MICROSECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .observeOn(Schedulers.io())
                .flatMap { APIService.pong().bindUntilEvent(this, ActivityEvent.DESTROY) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { Log.i("Walker", "it") },
                        onComplete = {Log.i("Walker", "onComplete")},
                        onError = { Log.e("Walker", "$it") }
                )

        RxView.clicks(reset)
                .subscribeBy {
//                    APIService.downloadAsync("http://download.i-school.net/apk/ischool_teacher_8.8.0.apk", "/sdcard", object : StringCallback {
//                        override fun onResponse(string: String) {
//                            Log.i("Walker", string)
//                        }
//
//                        override fun onFailure(request: Request, e: IOException) {
//                            Log.i("Walker", e.toString())
//                        }
//                    })
//                    val strs = Shell.SU.run("0 echo -BOC- id")
//                    Log.i("Walker", "$strs")
//                    if (Shell.SU.available())
//                        Shell.SU.run("pm install -r /sdcard/app-debug.apk")
//                    val p = execRuntimeProcess("su 0 reboot")
//                    val cn = ComponentName("com.studio.baoxu.gaofa", "com.studio.baoxu.gaofa.activity.LoginActivity");
//                    val intent = Intent()
//                    intent.component = cn
//                    startActivity(intent)

//                    reboot(null)

                }

        btn_start.setOnClickListener {
            ISUS.instance.startService()
//            Observable.just(null)
//                    .subscribe { "$it" }
//            val p = execRuntimeProcess("sunew /system/xbin/system_reboot.sh")
//            btn_start.snack("Hello Kotlin")
        }

        btn_stop.setOnClickListener{
//            Syslog.logI("Hello syslog")
            ISUS.instance.stopService()
//            btn_stop.snack("Hello Android") {
//                action("OK") { toast("Kotlin power!") }
//            }
        }

        mq_test.setOnClickListener {
            testMQ()
        }

        registerReceiver(register, IntentFilter(ACTION_COMMAND))
//        Syslog.logI("Hello syslog")

//        disableBar()

    }

    fun testInit(): Response {
        val url = "https://www.i-school.net/eqptapi/init"
        val formBody = FormBody.Builder()
                .add("cmdbid", "14")
                .add("sid", "1110599")
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
                .add("token", "649d88749cac57923a5f2fe3bd04d2665a5d5a58c1fab461290b02e2f64a2e7e835f930ca72763e34c883cf4f3d4a2db")
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

    fun installAPP() {
        val file = File("${Environment.getExternalStorageDirectory().path}/app-debug.apk")
        if (!file.exists())
            return
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file), "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        startActivity(intent)
    }

    fun execRuntimeProcess(cmd: String): Process? {
        var map = mapOf("1" to 1, "2" to 2)
        map.getOrElse("1") { 0 }
        map += "1" to 90
        var p: Process? = null
        try {
            val process = Runtime.getRuntime().exec(cmd);
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

            val reader1 = BufferedReader(InputStreamReader(in1));
            var line1 = reader1.readLine();
            while ( line1 != null) {
                Log.i("Walker", "返回结果=" + line1);
                line1 = reader1.readLine()
            }
            in1.close();
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return p;
    }

    val register = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val intent = checkNotNull(p1)
            if (intent.action == ACTION_COMMAND) {
                val cmd = intent.getStringExtra(EXTRA_CMD)
                val version = intent.getLongExtra(EXTRA_VERSION, 0L)
                val cmdbid = intent.getStringExtra(EXTRA_CMDB_ID)
                val args = intent.getBundleExtra(EXTRA_ARGS)
                val type = args.getString("type")
                val content = args.getString("content")

                Log.i("Walker", cmd)
                Log.i("Walker", "$version")
                Log.i("Walker", cmdbid)
                Log.i("Walker", type)
                Log.i("Walker", content)
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
            Log.i("Walker", "name: ${mechanism.name}")
            // 创建新的连接
            connection = factory.newConnection()
            // 创建通道
            channel = connection?.createChannel()
            // 处理完一个消息，再接收下一个消息
            channel?.basicQos(1)

            // 随机命名一个队列名称
            val queueName = "tester";
            // 声明交换机类型
            channel?.exchangeDeclare("equipment", "topic", true)
            // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
            val queue = channel?.queueDeclare(queueName, true, false, false, null)
            // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
            channel?.queueBind(queue?.queue, "equipment", "equipment.#")
            // 创建消费者获取rabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery（）方法，该方法可以获取到消息数据并进行相应处理
            val consumer = object : DefaultConsumer(channel) {
                override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
                    super.handleDelivery(consumerTag, envelope, properties, body)
                    val msg = body?.toString(charset("UTF-8"))
                    Log.e("Walker", "RabbitMQ message: $msg")
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
}
