package com.cn.board.database;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AppDao _appDao;

  private volatile CommonNodeDao _commonNodeDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(3, "82626bce9d0fce244f8405e05443dd98", "af9d96b72c0e0195ffd1a814d9b7bd17") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `app_info` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clazz` TEXT NOT NULL, `packageName` TEXT NOT NULL, `versionCode` TEXT NOT NULL, `versionName` TEXT NOT NULL, `appType` TEXT NOT NULL, `name` TEXT, `main` INTEGER NOT NULL, `mainIndex` INTEGER NOT NULL DEFAULT 0, `offlineMain` INTEGER NOT NULL, `offlineMainIndex` INTEGER NOT NULL DEFAULT 0)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_clazz` ON `app_info` (`clazz`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_packageName` ON `app_info` (`packageName`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_versionCode` ON `app_info` (`versionCode`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_versionName` ON `app_info` (`versionName`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_appType` ON `app_info` (`appType`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_name` ON `app_info` (`name`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_main` ON `app_info` (`main`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_mainIndex` ON `app_info` (`mainIndex`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_offlineMain` ON `app_info` (`offlineMain`)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_offlineMainIndex` ON `app_info` (`offlineMainIndex`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `common_node` (`id` TEXT NOT NULL, `nodeType` TEXT NOT NULL, `resourceType` INTEGER NOT NULL, `parentId` TEXT, `deptId` TEXT, `name` TEXT NOT NULL, `userId` TEXT, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_nodeType` ON `common_node` (`nodeType`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_resourceType` ON `common_node` (`resourceType`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_parentId` ON `common_node` (`parentId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_deptId` ON `common_node` (`deptId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_userId` ON `common_node` (`userId`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_resourceType_nodeType` ON `common_node` (`resourceType`, `nodeType`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_common_node_resourceType_deptId_nodeType` ON `common_node` (`resourceType`, `deptId`, `nodeType`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '82626bce9d0fce244f8405e05443dd98')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `app_info`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `common_node`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsAppInfo = new HashMap<String, TableInfo.Column>(11);
        _columnsAppInfo.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("clazz", new TableInfo.Column("clazz", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("versionCode", new TableInfo.Column("versionCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("versionName", new TableInfo.Column("versionName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("appType", new TableInfo.Column("appType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("main", new TableInfo.Column("main", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("mainIndex", new TableInfo.Column("mainIndex", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("offlineMain", new TableInfo.Column("offlineMain", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("offlineMainIndex", new TableInfo.Column("offlineMainIndex", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysAppInfo = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesAppInfo = new HashSet<TableInfo.Index>(10);
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_clazz", true, Arrays.asList("clazz"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_packageName", true, Arrays.asList("packageName"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_versionCode", true, Arrays.asList("versionCode"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_versionName", true, Arrays.asList("versionName"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_appType", true, Arrays.asList("appType"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_main", true, Arrays.asList("main"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_mainIndex", true, Arrays.asList("mainIndex"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_offlineMain", true, Arrays.asList("offlineMain"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_offlineMainIndex", true, Arrays.asList("offlineMainIndex"), Arrays.asList("ASC")));
        final TableInfo _infoAppInfo = new TableInfo("app_info", _columnsAppInfo, _foreignKeysAppInfo, _indicesAppInfo);
        final TableInfo _existingAppInfo = TableInfo.read(connection, "app_info");
        if (!_infoAppInfo.equals(_existingAppInfo)) {
          return new RoomOpenDelegate.ValidationResult(false, "app_info(com.cn.board.database.AppInfo).\n"
                  + " Expected:\n" + _infoAppInfo + "\n"
                  + " Found:\n" + _existingAppInfo);
        }
        final Map<String, TableInfo.Column> _columnsCommonNode = new HashMap<String, TableInfo.Column>(8);
        _columnsCommonNode.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("nodeType", new TableInfo.Column("nodeType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("resourceType", new TableInfo.Column("resourceType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("parentId", new TableInfo.Column("parentId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("deptId", new TableInfo.Column("deptId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("userId", new TableInfo.Column("userId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonNode.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCommonNode = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesCommonNode = new HashSet<TableInfo.Index>(7);
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_nodeType", false, Arrays.asList("nodeType"), Arrays.asList("ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_resourceType", false, Arrays.asList("resourceType"), Arrays.asList("ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_parentId", false, Arrays.asList("parentId"), Arrays.asList("ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_deptId", false, Arrays.asList("deptId"), Arrays.asList("ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_userId", false, Arrays.asList("userId"), Arrays.asList("ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_resourceType_nodeType", false, Arrays.asList("resourceType", "nodeType"), Arrays.asList("ASC", "ASC")));
        _indicesCommonNode.add(new TableInfo.Index("index_common_node_resourceType_deptId_nodeType", false, Arrays.asList("resourceType", "deptId", "nodeType"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoCommonNode = new TableInfo("common_node", _columnsCommonNode, _foreignKeysCommonNode, _indicesCommonNode);
        final TableInfo _existingCommonNode = TableInfo.read(connection, "common_node");
        if (!_infoCommonNode.equals(_existingCommonNode)) {
          return new RoomOpenDelegate.ValidationResult(false, "common_node(com.cn.board.database.CommonNode).\n"
                  + " Expected:\n" + _infoCommonNode + "\n"
                  + " Found:\n" + _existingCommonNode);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "app_info", "common_node");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "app_info", "common_node");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AppDao.class, AppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CommonNodeDao.class, CommonNodeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AppDao appDao() {
    if (_appDao != null) {
      return _appDao;
    } else {
      synchronized(this) {
        if(_appDao == null) {
          _appDao = new AppDao_Impl(this);
        }
        return _appDao;
      }
    }
  }

  @Override
  public CommonNodeDao commonNodeDao() {
    if (_commonNodeDao != null) {
      return _commonNodeDao;
    } else {
      synchronized(this) {
        if(_commonNodeDao == null) {
          _commonNodeDao = new CommonNodeDao_Impl(this);
        }
        return _commonNodeDao;
      }
    }
  }
}
