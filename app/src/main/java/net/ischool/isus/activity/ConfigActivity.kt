package net.ischool.isus.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.walker.anke.framework.setBase64
import net.ischool.isus.DeviceType
import net.ischool.isus.R
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.databinding.ActivityConfigBinding
import net.ischool.isus.preference.ExternalParameter
import net.ischool.isus.preference.PreferenceManager

/**
 * 配置界面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/21
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolBar.setTitle(R.string.config_title)
        setSupportActionBar(binding.toolBar)

        binding.apply {
            qrImage.setBase64(PreferenceManager.instance.getQR(), Base64.DEFAULT)
            textCmdbid.text = getString(R.string.config_cmdb, PreferenceManager.instance.getCMDB())
            textSchoolId.text = getString(R.string.config_sid, PreferenceManager.instance.getSchoolId())
            textServer.text = getString(R.string.config_server, PreferenceManager.instance.getCdnUrl())
            textPlatformApi.text = getString(R.string.config_platform_api, PreferenceManager.instance.getPlatformApi())
            textPlatformAtt.text = getString(R.string.config_platform_att, PreferenceManager.instance.getPlatformAtt())
            textPlatformStatic.text = getString(R.string.config_platform_static, PreferenceManager.instance.getPlatformStatic())
            textPlatformMq.text = getString(R.string.config_platform_mq, PreferenceManager.instance.getPlatformMq())
            textIamPackage.text = getString(R.string.config_iam_package, PreferenceManager.instance.getIamPackage())
            textDevice.text = getString(R.string.config_device_type, DeviceType.getDeviceName(PreferenceManager.instance.getDeviceType()))
        }

        val builder = StringBuilder()
        val map = PreferenceManager.instance.getParameter()
        for ( (key, value) in map ) {
            val title = ExternalParameter.getEXPName(key)?:key
            builder.append("$title：$value \n")
        }
        binding.textExternal.text = builder.toString()

        binding.btnReset.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(R.string.warning)
                setMessage(R.string.warning_reset)
                setNegativeButton(android.R.string.cancel) { _, _-> }
                setPositiveButton(android.R.string.ok) { _, _ -> CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null)) }
            }.show()
        }
    }
}