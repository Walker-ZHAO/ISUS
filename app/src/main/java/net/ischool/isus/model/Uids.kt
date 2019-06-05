package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 用户ID集合
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-05-30
 */
data class Uids(@SerializedName(value = "list") val uids: List<Long>)