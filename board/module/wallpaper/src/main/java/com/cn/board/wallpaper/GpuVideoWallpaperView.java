package com.cn.board.wallpaper;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author: cn
 * @time: 17/7/2026 下午 3:33
 * @history
 * @description:
 */
final class GpuVideoWallpaperView extends GLSurfaceView implements
        GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "GpuVideoWallpaper";
    private static final ThreadLocal<SurfaceHolder> CONSTRUCTION_HOLDER = new ThreadLocal<>();

    private static final float[] VERTICES = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f
    };

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n"
                    + "attribute vec2 aTextureCoord;\n"
                    + "uniform mat4 uTextureMatrix;\n"
                    + "varying vec2 vTextureCoord;\n"
                    + "void main() {\n"
                    + "  gl_Position = aPosition;\n"
                    + "  vTextureCoord = (uTextureMatrix * vec4(aTextureCoord, 0.0, 1.0)).xy;\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES uTexture;\n"
                    + "varying vec2 vTextureCoord;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = texture2D(uTexture, vTextureCoord);\n"
                    + "}\n";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FloatBuffer vertexBuffer = createFloatBuffer(VERTICES);
    private final FloatBuffer textureBuffer = createFloatBuffer(new float[] {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    });
    private final float[] textureMatrix = new float[16];
    private final float[] cropCoords = new float[8]; // 预分配，避 updateCropCoordinates 分配
    private final AtomicBoolean frameAvailable = new AtomicBoolean();
    private final AtomicBoolean cropDirty = new AtomicBoolean(true);

    private SurfaceHolder holderDelegate;
    private volatile SurfaceTexture surfaceTexture;
    private volatile Surface producerSurface;
    private volatile int glGeneration;

    private MediaPlayer mediaPlayer;
    private Surface videoSurface;
    private String requestedPath = "";
    private String currentPath;
    private boolean destroyed;
    private boolean glPaused;
    private boolean gpuReady;
    private boolean playbackEnabled;
    private boolean playerPrepared;
    private boolean wallpaperSurfaceAvailable;

    private volatile int surfaceWidth;
    private volatile int surfaceHeight;
    private volatile int videoWidth;
    private volatile int videoHeight;

    private int program;
    private int textureId;
    private int positionHandle;
    private int textureCoordinateHandle;
    private int textureMatrixHandle;
    private int textureSamplerHandle;

    static GpuVideoWallpaperView create(Context context, SurfaceHolder surfaceHolder) {
        CONSTRUCTION_HOLDER.set(surfaceHolder);
        try {
            return new GpuVideoWallpaperView(context, surfaceHolder);
        } finally {
            CONSTRUCTION_HOLDER.remove();
        }
    }

    private GpuVideoWallpaperView(Context context, SurfaceHolder surfaceHolder) {
        super(context, null);
        holderDelegate = surfaceHolder;
        Matrix.setIdentityM(textureMatrix, 0);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 0, 0, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public SurfaceHolder getHolder() {
        SurfaceHolder holder = holderDelegate;
        if (holder == null) {
            holder = CONSTRUCTION_HOLDER.get();
        }
        return holder != null ? holder : super.getHolder();
    }

    void setWallpaperSurfaceAvailable(boolean available) {
        wallpaperSurfaceAvailable = available;
        if (!available) {
            releasePlayer();
            return;
        }
        if (gpuReady && !TextUtils.isEmpty(requestedPath)) {
            loadVideo(requestedPath);
        }
    }

    void setPlaybackEnabled(boolean enabled) {
        playbackEnabled = enabled;
        if (enabled) {
            if (glPaused) {
                onResume();
                glPaused = false;
            }
            startPlayerIfReady();
        } else {
            pausePlayer();
            if (!glPaused) {
                onPause();
                glPaused = true;
            }
        }
    }

    String getCurrentPath() {
        return currentPath;
    }

    boolean isPlayerCreated() {
        return mediaPlayer != null;
    }

    void loadVideo(String path) {
        if (destroyed) {
            return;
        }

        String normalizedPath = normalizePath(path);
        requestedPath = normalizedPath;
        if (mediaPlayer != null && TextUtils.equals(currentPath, normalizedPath)) {
            startPlayerIfReady();
            return;
        }
        if (!gpuReady || !wallpaperSurfaceAvailable || videoSurface == null
                || !videoSurface.isValid()) {
            return;
        }

        File videoFile = TextUtils.isEmpty(normalizedPath) ? null : new File(normalizedPath);
        if (videoFile == null || !videoFile.isFile() || !videoFile.canRead()) {
            Log.e(TAG, "loadVideo: video unavailable, path=" + normalizedPath);
            releasePlayer();
            return;
        }

        releasePlayer();
        final MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        currentPath = videoFile.getAbsolutePath();
        player.setLooping(true);
        player.setVolume(0.0f, 0.0f);
        player.setSurface(videoSurface);
        player.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                updateVideoSize(width, height);
            }
        });
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer preparedPlayer) {
                if (preparedPlayer != mediaPlayer || destroyed) {
                    preparedPlayer.release();
                    return;
                }
                playerPrepared = true;
                updateVideoSize(preparedPlayer.getVideoWidth(), preparedPlayer.getVideoHeight());
                Log.d(TAG, "onPrepared: GPU path=" + currentPath);
                startPlayerIfReady();
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer failedPlayer, int what, int extra) {
                Log.e(TAG, "onError: what=" + what + ", extra=" + extra
                        + ", path=" + currentPath);
                if (failedPlayer == mediaPlayer) {
                    releasePlayer();
                }
                return true;
            }
        });

        try {
            player.setDataSource(getContext(), Uri.fromFile(videoFile));
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException
                 | SecurityException exception) {
            Log.e(TAG, "loadVideo failed: " + exception);
            if (player == mediaPlayer) {
                releasePlayer();
            } else {
                player.release();
            }
        }
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        playbackEnabled = false;
        wallpaperSurfaceAvailable = false;
        mainHandler.removeCallbacksAndMessages(null);
        releasePlayer();

        try {
            super.onDetachedFromWindow();
        } catch (RuntimeException exception) {
            Log.e(TAG, "destroy GL thread failed: " + exception);
        }
        getHolder().removeCallback(this);
        releaseSurfaceTexture();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        releaseSurfaceTexture();
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        textureCoordinateHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        textureMatrixHandle = GLES20.glGetUniformLocation(program, "uTextureMatrix");
        textureSamplerHandle = GLES20.glGetUniformLocation(program, "uTexture");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
        );
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
        );
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
        );
        GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
        );

        SurfaceTexture newSurfaceTexture = new SurfaceTexture(textureId);
        newSurfaceTexture.setOnFrameAvailableListener(this);
        Surface newProducerSurface = new Surface(newSurfaceTexture);
        surfaceTexture = newSurfaceTexture;
        producerSurface = newProducerSurface;

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        frameAvailable.set(false);
        Matrix.setIdentityM(textureMatrix, 0);

        final int generation = ++glGeneration;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onGpuSurfaceReady(generation, newProducerSurface);
            }
        });
        requestRender();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        cropDirty.set(true);
        GLES20.glViewport(0, 0, width, height);
        requestRender();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        SurfaceTexture texture = surfaceTexture;
        if (texture != null && frameAvailable.compareAndSet(true, false)) {
            try {
                texture.updateTexImage();
                texture.getTransformMatrix(textureMatrix);
            } catch (RuntimeException exception) {
                Log.e(TAG, "updateTexImage failed: " + exception);
                return;
            }
        }

        if (cropDirty.compareAndSet(true, false)) {
            updateCropCoordinates();
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (program == 0 || textureId == 0) {
            return;
        }

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(
                textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(textureSamplerHandle, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture texture) {
        if (destroyed || texture != surfaceTexture) {
            return;
        }
        frameAvailable.set(true);
        requestRender();
    }

    private void onGpuSurfaceReady(int generation, Surface surface) {
        if (destroyed || generation != glGeneration) {
            surface.release();
            return;
        }

        String pathToReload = requestedPath;
        releasePlayer();
        if (videoSurface != null && videoSurface != surface) {
            videoSurface.release();
        }
        videoSurface = surface;
        gpuReady = true;
        if (wallpaperSurfaceAvailable && !TextUtils.isEmpty(pathToReload)) {
            loadVideo(pathToReload);
        }
    }

    private void updateVideoSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        videoWidth = width;
        videoHeight = height;
        SurfaceTexture texture = surfaceTexture;
        if (texture != null) {
            try {
                texture.setDefaultBufferSize(width, height);
            } catch (RuntimeException exception) {
                Log.e(TAG, "setDefaultBufferSize failed: " + exception);
            }
        }
        cropDirty.set(true);
        requestRender();
    }

    private void updateCropCoordinates() {
        float left = 0.0f;
        float right = 1.0f;
        float top = 0.0f;
        float bottom = 1.0f;

        if (videoWidth > 0 && videoHeight > 0 && surfaceWidth > 0 && surfaceHeight > 0) {
            float videoAspect = (float) videoWidth / (float) videoHeight;
            float surfaceAspect = (float) surfaceWidth / (float) surfaceHeight;
            if (videoAspect > surfaceAspect) {
                float visibleWidth = surfaceAspect / videoAspect;
                left = (1.0f - visibleWidth) * 0.5f;
                right = 1.0f - left;
            } else if (videoAspect < surfaceAspect) {
                float visibleHeight = videoAspect / surfaceAspect;
                top = (1.0f - visibleHeight) * 0.5f;
                bottom = 1.0f - top;
            }
        }

        textureBuffer.clear();
        cropCoords[0] = left; cropCoords[1] = top;
        cropCoords[2] = right; cropCoords[3] = top;
        cropCoords[4] = left; cropCoords[5] = bottom;
        cropCoords[6] = right; cropCoords[7] = bottom;
        textureBuffer.put(cropCoords);
        textureBuffer.position(0);
    }

    private void startPlayerIfReady() {
        if (!playbackEnabled || !wallpaperSurfaceAvailable || mediaPlayer == null
                || !playerPrepared) {
            return;
        }
        try {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (IllegalStateException exception) {
            Log.e(TAG, "startPlayer failed: " + exception);
            releasePlayer();
        }
    }

    private void pausePlayer() {
        if (mediaPlayer == null || !playerPrepared) {
            return;
        }
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (IllegalStateException exception) {
            Log.e(TAG, "pausePlayer failed: " + exception);
            releasePlayer();
        }
    }

    private void releasePlayer() {
        MediaPlayer player = mediaPlayer;
        mediaPlayer = null;
        currentPath = null;
        playerPrepared = false;
        videoWidth = 0;
        videoHeight = 0;
        cropDirty.set(true);
        if (player == null) {
            return;
        }

        player.setOnPreparedListener(null);
        player.setOnErrorListener(null);
        player.setOnVideoSizeChangedListener(null);
        try {
            player.reset();
        } catch (IllegalStateException ignored) {
            // release() remains valid after asynchronous player errors.
        }
        player.release();
    }

    private void releaseSurfaceTexture() {
        int tid = textureId;
        int prog = program;
        textureId = 0;
        program = 0;
        if (tid != 0) {
            GLES20.glDeleteTextures(1, new int[]{tid}, 0);
        }
        if (prog != 0) {
            GLES20.glDeleteProgram(prog);
        }
        SurfaceTexture texture = surfaceTexture;
        surfaceTexture = null;
        if (texture != null) {
            texture.setOnFrameAvailableListener(null);
            texture.release();
        }

        Surface surface = producerSurface;
        producerSurface = null;
        if (surface != null) {
            surface.release();
        }
        Surface outputSurface = videoSurface;
        videoSurface = null;
        if (outputSurface != null && outputSurface != surface) {
            outputSurface.release();
        }
        gpuReady = false;
    }

    private static String normalizePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        return new File(path.trim()).getAbsolutePath();
    }

    private static FloatBuffer createFloatBuffer(float[] values) {
        FloatBuffer buffer = ByteBuffer
                .allocateDirect(values.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(values);
        buffer.position(0);
        return buffer;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        if (linkStatus[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            GLES20.glDeleteProgram(shaderProgram);
            throw new IllegalStateException("Could not link GPU wallpaper program: " + error);
        }
        return shaderProgram;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Could not compile GPU wallpaper shader: " + error);
        }
        return shader;
    }
}
