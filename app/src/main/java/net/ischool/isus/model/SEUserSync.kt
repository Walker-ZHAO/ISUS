package net.ischool.isus.model

/**
 * 安全增强模式下的用户同步
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-04
 */
data class SEUserSync(val id: String, val payload: SEUserPayload)
data class SEUserPayload(val uid: String)