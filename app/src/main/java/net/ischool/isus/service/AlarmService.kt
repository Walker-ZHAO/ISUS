package net.ischool.isus.service

import io.reactivex.rxjava3.core.Observable
import net.ischool.isus.RESULT_OK
import net.ischool.isus.model.ALARM_TYPE_DISCONNECT
import net.ischool.isus.model.ALARM_TYPE_UPGRADE
import net.ischool.isus.model.AlarmInfo
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import java.util.concurrent.TimeUnit

/**
 * 报警检测服务
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/5/30
 */

/**
 * 周期性检测报警信息
 *
 * @param cdnVersion 要求的最小CDN版本号
 * @param period 间隔周期，单位秒，默认60秒检测一次
 */
fun alarmIntervalDetect(cdnVersion: String, period: Long = 60): Observable<List<AlarmInfo>> {
    return Observable.interval(0, period, TimeUnit.SECONDS).flatMap {
        checkAlarm(cdnVersion)
    }
}

/**
 * 整合边缘云报警信息
 *
 * @param cdnVersion 要求的最小CDN版本号
 */
fun checkAlarm(cdnVersion: String): Observable<List<AlarmInfo>> {
    return checkCdnConnectivity().flatMap {
        // 无法连接边缘云，更新联系人信息后，发出报警
        if (it.isNotEmpty()) {
            val connectivityAlarm = it.first().copy(contact = PreferenceManager.instance.getContactDisconnect())
            Observable.just(listOf(connectivityAlarm))
        } else {
            // 可以连接边缘云，继续版本检测
            checkCdnVersion(cdnVersion)
        }
    }.flatMap {
        // 此处result的报警信息，可能是连接性问题，也可能是版本问题
        val result = it.toMutableList()
        // 边缘云版本过低，更新联系人信息后，进行下一步整合
        if (it.isNotEmpty() && it.first().type == ALARM_TYPE_UPGRADE) {
            val upgradeAlarm =
                it.first().copy(contact = PreferenceManager.instance.getContactUpgrade())
            // 清空之前的报警信息，重新添加更新联系人后的报警信息
            result.clear()
            result.add(upgradeAlarm)
        }
        // 将上游的检测报警信息传递给其他报警检测方法，统一整合后发布报警
        checkCdnAlarm(result)
    }.onErrorReturn {
        listOf()
    }
}

/**
 * 检测CDN服务器连通性，如果未联通，返回的报警列表中包含未连接的报警信息
 */
private fun checkCdnConnectivity(): Observable<List<AlarmInfo>> {
    val alarmInfo = AlarmInfo(
        ALARM_TYPE_DISCONNECT, System.currentTimeMillis() / 1000, "控制台设备无法连接边缘云",
        "请检查控制台网络连接是否正常。\n请检查边缘云设备网络/电源指示灯是否正常。",
        "",
    )
    return APIService.getNetworkStatus().flatMap {
        val status = checkNotNull(it.body())
        val result = if (status.errno == RESULT_OK && status.data.sids.contains(PreferenceManager.instance.getSchoolId())) {
            listOf()
        } else {
            listOf(alarmInfo.copy(ts = System.currentTimeMillis() / 1000))
        }
        Observable.just(result)
    }.onErrorReturn {
        listOf(alarmInfo.copy(ts = System.currentTimeMillis() / 1000))
    }
}

/**
 * 检查CDN服务器版本号，如果低于最低要求的版本号，返回的报警列表中包含CDN需升级的报警信息
 *
 * @param minVersion 要求的最小CDN版本号
 */
private fun checkCdnVersion(minVersion: String): Observable<List<AlarmInfo>> {
    val alarmInfo = AlarmInfo(
        ALARM_TYPE_UPGRADE, System.currentTimeMillis() / 1000, "边缘云系统版本较低，请更新至【$minVersion】以上。",
        "请联系平台服务商进行处理。",
        "",
    )
    return APIService.getCdnInfo().flatMap {
        val status = checkNotNull(it.body())
        val result = if (status.errno == RESULT_OK) {
            val minVersionNum = minVersion.split('p').first().toInt()
            val currentVersionNum = status.data.version?.split('p')?.first()?.toInt() ?: 0
            if (minVersionNum <= currentVersionNum) {
                listOf()
            } else {
                listOf(alarmInfo.copy(ts = System.currentTimeMillis() / 1000))
            }
        } else {
            listOf(alarmInfo.copy(ts = System.currentTimeMillis() / 1000))
        }
        Observable.just(result)
    }.onErrorReturn {
        listOf(alarmInfo.copy(ts = System.currentTimeMillis() / 1000))
    }
}

/**
 * 检查CDN服务器的报警信息
 */
private fun checkCdnAlarm(otherAlarms: List<AlarmInfo>): Observable<List<AlarmInfo>> {
    return APIService.getAlarmInfo().flatMap {
        val status = checkNotNull(it.body())
        val result = otherAlarms.toMutableList()
        if (status.errno == RESULT_OK) {
            // 对服务器报警信息进行清理，过滤无效条目（不包含报警内容的）
            // 然后与其他报警信息整合后发布
            result.addAll(status.data.filter { info -> info.reason.isNotEmpty() })
        }
        // 将报警按优先级排序
        result.sortBy { alarm -> alarm.priority }
        Observable.just(result.toList())
    }.onErrorReturn {
        otherAlarms
    }
}