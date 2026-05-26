package com.cn.board.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.opengl.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.io.File
import androidx.core.graphics.createBitmap
import com.cn.core.utils.FastBlurUtil

class BoardWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "BoardWallpaperService"
        var wallpaperPath: String? = "/storage/emulated/0/Download/wallpaper_dy_1.mp4"
            set(value) { value.takeIf { it != field }?.let {
                field = it
                // 通知所有活动的引擎更新壁纸
                activeEngines.apply {
                    Log.d(TAG, "activeEngines size: ${this.size}")
                }.forEach {
                    it.loadWallpaper()
                }
            } }
        private val activeEngines = mutableListOf<BoardWallpaperEngine>()
    }

    override fun onCreateEngine(): Engine {
        return BoardWallpaperEngine()
    }

    private inner class BoardWallpaperEngine : Engine(), SurfaceTexture.OnFrameAvailableListener {

        init {
            // 将引擎实例添加到活动列表
            activeEngines.add(this)
        }

        private var isVisible = false
        private var currentDrawable: Drawable? = null
        private var mediaPlayer: MediaPlayer? = null
        private var surfaceTexture: SurfaceTexture? = null
        private var blurBitmap: Bitmap? = null
        private var blurCanvas: Canvas? = null
        private var paint: Paint = Paint()
        private val handler = Handler(Looper.getMainLooper())
        private var glRenderer: GLRenderer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "onSurfaceCreated")

            loadWallpaper()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged: width=$width, height=$height")

            // 重新创建模糊用的Bitmap和Canvas
            blurBitmap = createBitmap(width, height)
            blurCanvas = Canvas(blurBitmap!!)

            // 初始化OpenGL渲染器
            glRenderer = GLRenderer(width, height)

            if (mediaPlayer != null && surfaceTexture != null) {
                // 视频壁纸需要重新设置surface
                mediaPlayer?.setSurface(android.view.Surface(surfaceTexture))
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                } else {
                }
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer?.pause()
                } else {
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            releaseMediaPlayer()
            // 从活动列表中移除引擎实例
            activeEngines.remove(this)
        }

        fun loadWallpaper() {
            val path = wallpaperPath
            if (path != null && File(path).exists()) {
                try {
                    // 释放之前的资源
                    releaseMediaPlayer()
                    currentDrawable = null

                    // 检查文件类型
                    if (isVideoFile(path)) {
                        // 加载视频
                        loadVideoWallpaper(path)
                    } else {
                        // 加载图片
                        val drawable = Drawable.createFromPath(path)
                        currentDrawable = drawable
                        Log.d(TAG, "Image wallpaper loaded from: $path")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load wallpaper: $e")
                    currentDrawable = null
                    releaseMediaPlayer()
                }
            } else {
                Log.w(TAG, "Wallpaper path not set or file does not exist")
                currentDrawable = null
                releaseMediaPlayer()
            }
        }

        private fun isVideoFile(path: String): Boolean {
            val videoExtensions = listOf(".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mkv")
            return videoExtensions.any { path.lowercase().endsWith(it) }
        }

        private fun loadVideoWallpaper(path: String) {
            try {
                Log.d(TAG, "Loading video wallpaper from: $path")
                // 检查文件是否存在
                val videoFile = File(path)
                if (!videoFile.exists()) {
                    Log.e(TAG, "Video file does not exist: $path")
                    return
                }
                Log.d(TAG, "Video file exists: ${videoFile.length()} bytes")

                // 创建SurfaceTexture用于接收视频帧
                surfaceTexture = SurfaceTexture(0)
                surfaceTexture?.setOnFrameAvailableListener(this)
                Log.d(TAG, "SurfaceTexture created")

                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(path)
                mediaPlayer?.setSurface(android.view.Surface(surfaceTexture))
                mediaPlayer?.isLooping = true
                mediaPlayer?.prepare()
                Log.d(TAG, "MediaPlayer prepared")
                if (isVisible) {
                    mediaPlayer?.start()
                    Log.d(TAG, "MediaPlayer started")
                }
                Log.d(TAG, "Video wallpaper loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video wallpaper: $e")
                // 检查是否是权限错误
                if (e.message?.contains("Permission denied") == true) {
                    Log.e(TAG, "Storage permission is required to load video wallpaper")
                }
                releaseMediaPlayer()
            }
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            // 当视频帧可用时，处理并渲染
            if (!isVisible) return

            handler.post {
                try {
                    Log.d(TAG, "Processing video frame")
                    val holder = surfaceHolder ?: run {
                        Log.e(TAG, "SurfaceHolder is null")
                        return@post
                    }
                    val canvas = holder.lockCanvas() ?: run {
                        Log.e(TAG, "Cannot lock canvas")
                        return@post
                    }

                    // 更新SurfaceTexture以获取最新帧
                    this.surfaceTexture?.updateTexImage()
                    Log.d(TAG, "SurfaceTexture updated")

                    // 使用OpenGL渲染器从SurfaceTexture中捕获帧
                    val videoBitmap = glRenderer?.renderFrame(this.surfaceTexture!!)
                    Log.d(TAG, "Video bitmap captured: ${videoBitmap != null}")

                    if (videoBitmap != null) {
                        Log.d(TAG, "Bitmap dimensions: ${videoBitmap.width}x${videoBitmap.height}")
                        // 应用模糊效果
                        val blurredBitmap = FastBlurUtil.blur(videoBitmap, 10) // 10是模糊半径
                        Log.d(TAG, "Blur applied")

                        // 绘制模糊后的帧到Wallpaper的Canvas
                        canvas.drawBitmap(blurredBitmap, 0f, 0f, null)
                        Log.d(TAG, "Blurred bitmap drawn")
                    } else {
                        Log.w(TAG, "Video bitmap is null, using black background")
                        // 模拟视频帧：使用黑色背景
                        blurBitmap?.eraseColor(0xFF000000.toInt())
                        val blurredBitmap = FastBlurUtil.blur(blurBitmap!!, 10)
                        canvas.drawBitmap(blurredBitmap, 0f, 0f, null)
                    }

                    holder.unlockCanvasAndPost(canvas)
                    Log.d(TAG, "Canvas unlocked and posted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing video frame: $e")
                }
            }
        }

        private fun releaseMediaPlayer() {
            mediaPlayer?.let {
                try {
                    it.stop()
                    it.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing media player: $e")
                }
                mediaPlayer = null
            }

            surfaceTexture?.let {
                it.release()
                surfaceTexture = null
            }

            glRenderer?.release()
            glRenderer = null

            blurBitmap?.let {
                it.recycle()
                blurBitmap = null
            }

            blurCanvas = null
        }

        // OpenGL渲染器类，用于从SurfaceTexture中捕获视频帧
        private inner class GLRenderer(private val width: Int, private val height: Int) {
            private var eglDisplay: EGLDisplay? = null
            private var eglContext: EGLContext? = null
            private var eglSurface: EGLSurface? = null
            private var program: Int = 0
            private var vertexShader: Int = 0
            private var fragmentShader: Int = 0
            private var positionHandle: Int = 0
            private var textureHandle: Int = 0
            private var textureId: Int = 0
            private var framebufferId: Int = 0
            private var renderbufferId: Int = 0

            init {
                initOpenGL()
            }

            private fun initOpenGL() {
                try {
                    Log.d(TAG, "Initializing OpenGL")
                    // 初始化EGL
                    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                    if (eglDisplay == null) {
                        Log.e(TAG, "EGL display is null")
                        return
                    }
                    val version = IntArray(2)
                    val initResult = EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
                    if (!initResult) {
                        Log.e(TAG, "EGL initialization failed")
                        return
                    }
                    Log.d(TAG, "EGL initialized: version ${version[0]}.${version[1]}")

                    // 选择EGL配置
                    val configAttributes = intArrayOf(
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_NONE
                    )

                    val configs = Array<EGLConfig?>(1) { null }
                    val numConfigs = IntArray(1)
                    val chooseResult = EGL14.eglChooseConfig(eglDisplay, configAttributes, 0, configs, 0, 1, numConfigs, 0)
                    if (!chooseResult || numConfigs[0] == 0) {
                        Log.e(TAG, "EGL config selection failed")
                        return
                    }
                    Log.d(TAG, "EGL config selected")

                    // 创建EGL上下文
                    val contextAttributes = intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                        EGL14.EGL_NONE
                    )

                    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
                    if (eglContext == null) {
                        Log.e(TAG, "EGL context creation failed")
                        return
                    }
                    Log.d(TAG, "EGL context created")

                    // 创建EGL表面
                    val surfaceAttributes = intArrayOf(
                        EGL14.EGL_WIDTH, width,
                        EGL14.EGL_HEIGHT, height,
                        EGL14.EGL_NONE
                    )

                    eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttributes, 0)
                    if (eglSurface == null) {
                        Log.e(TAG, "EGL surface creation failed")
                        return
                    }
                    Log.d(TAG, "EGL surface created")

                    val makeCurrentResult = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                    if (!makeCurrentResult) {
                        Log.e(TAG, "EGL make current failed")
                        return
                    }
                    Log.d(TAG, "EGL context made current")

                    // 加载着色器
                    vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                    if (vertexShader == 0) {
                        Log.e(TAG, "Vertex shader creation failed")
                        return
                    }
                    fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
                    if (fragmentShader == 0) {
                        Log.e(TAG, "Fragment shader creation failed")
                        return
                    }
                    Log.d(TAG, "Shaders loaded")

                    // 创建程序
                    program = GLES20.glCreateProgram()
                    if (program == 0) {
                        Log.e(TAG, "Program creation failed")
                        return
                    }
                    GLES20.glAttachShader(program, vertexShader)
                    GLES20.glAttachShader(program, fragmentShader)
                    GLES20.glLinkProgram(program)

                    // 检查程序链接状态
                    val linkStatus = IntArray(1)
                    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                    if (linkStatus[0] != GLES20.GL_TRUE) {
                        Log.e(TAG, "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
                        return
                    }
                    Log.d(TAG, "Program linked successfully")

                    // 获取属性和 uniform 位置
                    positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
                    textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
                    Log.d(TAG, "Shader locations obtained: position=$positionHandle, texture=$textureHandle")

                    // 生成纹理
                    val textureIds = IntArray(1)
                    GLES20.glGenTextures(1, textureIds, 0)
                    textureId = textureIds[0]
                    Log.d(TAG, "Texture generated: $textureId")

                    // 生成帧缓冲
                    val framebufferIds = IntArray(1)
                    GLES20.glGenFramebuffers(1, framebufferIds, 0)
                    framebufferId = framebufferIds[0]
                    Log.d(TAG, "Framebuffer generated: $framebufferId")

                    // 生成渲染缓冲
                    val renderbufferIds = IntArray(1)
                    GLES20.glGenRenderbuffers(1, renderbufferIds, 0)
                    renderbufferId = renderbufferIds[0]
                    Log.d(TAG, "Renderbuffer generated: $renderbufferId")

                    // 绑定渲染缓冲并设置大小
                    GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderbufferId)
                    GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA, width, height)
                    Log.d(TAG, "Renderbuffer storage set")

                    // 绑定帧缓冲并附加渲染缓冲
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
                    GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderbufferId)

                    // 检查帧缓冲状态
                    val framebufferStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
                    if (framebufferStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                        Log.e(TAG, "Framebuffer not complete: $framebufferStatus")
                        return
                    }
                    Log.d(TAG, "Framebuffer complete")

                    Log.d(TAG, "OpenGL initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing OpenGL: $e")
                }
            }

            fun renderFrame(surfaceTexture: SurfaceTexture): Bitmap? {
                try {
                    Log.d(TAG, "Rendering frame")
                    // 绑定纹理
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                    Log.d(TAG, "Texture bound and parameters set")

                    // 更新纹理
                    surfaceTexture.updateTexImage()
                    Log.d(TAG, "Texture updated")

                    // 使用程序
                    GLES20.glUseProgram(program)
                    Log.d(TAG, "Program used")

                    // 设置顶点数据
                    val vertices = floatArrayOf(
                        -1.0f, -1.0f,
                        1.0f, -1.0f,
                        -1.0f, 1.0f,
                        1.0f, 1.0f
                    )
                    val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                    vertexBuffer.put(vertices).position(0)

                    GLES20.glEnableVertexAttribArray(positionHandle)
                    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
                    Log.d(TAG, "Vertex data set")

                    // 设置纹理 uniform
                    GLES20.glUniform1i(textureHandle, 0)
                    Log.d(TAG, "Texture uniform set")

                    // 绑定帧缓冲
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
                    Log.d(TAG, "Framebuffer bound")

                    // 清除颜色
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    Log.d(TAG, "Color cleared")

                    // 绘制四边形
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                    Log.d(TAG, "Quad drawn")

                    // 检查OpenGL错误
                    val error = GLES20.glGetError()
                    if (error != GLES20.GL_NO_ERROR) {
                        Log.e(TAG, "OpenGL error after draw: $error")
                    }

                    // 读取像素数据
                    val pixelBuffer = java.nio.ByteBuffer.allocateDirect(width * height * 4).order(java.nio.ByteOrder.nativeOrder())
                    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)
                    Log.d(TAG, "Pixels read")

                    // 检查OpenGL错误
                    val errorAfterRead = GLES20.glGetError()
                    if (errorAfterRead != GLES20.GL_NO_ERROR) {
                        Log.e(TAG, "OpenGL error after read: $errorAfterRead")
                    }

                    // 转换为IntArray
                    val pixels = IntArray(width * height)
                    pixelBuffer.rewind()
                    var hasNonBlackPixel = false
                    for (i in 0 until width * height) {
                        val b = pixelBuffer.get().toInt() and 0xFF
                        val g = pixelBuffer.get().toInt() and 0xFF
                        val r = pixelBuffer.get().toInt() and 0xFF
                        val a = pixelBuffer.get().toInt() and 0xFF
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        if (r != 0 || g != 0 || b != 0) {
                            hasNonBlackPixel = true
                        }
                    }
                    Log.d(TAG, "Pixels converted, has non-black pixels: $hasNonBlackPixel")

                    // 创建Bitmap
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                    Log.d(TAG, "Bitmap created")

                    // 翻转Bitmap（因为OpenGL坐标系与Android不同）
                    val matrix = android.graphics.Matrix()
                    matrix.postScale(1f, -1f)
                    val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                    Log.d(TAG, "Bitmap flipped and returned")
                    return flippedBitmap

                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering frame: $e")
                    return null
                }
            }

            fun release() {
                try {
                    // 释放资源
                    GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                    GLES20.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
                    GLES20.glDeleteRenderbuffers(1, intArrayOf(renderbufferId), 0)
                    GLES20.glDeleteProgram(program)
                    GLES20.glDeleteShader(vertexShader)
                    GLES20.glDeleteShader(fragmentShader)

                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    EGL14.eglTerminate(eglDisplay)
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing OpenGL: $e")
                }
            }

            private fun loadShader(type: Int, shaderCode: String): Int {
                val shader = GLES20.glCreateShader(type)
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                return shader
            }

            private val vertexShaderCode = """
                attribute vec4 aPosition;
                varying vec2 vTextureCoord;
                void main() {
                    gl_Position = aPosition;
                    vTextureCoord = vec2((aPosition.x + 1.0) / 2.0, (aPosition.y + 1.0) / 2.0);
                }
            """

            private val fragmentShaderCode = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTextureCoord;
                void main() {
                    gl_FragColor = texture2D(uTexture, vTextureCoord);
                }
            """
        }
    }
}
