package net.ischool.isus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.content.FileProvider
import android.util.Log
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import com.walker.anke.framework.disableBar
import com.walker.anke.framework.disableNatigation
import com.walker.anke.framework.disableNotificationBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.*
import java.util.concurrent.TimeUnit

/**
 * 测试页
 */
class MainActivity : RxAppCompatActivity() {

    var mSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ISUS.init(this, DeviceType.VISION_PHONE)

        RxView.clicks(init)
                .debounce(500, TimeUnit.MILLISECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .observeOn(Schedulers.io())
                .flatMap { APIService.initDevice("15", "1110599").bindUntilEvent(this, ActivityEvent.DESTROY) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { Log.i("Walker", "${it.body()}") },
                        onComplete = {Log.i("Walker", "onComplete")},
                        onError = { Log.e("Walker", "$it") }
                )

        RxView.clicks(config)
                .debounce(500, TimeUnit.MILLISECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .observeOn(Schedulers.io())
                .flatMap { APIService.getConfig().bindUntilEvent(this, ActivityEvent.DESTROY) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { Log.i("Walker", "${it.body()}") },
                        onComplete = {Log.i("Walker", "onComplete")},
                        onError = { Log.e("Walker", "$it") }
                )

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

        RxView.clicks(command)
                .debounce(500, TimeUnit.MILLISECONDS)
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .observeOn(Schedulers.io())
                .flatMap { APIService.getCommand().bindUntilEvent(this, ActivityEvent.DESTROY) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { Log.i("Walker", "$it") },
                        onComplete = {Log.i("Walker", "onComplete")},
                        onError = { Log.e("Walker", "$it") }
                )

        RxView.clicks(reset)
                .subscribeBy {
                    APIService.downloadAsync("http://download.i-school.net/apk/ischool_teacher_8.8.0.apk", "/sdcard", object : StringCallback {
                        override fun onResponse(string: String) {
                            Log.i("Walker", string)
                        }

                        override fun onFailure(request: Request, e: IOException) {
                            Log.i("Walker", e.toString())
                        }
                    })
//                    val strs = Shell.SU.run("0 echo -BOC- id")
//                    Log.i("Walker", "$strs")
//                    if (Shell.SU.available())
//                        Shell.SU.run("pm install -r /sdcard/app-debug.apk")
//                    val p = execRuntimeProcess("su 0 echo -BOC- id")
//                    val cn = ComponentName("com.studio.baoxu.gaofa", "com.studio.baoxu.gaofa.activity.LoginActivity");
//                    val intent = Intent()
//                    intent.component = cn
//                    startActivity(intent)
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

        registerReceiver(register, IntentFilter(ACTION_COMMAND))
//        Syslog.logI("Hello syslog")

        disableBar()

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
        val file = File("/sdcard/app-debug.apk")
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
}
