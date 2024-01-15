package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 机构信息
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/12
 */
data class Organization(
    val id: Int,
    @SerializedName("group_name") val name: String,
    @SerializedName("group_type") val type: Int,
    @SerializedName("group_index") val index: Int
)

data class OrganizationList(
    val list: List<Organization>
)
