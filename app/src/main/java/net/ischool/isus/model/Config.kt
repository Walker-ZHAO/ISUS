package net.ischool.isus.model

/**
 * 配置信息
 *
 * 对应/eqptapi/getConfig
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
data class Config(val type: Int, val comet: String, val QR: String, val parameter: Map<String, String>)