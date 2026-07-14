package com.cn.board.database;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.IllegalArgumentException;
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
        return "INSERT OR ABORT INTO `app_info` (`id`,`clazz`,`packageName`,`versionCode`,`versionName`,`appType`,`name`,`main`,`mainIndex`,`offlineMain`,`offlineMainIndex`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final AppInfo entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getClazz() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getClazz());
        }
        if (entity.getPackageName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPackageName());
        }
        if (entity.getVersionCode() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVersionCode());
        }
        if (entity.getVersionName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getVersionName());
        }
        statement.bindText(6, __EmAppType_enumToString(entity.getAppType()));
        if (entity.getName() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getName());
        }
        statement.bindLong(8, entity.getMain());
        statement.bindLong(9, entity.getMainIndex());
        statement.bindLong(10, entity.getOfflineMain());
        statement.bindLong(11, entity.getOfflineMainIndex());
      }
    };
    this.__updateAdapterOfAppInfo = new EntityDeleteOrUpdateAdapter<AppInfo>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `app_info` SET `id` = ?,`clazz` = ?,`packageName` = ?,`versionCode` = ?,`versionName` = ?,`appType` = ?,`name` = ?,`main` = ?,`mainIndex` = ?,`offlineMain` = ?,`offlineMainIndex` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final AppInfo entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getClazz() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getClazz());
        }
        if (entity.getPackageName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPackageName());
        }
        if (entity.getVersionCode() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVersionCode());
        }
        if (entity.getVersionName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getVersionName());
        }
        statement.bindText(6, __EmAppType_enumToString(entity.getAppType()));
        if (entity.getName() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getName());
        }
        statement.bindLong(8, entity.getMain());
        statement.bindLong(9, entity.getMainIndex());
        statement.bindLong(10, entity.getOfflineMain());
        statement.bindLong(11, entity.getOfflineMainIndex());
        statement.bindLong(12, entity.getId());
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
    final String _sql = "SELECT * FROM app_info";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfClazz = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "clazz");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfVersionCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionCode");
        final int _columnIndexOfVersionName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionName");
        final int _columnIndexOfAppType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appType");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "main");
        final int _columnIndexOfMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mainIndex");
        final int _columnIndexOfOfflineMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMain");
        final int _columnIndexOfOfflineMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMainIndex");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final String _tmpClazz;
          if (_stmt.isNull(_columnIndexOfClazz)) {
            _tmpClazz = null;
          } else {
            _tmpClazz = _stmt.getText(_columnIndexOfClazz);
          }
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final String _tmpVersionCode;
          if (_stmt.isNull(_columnIndexOfVersionCode)) {
            _tmpVersionCode = null;
          } else {
            _tmpVersionCode = _stmt.getText(_columnIndexOfVersionCode);
          }
          final String _tmpVersionName;
          if (_stmt.isNull(_columnIndexOfVersionName)) {
            _tmpVersionName = null;
          } else {
            _tmpVersionName = _stmt.getText(_columnIndexOfVersionName);
          }
          final EmAppType _tmpAppType;
          _tmpAppType = __EmAppType_stringToEnum(_stmt.getText(_columnIndexOfAppType));
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final int _tmpMain;
          _tmpMain = (int) (_stmt.getLong(_columnIndexOfMain));
          final int _tmpMainIndex;
          _tmpMainIndex = (int) (_stmt.getLong(_columnIndexOfMainIndex));
          final int _tmpOfflineMain;
          _tmpOfflineMain = (int) (_stmt.getLong(_columnIndexOfOfflineMain));
          final int _tmpOfflineMainIndex;
          _tmpOfflineMainIndex = (int) (_stmt.getLong(_columnIndexOfOfflineMainIndex));
          _item = new AppInfo(_tmpId,_tmpClazz,_tmpPackageName,_tmpVersionCode,_tmpVersionName,_tmpAppType,_tmpName,_tmpMain,_tmpMainIndex,_tmpOfflineMain,_tmpOfflineMainIndex);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAppById(final int id, final Continuation<? super List<AppInfo>> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE id = ?";
    return DBUtil.performSuspending(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfClazz = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "clazz");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfVersionCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionCode");
        final int _columnIndexOfVersionName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionName");
        final int _columnIndexOfAppType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appType");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "main");
        final int _columnIndexOfMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mainIndex");
        final int _columnIndexOfOfflineMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMain");
        final int _columnIndexOfOfflineMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMainIndex");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final String _tmpClazz;
          if (_stmt.isNull(_columnIndexOfClazz)) {
            _tmpClazz = null;
          } else {
            _tmpClazz = _stmt.getText(_columnIndexOfClazz);
          }
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final String _tmpVersionCode;
          if (_stmt.isNull(_columnIndexOfVersionCode)) {
            _tmpVersionCode = null;
          } else {
            _tmpVersionCode = _stmt.getText(_columnIndexOfVersionCode);
          }
          final String _tmpVersionName;
          if (_stmt.isNull(_columnIndexOfVersionName)) {
            _tmpVersionName = null;
          } else {
            _tmpVersionName = _stmt.getText(_columnIndexOfVersionName);
          }
          final EmAppType _tmpAppType;
          _tmpAppType = __EmAppType_stringToEnum(_stmt.getText(_columnIndexOfAppType));
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final int _tmpMain;
          _tmpMain = (int) (_stmt.getLong(_columnIndexOfMain));
          final int _tmpMainIndex;
          _tmpMainIndex = (int) (_stmt.getLong(_columnIndexOfMainIndex));
          final int _tmpOfflineMain;
          _tmpOfflineMain = (int) (_stmt.getLong(_columnIndexOfOfflineMain));
          final int _tmpOfflineMainIndex;
          _tmpOfflineMainIndex = (int) (_stmt.getLong(_columnIndexOfOfflineMainIndex));
          _item = new AppInfo(_tmpId,_tmpClazz,_tmpPackageName,_tmpVersionCode,_tmpVersionName,_tmpAppType,_tmpName,_tmpMain,_tmpMainIndex,_tmpOfflineMain,_tmpOfflineMainIndex);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    }, $completion);
  }

  @Override
  public Object getAppByPackageName(final String packageName,
      final Continuation<? super List<AppInfo>> $completion) {
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
        final int _columnIndexOfClazz = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "clazz");
        final int _columnIndexOfPackageName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "packageName");
        final int _columnIndexOfVersionCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionCode");
        final int _columnIndexOfVersionName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "versionName");
        final int _columnIndexOfAppType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appType");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "main");
        final int _columnIndexOfMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mainIndex");
        final int _columnIndexOfOfflineMain = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMain");
        final int _columnIndexOfOfflineMainIndex = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "offlineMainIndex");
        final List<AppInfo> _result = new ArrayList<AppInfo>();
        while (_stmt.step()) {
          final AppInfo _item;
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          final String _tmpClazz;
          if (_stmt.isNull(_columnIndexOfClazz)) {
            _tmpClazz = null;
          } else {
            _tmpClazz = _stmt.getText(_columnIndexOfClazz);
          }
          final String _tmpPackageName;
          if (_stmt.isNull(_columnIndexOfPackageName)) {
            _tmpPackageName = null;
          } else {
            _tmpPackageName = _stmt.getText(_columnIndexOfPackageName);
          }
          final String _tmpVersionCode;
          if (_stmt.isNull(_columnIndexOfVersionCode)) {
            _tmpVersionCode = null;
          } else {
            _tmpVersionCode = _stmt.getText(_columnIndexOfVersionCode);
          }
          final String _tmpVersionName;
          if (_stmt.isNull(_columnIndexOfVersionName)) {
            _tmpVersionName = null;
          } else {
            _tmpVersionName = _stmt.getText(_columnIndexOfVersionName);
          }
          final EmAppType _tmpAppType;
          _tmpAppType = __EmAppType_stringToEnum(_stmt.getText(_columnIndexOfAppType));
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final int _tmpMain;
          _tmpMain = (int) (_stmt.getLong(_columnIndexOfMain));
          final int _tmpMainIndex;
          _tmpMainIndex = (int) (_stmt.getLong(_columnIndexOfMainIndex));
          final int _tmpOfflineMain;
          _tmpOfflineMain = (int) (_stmt.getLong(_columnIndexOfOfflineMain));
          final int _tmpOfflineMainIndex;
          _tmpOfflineMainIndex = (int) (_stmt.getLong(_columnIndexOfOfflineMainIndex));
          _item = new AppInfo(_tmpId,_tmpClazz,_tmpPackageName,_tmpVersionCode,_tmpVersionName,_tmpAppType,_tmpName,_tmpMain,_tmpMainIndex,_tmpOfflineMain,_tmpOfflineMainIndex);
          _result.add(_item);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __EmAppType_enumToString(@NonNull final EmAppType _value) {
    switch (_value) {
      case System: return "System";
      case tp: return "tp";
      case Third: return "Third";
      case Add: return "Add";
      case NONE: return "NONE";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private EmAppType __EmAppType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "System": return EmAppType.System;
      case "tp": return EmAppType.tp;
      case "Third": return EmAppType.Third;
      case "Add": return EmAppType.Add;
      case "NONE": return EmAppType.NONE;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
