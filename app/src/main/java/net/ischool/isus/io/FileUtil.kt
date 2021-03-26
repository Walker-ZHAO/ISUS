package net.ischool.isus.io

import android.os.Environment
import net.ischool.isus.ISUS
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件工具
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/21
 */

/**
 * 返回指定目录中一个唯一的空文件
 * 方法成功返回时文件已创建，用后请删除
 *
 * @param suffix    后缀, null则使用.tmp
 * @param fDir      文件目录
 *                  如果为null则会在扩展存储卡中的CacheDir中生成文件
 *                  如果扩展存储卡不存在则在应用CacheDir中生成文件
 *
 * @return  创建的临时文件
 */
@JvmOverloads fun createTempFile(suffix: String?, fDir: File? = null): File? {
    val context = ISUS.instance.context
    var dir: File? = null
    if (fDir == null) {
        dir = when (hasExternalStorage() == 2) {
            true   -> context.externalCacheDir
            false  -> context.cacheDir
        }
    }
    val prefix = SimpleDateFormat("yyMMddHH", Locale.CHINESE).format(Date())
    return try {
        File.createTempFile(prefix, suffix, dir)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

/**
 * 检索当前系统是否包含扩展存储卡
 *
 * @return 0:没有存储卡|1:有只读存储卡|2:有可读写存储卡
 */
fun hasExternalStorage() = when (Environment.getExternalStorageState()) {
    Environment.MEDIA_MOUNTED   ->  2
    Environment.MEDIA_MOUNTED_READ_ONLY ->  1
    else    ->  0
}