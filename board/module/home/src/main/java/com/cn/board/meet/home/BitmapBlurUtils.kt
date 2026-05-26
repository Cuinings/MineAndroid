package com.cn.board.meet.home

/**
 * @author: cn
 * @time: 2026/4/9 13:30
 * @history
 * @description:
 */
import android.content.Context
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.roundToInt

object BitmapBlurUtils {

    // 主方法：对bitmap进行高斯模糊
    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float = 15f, scale: Float = 0.1f): Bitmap? {
        if (bitmap.isRecycled) return null

        return try {
            // 1. 先对bitmap进行缩放，提高性能
            val scaledBitmap = scaleBitmap(bitmap, scale)

            // 2. 进行模糊处理
            val blurredBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // 使用RenderScript（API 17+）
                blurRenderScript(context, scaledBitmap, radius)
            } else {
                // 低版本使用快速模糊算法
                fastBlur(scaledBitmap, radius.toInt())
            }

            // 3. 清理中间bitmap
            scaledBitmap.recycle()

            blurredBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 缩放bitmap
     * @param scale 缩放比例，建议0.1-0.5之间
     */
    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale == 1f) return bitmap.copy(bitmap.config!!, true)

        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    /**
     * 使用RenderScript进行高斯模糊（推荐，性能最好）
     * 需要在build.gradle中配置：
     * android {
     *     defaultConfig {
     *         renderscriptTargetApi 21
     *         renderscriptSupportModeEnabled true
     *     }
     * }
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun blurRenderScript(context: Context, bitmap: Bitmap, radius: Float): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return null
        }

        var rs: RenderScript? = null
        var input: Allocation? = null
        var output: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null

        return try {
            // 创建输出bitmap
            val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

            // 初始化RenderScript
            rs = RenderScript.create(context)
            rs.messageHandler = RenderScript.RSMessageHandler()

            // 创建Allocation
            input = Allocation.createFromBitmap(
                rs, bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            output = Allocation.createTyped(rs, input.type)

            // 创建并配置模糊脚本
            blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            blur.setInput(input)
            blur.setRadius(radius.coerceIn(0.1f, 25f)) // 限制半径范围
            blur.forEach(output)

            // 复制到输出bitmap
            output.copyTo(outputBitmap)

            outputBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            // 清理资源
            input?.destroy()
            output?.destroy()
            blur?.destroy()
            rs?.destroy()
        }
    }

    /**
     * 快速模糊算法（兼容低版本）
     * 适用于API 16以下或RenderScript不可用的情况
     */
    private fun fastBlur(bitmap: Bitmap, radius: Int): Bitmap? {
        if (radius < 1) return bitmap.copy(bitmap.config!!, true)

        val src = IntArray(bitmap.width * bitmap.height)
        val dest = IntArray(bitmap.width * bitmap.height)

        bitmap.getPixels(src, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val w = bitmap.width
        val h = bitmap.height
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vmin = IntArray(w.coerceAtLeast(h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)

        for (i in 0 until 256 * divsum) {
            dv[i] = i / divsum
        }

        var yi = 0
        var yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        var yp: Int
        var i: Int

        for (y in 0 until h) {
            bsum = 0
            gsum = 0
            rsum = 0
            bsum = 0

            for (i in -radius..radius) {
                p = src[yi + wm.coerceAtMost(0.coerceAtLeast(i))]
                rsum += (p and 0xff0000) shr 16
                gsum += (p and 0x00ff00) shr 8
                bsum += p and 0x0000ff
            }

            stackpointer = radius

            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= rsum
                gsum -= gsum
                bsum -= bsum

                stackstart = stackpointer - radius + div
                p = src[yi + wm.coerceAtMost(x + radius)]

                stack[stackstart % div][0] = (p and 0xff0000) shr 16
                stack[stackstart % div][1] = (p and 0x00ff00) shr 8
                stack[stackstart % div][2] = p and 0x0000ff

                rsum += stack[stackstart % div][0]
                gsum += stack[stackstart % div][1]
                bsum += stack[stackstart % div][2]

                if (++stackpointer >= div) stackpointer = 0
                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            bsum = 0
            gsum = 0
            rsum = 0
            bsum = 0
            yp = -radius * w

            for (i in -radius..radius) {
                yi = 0.coerceAtLeast(yp) + x
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
            }

            yi = x
            stackpointer = radius

            for (y in 0 until h) {
                dest[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= r[yi]
                gsum -= g[yi]
                bsum -= b[yi]

                stackstart = stackpointer - radius + div

                rsum += stack[stackstart % div][0]
                gsum += stack[stackstart % div][1]
                bsum += stack[stackstart % div][2]

                stack[stackstart % div][0] = r[yi]
                stack[stackstart % div][1] = g[yi]
                stack[stackstart % div][2] = b[yi]

                if (++stackpointer >= div) stackpointer = 0
                yi += w
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dest, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * 快速模糊方法（使用Canvas绘制模糊效果）
     * 适用于简单模糊场景
     */
    fun fastBlurByCanvas(bitmap: Bitmap, radius: Int): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
        }

        // 使用Canvas的阴影模糊效果
        paint.maskFilter = BlurMaskFilter(radius.toFloat(), BlurMaskFilter.Blur.NORMAL)

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}