package net.ischool.isus.activity

import androidx.appcompat.app.AppCompatActivity
import net.ischool.isus.ISUSApp

/**
 * ISUS 基础页面
 *
 * 记录当前任务 ID，用于自动切换回前台
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/4/9
 */
open class ISUSActivity: AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        ISUSApp.lastTaskId = taskId
    }
}