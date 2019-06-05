package net.ischool.isus.model

import com.google.gson.annotations.SerializedName
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * 用户信息
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-04
 */
@Entity
data class User(
    @Id(assignable = true) var uid: Long,  // 用户ID
    val username: String,   // 用户名
    @Index @SerializedName(value = "cardnum") val cardNum: String, // 用户卡号
    val avatar: String, // 用户头像
    @SerializedName(value = "user_default_group_id") val groupId: Long, // 默认班级ID
    @SerializedName(value = "user_default_group_name") val groupName: String,   // 默认班级名
    @SerializedName(value = "user_type_text") val userType: String, //  用户类型
    @SerializedName(value = "student_type_text") val studentType: String,    //  学生类型
    var cacheAvatar: String = ""    // 缓存到本地的头像地址（文件路径）
)