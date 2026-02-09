package com.cn.other.test.blur

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.PixelCopy
import android.view.PixelCopy.OnPixelCopyFinishedListener
import android.view.SurfaceView
import android.view.View
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author: cn
 * @time: 2026/2/9 10:08
 * @history
 * @description:
 */
class BlurSurfaceView : GLSurfaceView, GLSurfaceView.Renderer {
    private var vertexBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null
    private var program = 0
    private var textureId = -1
    private var blurRadius = 4.0f
    private var sourceBitmap: Bitmap? = null

    // 顶点坐标
    private val vertices = floatArrayOf(
        -1.0f, -1.0f,  // 左下
        1.0f, -1.0f,  // 右下
        -1.0f, 1.0f,  // 左上
        1.0f, 1.0f // 右上
    )

    // 纹理坐标
    private val texCoords = floatArrayOf(
        0.0f, 1.0f,  // 左下
        1.0f, 1.0f,  // 右下
        0.0f, 0.0f,  // 左上
        1.0f, 0.0f // 右上
    )

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        setEGLContextClientVersion(2)
        setRenderer(this)
        setRenderMode(RENDERMODE_CONTINUOUSLY)

        // 初始化顶点缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer!!.put(vertices).position(0)

        textureBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        textureBuffer!!.put(texCoords).position(0)
    }

    fun setBlurRadius(radius: Float) {
        this.blurRadius = radius
    }

    fun updateBlurSource(sourceView: View?) {
        if (sourceView == null) return

        sourceView.post(object : Runnable {
            override fun run() {
                try {
                    // 使用PixelCopy捕获底层视图内容
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val bitmap = Bitmap.createBitmap(
                            sourceView.getWidth(),
                            sourceView.getHeight(),
                            Bitmap.Config.ARGB_8888
                        )

                        PixelCopy.request(
                            sourceView as SurfaceView,
                            bitmap,
                            object : OnPixelCopyFinishedListener {
                                override fun onPixelCopyFinished(copyResult: Int) {
                                    if (copyResult == PixelCopy.SUCCESS) {
                                        sourceBitmap = bitmap
                                        requestRender() // 请求重新渲染
                                    }
                                }
                            },
                            Handler()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 创建OpenGL程序
        program = createProgram()

        // 生成纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (sourceBitmap != null) {
            // 上传纹理数据
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourceBitmap, 0)

            // 使用着色器程序
            GLES20.glUseProgram(program)

            // 设置顶点坐标
            val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

            // 设置纹理坐标
            val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(
                texCoordHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                8,
                textureBuffer
            )

            // 设置纹理尺寸和模糊半径
            val textureSizeHandle = GLES20.glGetUniformLocation(program, "uTextureSize")
            GLES20.glUniform2f(
                textureSizeHandle,
                sourceBitmap!!.getWidth().toFloat(),
                sourceBitmap!!.getHeight().toFloat()
            )

            val blurRadiusHandle = GLES20.glGetUniformLocation(program, "uBlurRadius")
            GLES20.glUniform1f(blurRadiusHandle, blurRadius)

            // 绘制
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            // 禁用顶点数组
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }
    }

    private fun createProgram(): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, this.vertexShaderSource)
        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            this.fragmentShaderSource
        )

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, shaderCode: String?): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private val vertexShaderSource: String
        get() = "attribute vec4 aPosition;" +
                "attribute vec2 aTexCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "    gl_Position = aPosition;" +
                "    vTexCoord = aTexCoord;" +
                "}"

    private val fragmentShaderSource: String
        get() = "precision mediump float;" +
                "uniform sampler2D uTexture;" +
                "uniform vec2 uTextureSize;" +
                "uniform float uBlurRadius;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "    vec4 color = vec4(0.0);" +
                "    float total = 0.0;" +
                "    for (int i = -4; i <= 4; i++) {" +
                "        float weight = 1.0 - abs(float(i)) / 5.0;" +
                "        vec2 offset = vec2(float(i) * uBlurRadius / uTextureSize.x, 0.0);" +
                "        color += texture2D(uTexture, vTexCoord + offset) * weight;" +
                "        total += weight;" +
                "    }" +
                "    gl_FragColor = color / total;" +
                "}"
}