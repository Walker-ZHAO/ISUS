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
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import net.ischool.isus.LOG_TAG
import net.ischool.isus.SYSLOG_CATEGORY_BLE
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
class IBeaconAdvertiser {

    // 是否处于广播中
    private var isStart = false

    companion object {

        // GATT服务的UUID，用于通信
        const val GATT_SERVICE_UUID = "0000B000-0000-1000-8000-00805f9b34fb"
        const val GATT_CHARACTERISTIC_UUID = "0000B001-0000-1000-8000-00805f9b34fb"

        @Volatile
        lateinit var instance: IBeaconAdvertiser
            private set

        // 蓝牙适配器
        private var bleAdapter: BluetoothAdapter? = null

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
        private lateinit var gattServer: BluetoothGattServer
        // BLE 设备连接回调
        private val gattServerCallback = object : BluetoothGattServerCallback() {

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

            // 数据读取
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                // TODO 处理数据读取请求
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.value)
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
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                // TODO 处理接收到的数据
                value?.let {
                    val data = String(it)
                    Log.i(LOG_TAG, "收到数据: $data")
                }
            }
        }

        /**
         * 是否支持蓝牙设备
         * 希沃设备不支持蓝牙，但可以获取蓝牙适配器，会导致系统蓝牙应用崩溃
         */
        fun supportBle(): Boolean = bleAdapter != null && !isSeeWoDevice()

        @SuppressLint("CheckResult")
        @Synchronized
        @JvmStatic
        fun init(context: Context) {
            instance = IBeaconAdvertiser()
            bleAdapter = (context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (!supportBle()) {
                // 设备无蓝牙硬件，直接退出
                PreferenceManager.instance.setIBeacon(false)
                Syslog.logE("Device has no bluetooth hardware", category = SYSLOG_CATEGORY_BLE)
                return
            }
            // Android S 以上需要动态申请权限
            if (Build.VERSION.SDK_INT < 31) {
                initBle(context)
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
                            initBle(context)
                        }
                    }
            }
        }

        /**
         * 初始化BLE广播相关数据
         */
        @SuppressLint("MissingPermission", "CheckResult")
        private fun initBle(context: Context) {
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
                        .subscribe { setupBle(context) }
                } else {
                    setupBle(context)
                }
            }
        }

        /**
         * 设置BLE相关配置
         */
        private fun setupBle(context: Context) {
            setAdapterName()
            bleAdvertiser = bleAdapter?.bluetoothLeAdvertiser
            setAdvertiseSettings()
            setAdvertiseData(context)
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
        private fun setAdvertiseData(context: Context) {
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
            // 7字节保留
            for (i in 9..15) {
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
        private fun setGattService(context: Context) {
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
            gattServer.addService(service)
        }
    }

    /**
     * 开启BLE广播
     */
    fun startAdvertise(context: Context) {
        bleAdvDisposable?.dispose()
        // 每60秒更新一次广播数据并重新广播
        bleAdvDisposable = Observable.interval(0, 60, TimeUnit.SECONDS)
            .subscribe({
                // 停止广播
                stopAdvertiseSingle(context)
                // 更新广播数据
                setAdvertiseData(context)
                // 重新广播
                startAdvertiseSingle(context)
            }, {
                Syslog.logE("start advertise failed: ${it.message}", category = SYSLOG_CATEGORY_BLE)
            })
        // 配置连接服务
        setGattService(context)
    }

    /**
     * 停止BLE广播
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertise(context: Context) {
        bleAdvDisposable?.dispose()
        stopAdvertiseSingle(context)
        gattServer.close()
    }

    /**
     * 开始单次BLE广播
     */
    private fun startAdvertiseSingle(context: Context) {
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
    private fun stopAdvertiseSingle(context: Context) {
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