package net.ischool.isus.io

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.BLUETOOTH_SERVICE
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import net.ischool.isus.LOG_TAG
import net.ischool.isus.SYSLOG_CATEGORY_BLE
import net.ischool.isus.log.Syslog
import java.nio.ByteBuffer

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

        @Volatile
        lateinit var instance: IBeaconAdvertiser
            private set

        // 蓝牙适配器
        private lateinit var bleAdapter: BluetoothAdapter

        // BLE广播适配器
        private lateinit var bleAdvertiser: BluetoothLeAdvertiser

        // BLE广播设置
        private lateinit var advertiseSettings: AdvertiseSettings

        // BLE广播数据
        private lateinit var advertiseData: AdvertiseData

        // BLE广播额外数据，用于发送设备名称
        private lateinit var scanResponse: AdvertiseData

        // BLE广播结果回调
        private val advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Syslog.logI("advertise success: $settingsInEffect", category = SYSLOG_CATEGORY_BLE)
                Log.d(LOG_TAG, "advertise success: $settingsInEffect")
            }

            override fun onStartFailure(errorCode: Int) {
                Syslog.logE("advertise failed: $errorCode", category = SYSLOG_CATEGORY_BLE)
                Log.e(LOG_TAG, "advertise failed: $errorCode")
            }
        }

        @SuppressLint("CheckResult")
        @Synchronized
        @JvmStatic
        fun init(context: Context) {
            instance = IBeaconAdvertiser()
            bleAdapter = (context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
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
        @SuppressLint("MissingPermission")
        private fun initBle(context: Context) {
            // 开启蓝牙
            if (!bleAdapter.isEnabled) {
                if (Build.VERSION.SDK_INT > 33) {
                    Toast.makeText(context, "请开启蓝牙", Toast.LENGTH_LONG).show()
                    return
                }
                bleAdapter.enable()
            }
            setAdapterName()
            bleAdvertiser = bleAdapter.bluetoothLeAdvertiser
            setAdvertiseSettings()
            setAdvertiseData()
            setScanResponse()
        }

        /**
         * 设置蓝牙适配器名称
         */
        @SuppressLint("MissingPermission")
        private fun setAdapterName() {
            // TODO 修改成设备名称
            bleAdapter.name = "ISUS"
        }

        /**
         * BLE广播配置
         */
        private fun setAdvertiseSettings() {
            val builder = AdvertiseSettings.Builder()
            builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            builder.setConnectable(false)
            builder.setTimeout(0)
            builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            advertiseSettings = builder.build()
        }

        /**
         * 设置BLE广播数据
         */
        private fun setAdvertiseData() {
            val builder = AdvertiseData.Builder()
            val manufactureData = ByteBuffer.allocate(24)
            // iBeacon协议头
            manufactureData.put(0, (0x02).toByte())
            manufactureData.put(1, (0x15).toByte())
            // UUID
            for (i in 2..17) {
                manufactureData.put(i, (0x2F).toByte())
            }
            // Major
            manufactureData.put(18, 0x00.toByte())
            manufactureData.put(19, 0x09.toByte())
            // Minor
            manufactureData.put(20, 0x00.toByte())
            manufactureData.put(21, 0x06.toByte())
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
            scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()
        }
    }

    /**
     * 开启BLE广播
     */
    fun startAdvertise(context: Context) {
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
        bleAdvertiser.startAdvertising(
            advertiseSettings,
            advertiseData,
            scanResponse,
            advertiseCallback
        )
        isStart = true
    }

    /**
     * 停止BLE广播
     */
    fun stopAdvertise(context: Context) {
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
        bleAdvertiser.stopAdvertising(advertiseCallback)
        isStart = false
    }
}