package net.ischool.isus.widgets

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * 自定义字体
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/3/27
 */
class CustomTextView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attributeSet, defStyleAttr) {

    companion object {
        private const val FONT_URL = "fonts/YouSheBiaoTiHei-2.ttf"
    }

    override fun getTypeface(): Typeface {
        return Typeface.createFromAsset(context.assets, FONT_URL)
    }

    override fun setTypeface(tf: Typeface?) {
        super.setTypeface(Typeface.createFromAsset(context.assets, FONT_URL))
    }

}