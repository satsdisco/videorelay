package com.videorelay.app.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ViewHistoryDao_Impl implements ViewHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ViewHistoryEntity> __insertionAdapterOfViewHistoryEntity;

  public ViewHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfViewHistoryEntity = new EntityInsertionAdapter<ViewHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `view_history` (`videoId`,`watchedAt`,`watchedSeconds`,`totalSeconds`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ViewHistoryEntity entity) {
        statement.bindString(1, entity.getVideoId());
        statement.bindLong(2, entity.getWatchedAt());
        statement.bindLong(3, entity.getWatchedSeconds());
        statement.bindLong(4, entity.getTotalSeconds());
      }
    };
  }

  @Override
  public Object upsert(final ViewHistoryEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfViewHistoryEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecent(final int limit,
      final Continuation<? super List<ViewHistoryEntity>> $completion) {
    final String _sql = "SELECT * FROM view_history ORDER BY watchedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ViewHistoryEntity>>() {
      @Override
      @NonNull
      public List<ViewHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfWatchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedAt");
          final int _cursorIndexOfWatchedSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "watchedSeconds");
          final int _cursorIndexOfTotalSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "totalSeconds");
          final List<ViewHistoryEntity> _result = new ArrayList<ViewHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ViewHistoryEntity _item;
            final String _tmpVideoId;
            _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            final long _tmpWatchedAt;
            _tmpWatchedAt = _cursor.getLong(_cursorIndexOfWatchedAt);
            final int _tmpWatchedSeconds;
            _tmpWatchedSeconds = _cursor.getInt(_cursorIndexOfWatchedSeconds);
            final int _tmpTotalSeconds;
            _tmpTotalSeconds = _cursor.getInt(_cursorIndexOfTotalSeconds);
            _item = new ViewHistoryEntity(_tmpVideoId,_tmpWatchedAt,_tmpWatchedSeconds,_tmpTotalSeconds);
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
  public Object hasWatched(final String videoId, final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM view_history WHERE videoId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, videoId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
