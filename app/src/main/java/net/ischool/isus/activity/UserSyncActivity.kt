package net.ischool.isus.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.walker.anke.framework.longToast
import com.walker.anke.framework.toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_user_sync.*
import net.ischool.isus.R
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.ISUSService

/**
 * 全量同步用户信息页面
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-05
 */
class UserSyncActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()
    private val uids = mutableListOf<Long>()
    private var syncCount = 0   // 已成功同步的联系人数目

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
                    uids.addAll(result.data.uids)
                    syncSingle()
                },
                onError = {
                    longToast("用户信息列表获取失败, 请稍后重试")
                    Syslog.logE("用户信息列表获取失败(${it.message})")
                    finish()
                }
            )
        disposables.add(disposable)
    }

    /**
     * 单个用户同步
     */
    private fun syncSingle() {
        val index = syncCount++
        if (index < uids.size) {
            ISUSService.syncUserInfo(
                uids[index],
                success = {
                    val progress = ((index + 1).toFloat() / uids.size) * 100
                    progress_bar.progress = progress.toInt()
                    progress_text.text = getString(R.string.progress_text, progress.toInt())
                    if (index == uids.size - 1) {
                        // 全量同步计数
                        PreferenceManager.instance.apply {
                            setSyncCount(getSyncCount() + 1)
                        }
                        Syslog.logI("用户信息全量同步成功")
                        longToast("用户信息同步成功")
                        finish()
                    } else {
                        syncSingle()
                    }
                },
                fail = {
                    // 提醒&结束页面
                    longToast("用户信息(${uids[index]})获取失败, 请稍后重试")
                    finish()
                })
        }
    }

    override fun onBackPressed() {
        toast("用户信息同步中，请耐心等待...")
    }
}