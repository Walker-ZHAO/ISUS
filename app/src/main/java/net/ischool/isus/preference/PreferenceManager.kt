package net.ischool.isus.preference

import android.content.Context
import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.gson.Gson
import com.walker.anke.gson.fromJson
import net.ischool.isus.*

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
    private val _platformApi: Preference<String>     /** 部署平台的服务器地址 **/
    private val _platformMq: Preference<String>      /** 部署平台的MQ地址 **/
    private val _platformAtt: Preference<String>     /** 部署平台的附件服务器地址 **/
    private val _platformStatic: Preference<String>  /** 部署平台的Web页地址 **/
    private val _cdnUrl: Preference<String>          /** 边缘云服务器地址 **/
    private val _iamPackage: Preference<String>      /** 部署平台的IAM认证配置 **/
    private val _type: Preference<Int>               /** 设备类型 **/
    private val _base64QR: Preference<String>        /** Base64 编码的二维码 **/
    private val _parameter: Preference<String>       /** 额外的参数配置 **/
    private val _contactDisconnect: Preference<String>  /** 边缘云无法连接时的联系人信息 **/
    private val _contactUpgrade: Preference<String>     /** 边缘云版本过低时的联系人信息 **/
    private val _minCdnVersion: Preference<String>     /** 要求的边缘云最低版本号 **/

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
        _platformApi = rxPreference.getString(KEY_PLATFORM_API)
        _platformMq = rxPreference.getString(KEY_PLATFORM_MQ)
        _platformAtt = rxPreference.getString(KEY_PLATFORM_ATT)
        _platformStatic = rxPreference.getString(KEY_PLATFORM_STATIC)
        _cdnUrl = rxPreference.getString(KEY_CDN_URL)
        _iamPackage = rxPreference.getString(KEY_IAM_PACKAGE)
        _type = rxPreference.getInteger(KEY_TYPE)
        _base64QR = rxPreference.getString(KEY_QR)
        _parameter = rxPreference.getString(KEY_PARAMETER)
        _contactDisconnect = rxPreference.getString(ALARM_DISCONNECT_CONTACT)
        _contactUpgrade = rxPreference.getString(ALARM_UPGRADE_CONTACT)
        _minCdnVersion = rxPreference.getString(ALARM_MIN_CDN_VERSION)
        setDeviceType(deviceType)
    }

    override fun toString(): String {
        return "{CMDB ID : ${getCMDB()}, School ID : ${getSchoolId()}, Token : ${getToken()}, Server Address : ${getCdnUrl()}, Platform API : ${getPlatformApi()}, Platform MQ : ${getPlatformMq()}, Type : ${getDeviceType()}, Parameter : ${_parameter.get()}}"
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
        private const val  KEY_PLATFORM_API = "platform_api"
        private const val  KEY_PLATFORM_MQ = "platform_mq"
        private const val  KEY_PLATFORM_ATT = "platform_att"
        private const val  KEY_PLATFORM_STATIC = "platform_static"
        private const val  KEY_CDN_URL = "cdn_url"
        private const val  KEY_IAM_PACKAGE = "iam_package"
        private const val  KEY_TYPE = "TYPE"
        private const val  KEY_QR = "QR"
        private const val  KEY_PARAMETER = "PARAMETER"

        /** 报警接口需要持久化的联系人信息 **/
        private const val ALARM_DISCONNECT_CONTACT = "disconnect_contact"
        private const val ALARM_UPGRADE_CONTACT = "upgrade_contact"
        private const val ALARM_MIN_CDN_VERSION = "min_cdn_version"
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
    fun getPlatformApi() = _platformApi.get().ifEmpty {
        if (ISUS.instance.se)
            "$DEFAULT_SE_API_HOST$SE_API_PATH"
        else
            "$DEFAULT_API_HOST$API_PATH"
    }
    fun getPlatformMq() = _platformMq.get().ifEmpty {
        if (ISUS.instance.se)
            "amqps://$MQ_DEFAULT_SE_DOMAIN:$MQ_DEFAULT_SE_POST/"
        else
            "amqp://$MQ_DEFAULT_USERNAME:$MQ_DEFAULT_PASSWORD@$MQ_DEFAULT_DOMAIN:$MQ_DEFAULT_POST/"
    }
    fun getPlatformAtt() = _platformAtt.get()
    fun getPlatformStatic() = _platformStatic.get()
    fun getCdnUrl() = _cdnUrl.get()
    fun getIamPackage() = _iamPackage.get()
    fun getDeviceType() = _type.get()
    fun getQR() = _base64QR.get()
    fun getParameter() = Gson().fromJson<Map<String, String>>(_parameter.get())?:HashMap()
    fun getURL() = "${_protocal.get()}://${_serverAddress.get()}/"
    fun getContactDisconnect() = _contactDisconnect.get()
    fun getContactUpgrade() = _contactUpgrade.get()
    fun getMinCdnVersion() = _minCdnVersion.get()

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
    fun setPlatformApi(api: String) = _platformApi.set(api)
    fun setPlatformMq(mq: String) = _platformMq.set(mq)
    fun setPlatformAtt(att: String) = _platformAtt.set(att)
    fun setPlatformStatic(static: String) = _platformStatic.set(static)
    fun setCdnUrl(url: String) = _cdnUrl.set(url)
    fun setIamPackage(iamPackage: String) = _iamPackage.set(iamPackage)
    private fun setDeviceType(deviceType: Int) = _type.set(deviceType)
    fun setQR(qr: String) = _base64QR.set(qr)
    fun setParameter(param: Map<String, String>) = _parameter.set(Gson().toJson(param))
    fun setContactDisconnect(contact: String) = _contactDisconnect.set(contact)
    fun setContactUpgrade(contact: String) = _contactUpgrade.set(contact)
    fun setMinCdnVersion(version: String) = _minCdnVersion.set(version)

    /**
     * 语音网关
     */
    fun getEXVoIPGateway() = getParameter()["VoIPGW"] ?: ""
    /**
     * 语音网关转发设备
     */
    fun getEXVoIPSTUN() = getParameter()["VoIPSTUN"] ?: ""
    /**
     * 设备内网IP
     */
    fun getEXSelfLanIP() = getParameter()["selfLanIP"] ?: ""
    /**
     * 设备公网IP
     */
    fun getEXSelfWanIP() = getParameter()["selfWanIP"] ?: ""
    /**
     * 用于管理员登录设备的密码
     */
    fun getEXSelfMKey() = getParameter()["selfMKey"] ?: ""
    /**
     * 用于API签名的密钥
     */
    fun getEXSelfSKey() = getParameter()["selfSKey"] ?: ""
    /**
     * 远程日志服务器地址（域名或IP）
     */
    fun getSyslog() = getParameter()["syslog"] ?: ""
    /**
     * 学校名称
     */
    fun getSchoolName() = getParameter()["schoolName"] ?: ""
    /**
     * 学校Logo
     */
    fun getSchoolLogo() = getParameter()["schoolLogo"] ?: ""
    /**
     * 班级名称
     */
    fun getClassName() = getParameter()["className"] ?: ""
    /**
     * 班级ID
     */
    fun getClassId() = getParameter()["classId"] ?: ""
    /**
     * 设备名称（CMDB代号）
     */
    fun getDeviceName() = getParameter()["deviceName"] ?: ""
    /**
     * 显示模式
     *
     * @see net.ischool.isus.DisplayModel
     */
    fun getDisplayModel() = getParameter()["displayModel"] ?: DisplayModel.LEGACY
    /**
     * 设备所在考勤区域
     */
    fun getAreaId() = getParameter()["areaId"]?.toInt() ?: 0
    /**
     * 设备所在考勤检查点
     */
    fun getCheckpointId() = getParameter()["checkpointId"]?.toInt() ?: 0
    /**
     * 设备所在考勤通道
     */
    fun getTunnelId() = getParameter()["tunnelId"]?.toInt() ?: 0
    /**
     * 考勤模式
     *
     * @see net.ischool.isus.AttendModel
     */
    fun getAttendModel() = getParameter()["attendModel"]?.toInt() ?: AttendModel.NONE
    /**
     * 支持的外设
     *
     * @see net.ischool.isus.PeripheralFlag
     */
    fun getPeripherals() = getParameter()["peripherals"]?.toInt() ?: PeripheralFlag.NONE

    /**
     * 读卡器类型
     *
     * @see net.ischool.isus.CReaderType
     */
    fun getCReaderType() = getParameter()["internalICReaderType"]?.toInt() ?: CReaderType.NONE

    /**
     * 门禁控制器类型
     *
     * @see net.ischool.isus.EntranceGuardType
     */
    fun getEntranceGuardType() = getParameter()["GateControllerType"]?.toInt() ?: EntranceGuardType.NONE

    /**
     * 获取经度
     */
    fun getLongitude() = getParameter()["longitude"] ?: ""

    /**
     * 获取纬度
     */
    fun getLatitude() = getParameter()["latitude"] ?: ""

    /**
     * 获取锁屏模式
     * @see net.ischool.isus.LockScreenMode
     */
    fun getLockScreen() = getParameter()["lockScreen"]?.toInt() ?: LockScreenMode.NONE

    /**
     * 获取设备型号
     * @see net.ischool.isus.DeviceId
     */
    fun getDeviceId() = getParameter()["zxprdid"] ?: ""

    /**
     * 获取新消息提醒区域ID
     */
    fun getBeepPagingAreaId() = getParameter()["beepPagingAreaId"] ?: "0"

    /**
     * 获取设备关联的环境监测通道ID列表，用 , 分隔
     */
    fun getEnvironmentTunnels() = getParameter()["environment_tunnels"] ?: ""

    /**
     * Hybrid模式下，需要加载的Web页地址
     */
    fun getHomePage(): String {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        return (parameter["homepage"] as? String) ?: ""
    }

    /**
     * Hybrid模式下，刷新Web页的间隔，单位秒
     */
    fun getRefreshInterval(): Int {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val interval = parameter["refresh_interval"] as? String
        return interval?.toIntOrNull() ?: -1
    }

    /**
     * Hybrid模式下，是否可以控制Web页
     */
    fun canControlWebview(): Boolean {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val navigation = parameter["navigation"] as? List<*>
        return !navigation.isNullOrEmpty()
    }

    /**
     * Hybrid模式下，是否可以控制Web页前进
     */
    fun canForwardWebview(): Boolean {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val navigation = parameter["navigation"] as? List<*>
        return navigation?.contains("forword") ?: false
    }

    /**
     * Hybrid模式下，是否可以控制Web页后退
     */
    fun canBackWebview(): Boolean {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val navigation = parameter["navigation"] as? List<*>
        return navigation?.contains("back") ?: false
    }

    /**
     * Hybrid模式下，是否可以控制Web页刷新
     */
    fun canRefreshWebview(): Boolean {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val navigation = parameter["navigation"] as? List<*>
        return navigation?.contains("refresh") ?: false
    }

    /**
     * Hybrid模式下，刷新Web页时是否使用的原始地址
     */
    fun refreshWithOriginal(): Boolean {
        val displayModelParameter = getParameter()["displayModelParams"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(displayModelParameter)?:HashMap()
        val refreshMode = parameter["refresh_mode"] as? String ?: "current"
        return refreshMode == "original"
    }

    /**
     * 是否启用X5内核
     */
    fun useX5Core(): Boolean {
        val webConfig = getParameter()["webview"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(webConfig) ?: HashMap()
        return parameter["engine"] as? String? == "x5"
    }

    /**
     * 获取用于生成TOTP码的Base32串
     */
    fun totpKeyWithBase32(): String {
        val totp = getParameter()["totp"] ?: ""
        val parameter = Gson().fromJson<Map<String, Any>>(totp) ?: HashMap()
        return parameter["key"] as? String? ?: ""
    }
}