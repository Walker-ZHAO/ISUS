package net.ischool.isus.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.reactivex.Observable
import net.ischool.isus.*
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.model.*
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.network.interceptor.CacheInterceptor
import net.ischool.isus.network.interceptor.URLInterceptor
import net.ischool.isus.network.se.SSLSocketFactoryProvider
import net.ischool.isus.preference.PreferenceManager
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
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
    fun _initDevice(@Field("cmdbid") cmdbid: String, @Field("sid") sid: String): Observable<Response<Result<Metadata>>>

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
     */
    @GET("www/schoolcdn/getAllUids")
    fun _getUids(): Observable<Response<Result<Uids>>>

    object Factory {
        fun createService(client: OkHttpClient): APIService {
            val retrofit = Retrofit.Builder()
                    .baseUrl(END_POINT)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()

            return retrofit.create(APIService::class.java)
        }
    }

    companion object {

        private val instance: APIService by lazy { Factory.createService(client) }

        private val client: OkHttpClient by lazy {
            val cacheFile = File(ISUS.instance.context.externalCacheDir.toString(), "cache")
            val cacheSize = 10 * 1024 * 1024
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
            if (ISUS.instance.se) {
                builder
                    .sslSocketFactory(SSLSocketFactoryProvider.getSSLSocketFactory())
                    .hostnameVerifier { hostname, session -> true }
            }
            builder.build()
        }

        private val delivery: Handler by lazy { Handler(Looper.getMainLooper()) }

        fun getSchoolId() = instance._getSchoolId()

        fun initDevice(cmdbid: String, sid: String): Observable<Response<Result<Metadata>>> {
            return instance._initDevice(cmdbid, sid)
                    .flatMap {
                        val result = checkNotNull(it.body())
                        if (result.errno == RESULT_OK) {
                            Log.i("ISUS", "init: ${result.data}")
                            with(PreferenceManager.instance) {
                                setCMDB(cmdbid)
                                setSchoolId(sid)
                                setToken(result.data.token)
                                setServer(result.data.APIServer)
                                setProtocal(result.data.protocal)
                            }
                            Observable.just(it)
                        } else {
                            Observable.error(Throwable(result.error))
                        }
                    }
        }

        fun getConfig(): Observable<Response<Result<Config>>> {
            return instance._getConfig(PreferenceManager.instance.getToken())
                    .flatMap {
                        val result = checkNotNull(it.body())
                        if (result.errno == RESULT_OK) {
                            Log.i("ISUS", "config: ${result.data}")
                            with(PreferenceManager.instance) {
                                if (getDeviceType() == result.data.type) {
                                    setQR(result.data.QR)
                                    setParameter(result.data.parameter)
                                } else {
                                    /** 设备类型不匹配，可能是CMDB ID配置错误，重置应用 **/
                                    ISUS.instance.context.runOnUiThread { toast(getString(R.string.device_error)) }
                                    Thread.sleep(2 * 1000)
                                    CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null))
                                }
                            }
                            Observable.just(it)
                        } else {
                            Observable.error(Throwable(result.error))
                        }
                    }
        }

        fun pong() = instance._pong(PreferenceManager.instance.getToken())

        fun getUids() = instance._getUids()

        /**
         * 取消所有网络请求
         */
        public fun cancel() {
            client.dispatcher().cancelAll()
        }

        /**
         * 下载网络文件
         *
         * @param url           下载地址
         * @param destFileDir   本地下载目录
         * @param callback      下载结果回调
         */
        fun downloadAsync(url: String, destFileDir: String, callback: StringCallback) {
            val request = Request.Builder()
                    .url(url)
                    .build()
            // 下载文件使用独立的Http Client
            val call = checkNotNull(OkHttpClient.Builder().build()).newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sendFailedStringCallback(request, e, callback)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    var inputStream: InputStream? = null
                    val buf = ByteArray(2048)
                    var len: Int
                    var fos: FileOutputStream? = null
                    try {
                        inputStream = response.body()?.byteStream()
                        val file = File(destFileDir, getFileName(url))
                        fos = FileOutputStream(file)
                        len = inputStream?.read(buf)!!
                        while ( len != -1 ) {
                            fos.write(buf, 0, len)
                            len = inputStream.read(buf)
                        }
                        fos.flush()
                        sendSuccessStringCallback(file.absolutePath, callback)
                    } catch (e: IOException) {
                        sendFailedStringCallback(response.request(), e, callback)
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