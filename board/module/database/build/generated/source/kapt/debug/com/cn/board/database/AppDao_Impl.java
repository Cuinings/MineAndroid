package com.cn.board.database;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class AppDao_Impl implements AppDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<AppInfo> __insertAdapterOfAppInfo;

  private final EntityDeleteOrUpdateAdapter<AppInfo> __updateAdapterOfAppInfo;

  public AppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfAppInfo = new EntityInsertAdapter<AppInfo>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `app_info` (`id`,`isSystemApp`,`packageName`,`lastUsedTime`,`usageCount`,`sortOrder`,`appFlag`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final AppInfo entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.isSystemApp() ? 1 : 0;
        statement.bindLong(2, _tmp);
        if (entity.getPackageName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPackageName());
        }
        statement.bindLong(4, entity.getLastUsedTime());
        statement.bindLong(5, entity.getUsageCount());
        statement.bindLong(6, entity.getSortOrder());
        statement.bindLong(7, entity.getAppFlag());
      }
    };
    this.__updateAdapterOfAppInfo = new EntityDeleteOrUpdateAdapter<AppInfo>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `app_info` SET `id` = ?,`isSystemApp` = ?,`packageName` = ?,`lastUsedTime` = ?,`usageCount` = ?,`sortOrder` = ?,`appFlag` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final AppInfo entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.isSystemApp() ? 1 : 0;
        statement.bindLong(2, _tmp);
        if (entity.getPackageName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPackageName());
        }
        statement.bindLong(4, entity.getLastUsedTime());
        statement.bindLong(5, entity.getUsageCount());
        statement.bindLong(6, entity.getSortOrder());
        statement.bindLong(7, entity.getAppFlag());
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public Object insertApp(final AppInfo appInfo, final Continuation<? super Unit> $completion) {
    if (appInfo == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __insertAdapterOfAppInfo.insert(_connection, appInfo);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object insertApps(final List<AppInfo> apps, final Continuation<? super Unit> $completion) {
    if (apps == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __insertAdapterOfAppInfo.insert(_connection, apps);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object updateApp(final AppInfo appInfo, final Continuation<? super Unit> $completion) {
    if (appInfo == null) throw new NullPointerException();
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      __updateAdapterOfAppInfo.handle(_connection, appInfo);
      return Unit.INSTANCE;
    }, $completion);
  }

  @Override
  public Object getAllApps(final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info ORDER BY sortOrder ASC";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _item = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAppsByType(final boolean isSystemApp,
      final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE isSystemApp = ? ORDER BY sortOrder ASC";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        final int _tmp = isSystemApp ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp_1 != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _item = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object searchAppsByPackage(final String packageNamePattern,
      final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE packageName LIKE ? ORDER BY sortOrder ASC";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (packageNamePattern == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, packageNamePattern);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _item = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getRecentApps(final int limit,
      final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info ORDER BY lastUsedTime DESC LIMIT ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _item = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getMostUsedApps(final int limit,
      final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info ORDER BY usageCount DESC LIMIT ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, limit);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _item = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAppById(final int id, final Continuation<? super AppInfo> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE id = ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final AppInfo _result;
        if (_stmt.step()) {
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _result = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAppByPackageName(final String packageName,
      final Continuation<? super AppInfo> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE packageName = ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (packageName == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, packageName);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfIsSystemApp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "isSystemApp");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfLastUsedTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastUsedTime");
        final int _columnIndexOfUsageCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "usageCount");
        final int _columnIndexOfSortOrder = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "sortOrder");
        final int _columnIndexOfAppFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appFlag");
        final AppInfo _result;
        if (_stmt.step()) {
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final boolean _tmpIsSystemApp;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsSystemApp));
          _tmpIsSystemApp = _tmp != 0;
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final long _tmpLastUsedTime;
          _tmpLastUsedTime = _stmt.getLong(_columnIndexOfLastUsedTime);
          final int _tmpUsageCount;
          _tmpUsageCount = (int) (_stmt.getLong(_columnIndexOfUsageCount));
          final int _tmpSortOrder;
          _tmpSortOrder = (int) (_stmt.getLong(_columnIndexOfSortOrder));
          final int _tmpAppFlag;
          _tmpAppFlag = (int) (_stmt.getLong(_columnIndexOfAppFlag));
          _result = new AppInfo(_tmpId,_tmpIsSystemApp,_tmpPackageName,_tmpLastUsedTime,_tmpUsageCount,_tmpSortOrder,_tmpAppFlag);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object deleteAppById(final int id, final Continuation<? super Unit> $completion) {
    final String _sql = "DELETE FROM app_info WHERE id = ?";
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object deleteAllApps(final Continuation<? super Unit> $completion) {
    final String _sql = "DELETE FROM app_info";
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object updateAppUsage(final int id, final long lastUsedTime,
      final Continuation<? super Unit> $completion) {
    final String _sql = "UPDATE app_info SET lastUsedTime = ?, usageCount = usageCount + 1 WHERE id = ?";
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, lastUsedTime);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object updateAppSortOrder(final String packageName, final int sortOrder,
      final Continuation<? super Unit> $completion) {
    final String _sql = "UPDATE app_info SET sortOrder = ? WHERE packageName = ?";
    return DBUtil.performSuspending(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sortOrder);
        _argIndex = 2;
        if (packageName == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, packageName);
        }
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
