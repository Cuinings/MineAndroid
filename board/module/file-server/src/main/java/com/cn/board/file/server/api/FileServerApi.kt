package com.cn.board.file.server.api

import com.cn.board.file.server.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * File Server 客户端 API 接口定义（对齐 MinePython `app/` FastAPI，v4.6）
 *
 * 认证方式: Header "Authorization: Bearer <token>"
 * 需要鉴权的接口均通过 `@Header("Authorization")` 注入，由 [com.cn.board.file.server.FileServerClient]
 * 在登录后自动携带。
 */
interface FileServerApi {

    // =========================================================================
    // Auth
    // =========================================================================

    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): OpResponse

    @GET("api/auth/me")
    suspend fun me(
        @Header("Authorization") token: String
    ): MeResponse

    @PUT("api/auth/me/password")
    suspend fun changeMyPassword(
        @Header("Authorization") token: String,
        @Body body: PasswordChangeRequest
    ): AuthResponse

    @POST("api/auth/me/deactivate")
    suspend fun deactivateMyAccount(
        @Header("Authorization") token: String,
        @Body body: DeactivateRequest? = null
    ): AuthResponse

    // =========================================================================
    // Admin - 用户管理
    // =========================================================================

    @GET("api/admin/users")
    suspend fun adminListUsers(
        @Header("Authorization") token: String
    ): AdminUserListResponse

    @POST("api/admin/users")
    suspend fun adminCreateUser(
        @Header("Authorization") token: String,
        @Body body: AdminUserRequest
    ): OpResponse

    @PUT("api/admin/users/{userId}")
    suspend fun adminUpdateUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Body body: AdminUserRequest
    ): OpResponse

    @DELETE("api/admin/users/{userId}")
    suspend fun adminDeleteUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): OpResponse

    @PUT("api/admin/users/{userId}/approve")
    suspend fun adminApproveUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): OpResponse

    @PUT("api/admin/users/{userId}/reject")
    suspend fun adminRejectUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): OpResponse

    @POST("api/admin/users/batch")
    suspend fun adminBatchUsers(
        @Header("Authorization") token: String,
        @Body body: AdminBatchRequest
    ): AdminBatchResponse

    @GET("api/admin/pending")
    suspend fun adminPendingUsers(
        @Header("Authorization") token: String
    ): PendingUsersResponse

    @GET("api/admin/audit")
    suspend fun adminAuditLog(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 100
    ): AuditListResponse

    // =========================================================================
    // Categories
    // =========================================================================

    @GET("api/categories")
    suspend fun listCategories(): CategoryListResponse

    @DELETE("api/categories/{name}")
    suspend fun deleteCategory(
        @Path("name") name: String
    ): OpResponse

    @POST("api/organize")
    suspend fun organize(
        @Header("Authorization") token: String? = null
    ): OrganizeResponse

    // =========================================================================
    // Files
    // =========================================================================

    @GET("api/files")
    suspend fun listFiles(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): FileListResponse

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("category") category: RequestBody,
        @Header("Authorization") token: String? = null
    ): UploadResponse

    @Multipart
    @POST("api/upload/multiple")
    suspend fun uploadMultipleFiles(
        @Part files: List<MultipartBody.Part>,
        @Part("category") category: RequestBody,
        @Header("Authorization") token: String? = null
    ): BatchUploadResponse

    @Streaming
    @GET("api/download/{filePath}")
    suspend fun downloadFile(
        @Path("filePath", encoded = true) filePath: String,
        @Header("Authorization") token: String? = null,
        @Query("token") tokenQuery: String? = null
    ): Response<ResponseBody>

    @DELETE("api/files/{filePath}")
    suspend fun deleteFile(
        @Path("filePath", encoded = true) filePath: String,
        @Header("Authorization") token: String? = null
    ): OpResponse

    @POST("api/files/batch-delete")
    suspend fun batchDeleteFiles(
        @Header("Authorization") token: String? = null,
        @Body body: PathsRequest
    ): BatchDeleteResponse

    @Streaming
    @POST("api/files/batch-download")
    suspend fun batchDownloadFiles(
        @Header("Authorization") token: String? = null,
        @Body body: PathsRequest
    ): Response<ResponseBody>
}
