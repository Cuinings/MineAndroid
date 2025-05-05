package com.cn.sample.test

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.PathParser
import androidx.core.graphics.toColorInt
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class Mic: View {

    companion object {
        val TAG = Mic::class.simpleName
    }

    var micPath: Path = Path()
    var micPowerPath: Path = Path()

    val mPaint = Paint().apply {
        isAntiAlias = true
    }

    private var powerColorNormal = "#E0E0E0".toColorInt()
    private var powerColor = "#1A9AEB".toColorInt()

    private var micWidth = 0
    private var micHeight = 0

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.assets.open("icon_mic_bottom.xml")).let {
            it.getElementsByTagName("vector").let {
                for (index in 0 until it.length) {
                    (it.item(index) as Element).getAttribute("android:viewportWidth")?.let {
                        Log.d(TAG, "android:viewportWidth:$it")
                        micWidth = it.toInt()
                    }
                    (it.item(index) as Element).getAttribute("android:viewportHeight")?.let {
                        Log.d(TAG, "android:viewportHeight:$it")
                        micHeight = it.toInt()
                    }
                }
            }
            it.getElementsByTagName("path").let {
                for (index in 0 until it.length) {
                    val item: Element = it.item(index) as Element
                    item.getAttribute("android:pathData").let {
                        Log.d(TAG, "android:pathData:$it")
                        micPath = PathParser.createPathFromPathData(it)
                    }
                    item.getAttribute("android:fillColor").let {
                        Log.d(TAG, "android:fillColor:$it")
                        mPaint.color = it.toColorInt()
                    }
                }
            }
        }
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.assets.open("icon_mic_power.xml")).let {
            it.getElementsByTagName("path").let {
                for (index in 0 until it.length) {
                    val item: Element = it.item(index) as Element
                    item.getAttribute("android:pathData").let {
                        micPath.addPath(PathParser.createPathFromPathData(it))
                        micPowerPath = PathParser.createPathFromPathData(it)
                        Log.d(TAG, "android:pathData:$it")
                    }
                    item.getAttribute("android:fillColor").let {
                        Log.d(TAG, "android:fillColor:$it")
                    }
                }
            }
        }
    }

    private var mWidth = 0
    private var mHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
        mHeight = MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.UNSPECIFIED)
    }

    var power = 30.0f
        set(value) {
            field = value
            postInvalidate()
        }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw: $mWidth, $mHeight, $micWidth, $micHeight")
        val scale = mWidth.coerceAtMost(mHeight) / micWidth.coerceAtMost(micHeight)
        canvas.scale(scale.toFloat(), scale.toFloat())
        canvas.drawPath(micPath, mPaint.apply { color = powerColorNormal })
//        canvas.drawPath(micPowerPath, mPaint.apply { color = powerColorNormal })
    }

}