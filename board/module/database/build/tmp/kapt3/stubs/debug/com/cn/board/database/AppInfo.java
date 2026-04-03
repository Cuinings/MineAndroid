package com.cn.board.database;

/**
 * @author: cn
 * @time: 2026/2/9 17:12
 * @history
 * @description:应用信息数据类
 */
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u001c\b\u0087\b\u0018\u00002\u00020\u0001BM\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\f\u001a\u00020\u0003\u00a2\u0006\u0004\b\r\u0010\u000eJ\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\tH\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003JO\u0010 \u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\u00032\b\b\u0002\u0010\f\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010!\u001a\u00020\u00052\b\u0010\"\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010#\u001a\u00020\u0003H\u00d6\u0001J\t\u0010$\u001a\u00020\u0007H\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u0011R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\n\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0010R\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0010R\u0011\u0010\f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0010\u00a8\u0006%"}, d2 = {"Lcom/cn/board/database/AppInfo;", "", "id", "", "isSystemApp", "", "packageName", "", "lastUsedTime", "", "usageCount", "sortOrder", "appFlag", "<init>", "(IZLjava/lang/String;JIII)V", "getId", "()I", "()Z", "getPackageName", "()Ljava/lang/String;", "getLastUsedTime", "()J", "getUsageCount", "getSortOrder", "getAppFlag", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "toString", "database_debug"})
@androidx.room.Entity(tableName = "app_info", indices = {@androidx.room.Index(value = {"packageName"}, unique = true), @androidx.room.Index(value = {"lastUsedTime"}, unique = false), @androidx.room.Index(value = {"isSystemApp"}, unique = false), @androidx.room.Index(value = {"usageCount"}, unique = false), @androidx.room.Index(value = {"sortOrder"}, unique = false), @androidx.room.Index(value = {"appFlag"}, unique = false)})
public final class AppInfo {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final int id = 0;
    private final boolean isSystemApp = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String packageName = null;
    private final long lastUsedTime = 0L;
    private final int usageCount = 0;
    private final int sortOrder = 0;
    private final int appFlag = 0;
    
    public AppInfo(int id, boolean isSystemApp, @org.jetbrains.annotations.NotNull()
    java.lang.String packageName, long lastUsedTime, int usageCount, int sortOrder, int appFlag) {
        super();
    }
    
    public final int getId() {
        return 0;
    }
    
    public final boolean isSystemApp() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPackageName() {
        return null;
    }
    
    public final long getLastUsedTime() {
        return 0L;
    }
    
    public final int getUsageCount() {
        return 0;
    }
    
    public final int getSortOrder() {
        return 0;
    }
    
    public final int getAppFlag() {
        return 0;
    }
    
    public AppInfo() {
        super();
    }
    
    public final int component1() {
        return 0;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    public final long component4() {
        return 0L;
    }
    
    public final int component5() {
        return 0;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final int component7() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.cn.board.database.AppInfo copy(int id, boolean isSystemApp, @org.jetbrains.annotations.NotNull()
    java.lang.String packageName, long lastUsedTime, int usageCount, int sortOrder, int appFlag) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}