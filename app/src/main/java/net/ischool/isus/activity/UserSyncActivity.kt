package net.ischool.isus.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_user_sync.*
import net.ischool.isus.R
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.ISUSService
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast

/**
 * 全量同步用户信息页面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-05
 */
class UserSyncActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_sync)
        tool_bar.setTitle(R.string.user_sync_title)
        setSupportActionBar(tool_bar)

        startSync()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun startSync() {
        // 已同步成功的用户集
        val finishSet = mutableSetOf<Long>()
        val disposable = APIService.getUids()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    val result = checkNotNull(it.body())
                    if (result.errno != 0) {
                        longToast("用户信息列表获取失败, 请稍后重试")
                        Syslog.logE("用户信息列表获取失败(${result.error})")
                        finish()
                        return@subscribeBy
                    }
                    // 真正开始全量同步前，先清除用户数据，防止脏数据产生
                    ObjectBox.clearUser()
                    val total = result.data.uids.size
                    result.data.uids.forEach { uid ->
                        ISUSService.syncUserInfo(
                            uid,
                            success = {
                                finishSet.add(uid)
                                val progress = (finishSet.size / total) * 100
                                progress_bar.progress = progress
                                progress_text.text = getString(R.string.progress_text, progress)
                                if (finishSet.size == total) {
                                    // 全量同步计数
                                    PreferenceManager.instance.apply {
                                        setSyncCount(getSyncCount() + 1)
                                    }
                                    Syslog.logI("用户信息全量同步成功")
                                    longToast("用户信息同步成功")
                                    finish()
                                }
                            },
                            fail = {
                                // 提醒&结束页面
                                longToast("用户信息($uid)获取失败, 请稍后重试")
                                finish()
                            })
                    }
                },
                onError = {
                    longToast("用户信息列表获取失败, 请稍后重试")
                    Syslog.logE("用户信息列表获取失败(${it.message})")
                    finish()
                }
            )
        disposables.add(disposable)
    }

    override fun onBackPressed() {
        toast("用户信息同步中，请耐心等待...")
    }
}