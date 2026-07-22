package com.cn.board.file.server

import android.util.Log
import com.cn.board.file.server.api.FileServerApi
import com.cn.board.file.server.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * File Server Android 客户端（对齐 MinePython `app/` FastAPI，v4.6）
 *
 * 封装 Retrofit，提供：
 * - Token 管理（登录自动保存、请求自动携带）
 * - 所有 API 的挂起函数调用
 * - 文件上传/下载/批量删除/批量下载的高层封装
 * - 统一的错误处理
 *
 * 使用示例：
 * ```kotlin
 * val client = FileServerClient("http://192.168.1.100:8000")
 *
 * // 登录
 * val result = client.login("admin", "admin123")
 * if (result.ok) {
 *     // 后续请求自动携带 token
 *     val files = client.listFiles("图片")
 * }
 * ```
 */
class FileServerClient(
    private val baseUrl: String,
    private val debug: Boolean = false
) {
    companion object {
        private const val TAG = "FileServerClient"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L
    }

    // ── Token 管理 ────────────────────────────────────────────
    private var authToken: String? = null
    private var currentUser: CurrentUserInfo? = null

    /** 认证头字段名（Retrofit 接口中使用） */
    private val authHeader: String
        get() = if (authToken != null) "Bearer $authToken" else ""

    // ── Retrofit 实例 ─────────────────────────────────────────
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .apply {
                if (debug) {
                    addInterceptor(
                        HttpLoggingInterceptor { msg ->
                            Log.d(TAG, msg)
                        }.setLevel(HttpLoggingInterceptor.Level.BODY)
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * FileServerApi 实例（可直接使用底层 Retrofit 接口）
     */
    val api: FileServerApi by lazy { retrofit.create(FileServerApi::class.java) }

    // ========================================================================
    // Auth
    // ========================================================================

    /**
     * 注册新用户（需管理员审批后生效）
     */
    suspend fun register(
        username: String,
        password: String,
        nickname: String? = null
    ): Result<AuthResponse> = safeCall {
        api.register(AuthRequest(username, password, nickname))
    }

    /**
     * 登录，成功后自动保存 token
     */
    suspend fun login(username: String, password: String): Result<AuthResponse> = safeCall {
        api.login(AuthRequest(username, password)).also { response ->
            if (response.ok && response.token != null) {
                setAuth(response.token, response.role, response.nickname)
            }
        }
    }

    /**
     * 手动设置认证信息（用于恢复已保存的 token）
     */
    fun setAuth(token: String?, role: String? = null, nickname: String? = null) {
        authToken = token
        currentUser = if (token != null) {
            CurrentUserInfo(role = role, nickname = nickname)
        } else null
    }

    /** 是否已登录 */
    val isLoggedIn: Boolean get() = authToken != null

    /** 当前用户信息 */
    val user: CurrentUserInfo? get() = currentUser

    /** 退出登录（使当前会话 token 失效） */
    suspend fun logout(): Result<OpResponse> = safeCall {
        api.logout(authHeader).also { _ -> logoutLocal() }
    }

    /** 仅本地清除登录态（不通知服务端） */
    fun logoutLocal() {
        authToken = null
        currentUser = null
    }

    /**
     * 拉取当前用户资料与有效权限（GET /api/auth/me）
     */
    suspend fun me(): Result<MeResponse> = safeCall {
        api.me(authHeader)
    }

    /**
     * 修改当前用户密码（会使所有会话失效，需重新登录）
     */
    suspend fun changeMyPassword(
        oldPassword: String,
        newPassword: String
    ): Result<AuthResponse> = safeCall {
        api.changeMyPassword(authHeader, PasswordChangeRequest(oldPassword, newPassword))
    }

    /**
     * 注销当前用户账号（默认管理员受保护）
     */
    suspend fun deactivateMyAccount(password: String? = null): Result<AuthResponse> = safeCall {
        api.deactivateMyAccount(authHeader, if (password != null) DeactivateRequest(password) else null)
    }

    // ========================================================================
    // Admin - 用户管理
    // ========================================================================

    /** 获取所有用户（管理员） */
    suspend fun adminListUsers(): Result<AdminUserListResponse> = safeCall {
        api.adminListUsers(authHeader)
    }

    /** 管理员创建用户 */
    suspend fun adminCreateUser(
        username: String,
        password: String,
        nickname: String? = null,
        role: String? = null,
        status: String? = null
    ): Result<OpResponse> = safeCall {
        api.adminCreateUser(authHeader, AdminUserRequest(username, password, nickname, role, status))
    }

    /** 管理员修改用户 */
    suspend fun adminUpdateUser(
        userId: Int,
        username: String,
        password: String,
        nickname: String? = null,
        role: String? = null,
        status: String? = null
    ): Result<OpResponse> = safeCall {
        api.adminUpdateUser(authHeader, userId, AdminUserRequest(username, password, nickname, role, status))
    }

    /** 管理员删除用户 */
    suspend fun adminDeleteUser(userId: Int): Result<OpResponse> = safeCall {
        api.adminDeleteUser(authHeader, userId)
    }

    /** 管理员审批通过用户 */
    suspend fun adminApproveUser(userId: Int): Result<OpResponse> = safeCall {
        api.adminApproveUser(authHeader, userId)
    }

    /** 管理员拒绝用户 */
    suspend fun adminRejectUser(userId: Int): Result<OpResponse> = safeCall {
        api.adminRejectUser(authHeader, userId)
    }

    /**
     * 管理员批量操作用户
     * @param ids 目标用户 id 列表
     * @param action "approve" | "reject" | "delete"
     */
    suspend fun adminBatchUsers(
        ids: List<Int>,
        action: String
    ): Result<AdminBatchResponse> = safeCall {
        api.adminBatchUsers(authHeader, AdminBatchRequest(ids, action))
    }

    /** 获取待审批用户列表 */
    suspend fun adminPendingUsers(): Result<PendingUsersResponse> = safeCall {
        api.adminPendingUsers(authHeader)
    }

    /** 获取审计日志（管理员） */
    suspend fun adminAuditLog(limit: Int = 100): Result<AuditListResponse> = safeCall {
        api.adminAuditLog(authHeader, limit)
    }

    // ========================================================================
    // Categories
    // ========================================================================

    /** 获取分类列表 */
    suspend fun listCategories(): Result<CategoryListResponse> = safeCall {
        api.listCategories()
    }

    /** 删除分类（及其下所有文件） */
    suspend fun deleteCategory(name: String): Result<OpResponse> = safeCall {
        api.deleteCategory(name)
    }

    /** 整理根目录散落文件到分类目录 */
    suspend fun organize(): Result<OrganizeResponse> = safeCall {
        api.organize(authHeader)
    }

    // ========================================================================
    // Files
    // ========================================================================

    /**
     * 获取文件列表，可选按分类筛选 / 分页 / 关键字搜索
     *
     * @param category 分类名（可空）
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量
     * @param search 文件名模糊搜索关键字
     */
    suspend fun listFiles(
        category: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<FileListResponse> = safeCall {
        api.listFiles(category, page, pageSize, search)
    }

    /**
     * 上传单个文件（无进度回调）
     *
     * @param file 本地文件
     * @param category 分类名，"auto" 为自动识别
     */
    suspend fun uploadFile(
        file: File,
        category: String = "auto"
    ): Result<UploadResponse> = uploadFile(file, category, null)

    /**
     * 上传单个文件，支持进度回调
     *
     * @param file 本地文件
     * @param category 分类名，"auto" 为自动识别
     * @param onProgress 进度回调 (bytesWritten, totalBytes)，totalBytes = file.length()
     */
    suspend fun uploadFile(
        file: File,
        category: String = "auto",
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<UploadResponse> = safeCall {
        val originalBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val requestBody = if (onProgress != null) {
            ProgressRequestBody(originalBody, file.length(), onProgress)
        } else {
            originalBody
        }
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
        api.uploadFile(part, categoryBody, authHeader)
    }

    /**
     * 流式上传（Flow），回调每块写入进度
     */
    fun uploadFileFlow(
        file: File,
        category: String = "auto"
    ): Flow<IoProgress> = callbackFlow {
        val totalSize = file.length()
        try {
            val originalBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val requestBody = ProgressRequestBody(originalBody, totalSize) { written, _ ->
                trySend(IoProgress.Progress(written, totalSize))
            }
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            api.uploadFile(part, categoryBody, authHeader)
            trySend(IoProgress.Complete)
        } catch (e: Exception) {
            trySend(IoProgress.Error(e))
        }
        close()
    }.flowOn(Dispatchers.IO)

    /**
     * 批量上传文件
     */
    suspend fun uploadFiles(
        files: List<File>,
        category: String = "auto"
    ): Result<BatchUploadResponse> = safeCall {
        val parts = files.map { file ->
            val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, requestBody)
        }
        val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
        api.uploadMultipleFiles(parts, categoryBody, authHeader)
    }

    /**
     * 下载文件到本地
     *
     * @param filePath 远程路径，格式 "分类/文件名"
     * @param destDir 目标目录
     * @return 下载后的本地文件绝对路径
     */
    suspend fun downloadFile(
        filePath: String,
        destDir: File
    ): Result<String> = safeCall {
        val response = api.downloadFile(filePath, authHeader)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "Download failed: ${response.message()}")
        }

        val body = response.body()
            ?: throw ApiException(response.code(), "Empty response body")

        val filename = filePath.substringAfterLast("/")
        val destFile = File(destDir, filename)
        destDir.mkdirs()

        withContext(Dispatchers.IO) {
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        destFile.absolutePath
    }

    /**
     * 流式下载文件，返回 [InputStream]
     *
     * @return Pair<InputStream, contentLength>，调用方负责关闭流
     */
    suspend fun downloadFileStream(filePath: String): Result<Pair<InputStream, Long>> = safeCall {
        val response = api.downloadFile(filePath, authHeader)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "Download failed: ${response.message()}")
        }
        val body = response.body()
            ?: throw ApiException(response.code(), "Empty response body")
        val contentLength = response.headers()["Content-Length"]?.toLongOrNull() ?: -1L
        body.byteStream() to contentLength
    }

    /**
     * 流式下载（Flow），支持进度回调
     */
    fun downloadFileFlow(filePath: String, bufferSize: Int = 8 * 1024): Flow<IoProgress> = flow {
        val result = downloadFileStream(filePath)
        result.fold(
            onSuccess = { (inputStream, total) ->
                try {
                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Long = 0
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        bytesRead += read
                        emit(IoProgress.Progress(bytesRead, total))
                    }
                    emit(IoProgress.Complete)
                } finally {
                    closeQuietly(inputStream)
                }
            },
            onFailure = { e ->
                emit(IoProgress.Error(e))
            }
        )
    }.flowOn(Dispatchers.IO)

    /** 删除文件（需登录，受 RBAC 约束） */
    suspend fun deleteFile(filePath: String): Result<OpResponse> = safeCall {
        api.deleteFile(filePath, authHeader)
    }

    /**
     * 批量删除文件
     *
     * @param paths 远程存储路径列表，格式 "分类/文件名"
     * @return 含成功/失败明细的 [BatchDeleteResponse]
     */
    suspend fun batchDeleteFiles(paths: List<String>): Result<BatchDeleteResponse> = safeCall {
        api.batchDeleteFiles(authHeader, PathsRequest(paths))
    }

    /**
     * 批量下载文件为 ZIP 到本地
     *
     * @param paths 远程存储路径列表，格式 "分类/文件名"
     * @param destDir 目标目录
     * @return 下载后的本地 ZIP 文件绝对路径
     */
    suspend fun downloadFilesAsZip(
        paths: List<String>,
        destDir: File
    ): Result<String> = safeCall {
        val response = api.batchDownloadFiles(authHeader, PathsRequest(paths))
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "Batch download failed: ${response.message()}")
        }
        val body = response.body()
            ?: throw ApiException(response.code(), "Empty response body")

        val filename = parseDispositionFilename(response.headers()["Content-Disposition"]) ?: "files.zip"
        val destFile = File(destDir, filename)
        destDir.mkdirs()

        withContext(Dispatchers.IO) {
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        destFile.absolutePath
    }

    // ========================================================================
    // Internal
    // ========================================================================

    /**
     * 从 Content-Disposition 头解析文件名，失败返回 null。
     * 形如: `attachment; filename=files_20260101_120000.zip`
     */
    private fun parseDispositionFilename(header: String?): String? {
        if (header == null) return null
        val match = "filename=\"?([^\";]+)\"?".toRegex().find(header) ?: return null
        return match.groupValues[1].takeIf { it.isNotBlank() }
    }

    /**
     * 统一安全调用，捕获 Retrofit/网络异常
     */
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Log.e(TAG, "API call failed", e)
            Result.failure(e)
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}

private fun closeQuietly(inputStream: InputStream) {
    try {
        inputStream.close()
    } catch (_: Exception) {
    }
}

// ── Public types ─────────────────────────────────────────────────────────

/**
 * 当前登录用户信息
 */
data class CurrentUserInfo(
    val role: String? = null,
    val nickname: String? = null
)

/**
 * 上传/下载 IO 进度状态
 */
sealed class IoProgress {
    /** 传输中 */
    data class Progress(
        val bytesTransferred: Long,
        val totalBytes: Long
    ) : IoProgress()

    /** 传输完成 */
    data object Complete : IoProgress()

    /** 传输出错 */
    data class Error(val exception: Throwable) : IoProgress()
}

/**
 * 带进度上报的 RequestBody，用于上传进度回调
 */
private class ProgressRequestBody(
    private val delegate: RequestBody,
    private val totalBytes: Long,
    private val onProgress: (Long, Long) -> Unit
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = object : okio.ForwardingSink(sink) {
            var bytesWritten: Long = 0
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, totalBytes)
            }
        }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}

/**
 * API 返回非 2xx 状态码时抛出的异常
 */
class ApiException(
    val statusCode: Int,
    override val message: String
) : RuntimeException(message)
