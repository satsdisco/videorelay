package com.videorelay.app.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DownloadDao_Impl implements DownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DownloadEntity> __insertionAdapterOfDownloadEntity;

  private final EntityDeletionOrUpdateAdapter<DownloadEntity> __deletionAdapterOfDownloadEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  public DownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDownloadEntity = new EntityInsertionAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `downloads` (`videoId`,`title`,`thumbnail`,`videoUrl`,`localPath`,`status`,`progress`,`fileSize`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindString(1, entity.getVideoId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getThumbnail());
        statement.bindString(4, entity.getVideoUrl());
        statement.bindString(5, entity.getLocalPath());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getProgress());
        statement.bindLong(8, entity.getFileSize());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfDownloadEntity = new EntityDeletionOrUpdateAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `downloads` WHERE `videoId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindString(1, entity.getVideoId());
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE downloads SET status = ?, progress = ?, localPath = ? WHERE videoId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DownloadEntity download,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDownloadEntity.insert(download);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final DownloadEntity download,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDownloadEntity.handle(download);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final String videoId, final String status, final int progress,
      final String localPath, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, progress);
        _argIndex = 3;
        _stmt.bindString(_argIndex, localPath);
        _argIndex = 4;
        _stmt.bindString(_argIndex, videoId);
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
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DownloadEntity>> observeAll() {
    final String _sql = "SELECT * FROM downloads ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<List<DownloadEntity>>() {
      @Override
      @NonNull
      public List<DownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<DownloadEntity> _result = new ArrayList<DownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadEntity _item;
            final String _tmpVideoId;
            _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpLocalPath;
            _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new DownloadEntity(_tmpVideoId,_tmpTitle,_tmpThumbnail,_tmpVideoUrl,_tmpLocalPath,_tmpStatus,_tmpProgress,_tmpFileSize,_tmpCreatedAt);
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
  public Object getCompleted(final Continuation<? super List<DownloadEntity>> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE status = 'complete' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DownloadEntity>>() {
      @Override
      @NonNull
      public List<DownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<DownloadEntity> _result = new ArrayList<DownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadEntity _item;
            final String _tmpVideoId;
            _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpLocalPath;
            _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new DownloadEntity(_tmpVideoId,_tmpTitle,_tmpThumbnail,_tmpVideoUrl,_tmpLocalPath,_tmpStatus,_tmpProgress,_tmpFileSize,_tmpCreatedAt);
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
  public Object getByVideoId(final String videoId,
      final Continuation<? super DownloadEntity> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE videoId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, videoId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadEntity>() {
      @Override
      @Nullable
      public DownloadEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "localPath");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final DownloadEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpVideoId;
            _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpLocalPath;
            _tmpLocalPath = _cursor.getString(_cursorIndexOfLocalPath);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new DownloadEntity(_tmpVideoId,_tmpTitle,_tmpThumbnail,_tmpVideoUrl,_tmpLocalPath,_tmpStatus,_tmpProgress,_tmpFileSize,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
