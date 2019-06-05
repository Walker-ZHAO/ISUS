package net.ischool.isus.db

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import io.objectbox.kotlin.boxFor
import net.ischool.isus.BuildConfig
import net.ischool.isus.model.MyObjectBox
import net.ischool.isus.model.User

/**
 * DB 工具
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-05
 */
class ObjectBox {
    companion object {
        @Volatile
        private lateinit var boxStore: BoxStore

        @Synchronized
        fun init(context: Context) {
            boxStore = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()

            if (BuildConfig.DEBUG) {
                // 浏览器可查看数据
                // 'adb forward tcp:[pc port] tcp:[device port]'
                // http://localhost:pc_port/index.html
                AndroidObjectBrowser(boxStore).start(context.applicationContext)
            }
        }

        // 用户实体操作类
        val userBox by lazy { boxStore.boxFor<User>() }
    }
}