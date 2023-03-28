package net.ischool.isus.activity

import android.Manifest
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.android.synthetic.main.activity_scan.*
import net.ischool.isus.R
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 扫码页
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/3/27
 */
class ScanActivity: AppCompatActivity() {

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    // CameraX 相关
    private val lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var camera: Camera? = null

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) setUpCameraX()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }

    private fun setUpCameraX() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder.post { bindCameraUseCases() }
    }

    /**
     * 绑定需要实现的用例（预览，拍照，数据分析）
     */
    private fun bindCameraUseCases() {
        // 获取用于设置全屏分辨率相机的屏幕值
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }

        // 获取使用的屏幕比例分辨率属性
        val screenAspectRatio = aspectRatio(metrics.widthPixels / 2, metrics.heightPixels / 2)
        val width = viewFinder.measuredWidth
        val height = if (screenAspectRatio == AspectRatio.RATIO_16_9) {
            (width * RATIO_16_9_VALUE).toInt()
        } else {
            (width * RATIO_4_3_VALUE).toInt()
        }
        val size = Size(width, height)

        // 获取旋转角度
        val rotation = viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 预览用例
            preview = Preview.Builder()
                .setTargetResolution(size)
                .setTargetRotation(rotation)
                .build()

            // 图像分析用例
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(size)
                .setTargetRotation(rotation)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor!!, ZXingCodeAnalyzer {
                        Log.i("Walker", "result: $it")
                        finish()
                    })
                }

            // 必须在重新绑定用例之前取消之前绑定
            cameraProvider.unbindAll()

            try {
                // 绑定Fragment生命周期
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                // 设置预览的View
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 根据传入的值获取相机应该设置的分辨率比例
     *
     * @param width     预览宽
     * @param height    预览高
     *
     * @return 最适合的比例
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}

/**
 * ZXing 扫码分析仪
 */
private class ZXingCodeAnalyzer(val resultCallBack: (String) -> Unit): ImageAnalysis.Analyzer {

    private val reader: MultiFormatReader = initReader()

    // 一旦解析成功，不再进行解析
    private var success = false

    /**
     * 将buffer写入数组
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    override fun analyze(image: ImageProxy) {
        if (success)
            return

        // 如果不是yuv_420_888格式直接不处理
        if (ImageFormat.YUV_420_888 != image.format) {
            image.close()
            return
        }

        // 将buffer数据写入数组
        val data = image.planes[0].buffer.toByteArray()

        // 获取图片宽高
        val height = image.height
        val width = image.width

        // 将图片旋转，这是竖屏扫描的关键一步，因为默认输出图像是横的，我们需要将其旋转90度
        val rotationData = ByteArray(data.size)
        var j: Int
        var k: Int
        for (y in 0 until height) {
            for (x in 0 until width) {
                j = x * height + height - y - 1
                k = x + y * width
                rotationData[j] = data[k]
            }
        }
        // ZXing 核心解码块，因为图片旋转了90度，所以宽高互换，最后一个参数是左右翻转
        val source = PlanarYUVLuminanceSource(rotationData, height, width, 0, 0, height, width, false)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = reader.decode(bitmap)
            success = true
            resultCallBack(result.text)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }

    private fun initReader(): MultiFormatReader {
        val formatReader = MultiFormatReader()
        val hints = Hashtable<DecodeHintType, Any>()
        val decodeFormats = Vector<BarcodeFormat>()

        //添加条码解码格式
        decodeFormats.addAll(ZXingDecodeFormat.BAR_CODE_FORMATS)
        //添加二维码解码格式
        decodeFormats.addAll(ZXingDecodeFormat.QR_CODE_FORMATS)
        //这个不知道干啥的，可以不加
        decodeFormats.addAll(ZXingDecodeFormat.DATA_MATRIX_FORMATS)

        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        //设置解码的字符类型
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        formatReader.setHints(hints)
        return formatReader
    }
}

object ZXingDecodeFormat {
    private var PRODUCT_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED)
    private var INDUSTRIAL_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR)
    var BAR_CODE_FORMATS: MutableSet<BarcodeFormat> = EnumSet.copyOf(PRODUCT_FORMATS).apply { addAll(INDUSTRIAL_FORMATS) }
    val QR_CODE_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.QR_CODE)
    val DATA_MATRIX_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.DATA_MATRIX)
}