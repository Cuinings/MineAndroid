package com.cn.board.file.server.model

import com.google.gson.annotations.SerializedName

/**
 * 文件信息（单条）
 *
 * 字段与 MinePython `FileItem` 一致：
 * id / filename / category / path / size / size_human /
 * uploaded_by / uploaded_ip / uploaded_at / uploader_nickname
 */
data class FileInfo(
    val id: Int,
    val filename: String,
    val category: String,
    val path: String,
    val size: Long,
    @SerializedName("size_human")
    val sizeHuman: String,
    @SerializedName("uploaded_by")
    val uploadedBy: String,
    @SerializedName("uploaded_ip")
    val uploadedIp: String,
    @SerializedName("uploaded_at")
    val uploadedAt: String,
    @SerializedName("uploader_nickname")
    val uploaderNickname: String? = null
)

/**
 * 文件列表响应
 *
 * 对齐 MinePython `FileListResponse`：`{ files, total, page, page_size }`
 */
data class FileListResponse(
    val files: List<FileInfo>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int
)

/**
 * 单文件上传响应
 *
 * 对齐 MinePython `POST /api/upload` 的扁平返回：
 * `{ ok, filename, category, size, size_fmt }`
 */
data class UploadResponse(
    val ok: Boolean,
    val filename: String? = null,
    val category: String? = null,
    val size: Long = 0,
    @SerializedName("size_fmt")
    val sizeFmt: String? = null
)

/**
 * 批量上传响应
 *
 * 对齐 MinePython `POST /api/upload/multiple`：
 * `{ ok, count, files: [{ filename, category, size, size_fmt }] }`
 */
data class BatchUploadResponse(
    val ok: Boolean,
    val count: Int,
    val files: List<UploadedFileInfo>
)

/**
 * 上传成功的单个文件信息（来自批量上传接口的 files 数组）
 */
data class UploadedFileInfo(
    val filename: String,
    val category: String,
    val size: Long,
    @SerializedName("size_fmt")
    val sizeFmt: String? = null
)

/**
 * 批量删除请求体
 *
 * 对齐 MinePython `PathsRequest`：`{ paths: [string] }`
 */
data class PathsRequest(
    val paths: List<String>
)

/**
 * 批量删除中单个失败项
 */
data class BatchDeleteFailedItem(
    val path: String,
    val error: String
)

/**
 * 批量删除响应
 *
 * 对齐 MinePython `POST /api/files/batch-delete`：
 * `{ ok, deleted: [string], failed: [{ path, error }], count }`
 */
data class BatchDeleteResponse(
    val ok: Boolean,
    val deleted: List<String> = emptyList(),
    val failed: List<BatchDeleteFailedItem> = emptyList(),
    val count: Int = 0
)
