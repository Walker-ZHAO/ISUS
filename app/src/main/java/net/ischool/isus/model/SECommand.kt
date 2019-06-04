package net.ischool.isus.model

/**
 * 安全增强模式下的命令
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-04
 */
data class SECommand(val id: String, val payload: SEPayload)
data class SEPayload(val channel: String, val payload: Command)