package net.ischool.isus.model

/**
 * 命令
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
data class Command(val cmd_version: Long, val args: HashMap<String, String>, val cmd: String, val cmdbid: String)