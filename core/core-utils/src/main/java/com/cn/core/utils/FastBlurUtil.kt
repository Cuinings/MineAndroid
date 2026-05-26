package com.cn.core.utils

import android.graphics.Bitmap

/**
 * @author: cn
 * @time: 2026/3/30 11:06
 * @history
 * @description:
 */
object FastBlurUtil {

    fun blur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return bitmap

        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blurredPixels = blur(pixels, width, height, radius)

        val result = Bitmap.createBitmap(width, height, bitmap.config!!)
        result.setPixels(blurredPixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val result = IntArray(pixels.size)

        // 水平模糊
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0

                for (i in -radius..radius) {
                    val nx = x + i
                    if (nx in 0 until width) {
                        val pixel = pixels[y * width + nx]
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }

                result[y * width + x] = (255 shl 24) or
                        ((r / count) shl 16) or
                        ((g / count) shl 8) or
                        (b / count)
            }
        }

        // 垂直模糊
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0

                for (i in -radius..radius) {
                    val ny = y + i
                    if (ny in 0 until height) {
                        val pixel = result[ny * width + x]
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }

                pixels[y * width + x] = (255 shl 24) or
                        ((r / count) shl 16) or
                        ((g / count) shl 8) or
                        (b / count)
            }
        }

        return pixels
    }
}

