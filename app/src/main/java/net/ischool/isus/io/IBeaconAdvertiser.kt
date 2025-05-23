package net.ischool.isus.io

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity.BLUETOOTH_SERVICE
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import net.ischool.isus.LOG_TAG
import net.ischool.isus.SYSLOG_CATEGORY_BLE
import net.ischool.isus.command.CommandParser
import net.ischool.isus.inSleep
import net.ischool.isus.isSeeWoDevice
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.ALARM_TYPE_DISCONNECT
import net.ischool.isus.model.ALARM_TYPE_MQ
import net.ischool.isus.model.ALARM_TYPE_PLATFORM
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.AlarmService
import net.ischool.isus.util.NetworkUtil
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * iBeacon标签广播器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/12/17
 */
class IBeaconAdvertiser(private val context: Context) {

    // 是否处于广播中
    private var isStart = false

    companion object {

        // GATT服务的UUID，用于通信
        const val GATT_SERVICE_UUID = "0000B000-0000-1000-8000-00805f9b34fb"
        const val GATT_CHARACTERISTIC_UUID = "0000B001-0000-1000-8000-00805f9b34fb"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        lateinit var instance: IBeaconAdvertiser
            private set

        // 蓝牙适配器
        private var bleAdapter: BluetoothAdapter? = null

        /**
         * 是否支持蓝牙设备
         * 希沃设备不支持蓝牙，但可以获取蓝牙适配器，会导致系统蓝牙应用崩溃
         * 3368芯片设备不支持蓝牙，但可以获取蓝牙适配器，会导致系统蓝牙应用崩溃
         */
        fun supportBle(): Boolean = bleAdapter != null && !isSeeWoDevice() && !Build.PRODUCT.contains("3368")

        @SuppressLint("CheckResult")
        @Synchronized
        @JvmStatic
        fun init(context: Context) {
            instance = IBeaconAdvertiser(context)
            bleAdapter = (context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (!supportBle()) {
                // 设备无蓝牙硬件，直接退出
                PreferenceManager.instance.setIBeacon(false)
                Syslog.logE("Device has no bluetooth hardware", category = SYSLOG_CATEGORY_BLE)
                return
            }
            // Android S 以上需要动态申请权限
            if (Build.VERSION.SDK_INT < 31) {
                instance.initBle()
            } else if (context is FragmentActivity) {
                RxPermissions(context)
                    .request(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    )
                    .subscribe {
                        if (!it) {
                            Syslog.logE(
                                "get bluetooth permissions failed",
                                category = SYSLOG_CATEGORY_BLE
                            )
                        } else {
                            instance.initBle()
                        }
                    }
            }
        }
    }

    // BLE广播适配器
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    // BLE广播设置
    private lateinit var advertiseSettings: AdvertiseSettings

    // BLE广播数据
    private lateinit var advertiseData: AdvertiseData

    // BLE广播额外数据，用于发送设备名称
    private lateinit var scanResponse: AdvertiseData

    // BLE广播结果回调
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(LOG_TAG, "advertise success: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            Syslog.logE("advertise failed: $errorCode", category = SYSLOG_CATEGORY_BLE)
            Log.e(LOG_TAG, "advertise failed: $errorCode")
        }
    }

    // 周期性广播
    private var bleAdvDisposable: Disposable? = null

    // BLE 设备连接
    private var gattServer: BluetoothGattServer? = null
    // BLE 设备连接回调
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        // 默认MTU大小（ATT_MTU 23 - 3字节ATT头部）
        private var currentMtu = 20
        // 设备信息
        private var deviceInfo: BLEDeviceInfo? = null

        // 连接状态变化
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Syslog.logE("BLE设备已连接: ${device?.name}[${device?.address}]", category = SYSLOG_CATEGORY_BLE)
                    Log.i(LOG_TAG, "BLE设备已连接: ${device?.name}[${device?.address}]")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Syslog.logE("BLE设备已断开: ${device?.name}[${device?.address}]", category = SYSLOG_CATEGORY_BLE)
                    Log.i(LOG_TAG, "BLE设备已断开: ${device?.name}[${device?.address}]")
                }
            }
        }

        // MTU发生变化
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            // 更新当前MTU值（减去3字节ATT头部）
            currentMtu = mtu - 3
            Log.d(LOG_TAG, "BLE MTU已更改: $mtu, 有效负载大小: $currentMtu, 设备: ${device?.address}")
            Syslog.logI("BLE MTU已更改: $mtu, 有效负载大小: $currentMtu, 设备: ${device?.address}", category = SYSLOG_CATEGORY_BLE)
        }

        // 数据读取
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

            // 创建设备信息对象
            if (deviceInfo == null) {
                deviceInfo = createDeviceInfo()
            }
            // 将设备信息转换为JSON字符串
            val jsonString = Gson().toJson(deviceInfo)
            // 将JSON字符串转换为字节数组
            val responseData = jsonString.toByteArray()

            // 处理分片读取
            if (offset >= responseData.size) {
                // 偏移量超出范围，返回空数组
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, ByteArray(0))
                Log.d(LOG_TAG, "BLE设备读取数据完成: offset=$offset 超出数据范围")
                // 数据读取完毕，将设备信息重置为空
                deviceInfo = null
            } else {
                // 计算剩余数据长度
                val remaining = responseData.size - offset

                // 计算本次应该发送的数据长度（不超过MTU）
                val chunkSize = remaining.coerceAtMost(currentMtu)

                // 创建分片数据
                val chunk = responseData.copyOfRange(offset, offset + chunkSize)

                // 发送分片数据
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)

                // 如果一次性读取完毕，将设备信息重置为空
                if (chunk.size == responseData.size) {
                    deviceInfo = null
                }

                Log.d(LOG_TAG, "BLE设备读取数据分片: offset=$offset, 分片大小=${chunk.size}, 总大小=${responseData.size}, MTU=$currentMtu")
                if (offset == 0) {
                    // 只在第一次读取时记录完整数据
                    Syslog.logI("BLE设备读取数据: $jsonString", category = SYSLOG_CATEGORY_BLE)
                }
            }
        }

        // 数据写入
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device, requestId, characteristic, preparedWrite,
                responseNeeded, offset, value
            )
            // 处理数据写入请求
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
            // 处理接收到的数据
            value?.let {
                try {
                    val cmd = String(it).trim().lowercase()
                    Log.i(LOG_TAG, "收到BLE命令: $cmd")
                    Syslog.logI("收到BLE命令: $cmd", category = SYSLOG_CATEGORY_BLE)
                    val command = CommandParser.instance.genCommand(cmd, null)
                    CommandParser.instance.processCommand(command)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "处理BLE命令失败: ${e.message}")
                    Syslog.logE("处理BLE命令失败: ${e.message}", category = SYSLOG_CATEGORY_BLE)
                }
            }
        }
    }

    /**
     * 初始化BLE广播相关数据
     */
    @SuppressLint("MissingPermission", "CheckResult")
    private fun initBle() {
        // 开启蓝牙
        bleAdapter?.let {
            if (!it.isEnabled) {
                if (Build.VERSION.SDK_INT > 33) {
                    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    Toast.makeText(context, "请开启蓝牙", Toast.LENGTH_LONG).show()
                } else {
                    it.enable()
                }
                // 延迟等待设备开启蓝牙后再设置蓝牙相关配置
                Observable.timer(5, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { setupBle() }
            } else {
                setupBle()
            }
        }
    }

    /**
     * 设置BLE相关配置
     */
    private fun setupBle() {
        setAdapterName()
        bleAdvertiser = bleAdapter?.bluetoothLeAdvertiser
        setAdvertiseSettings()
        setAdvertiseData()
        setScanResponse()
    }

    /**
     * 设置蓝牙适配器名称
     */
    @SuppressLint("MissingPermission")
    private fun setAdapterName() {
        // 设置设备名称，iBeacon最多支持9个汉字字符
        bleAdapter?.name = PreferenceManager.instance.getDeviceName().take(9)
    }

    /**
     * BLE广播配置
     */
    private fun setAdvertiseSettings() {
        val builder = AdvertiseSettings.Builder()
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        builder.setConnectable(true)
        builder.setTimeout(0)
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        advertiseSettings = builder.build()
    }

    /**
     * 设置BLE广播数据
     */
    private fun setAdvertiseData() {
        // UUID
        val uuid = ByteBuffer.allocate(16)
        // 1字节自诊断状态
        var status = 0
        if (AlarmService.alarmInfos.firstOrNull { it.type == ALARM_TYPE_DISCONNECT } != null) {
            // 边缘云无法连通
            status = 1
        } else if (AlarmService.alarmInfos.firstOrNull { it.type == ALARM_TYPE_MQ || it.type == ALARM_TYPE_PLATFORM } != null) {
            // 平台无法连通
            status = 2
        }
        uuid.put(0, status.toByte())
        // 4字节IPV4地址
        val ipSegments = NetworkUtil.getIpAddress(context).split(".")
        uuid.put(1, ipSegments[0].toUByte().toByte())
        uuid.put(2, ipSegments[1].toUByte().toByte())
        uuid.put(3, ipSegments[2].toUByte().toByte())
        uuid.put(4, ipSegments[3].toUByte().toByte())
        // 4字节CMDB ID
        val cmdb = PreferenceManager.instance.getCMDB().toInt()
        uuid.put(5, ((cmdb shr 24) and 0xff).toByte())
        uuid.put(6, ((cmdb shr 16) and 0xff).toByte())
        uuid.put(7, ((cmdb shr 8) and 0xff).toByte())
        uuid.put(8, (cmdb and 0xff).toByte())
        // 1字节休眠状态
        uuid.put(9, if (context.inSleep()) 0x01 else 0x00)
        // 6字节保留
        for (i in 10..15) {
            uuid.put(i, (0x00).toByte())
        }

        // 计算UUID校验值（SHA-256）
        val totpSecret = Base32().decode(PreferenceManager.instance.totpKeyWithBase32())
        val hmacBuffer = ByteBuffer.allocate(16 + totpSecret.size)
        // UUID与TOTP拼接后进行SHA-256计算
        hmacBuffer.put(uuid.array())
        hmacBuffer.put(totpSecret)
        // 计算校验值
        val hmac = MessageDigest.getInstance("SHA-256").digest(hmacBuffer.array())
        // UUID最后一位使用校验值的最后一位
        uuid.put(15, hmac.last())

        // 构建广播消息
        val builder = AdvertiseData.Builder()
        val manufactureData = ByteBuffer.allocate(24)
        // iBeacon协议头
        manufactureData.put(0, (0x02).toByte())
        manufactureData.put(1, (0x15).toByte())
        // UUID
        for (i in 2..17) {
            manufactureData.put(i, uuid.get(i - 2))
        }
        // Major：固定值zx
        manufactureData.put(18, 'z'.code.toByte())
        manufactureData.put(19, 'x'.code.toByte())
        // Minor: 设备类型 - uuid版本号
        manufactureData.put(20, PreferenceManager.instance.getDeviceType().toByte())
        manufactureData.put(21, 1)
        // txPower
        manufactureData.put(22, (-75).toByte())
        // Apple厂商ID
        builder.addManufacturerData(0x4c, manufactureData.array())
        // Google厂商ID
        // builder.addManufacturerData(0xe0, manufactureData.array())
        advertiseData = builder.build()
    }

    /**
     * BLE广播增加设备名称
     */
    private fun setScanResponse() {
        val uuid = "${'z'.code.toString(16)}${'x'.code.toString(16)}"
        scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            // 服务UUID，用于标识服务提供商，可用于扫描端过滤
            .addServiceUuid(ParcelUuid.fromString("0000${uuid}-0000-1000-8000-00805f9b34fb"))
            .build()
    }

    /**
     * 配置 BLE 设备连接服务
     */
    @SuppressLint("MissingPermission")
    private fun setGattService() {
        // 初始化GATT服务
        val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        // 添加服务和特征
        val service = BluetoothGattService(
            UUID.fromString(GATT_SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(GATT_CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    /**
     * 创建设备信息对象
     */
    private fun createDeviceInfo(): BLEDeviceInfo {
        // 获取设备状态
        var diagState = 0
        if (AlarmService.alarmInfos.firstOrNull { it.type == ALARM_TYPE_DISCONNECT } != null) {
            // 边缘云无法连通
            diagState = 1
        } else if (AlarmService.alarmInfos.firstOrNull { it.type == ALARM_TYPE_MQ || it.type == ALARM_TYPE_PLATFORM } != null) {
            // 平台无法连通
            diagState = 2
        }

        return BLEDeviceInfo(
            deviceName = PreferenceManager.instance.getDeviceName(),
            schoolId = PreferenceManager.instance.getSchoolId(),
            schoolName = PreferenceManager.instance.getSchoolName(),
            classId = PreferenceManager.instance.getClassId(),
            className = PreferenceManager.instance.getClassName(),
            cmdbId = PreferenceManager.instance.getCMDB(),
            ip = NetworkUtil.getIpAddress(context),
            diagState = diagState,
            isSleep = context.inSleep()
        )
    }

    /**
     * 开启BLE广播
     */
    fun startAdvertise() {
        bleAdvDisposable?.dispose()
        // 每60秒更新一次广播数据并重新广播
        bleAdvDisposable = Observable.interval(0, 60, TimeUnit.SECONDS)
            .subscribe({
                // 停止广播
                stopAdvertiseSingle()
                // 更新广播数据
                setAdvertiseData()
                // 重新广播
                startAdvertiseSingle()
            }, {
                Syslog.logE("start advertise failed: ${it.message}", category = SYSLOG_CATEGORY_BLE)
            })
        // 配置连接服务
        setGattService()
    }

    /**
     * 停止BLE广播
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertise() {
        bleAdvDisposable?.dispose()
        stopAdvertiseSingle()
        gattServer?.close()
    }

    /**
     * 开始单次BLE广播
     */
    private fun startAdvertiseSingle() {
        if (Build.VERSION.SDK_INT >= 31 && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Syslog.logE(
                "Can't start ble advertise due to permissions",
                category = SYSLOG_CATEGORY_BLE
            )
            return
        }
        bleAdvertiser?.startAdvertising(
            advertiseSettings,
            advertiseData,
            scanResponse,
            advertiseCallback
        )
        isStart = true
    }

    /**
     * 停止单次BLE广播
     */
    private fun stopAdvertiseSingle() {
        if (Build.VERSION.SDK_INT >= 31 && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Syslog.logE(
                "Can't stop ble advertise due to permissions",
                category = SYSLOG_CATEGORY_BLE
            )
            return
        }
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        isStart = false
    }
}