# file-server

File Server Android 客户端模块，封装 Retrofit 调用 MinePython `app/` FastAPI（对齐至 **v4.6**）。

## 模块信息

| 项 | 值 |
|----|-----|
| 路径 | `:board:module:file-server` |
| 包名 | `com.cn.board.file.server` |
| 依赖 | Retrofit 2.11 / OkHttp 4.12 / Gson 2.11 |

## 引用

```kotlin
implementation(project(":board:module:file-server"))
```

## 快速入门

```kotlin
val client = FileServerClient("http://192.168.1.100:8000", debug = true)

// 登录（token 自动保存，后续请求自动携带）
client.login("admin", "admin123").onSuccess { resp ->
    // resp.ok, resp.token, resp.role, resp.nickname, resp.permissions
}

// 文件列表（支持分类筛选 / 分页 / 搜索）
client.listFiles(category = "图片", page = 1, pageSize = 20).onSuccess { resp ->
    println("total=${resp.total} page=${resp.page}/${resp.pageSize}")
    resp.files.forEach { println(it.filename) }
}

// 上传（回调进度）
client.uploadFile(File("/sdcard/test.png"), category = "auto") { written, total ->
    updateProgress(written, total)
}.onSuccess { resp -> println("${resp.filename} -> ${resp.sizeFmt}") }

// 批量删除
client.batchDeleteFiles(listOf("图片/a.png", "文档/b.pdf")).onSuccess { resp ->
    println("deleted=${resp.deleted} failed=${resp.failed}")
}

// 批量下载为 ZIP
client.downloadFilesAsZip(listOf("图片/a.png", "文档/b.pdf"), File(cacheDir))
    .onSuccess { zipPath -> println("zip at $zipPath") }

// 下载（Flow 进度）
client.downloadFileFlow("图片/abc_test.png").collect { progress ->
    when (progress) {
        is IoProgress.Progress -> updateProgress(progress.bytesTransferred, progress.totalBytes)
        is IoProgress.Complete -> onDone()
        is IoProgress.Error    -> onError(progress.exception)
    }
}
```

## API 覆盖

| 分组 | 方法 | 端点 |
|------|------|------|
| Auth | `login()` / `register()` / `logout()` / `me()` / `changeMyPassword()` / `deactivateMyAccount()` | POST/GET/PUT `/api/auth/*` |
| Admin | `adminListUsers()` / `adminCreateUser()` / `adminUpdateUser()` / `adminDeleteUser()` / `adminApproveUser()` / `adminRejectUser()` / `adminBatchUsers()` / `adminPendingUsers()` / `adminAuditLog()` | GET/POST/PUT/DELETE `/api/admin/*` |
| Category | `listCategories()` / `deleteCategory()` / `organize()` | GET/DELETE/POST `/api/categories` |
| Files | `listFiles()` / `uploadFile()` / `uploadFileFlow()` / `uploadFiles()` / `downloadFile()` / `downloadFileFlow()` / `deleteFile()` / `batchDeleteFiles()` / `downloadFilesAsZip()` | GET/POST/DELETE `/api/files` |

## 与 MinePython 后端的契约对齐要点

- `GET /api/files` → `FileListResponse { files, total, page, page_size }`
- `POST /api/upload` → `UploadResponse { ok, filename, category, size, size_fmt }`（扁平结构）
- `POST /api/upload/multiple` → `BatchUploadResponse { ok, count, files: [UploadedFileInfo] }`
- `GET /api/categories` → `CategoryListResponse { categories: [{ category, count, total_size }] }`
- `POST /api/organize` → `OrganizeResponse { ok, message }`
- `POST /api/files/batch-delete` → `BatchDeleteResponse { ok, deleted, failed, count }`
- `POST /api/files/batch-download` → 流式 ZIP（`downloadFilesAsZip` 落盘为文件）
- `login()` 返回额外携带 `permissions`、`require_password_change`

## FileServerClient 特性

- **Token 自动管理**：`login()` 成功后自动保存；后续请求自动注入 `Authorization: Bearer <token>`
- **统一错误处理**：所有 API 返回 `Result<T>`，异常统一捕获
- **三种下载模式**：`downloadFile()` 直接存文件 / `downloadFileStream()` 返回流 / `downloadFileFlow()` 协程 Flow + 进度回调；以及 `downloadFilesAsZip()` 批量打包下载
- **两种批量文件操作**：`batchDeleteFiles()` 批量删除、`downloadFilesAsZip()` 批量下载为 ZIP
- **上传进度**：`uploadFile(callback)` 回调模式 / `uploadFileFlow()` Flow 模式，通过 `ProgressRequestBody` 包装实现
- **OkHttp 日志**：`debug=true` 时开启 `HttpLoggingInterceptor(BODY)`
- **超时**：连接 15s，读写 60s

## 关键类型

| 类型 | 说明 |
|------|------|
| `FileServerClient` | 主客户端，高层封装 |
| `FileServerApi` | Retrofit 接口，可直接使用 |
| `IoProgress` | sealed class，Progress/Complete/Error，统一上传/下载进度 |
| `CurrentUserInfo` | 当前用户 role/nickname |
| `ApiException` | HTTP 非 2xx 异常 |
