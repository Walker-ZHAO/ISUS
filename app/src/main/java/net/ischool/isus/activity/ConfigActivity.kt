package net.ischool.isus.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import com.walker.anke.framework.setBase64
import kotlinx.android.synthetic.main.activity_config.*
import net.ischool.isus.DeviceType
import net.ischool.isus.R
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.preference.ExternalParameter
import net.ischool.isus.preference.PreferenceManager
import org.jetbrains.anko.*

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
        text_server.text = getString(R.string.config_server, "${PreferenceManager.instance.getProtocal()}://${PreferenceManager.instance.getServer()}")
        text_device.text = getString(R.string.config_device_type, DeviceType.getDeviceName(PreferenceManager.instance.getDeviceType()))

        val builder = StringBuilder()
        val map = PreferenceManager.instance.getParameter()
        for ( (key, value) in map ) {
            val title = ExternalParameter.getEXPName(key)?:key
            builder.append("$title：$value \n")
        }
        text_external.text = builder.toString()

        btn_reset.setOnClickListener {
            alert(R.string.warning_reset) {
                titleResource = R.string.warning
                yesButton { CommandParser.instance.processCommand(CommandParser.instance.genCommand(ICommand.COMMAND_RESET, null)) }
                noButton {  }
            }.show()
        }
    }
}