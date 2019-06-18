package net.ischool.isus.preference

import android.content.Context
import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.gson.Gson
import com.walker.anke.gson.fromJson

/**
 * Preference 管理器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/12
 */
class PreferenceManager private constructor(context: Context, deviceType: Int) {

    private val preference: SharedPreferences
    private val rxPreference: RxSharedPreferences

    private val _sePemPath: Preference<String>       /** 安全增强模式下的PEM证书路径 **/
    private val _keyPass: Preference<String>         /** 安全增强模式下的私钥口令 **/
    private val _syncCount: Preference<Int>          /** 用户信息全量同步次数 **/
    private val _needUpdate: Preference<Boolean>     /** 是否需要更新CMDB ID **/
    private val _initialized: Preference<Boolean>    /** 设备是否已经初始化 **/
    private val _cmdbId: Preference<String>          /** 硬件的CMDB ID **/
    private val _schoolId: Preference<String>        /** 学校ID **/
    private val _token: Preference<String>           /** 加密后的CMDB ID **/
    private val _serverAddress: Preference<String>   /** 学校相关的服务器地址 **/
    private val _protocal: Preference<String>        /** 服务器支持的协议 **/
    private val _type: Preference<Int>               /** 设备类型 **/
    private val _base64QR: Preference<String>        /** Base64 编码的二维码 **/
    private val _parameter: Preference<String>       /** 额外的参数配置 **/

    init {
        preference = context.getSharedPreferences(CONFIG_PATH, Context.MODE_PRIVATE)
        rxPreference = RxSharedPreferences.create(preference)
        _sePemPath = rxPreference.getString(KEY_SE_PEM_PATH)
        _keyPass = rxPreference.getString(KEY_KEY_PASS)
        _syncCount = rxPreference.getInteger(KEY_SYNC_COUNT, 0)
        _needUpdate = rxPreference.getBoolean(KEY_NEED_UPDATE, false)
        _initialized = rxPreference.getBoolean(KEY_INITED, false)
        _cmdbId = rxPreference.getString(KEY_CMDB_ID)
        _schoolId = rxPreference.getString(KEY_SCHOOL_ID)
        _token = rxPreference.getString(KEY_TOKEN)
        _serverAddress = rxPreference.getString(KEY_SERVER_ADDRESS)
        _protocal = rxPreference.getString(KEY_PROTOCAL)
        _type = rxPreference.getInteger(KEY_TYPE)
        _base64QR = rxPreference.getString(KEY_QR)
        _parameter = rxPreference.getString(KEY_PARAMETER)
        setDeviceType(deviceType)
    }

    override fun toString(): String {
        return "{CMDB ID : ${getCMDB()}, School ID : ${getSchoolId()}, Token : ${getToken()}, Server Address : ${getServer()}, Protocal : ${getProtocal()}, Type : ${getDeviceType()}, Parameter : ${_parameter.get()}}"
    }

    companion object {

        @Volatile
        lateinit var instance: PreferenceManager
            private set

        @Synchronized
        fun initPreference(context: Context, deviceType: Int): PreferenceManager {
            instance = PreferenceManager(context, deviceType)
            return instance
        }

        private var CONFIG_PATH = "ISUS_CONFIG"

        /** ISUS 内部**/
        private const val  KEY_NEED_UPDATE = "NEED_UPDATE"
        private const val  KEY_SE_PEM_PATH = "SE_PEM_PATH"
        private const val  KEY_KEY_PASS = "KEY_PASS"
        private const val  KEY_SYNC_COUNT = "SYNC_COUNT"
        private const val  KEY_INITED = "INITED"

        /** 初始化时网络获取 **/
        private const val  KEY_CMDB_ID = "CMDB_ID"
        private const val  KEY_SCHOOL_ID = "SCHOOL_ID"
        private const val  KEY_TOKEN = "TOKEN"
        private const val  KEY_SERVER_ADDRESS = "SERVER_ADDRESS"
        private const val  KEY_PROTOCAL = "PROTOCAL"
        private const val  KEY_TYPE = "TYPE"
        private const val  KEY_QR = "QR"
        private const val  KEY_PARAMETER = "PARAMETER"
    }

    fun getSePemPath() = _sePemPath.get()
    fun getKeyPass() = _keyPass.get()
    fun getSyncCount() = _syncCount.get()
    fun getInitialized() = if (_needUpdate.get()) false else _initialized.get()
    fun getCMDB() = _cmdbId.get()
    fun getSchoolId() = _schoolId.get()
    fun getToken() = _token.get()
    fun getServer() = _serverAddress.get()
    fun getProtocal() = _protocal.get()
    fun getDeviceType() = _type.get()
    fun getQR() = _base64QR.get()
    fun getParameter() = Gson().fromJson<Map<String, String>>(_parameter.get())?:HashMap()
    fun getURL() = "${_protocal.get()}://${_serverAddress.get()}/"

    fun setSePemPath(path: String) = _sePemPath.set(path)
    fun setKeyPass(pass: String) = _keyPass.set(pass)
    fun setSyncCount(count: Int) = _syncCount.set(count)
    fun setNeedUpdate(need: Boolean) = _needUpdate.set(need)
    fun setInitialized(init: Boolean) {
        _initialized.set(init)
        if (init) _needUpdate.set(false)
    }
    fun setCMDB(cmdb: String) = _cmdbId.set(cmdb)
    fun setSchoolId(id: String) = _schoolId.set(id)
    fun setToken(t: String) = _token.set(t)
    fun setServer(server: String) = _serverAddress.set(server)
    fun setProtocal(pro: String) = _protocal.set(pro)
    private fun setDeviceType(deviceType: Int) = _type.set(deviceType)
    fun setQR(qr: String) = _base64QR.set(qr)
    fun setParameter(param: Map<String, String>) = _parameter.set(Gson().toJson(param))

    /**
     * 语音网关
     */
    fun getEXVoIPGateway() = getParameter()["VoIPGW"]
    /**
     * 语音网关转发设备
     */
    fun getEXVoIPSTUN() = getParameter()["VoIPSTUN"]
    /**
     * 设备内网IP
     */
    fun getEXSelfLanIP() = getParameter()["selfLanIP"]
    /**
     * 设备公网IP
     */
    fun getEXSelfWanIP() = getParameter()["selfWanIP"]
    /**
     * 用于管理员登录设备的密码
     */
    fun getEXSelfMKey() = getParameter()["selfMKey"]
    /**
     * 用于API签名的密钥
     */
    fun getEXSelfSKey() = getParameter()["selfSKey"]
    /**
     * 远程日志服务器地址（域名或IP）
     */
    fun getSyslog() = getParameter()["syslog"]
    /**
     * 学校名称
     */
    fun getSchoolName() = getParameter()["schoolName"]
    /**
     * 学校Logo
     */
    fun getSchoolLogo() = getParameter()["schoolLogo"]
    /**
     * 班级名称
     */
    fun getClassName() = getParameter()["className"]
    /**
     * 班级ID
     */
    fun getClassId() = getParameter()["classId"]
    /**
     * 设备名称（CMDB代号）
     */
    fun getDeviceName() = getParameter()["deviceName"]
    /**
     * 显示模式
     *
     * @see net.ischool.isus.DisplayModel
     */
    fun getDisplayModel() = getParameter()["displayModel"]
    /**
     * 设备所在考勤区域
     */
    fun getAreaId() = getParameter()["areaId"]?.toInt()
    /**
     * 设备所在考勤检查点
     */
    fun getCheckpointId() = getParameter()["checkpointId"]?.toInt()
    /**
     * 设备所在考勤通道
     */
    fun getTunnelId() = getParameter()["tunnelId"]?.toInt()
    /**
     * 考勤模式
     *
     * @see net.ischool.isus.AttendModel
     */
    fun getAttendModel() = getParameter()["attendModel"]?.toInt()
    /**
     * 支持的外设
     *
     * @see net.ischool.isus.PeripheralFlag
     */
    fun getPeripherals() = getParameter()["peripherals"]?.toInt()
}