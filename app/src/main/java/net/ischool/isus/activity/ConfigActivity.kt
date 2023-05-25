package net.ischool.isus.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.walker.anke.framework.setBase64
import kotlinx.android.synthetic.main.activity_config.*
import net.ischool.isus.DeviceType
import net.ischool.isus.R
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        tool_bar.setTitle(R.string.config_title)
        setSupportActionBar(tool_bar)

        qr_image.setBase64(PreferenceManager.instance.getQR(), Base64.DEFAULT)
        text_cmdbid.text = getString(R.string.config_cmdb, PreferenceManager.instance.getCMDB())
        text_school_id.text = getString(R.string.config_sid, PreferenceManager.instance.getSchoolId())
        text_server.text = getString(R.string.config_server, PreferenceManager.instance.getCdnUrl())
        text_platform_api.text = getString(R.string.config_platform_api, PreferenceManager.instance.getPlatformApi())
        text_platform_att.text = getString(R.string.config_platform_att, PreferenceManager.instance.getPlatformAtt())
        text_platform_static.text = getString(R.string.config_platform_static, PreferenceManager.instance.getPlatformStatic())
        text_platform_mq.text = getString(R.string.config_platform_mq, PreferenceManager.instance.getPlatformMq())
        text_iam_package.text = getString(R.string.config_iam_package, PreferenceManager.instance.getIamPackage())
        text_device.text = getString(R.string.config_device_type, DeviceType.getDeviceName(PreferenceManager.instance.getDeviceType()))

        val builder = StringBuilder()
        val map = PreferenceManager.instance.getParameter()
        for ( (key, value) in map ) {
            val title = ExternalParameter.getEXPName(key)?:key
            builder.append("$title：$value \n")
        }
        text_external.text = builder.toString()

        btn_reset.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(R.string.warning)
                setMessage(R.string.warning_reset)
                setNegativeButton(android.R.string.cancel) { _, _-> }
                setPositiveButton(android.R.string.ok) { _, _ -> CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null)) }
            }.show()
        }
    }
}