package com.cn.sample.test.child

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.FillType
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntRange
import androidx.core.graphics.PathParser
import androidx.core.graphics.toColorInt

/**
 * @Author: CuiNing
 * @Time: 2025/8/18 16:48
 * @Description:
 */
class FavoriteView: View {

    companion object {
        private const val STATE_OFF_NORMAL = 0
        private const val STATE_OFF_FOCUSED = 1
        private const val STATE_ON = 2
    }

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, ): super(context, attrs, defStyleAttr)


    private val colorFavorite = "#FCB537".toColorInt()
    private val colorUnFavoriteNormal = "#939393".toColorInt()
    private val colorUnFavoriteFocused = "#FFFFFF".toColorInt()
    private var pathFavorite: Path = PathParser.createPathFromPathData("M21.26,8.037L15.941,7.406C15.596,7.365 15.297,7.148 15.151,6.832L12.908,1.969C12.551,1.194 11.449,1.194 11.092,1.969L8.849,6.832C8.703,7.148 8.404,7.365 8.058,7.406L2.74,8.037C1.893,8.137 1.553,9.184 2.179,9.764L6.111,13.4C6.366,13.636 6.481,13.988 6.413,14.329L5.369,19.582C5.203,20.419 6.094,21.066 6.838,20.649L11.512,18.033C11.815,17.864 12.185,17.864 12.488,18.033L17.162,20.649C17.906,21.066 18.797,20.419 18.631,19.582L17.587,14.329C17.519,13.988 17.634,13.636 17.889,13.4L21.821,9.764C22.448,9.184 22.107,8.137 21.26,8.037Z")
    private var pathUnFavorite: Path = PathParser.createPathFromPathData("M21.26,8.036L15.941,7.406C15.596,7.365 15.297,7.148 15.151,6.832L12.908,1.969C12.55,1.194 11.449,1.194 11.092,1.969L8.849,6.832C8.703,7.148 8.404,7.365 8.058,7.406L2.74,8.036C1.893,8.137 1.552,9.184 2.179,9.764L6.111,13.4C6.366,13.636 6.481,13.988 6.413,14.329L5.369,19.582C5.203,20.419 6.094,21.066 6.838,20.649L11.512,18.033C11.815,17.863 12.185,17.863 12.488,18.033L17.162,20.649C17.906,21.066 18.797,20.419 18.631,19.582L17.587,14.329C17.519,13.988 17.634,13.636 17.889,13.4L21.821,9.764C22.447,9.184 22.107,8.137 21.26,8.036ZM21.142,9.03L15.824,8.399C15.478,8.358 15.165,8.235 14.884,8.031C14.602,7.826 14.389,7.566 14.243,7.251L12,2.387L9.757,7.251C9.611,7.566 9.398,7.826 9.116,8.031C8.835,8.235 8.522,8.358 8.176,8.399L2.858,9.03L6.79,12.666C7.045,12.902 7.227,13.185 7.334,13.516C7.442,13.847 7.461,14.183 7.394,14.524L6.35,19.777L11.023,17.161C11.326,16.991 11.652,16.906 12,16.906C12.348,16.906 12.673,16.991 12.977,17.161L17.65,19.777L16.606,14.524C16.538,14.183 16.558,13.847 16.666,13.516C16.773,13.185 16.955,12.902 17.21,12.666L21.142,9.03Z").apply {
        fillType = FillType.EVEN_ODD
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    @IntRange(from = STATE_OFF_NORMAL.toLong(), to = STATE_ON.toLong())
    private var currentState: Int = STATE_OFF_NORMAL

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val scale = width.coerceAtMost(height) / 24f
        val matrix = Matrix().apply { setScale(scale, scale) }
        pathFavorite.transform(matrix)
        pathUnFavorite.transform(matrix)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        currentState = when{
            currentState == STATE_OFF_NORMAL && gainFocus -> STATE_OFF_FOCUSED
            currentState == STATE_OFF_FOCUSED && !gainFocus -> STATE_OFF_NORMAL
            else -> currentState
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when(currentState) {
            STATE_OFF_NORMAL -> {
                canvas.drawPath(pathUnFavorite, paint.apply {
                    color = colorUnFavoriteNormal
                })
            }
            STATE_OFF_FOCUSED -> {
                canvas.drawPath(pathUnFavorite, paint.apply {
                    color = colorUnFavoriteFocused
                })
            }
            STATE_ON -> {
                canvas.drawPath(pathFavorite, paint.apply {
                    color = colorFavorite
                })
            }
        }
    }

    /**
     * @param active true 激活、false 未激活
     */
    fun setActive(active: Boolean) {
        currentState = when(active) {
            true -> STATE_ON
            false -> when(hasFocus()) {
                true -> STATE_OFF_FOCUSED
                else -> STATE_OFF_NORMAL
            }
        }
        invalidate()
    }

}