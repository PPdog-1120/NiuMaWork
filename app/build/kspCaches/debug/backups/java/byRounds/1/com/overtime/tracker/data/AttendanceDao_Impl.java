package com.overtime.tracker.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AttendanceDao_Impl implements AttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AttendanceRecord> __insertionAdapterOfAttendanceRecord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDate;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public AttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAttendanceRecord = new EntityInsertionAdapter<AttendanceRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `attendance_records` (`id`,`date`,`clockInTime`,`clockOutTime`,`isRestDay`,`overtimeMinutes`,`type`,`source`,`modificationHistory`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        if (entity.getClockInTime() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getClockInTime());
        }
        if (entity.getClockOutTime() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getClockOutTime());
        }
        final int _tmp = entity.isRestDay() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getOvertimeMinutes());
        statement.bindString(7, entity.getType());
        statement.bindString(8, entity.getSource());
        if (entity.getModificationHistory() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getModificationHistory());
        }
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteByDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM attendance_records WHERE date = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM attendance_records";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final AttendanceRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAttendanceRecord.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<AttendanceRecord> records,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAttendanceRecord.insert(records);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDate(final String date, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDate.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, date);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByDate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final String date,
      final Continuation<? super AttendanceRecord> $completion) {
    final String _sql = "SELECT * FROM attendance_records WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AttendanceRecord>() {
      @Override
      @Nullable
      public AttendanceRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final AttendanceRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<AttendanceRecord> getByDateFlow(final String date) {
    final String _sql = "SELECT * FROM attendance_records WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_records"}, new Callable<AttendanceRecord>() {
      @Override
      @Nullable
      public AttendanceRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final AttendanceRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getBetween(final String startDate, final String endDate,
      final Continuation<? super List<AttendanceRecord>> $completion) {
    final String _sql = "SELECT * FROM attendance_records WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AttendanceRecord>> getBetweenFlow(final String startDate, final String endDate) {
    final String _sql = "SELECT * FROM attendance_records WHERE date BETWEEN ? AND ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_records"}, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AttendanceRecord>> getAllFlow() {
    final String _sql = "SELECT * FROM attendance_records ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_records"}, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<AttendanceRecord>> $completion) {
    final String _sql = "SELECT * FROM attendance_records ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfClockInTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockInTime");
          final int _cursorIndexOfClockOutTime = CursorUtil.getColumnIndexOrThrow(_cursor, "clockOutTime");
          final int _cursorIndexOfIsRestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRestDay");
          final int _cursorIndexOfOvertimeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "overtimeMinutes");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfModificationHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "modificationHistory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpClockInTime;
            if (_cursor.isNull(_cursorIndexOfClockInTime)) {
              _tmpClockInTime = null;
            } else {
              _tmpClockInTime = _cursor.getString(_cursorIndexOfClockInTime);
            }
            final String _tmpClockOutTime;
            if (_cursor.isNull(_cursorIndexOfClockOutTime)) {
              _tmpClockOutTime = null;
            } else {
              _tmpClockOutTime = _cursor.getString(_cursorIndexOfClockOutTime);
            }
            final boolean _tmpIsRestDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRestDay);
            _tmpIsRestDay = _tmp != 0;
            final int _tmpOvertimeMinutes;
            _tmpOvertimeMinutes = _cursor.getInt(_cursorIndexOfOvertimeMinutes);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpModificationHistory;
            if (_cursor.isNull(_cursorIndexOfModificationHistory)) {
              _tmpModificationHistory = null;
            } else {
              _tmpModificationHistory = _cursor.getString(_cursorIndexOfModificationHistory);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceRecord(_tmpId,_tmpDate,_tmpClockInTime,_tmpClockOutTime,_tmpIsRestDay,_tmpOvertimeMinutes,_tmpType,_tmpSource,_tmpModificationHistory,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getOvertimeDaysCount(final String startDate, final String endDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM attendance_records WHERE date BETWEEN ? AND ? AND overtimeMinutes > 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecordDaysCount(final String startDate, final String endDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM attendance_records WHERE date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWorkdayOvertimeMinutes(final String startDate, final String endDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN ? AND ? AND type = 'WORKDAY_OVERTIME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRestDayOvertimeMinutes(final String startDate, final String endDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN ? AND ? AND type = 'REST_DAY_OVERTIME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTotalOvertimeMinutes(final String startDate, final String endDate,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(SUM(overtimeMinutes), 0) FROM attendance_records WHERE date BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
