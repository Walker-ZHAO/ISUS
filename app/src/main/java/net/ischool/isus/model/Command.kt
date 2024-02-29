package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 命令
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
data class Command(@SerializedName(value = "cmd_version", alternate = ["cmdVersion"]) val cmd_version: Long, val args: HashMap<String, String>?, val cmd: String, val cmdbid: String)