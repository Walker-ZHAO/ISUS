package net.ischool.isus.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.seewo.udsservice.client.plugins.device.UDSDeviceHelper
import io.reactivex.rxjava3.core.Observable
import net.ischool.isus.databinding.ActivityBlackHoleBinding
import net.ischool.isus.isSeeWoDevice
import java.util.concurrent.TimeUnit

/**
 * 用于休眠的占位页面
 *
 * 该页面适用于无法通过删除触屏设备节点实现休眠的设备，通过定期将屏幕调整为黑屏的方式实现休眠
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/1/6
 */
class BlackHoleActivity: AppCompatActivity() {

    private lateinit var binding: ActivityBlackHoleBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlackHoleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 定期将屏幕调整为黑屏
        Observable.interval(0, 3, TimeUnit.SECONDS)
            .subscribe { blackScreen() }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 检测到任何触摸事件（代表当前屏幕处于点亮状态），将屏幕置为黑屏
        blackScreen()
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 检测到任何键盘事件，将屏幕置为黑屏
        blackScreen()
        return true
    }

    override fun onPause() {
        super.onPause()
        // 页面被切换到后台，重启启动该页面，防止休眠期间，其他页面处于可见状态
        val intent = Intent(this, BlackHoleActivity::class.java)
        startActivity(intent)
    }

    /**
     * 将屏幕置为黑屏
     */
    private fun blackScreen() {
        if (isSeeWoDevice()) {
            UDSDeviceHelper().setScreenStatus(false)
        }
    }
}