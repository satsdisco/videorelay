package com.videorelay.app.data.db;

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
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class VideoDao_Impl implements VideoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VideoEntity> __insertionAdapterOfVideoEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public VideoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVideoEntity = new EntityInsertionAdapter<VideoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `videos` (`id`,`pubkey`,`title`,`summary`,`thumbnail`,`videoUrl`,`duration`,`durationSeconds`,`publishedAt`,`tags`,`zapCount`,`isShort`,`kind`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VideoEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPubkey());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getSummary());
        statement.bindString(5, entity.getThumbnail());
        statement.bindString(6, entity.getVideoUrl());
        statement.bindString(7, entity.getDuration());
        statement.bindLong(8, entity.getDurationSeconds());
        statement.bindLong(9, entity.getPublishedAt());
        statement.bindString(10, entity.getTags());
        statement.bindLong(11, entity.getZapCount());
        final int _tmp = entity.isShort() ? 1 : 0;
        statement.bindLong(12, _tmp);
        statement.bindLong(13, entity.getKind());
        statement.bindLong(14, entity.getCachedAt());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM videos WHERE cachedAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<VideoEntity> videos,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVideoEntity.insert(videos);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
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
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentVideos(final int limit,
      final Continuation<? super List<VideoEntity>> $completion) {
    final String _sql = "SELECT * FROM videos ORDER BY publishedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
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
  public Object getMostZapped(final int limit,
      final Continuation<? super List<VideoEntity>> $completion) {
    final String _sql = "SELECT * FROM videos ORDER BY zapCount DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
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
  public Object getShorts(final int limit,
      final Continuation<? super List<VideoEntity>> $completion) {
    final String _sql = "SELECT * FROM videos WHERE isShort = 1 ORDER BY publishedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
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
  public Object getByAuthors(final List<String> pubkeys, final int limit,
      final Continuation<? super List<VideoEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM videos WHERE pubkey IN (");
    final int _inputSize = pubkeys.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY publishedAt DESC LIMIT ");
    _stringBuilder.append("?");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 1 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : pubkeys) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    _argIndex = 1 + _inputSize;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item_1 = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
            _result.add(_item_1);
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
  public Object getByTag(final String tag, final int limit,
      final Continuation<? super List<VideoEntity>> $completion) {
    final String _sql = "SELECT * FROM videos WHERE tags LIKE '%' || ? || '%' ORDER BY publishedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tag);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VideoEntity>>() {
      @Override
      @NonNull
      public List<VideoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<VideoEntity> _result = new ArrayList<VideoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VideoEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
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
  public Object getById(final String id, final Continuation<? super VideoEntity> $completion) {
    final String _sql = "SELECT * FROM videos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VideoEntity>() {
      @Override
      @Nullable
      public VideoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfThumbnail = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnail");
          final int _cursorIndexOfVideoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "videoUrl");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfZapCount = CursorUtil.getColumnIndexOrThrow(_cursor, "zapCount");
          final int _cursorIndexOfIsShort = CursorUtil.getColumnIndexOrThrow(_cursor, "isShort");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final VideoEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            final String _tmpThumbnail;
            _tmpThumbnail = _cursor.getString(_cursorIndexOfThumbnail);
            final String _tmpVideoUrl;
            _tmpVideoUrl = _cursor.getString(_cursorIndexOfVideoUrl);
            final String _tmpDuration;
            _tmpDuration = _cursor.getString(_cursorIndexOfDuration);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final int _tmpZapCount;
            _tmpZapCount = _cursor.getInt(_cursorIndexOfZapCount);
            final boolean _tmpIsShort;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsShort);
            _tmpIsShort = _tmp != 0;
            final int _tmpKind;
            _tmpKind = _cursor.getInt(_cursorIndexOfKind);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _result = new VideoEntity(_tmpId,_tmpPubkey,_tmpTitle,_tmpSummary,_tmpThumbnail,_tmpVideoUrl,_tmpDuration,_tmpDurationSeconds,_tmpPublishedAt,_tmpTags,_tmpZapCount,_tmpIsShort,_tmpKind,_tmpCachedAt);
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
