package net.ischool.isus.command

/**
 * 命令执行结果
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/24
 */
class CommandResult(private val cmd: String, private var code: Int = 0, var result: HashMap<String, String> = hashMapOf()) {

    fun fail(reason: String?) {
        code = -1
        result["reason"] = reason ?: ""
    }
}