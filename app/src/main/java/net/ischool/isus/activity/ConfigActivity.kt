package net.ischool.isus.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.ischool.isus.R
import net.ischool.isus.command.CommandParser
import net.ischool.isus.databinding.ActivityConfigBinding
import net.ischool.isus.model.ALARM_TYPE_CAMPUSNG
import net.ischool.isus.model.ALARM_TYPE_DISCONNECT
import net.ischool.isus.model.ALARM_TYPE_MQ
import net.ischool.isus.model.ALARM_TYPE_NOT_MATCH
import net.ischool.isus.model.ALARM_TYPE_PLATFORM
import net.ischool.isus.model.ALARM_TYPE_UPGRADE
import net.ischool.isus.model.AlarmInfo
import net.ischool.isus.network.APIService
import net.ischool.isus.service.checkAlarm

/**
 * 配置界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/21
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translucentStatusBar()
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rebindClass.setOnClickListener {  }
        binding.systemSetting.setOnClickListener { CommandParser.instance.processor?.setting() }
        binding.syncConfig.setOnClickListener { CommandParser.instance.processor?.reload() }
        binding.reset.setOnClickListener { CommandParser.instance.processor?.reset() }
        binding.reboot.setOnClickListener { CommandParser.instance.processor?.reboot() }
        binding.sleep.setOnClickListener { CommandParser.instance.processor?.sleep() }

        binding.rediagnosis.setOnClickListener { performDiag() }

        binding.back.setOnClickListener { finish() }

        // 执行诊断程序
        performDiag()

//        binding.apply {
//            qrImage.setBase64(PreferenceManager.instance.getQR(), Base64.DEFAULT)
//            textCmdbid.text = getString(R.string.config_cmdb, PreferenceManager.instance.getCMDB())
//            textSchoolId.text = getString(R.string.config_sid, PreferenceManager.instance.getSchoolId())
//            textServer.text = getString(R.string.config_server, PreferenceManager.instance.getCdnUrl())
//            textPlatformApi.text = getString(R.string.config_platform_api, PreferenceManager.instance.getPlatformApi())
//            textPlatformAtt.text = getString(R.string.config_platform_att, PreferenceManager.instance.getPlatformAtt())
//            textPlatformStatic.text = getString(R.string.config_platform_static, PreferenceManager.instance.getPlatformStatic())
//            textPlatformMq.text = getString(R.string.config_platform_mq, PreferenceManager.instance.getPlatformMq())
//            textIamPackage.text = getString(R.string.config_iam_package, PreferenceManager.instance.getIamPackage())
//            textDevice.text = getString(R.string.config_device_type, DeviceType.getDeviceName(PreferenceManager.instance.getDeviceType()))
//        }

//        val builder = StringBuilder()
//        val map = PreferenceManager.instance.getParameter()
//        for ( (key, value) in map ) {
//            val title = ExternalParameter.getEXPName(key)?:key
//            builder.append("$title：$value \n")
//        }
//        binding.textExternal.text = builder.toString()
//
//        binding.btnReset.setOnClickListener {
//            MaterialAlertDialogBuilder(this).apply {
//                setTitle(R.string.warning)
//                setMessage(R.string.warning_reset)
//                setNegativeButton(android.R.string.cancel) { _, _-> }
//                setPositiveButton(android.R.string.ok) { _, _ -> CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null)) }
//            }.show()
//        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    /**
     * 将状态栏设置为全透明
     */
    private fun translucentStatusBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT

    }

    /**
     * 执行诊断程序
     */
    private fun performDiag() {
        val versionDisposable = APIService.getCdnInfo()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val status = checkNotNull(it.body())
                val currentVersionNum = status.data.version ?: ""
                binding.cdnVersion.text = getString(R.string.cdn_version, currentVersionNum)
            }
        val diagDisposable = checkAlarm()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateDiagInfo(it)
            }

        compositeDisposable.add(versionDisposable)
        compositeDisposable.add(diagDisposable)
    }

    /**
     * 更新诊断结果
     */
    private fun updateDiagInfo(infos: List<AlarmInfo>) {
        val cdnDiagContent = StringBuilder()
        val platformDiagContent = StringBuilder()
        infos.forEach {
            // 分组展示报错信息
            when (it.type) {
                ALARM_TYPE_DISCONNECT, ALARM_TYPE_UPGRADE, ALARM_TYPE_CAMPUSNG, ALARM_TYPE_NOT_MATCH -> {
                    cdnDiagContent.append("${it.reason}\n")
                }
                ALARM_TYPE_MQ, ALARM_TYPE_PLATFORM -> {
                    platformDiagContent.append("${it.reason}\n")
                }
            }
        }
        binding.cdnContent.text = cdnDiagContent.toString()
        binding.platformContent.text = platformDiagContent.toString()

        // 边缘云、平台的连接状态
        if (infos.firstOrNull { it.type == ALARM_TYPE_DISCONNECT } != null) {
            binding.cdnState.setBackgroundResource(R.drawable.magenta_opposite_angles_corner_rect)
            binding.cdnStateIcon.setImageResource(R.mipmap.connection_failed)
            binding.cdnStateTitle.text = getString(R.string.connection_failed)
            binding.platformState.setBackgroundResource(R.drawable.gray_opposite_angles_corner_rect)
            binding.platformStateIcon.setImageResource(R.mipmap.connection_unknown)
            binding.platformStateTitle.text = getString(R.string.connection_unknown)
        } else {
            binding.cdnState.setBackgroundResource(R.drawable.green_opposite_angles_corner_rect)
            binding.cdnStateIcon.setImageResource(R.mipmap.connection_success)
            binding.cdnStateTitle.text = getString(R.string.connection_success)
            if (infos.firstOrNull { it.type == ALARM_TYPE_MQ || it.type == ALARM_TYPE_PLATFORM } != null) {
                binding.platformState.setBackgroundResource(R.drawable.magenta_opposite_angles_corner_rect)
                binding.platformStateIcon.setImageResource(R.mipmap.connection_failed)
                binding.platformStateTitle.text = getString(R.string.connection_failed)
            } else {
                binding.platformState.setBackgroundResource(R.drawable.green_opposite_angles_corner_rect)
                binding.platformStateIcon.setImageResource(R.mipmap.connection_success)
                binding.platformStateTitle.text = getString(R.string.connection_success)
            }
        }
    }
}