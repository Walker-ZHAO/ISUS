package net.ischool.isus.command

/**
 * 命令执行结果
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/24
 */
class CommandResult(cmd: String, var args: HashMap<String, String> = hashMapOf()) {

    init {
        args["response"] = cmd
        if (!args.containsKey("code"))
            args["code"] = "0"
    }

    fun fail(reason: String?) {
        args["code"] = "-1"
        args["reason"] = reason ?: ""
    }

    fun success(text: String) {
        args["code"] = "0"
        args["reason"] = text
    }

    override fun toString(): String {
        return "[cmd: ${args["response"]}, success: ${args["code"] == "0"}, reason: ${args["reason"]}]"
    }
}