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

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(2, "f50d523bb2121c7e1814ff560d19ede6", "2866915ea0a61686931852f9e55e71fe") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `app_info` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isSystemApp` INTEGER NOT NULL, `packageName` TEXT NOT NULL, `lastUsedTime` INTEGER NOT NULL, `usageCount` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `appFlag` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_info_packageName` ON `app_info` (`packageName`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_app_info_lastUsedTime` ON `app_info` (`lastUsedTime`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_app_info_isSystemApp` ON `app_info` (`isSystemApp`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_app_info_usageCount` ON `app_info` (`usageCount`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_app_info_sortOrder` ON `app_info` (`sortOrder`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_app_info_appFlag` ON `app_info` (`appFlag`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f50d523bb2121c7e1814ff560d19ede6')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `app_info`");
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
        final Map<String, TableInfo.Column> _columnsAppInfo = new HashMap<String, TableInfo.Column>(7);
        _columnsAppInfo.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("isSystemApp", new TableInfo.Column("isSystemApp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("lastUsedTime", new TableInfo.Column("lastUsedTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("usageCount", new TableInfo.Column("usageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("appFlag", new TableInfo.Column("appFlag", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysAppInfo = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesAppInfo = new HashSet<TableInfo.Index>(6);
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_packageName", true, Arrays.asList("packageName"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_lastUsedTime", false, Arrays.asList("lastUsedTime"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_isSystemApp", false, Arrays.asList("isSystemApp"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_usageCount", false, Arrays.asList("usageCount"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_sortOrder", false, Arrays.asList("sortOrder"), Arrays.asList("ASC")));
        _indicesAppInfo.add(new TableInfo.Index("index_app_info_appFlag", false, Arrays.asList("appFlag"), Arrays.asList("ASC")));
        final TableInfo _infoAppInfo = new TableInfo("app_info", _columnsAppInfo, _foreignKeysAppInfo, _indicesAppInfo);
        final TableInfo _existingAppInfo = TableInfo.read(connection, "app_info");
        if (!_infoAppInfo.equals(_existingAppInfo)) {
          return new RoomOpenDelegate.ValidationResult(false, "app_info(com.cn.board.database.AppInfo).\n"
                  + " Expected:\n" + _infoAppInfo + "\n"
                  + " Found:\n" + _existingAppInfo);
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
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "app_info");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "app_info");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AppDao.class, AppDao_Impl.getRequiredConverters());
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
}
