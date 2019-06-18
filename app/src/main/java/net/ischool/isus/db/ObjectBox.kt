package net.ischool.isus.db

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import net.ischool.isus.BuildConfig
import net.ischool.isus.model.MyObjectBox
import net.ischool.isus.model.User
import net.ischool.isus.model.User_

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

            // 浏览器可查看数据
            // 'adb forward tcp:[pc port] tcp:[device port]'
            // http://localhost:pc_port/index.html
            AndroidObjectBrowser(boxStore).start(context.applicationContext)
        }

        // 用户实体操作类
        private val userBox by lazy { boxStore.boxFor<User>() }

        /**
         * 清空所有用户信息
         */
        fun clearUser() = userBox.removeAll()

        /**
         * 添加或更新用户信息
         */
        fun updateUser(user: User) = userBox.put(user)

        /**
         * 删除用户信息
         * @param uid   用户ID
         */
        fun removeUser(uid: Long) = userBox.remove(uid)

        /**
         * 查找用户
         * @param cardNum   卡号
         */
        fun findUser(cardNum: String) = userBox.query { equal(User_.cardNum, cardNum) }.findFirst()

        /**
         * 关闭数据库
         */
        fun destroy() {
            if (!boxStore.isClosed)
                boxStore.close()
        }
    }
}