package net.ischool.isus.command

import android.content.Intent
import android.os.Bundle
import net.ischool.isus.*
import net.ischool.isus.log.Syslog
import net.ischool.isus.model.Command
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.UDPService
import java.util.*
import kotlin.collections.HashMap

/**
 * 命令解析器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/19
 */
class CommandParser private constructor() {

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
        commandMap[ICommand.COMMAND_QUERY_STATUS] = 202006
        commandMap[ICommand.COMMAND_SLEEP] = 202306
        commandMap[ICommand.COMMAND_WAKEUP] = 202306
    }

    companion object {
        @ExperimentalStdlibApi
        fun init(commandProcessor: ICommand? = null) {
            instance = CommandParser()
            if (commandProcessor != null)
                instance.processor = commandProcessor
            else
                instance.processor = when {
                    isHikDevice() -> CommandProcessorHik(ISUS.instance.context)
                    isSeeWoDevice() -> CommandProcessorSeeWo(ISUS.instance.context)
                    isHongHeDevice() -> CommandProcessorHonghe(ISUS.instance.context)
                    else -> CommandProcessorCommon(ISUS.instance.context)   // 同时兼容触沃、大华设备
                }
            // 添加基于UDP协议的命令结果回调
            instance.processor?.addResultCallback(UDPService::sendResult)
        }
        lateinit var instance: CommandParser
    }

    /**
     * 命令处理器
     */
    fun processCommand(command: Command, remoteUUID: String = "") {
        Syslog.logI("ISUS process command: $command", SYSLOG_CATEGORY_RABBITMQ)
        if (canProcess(command)) {
            when (command.cmd.lowercase(Locale.getDefault())) {
                ICommand.COMMAND_PING -> processor?.ping(remoteUUID)
                ICommand.COMMAND_CONFIG -> processor?.config(remoteUUID)
                ICommand.COMMAND_RESET -> processor?.reset(remoteUUID)
                ICommand.COMMAND_REBOOT -> processor?.reboot(remoteUUID)
                ICommand.COMMAND_QUIT -> processor?.quit(remoteUUID)
                ICommand.COMMAND_UPDATE -> processor?.update(command.args["url"], remoteUUID)
                ICommand.COMMAND_SETTING -> processor?.setting(remoteUUID)
                ICommand.COMMAND_BACK -> processor?.backPage(remoteUUID)
                ICommand.COMMAND_ADB -> processor?.openAdb(remoteUUID)
                ICommand.COMMAND_RELOAD -> processor?.reload(remoteUUID)
                ICommand.COMMAND_LAUNCH_PAGE -> processor?.launchPage(command.args["intent"], remoteUUID)
                ICommand.COMMAND_QUERY_STATUS -> processor?.queryStatus(command.args["type"], remoteUUID)
                ICommand.COMMAND_SLEEP -> processor?.sleep(remoteUUID)
                ICommand.COMMAND_WAKEUP -> processor?.wakeup(remoteUUID)
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
        val version = commandMap[command.cmd.lowercase(Locale.getDefault())] ?: -1
        if (version >= command.cmd_version && cmdbid == command.cmdbid) {
            return true
        }
        return false
    }

    fun genCommand(cmd: String, args: HashMap<String, String>?): Command {
        val version = commandMap[cmd.lowercase(Locale.getDefault())] ?: 0
        return Command(version, args ?: hashMapOf(), cmd, PreferenceManager.instance.getCMDB())
    }
}