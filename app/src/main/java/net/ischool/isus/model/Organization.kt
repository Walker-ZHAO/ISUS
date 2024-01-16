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
    // 机构名称
    @SerializedName("group_name") val name: String,
    // 机构排序（为班级时，代表班级序列）
    @SerializedName("group_index") val index: Int,
    // 机构类型
    @SerializedName("group_type") val groupType: Int,
    // 教育阶段（学段）
    @SerializedName("education_type") val educationType: Int,
    // 学制
    @SerializedName("education_length") val educationLength: Int,
    // 入学年份
    @SerializedName("begin_year") val beginYear: Int,
    // 毕业年份
    @SerializedName("end_year") val endYear: Int
) {
    // 该组织是否是班级类型
    val isClass: Boolean
        get() = groupType == 980
    // 可读的学段类型
    val readebleEducationType: String
        get() =
            when (educationType) {
                11 -> "幼儿园"
                21 -> "小学"
                31 -> "初中"
                34 -> "高中"
                32 -> "职业初中"
                36 -> "中等职业学校"
                41 -> "高等教育"
                else -> "$educationType"
            }
}

data class OrganizationList(
    val list: List<Organization>
)
