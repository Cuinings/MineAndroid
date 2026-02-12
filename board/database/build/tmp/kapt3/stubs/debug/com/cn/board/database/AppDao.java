package com.cn.board.database;

/**
 * @author: cn
 * @time: 2026/2/10 10:00
 * @history
 * @description: Room DAO接口
 */
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\t\n\u0002\b\b\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\u00020\u00032\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u000e\u0010\u0010\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\tH\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001c\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u0014\u001a\u00020\u0015H\u00a7@\u00a2\u0006\u0002\u0010\u0016J\u001c\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u0018\u001a\u00020\u0019H\u00a7@\u00a2\u0006\u0002\u0010\u001aJ\u001c\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u001c\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001c\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u001c\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u001e\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u001f\u001a\u00020 H\u00a7@\u00a2\u0006\u0002\u0010!J\u0018\u0010\"\u001a\u0004\u0018\u00010\u00052\u0006\u0010\r\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u0018\u0010#\u001a\u0004\u0018\u00010\u00052\u0006\u0010$\u001a\u00020\u0019H\u00a7@\u00a2\u0006\u0002\u0010\u001aJ\u001e\u0010%\u001a\u00020\u00032\u0006\u0010$\u001a\u00020\u00192\u0006\u0010&\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010'\u00a8\u0006("}, d2 = {"Lcom/cn/board/database/AppDao;", "", "insertApp", "", "appInfo", "Lcom/cn/board/database/AppInfo;", "(Lcom/cn/board/database/AppInfo;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertApps", "apps", "", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateApp", "deleteAppById", "id", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAllApps", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllApps", "getAppsByType", "isSystemApp", "", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "searchAppsByPackage", "packageNamePattern", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getRecentApps", "limit", "getMostUsedApps", "updateAppUsage", "lastUsedTime", "", "(IJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAppById", "getAppByPackageName", "packageName", "updateAppSortOrder", "sortOrder", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "database_debug"})
@androidx.room.Dao()
public abstract interface AppDao {
    
    @androidx.room.Insert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertApp(@org.jetbrains.annotations.NotNull()
    com.cn.board.database.AppInfo appInfo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertApps(@org.jetbrains.annotations.NotNull()
    java.util.List<com.cn.board.database.AppInfo> apps, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateApp(@org.jetbrains.annotations.NotNull()
    com.cn.board.database.AppInfo appInfo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM app_info WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAppById(int id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM app_info")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAllApps(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info ORDER BY sortOrder ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllApps(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.cn.board.database.AppInfo>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info WHERE isSystemApp = :isSystemApp ORDER BY sortOrder ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAppsByType(boolean isSystemApp, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.cn.board.database.AppInfo>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info WHERE packageName LIKE :packageNamePattern ORDER BY sortOrder ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object searchAppsByPackage(@org.jetbrains.annotations.NotNull()
    java.lang.String packageNamePattern, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.cn.board.database.AppInfo>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info ORDER BY lastUsedTime DESC LIMIT :limit")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getRecentApps(int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.cn.board.database.AppInfo>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info ORDER BY usageCount DESC LIMIT :limit")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMostUsedApps(int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.cn.board.database.AppInfo>> $completion);
    
    @androidx.room.Query(value = "UPDATE app_info SET lastUsedTime = :lastUsedTime, usageCount = usageCount + 1 WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateAppUsage(int id, long lastUsedTime, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAppById(int id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.cn.board.database.AppInfo> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM app_info WHERE packageName = :packageName")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAppByPackageName(@org.jetbrains.annotations.NotNull()
    java.lang.String packageName, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.cn.board.database.AppInfo> $completion);
    
    /**
     * 更新应用排序顺序
     * @param packageName 包名
     * @param sortOrder 排序顺序
     */
    @androidx.room.Query(value = "UPDATE app_info SET sortOrder = :sortOrder WHERE packageName = :packageName")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateAppSortOrder(@org.jetbrains.annotations.NotNull()
    java.lang.String packageName, int sortOrder, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}