package net.ischool.isus.command

import android.content.Context
import android.content.Intent
import android.os.Bundle
import net.ischool.isus.*
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.Command
import net.ischool.isus.preference.PreferenceManager
import java.util.*
import kotlin.collections.HashMap

/**
 * 命令解析器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/19
 */
class CommandParser private constructor(context: Context){

    private var processor: ICommand? = null
    private val commandMap = mutableMapOf<String, Long>()

    init {
        // 命令注册
        commandMap[ICommand.COMMAND_PING] = 201708
        commandMap[ICommand.COMMAND_CONFIG] = 201708
        commandMap[ICommand.COMMAND_RESET] = 201708
        commandMap[ICommand.COMMAND_REBOOT] = 201708
        commandMap[ICommand.COMMAND_QUIT] = 201708
        commandMap[ICommand.COMMAND_UPDATE] = 201708
        commandMap[ICommand.COMMAND_SETTING] = 201708
        commandMap[ICommand.COMMAND_BACK] = 201708
        commandMap[ICommand.COMMAND_ADB] = 201708
        commandMap[ICommand.COMMAND_RELOAD] = 201708
        commandMap[ICommand.COMMAND_LAUNCH_PAGE] = 201708
    }

    companion object {
        fun init(commandProcessor: ICommand? = null) {
            instance = CommandParser(ISUS.instance.context)
            if (commandProcessor != null)
                instance.processor = commandProcessor
            else
                instance.processor = if (isHikDevice()) HikVisionCommandImpl(ISUS.instance.context) else CommandImpl(ISUS.instance.context)
        }
        lateinit var instance: CommandParser
    }

    /**
     * 命令处理器
     */
    fun processCommand(command: Command) {
        Syslog.logI("ISUS process command: $command")
        if (canProcess(command)) {
            when (command.cmd.toLowerCase(Locale.getDefault())) {
                ICommand.COMMAND_PING -> processor?.ping()
                ICommand.COMMAND_CONFIG -> processor?.config()
                ICommand.COMMAND_RESET -> processor?.reset()
                ICommand.COMMAND_REBOOT -> processor?.reboot()
                ICommand.COMMAND_QUIT -> processor?.quit()
                ICommand.COMMAND_UPDATE -> processor?.update(command.args["url"])
                ICommand.COMMAND_SETTING -> processor?.setting()
                ICommand.COMMAND_BACK -> processor?.backPage()
                ICommand.COMMAND_ADB -> processor?.openAdb()
                ICommand.COMMAND_RELOAD -> processor?.reload()
                ICommand.COMMAND_LAUNCH_PAGE -> processor?.launchPage(command.args["intent"])
            }
        } else {
            // 广播无法处理的command
            val intent = Intent(ACTION_COMMAND)
            intent.putExtra(EXTRA_VERSION, command.cmd_version)
            intent.putExtra(EXTRA_CMD, command.cmd)
            intent.putExtra(EXTRA_CMDB_ID, command.cmdbid)
            val bundle = Bundle()
            for ((k, v) in command.args) {
                bundle.putString(k, v)
            }
            intent.putExtra(EXTRA_ARGS, bundle)
            ISUS.instance.context.sendBroadcast(intent)
        }
    }

    /**
     * 命令是否可执行
     */
    private fun canProcess(command: Command): Boolean {
        val cmdbid = PreferenceManager.instance.getCMDB()
        val version = commandMap[command.cmd.toLowerCase(Locale.getDefault())] ?: -1
        if (version >= command.cmd_version && cmdbid == command.cmdbid) {
            return true
        }
        return false
    }

    fun genCommand(cmd: String, args: HashMap<String, String>?): Command {
        val version = commandMap[cmd.toLowerCase(Locale.getDefault())] ?: 0
        return Command(version, args ?: hashMapOf(), cmd, PreferenceManager.instance.getCMDB())
    }
}