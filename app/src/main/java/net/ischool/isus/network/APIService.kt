package net.ischool.isus.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.walker.anke.framework.longToast
import com.walker.anke.framework.runOnUiThread
import io.reactivex.rxjava3.core.Observable
import net.ischool.isus.*
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.model.*
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.network.interceptor.CacheInterceptor
import net.ischool.isus.network.interceptor.URLInterceptor
import net.ischool.isus.network.se.NullX509TrustManager
import net.ischool.isus.network.se.SSLSocketFactoryProvider
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.StatusPostService
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 网络接口
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/23
 */
interface APIService {

    /**
     * 获取学校信息
     *
     * @return  服务器返回该学校ID及名称等相关信息
     */
    @GET("sgrid/cmdb/getSchool")
    fun _getSchoolId(): Observable<Response<Result<SchoolInfo>>>

    /**
     * 登录
     *
     * @param cmdbid    设备唯一标示符
     * @param sid       学校ID
     *
     * @return          服务器返回的数据，包括token，服务器地址，服务器协议
     *
     */
    @FormUrlEncoded
    @POST("eqptapi/init")
    fun _initDevice(@Field("cmdbid") cmdbid: String, @Field("sid") sid: String, @Field("se") se: Int, @Field("iam") iam: String): Observable<Response<Result<Metadata>>>

    /**
     * 获取配置信息
     *
     * @return  服务器返回的配置信息，包括设备类型，comet地址，QR码及设备相关参数列表
     *
     */
    @FormUrlEncoded
    @POST("eqptapi/getConfig")
    fun _getConfig(@Field("token") token: String): Observable<Response<Result<Config>>>

    /**
     * 响应ping指令
     */
    @FormUrlEncoded
    @POST("eqptapi/pong")
    fun _pong(@Field("token") token: String): Observable<ResponseBody>

    /**
     * 获取用户列表
     * http://192.168.0.20/campus/API/学校CDN服务器接口文档.md
     */
    @GET("schoolcdn/getAllUids")
    fun _getUids(): Observable<Response<Result<Uids>>>

    /**
     * 获取用户信息（简版）
     * http://192.168.0.20/campus/API/学校CDN服务器接口文档.md
     */
    @FormUrlEncoded
    @POST("schoolcdn/getUserInfoSimple")
    fun _getUserInfo(@Field("uid") uid: Long): Observable<Response<Result<User>>>

    /**
     * 状态上报（每分钟一次）
     * http://192.168.0.20/campus/API/校内监控.md
     * @param sid           学校id
     * @param deviceTypeId  监控项id
     * @param service_id    设备cmdbid,后续可能会变更为其它值
     * @param name          监控项注释、说明
     * @param labelId       是否正常，当前固定为1
     * @param info          当前状态描述信息
     * @param label         当前状态
     * @param ts            每分钟整的时间戳(单位秒)
     * @param type          数据类型，当前固定为3
     * @param cmdb_id       扩展信息，设备cmdbid
     * @param cdn_ip        设备上解析到CDN服务器的IP地址
     * @param wakeState     是否处于唤醒状态：1：唤醒；2：待机
     */
    @FormUrlEncoded
    @POST("sgrid/psi/hungribles")
    fun _postStatus(@Field("sid") sid: String, @Field("element_id") deviceTypeId: String, @Field("service_id") service_id: String, @Field("element_name_tail") name: String, @Field("label_character") labelId: Int, @Field("info") info: String, @Field("label") label: String, @Field("uptime") ts: Long, @Field("type") type: Int, @Field("ext_cmdbid") cmdb_id: String, @Field("client_cdn_ip") cdn_ip: String, @Field("waken_status") wakeState: Int): Observable<ResponseBody>

    /**
     * 获取网络状态信息
     */
    @GET("ischoolsrv/ping?v=2")
    fun _getNetworkStatus(): Observable<Response<Result<NetworkStatus>>>

    /**
     * 获取边缘云服务器信息
     */
    @GET("cdnwebui/Device/SystemPreset")
    fun _getCdnInfo(): Observable<Response<Result<CdnServerInfo>>>

    /**
     * 获取报警信息
     *
     * TODO 具体接口地址跟数据字段，需要跟后台确认
     *
     * @param cmdbid    设备唯一标示符
     * @param ip        设备IP
     *
     * @return          服务器返回的数据，包括类型，报警时间，报警原因，快速诊断，联系人信息
     *
     */
    @FormUrlEncoded
    @POST("/sgrid/psi/consoleGetAlarmsList")
    fun _getAlarm(@Field("cmdbid") cmdbid: String, @Field("ip") ip: String): Observable<Response<Result<List<AlarmInfo>>>>

    object Factory {
        fun createService(client: OkHttpClient): APIService {
            val retrofit = Retrofit.Builder()
                .baseUrl(END_POINT)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()

            return retrofit.create(APIService::class.java)
        }
    }

    companion object {

        private val instance: APIService by lazy { Factory.createService(client) }

        private val client: OkHttpClient by lazy {
            val cacheFile = File(ISUS.instance.context.externalCacheDir?.toString()?:"", "cache")
            val cacheSize = 5 * 10 * 1024 * 1024
            val cache = Cache(cacheFile, cacheSize.toLong())

            val builder = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .cache(cache)
                    .addNetworkInterceptor(CacheInterceptor())
                    .addInterceptor(URLInterceptor())
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    })
            // 如果处于安全增强模式，并且服务端提供了证书，则需要配置双向认证
            if (ISUS.instance.se && PreferenceManager.instance.getSePemPath().isNotEmpty()) {
                builder
                    .sslSocketFactory(SSLSocketFactoryProvider.getSSLSocketFactory(), NullX509TrustManager())
                    .hostnameVerifier { hostname, session -> true }
            }
            builder.build()
        }

        private val downloadClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }

        private val delivery: Handler by lazy { Handler(Looper.getMainLooper()) }

        fun getSchoolId(): Observable<Response<Result<SchoolInfo>>> {
            return instance._getSchoolId().flatMap {
                val result = checkNotNull(it.body())
                if (result.errno == RESULT_OK) {
                    PreferenceManager.instance.setCdnUrl(result.data.httpsApi)
                    Observable.just(it)
                } else {
                    Observable.error(Throwable("getSchool: ${result.errno} : ${result.error}"))
                }
            }
        }

        fun initDevice(cmdbid: String, sid: String): Observable<Response<Result<Metadata>>> {
            val se = if (ISUS.instance.se) 1 else 0
            return instance._initDevice(cmdbid, sid, se, ISUS.instance.iam)
                    .flatMap {
                        val result = checkNotNull(it.body())
                        if (result.errno == RESULT_OK) {
                            Log.i(LOG_TAG, "init: ${result.data}")
                            with(PreferenceManager.instance) {
                                setCMDB(cmdbid)
                                setSchoolId(sid)
                                setToken(result.data.token)
                                setServer(result.data.APIServer)
                                setProtocal(result.data.protocal)
                                val platformApi = result.data.platformApi
                                val platformMq = result.data.platformMq
                                val platformAtt = result.data.platformAtt
                                val platformStatic = result.data.platformStatic
                                val iamPackage = result.data.iamPackage
                                // 如果服务端未传递自定义配置，则使用默认配置
                                if (platformApi.isEmpty()) {
                                    if (ISUS.instance.se)
                                        setPlatformApi("$DEFAULT_SE_API_HOST$SE_API_PATH")
                                    else
                                        setPlatformApi("$DEFAULT_API_HOST$API_PATH")
                                } else {
                                    if (ISUS.instance.se)
                                        setPlatformApi("$platformApi$SE_API_PATH")
                                    else
                                        setPlatformApi("$platformApi$API_PATH")
                                }
                                if (platformMq.isEmpty()) {
                                    if (ISUS.instance.se)
                                        setPlatformMq("amqps://$MQ_DEFAULT_SE_DOMAIN:$MQ_DEFAULT_SE_POST/")
                                    else
                                        setPlatformMq("amqp://$MQ_DEFAULT_USERNAME:$MQ_DEFAULT_PASSWORD@$MQ_DEFAULT_DOMAIN:$MQ_DEFAULT_POST/")
                                } else {
                                    setPlatformMq(platformMq)
                                }
                                if (platformAtt.isEmpty()) {
                                    setPlatformAtt(DEFAULT_ATT_HOST)
                                } else {
                                    setPlatformAtt("$platformAtt/")
                                }
                                if (platformStatic.isEmpty()) {
                                    setPlatformStatic(DEFAULT_STATIC_HOST)
                                } else {
                                    setPlatformStatic("$platformStatic/")
                                }
                                setIamPackage(iamPackage)
                            }
                            Observable.just(it)
                        } else {
                            Observable.error(Throwable("init(cmdbid: $cmdbid, sid: $sid): ${result.errno} : ${result.error}"))
                        }
                    }
        }

        fun getConfig(): Observable<Response<Result<Config>>> {
            return instance._getConfig(PreferenceManager.instance.getToken())
                    .flatMap {
                        val result = checkNotNull(it.body())
                        if (result.errno == RESULT_OK) {
                            Log.i(LOG_TAG, "config: ${result.data}")
                            with(PreferenceManager.instance) {
                                when (result.data.parameter["internalICReaderType"]?.toInt()) {
                                    CReaderType.AUTO -> {
                                        /** 读卡器配置错误，需要明确指明读卡器类型，重置应用 **/
                                        ISUS.instance.context.runOnUiThread { longToast(getString(R.string.ic_reader_error)) }
                                        Thread.sleep(2 * 1000)
                                        CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null))
                                    }
                                    else -> {
                                        if (getDeviceType() == result.data.type) {
                                            setQR(result.data.QR)
                                            setParameter(result.data.parameter)
                                            setInitialized(true)
                                            // 初始化成功，非SE模式下，启动状态上报服务
                                            ISUS.instance.apply {
                                                if (!se)
                                                    StatusPostService.startService(context)
                                            }
                                        } else {
                                            /** 设备类型不匹配，可能是CMDB ID配置错误，重置应用 **/
                                            ISUS.instance.context.runOnUiThread { longToast(getString(R.string.device_error)) }
                                            Thread.sleep(2 * 1000)
                                            CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null))
                                        }
                                    }
                                }
                            }
                            Observable.just(it)
                        } else {
                            Observable.error(Throwable("config: ${result.errno} : ${result.error}"))
                        }
                    }
        }

        fun pong() = instance._pong(PreferenceManager.instance.getToken())

        fun getUids() = instance._getUids()

        fun getUserInfo(uid: Long) = instance._getUserInfo(uid)

        /**
         * 状态上报（每分钟一次）
         *
         * @param info  当前状态描述信息
         * @param label 当前状态
         * @param inSleep 是否处于休眠状态
         */
        suspend fun postStatus(info: String = "Normal", label: String = "Normal", inSleep: Boolean): Observable<ResponseBody> {
            val ts = System.currentTimeMillis() / 1000
            val extra = ts % 60
            val ip = parseHostGetIPAddress()
            return instance._postStatus(PreferenceManager.instance.getSchoolId(),
                DeviceType.getDeviceTypeId(PreferenceManager.instance.getDeviceType()),
                PreferenceManager.instance.getCMDB(),
                "CmdbId=${PreferenceManager.instance.getCMDB()}",
                1,
                info,
                label,
                ts - extra,
                3,
                PreferenceManager.instance.getCMDB(),
                ip,
                if (inSleep) 2 else 1)
        }

        /**
         * 获取网络状态，用于判断边缘云连通性
         */
        fun getNetworkStatus() = instance._getNetworkStatus()

        /**
         * 获取边缘云信息，用于判断边缘云是否满足最低要求版本号
         */
        fun getCdnInfo() = instance._getCdnInfo()

        /**
         * 获取边缘云报警信息
         */
        fun getAlarmInfo(): Observable<Response<Result<List<AlarmInfo>>>> {
            return instance._getAlarm(PreferenceManager.instance.getCMDB(), getIpAddress(ISUS.instance.context))
                .flatMap {
                    val result = checkNotNull(it.body())
                    if (result.errno == RESULT_OK) {
                        result.data.forEach { info ->
                            // 保存自检类型的联系人信息
                            when (info.type) {
                                ALARM_TYPE_DISCONNECT -> PreferenceManager.instance.setContactDisconnect(info.contact)
                                ALARM_TYPE_UPGRADE -> PreferenceManager.instance.setContactUpgrade(info.contact)
                            }
                        }
                        Observable.just(it)
                    }
                    else {
                        Observable.error(Throwable("Alarm: ${result.errno} : ${result.error}"))
                    }
                }
        }

        /**
         * 取消所有网络请求
         */
        fun cancel() {
            client.dispatcher.cancelAll()
            downloadClient.dispatcher.cancelAll()
        }

        /**
         * 下载网络文件
         *
         * @param url           下载地址
         * @param destFileDir   本地下载目录
         * @param fileName      下载的文件名，若不传，自动使用url地址命名
         * @param callback      下载结果回调
         */
        fun downloadAsync(url: String, destFileDir: String, fileName: String = "", callback: StringCallback) {
            val request = Request.Builder()
                    .url(url)
                    .build()
            // 下载文件使用独立的Http Client
            val call = downloadClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sendFailedStringCallback(request, e, callback)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    // 非200代表下载失败
                    val code = response.code
                    if (code != 200) {
                        sendFailedStringCallback(request, IOException("Http Code : $code"), callback)
                        return
                    }
                    var inputStream: InputStream? = null
                    val buf = ByteArray(2048)
                    var len: Int
                    var fos: FileOutputStream? = null
                    try {
                        inputStream = response.body?.byteStream()
                        val file = File(destFileDir, fileName.ifEmpty { getFileName(url) })
                        file.parentFile?.mkdir()
                        file.createNewFile()
                        fos = FileOutputStream(file)
                        len = inputStream?.read(buf)!!
                        while ( len != -1 ) {
                            fos.write(buf, 0, len)
                            len = inputStream.read(buf)
                        }
                        fos.flush()
                        sendSuccessStringCallback(file.absolutePath, callback)
                    } catch (e: IOException) {
                        sendFailedStringCallback(response.request, e, callback)
                    } finally {
                        try {
                            inputStream?.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        try {
                            fos?.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        }

        private fun getFileName(path: String): String {
            val separatorIndex = path.lastIndexOf("/")
            return if (separatorIndex < 0) path else path.substring(separatorIndex + 1, path.length)
        }

        private fun sendFailedStringCallback(request: Request, e: IOException, callback: StringCallback) {
            delivery.post {
                callback.onFailure(request, e)
            }
        }

        private fun sendSuccessStringCallback(string: String, callback: StringCallback) {
            delivery.post {
                callback.onResponse(string)
            }
        }
    }
}