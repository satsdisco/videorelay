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
public final class ProfileDao_Impl implements ProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProfileEntity> __insertionAdapterOfProfileEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public ProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProfileEntity = new EntityInsertionAdapter<ProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `profiles` (`pubkey`,`name`,`displayName`,`picture`,`banner`,`about`,`lud16`,`lud06`,`nip05`,`fetchedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProfileEntity entity) {
        statement.bindString(1, entity.getPubkey());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDisplayName());
        statement.bindString(4, entity.getPicture());
        statement.bindString(5, entity.getBanner());
        statement.bindString(6, entity.getAbout());
        statement.bindString(7, entity.getLud16());
        statement.bindString(8, entity.getLud06());
        statement.bindString(9, entity.getNip05());
        statement.bindLong(10, entity.getFetchedAt());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM profiles WHERE fetchedAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<ProfileEntity> profiles,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProfileEntity.insert(profiles);
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
  public Object getByPubkey(final String pubkey,
      final Continuation<? super ProfileEntity> $completion) {
    final String _sql = "SELECT * FROM profiles WHERE pubkey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pubkey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProfileEntity>() {
      @Override
      @Nullable
      public ProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPicture = CursorUtil.getColumnIndexOrThrow(_cursor, "picture");
          final int _cursorIndexOfBanner = CursorUtil.getColumnIndexOrThrow(_cursor, "banner");
          final int _cursorIndexOfAbout = CursorUtil.getColumnIndexOrThrow(_cursor, "about");
          final int _cursorIndexOfLud16 = CursorUtil.getColumnIndexOrThrow(_cursor, "lud16");
          final int _cursorIndexOfLud06 = CursorUtil.getColumnIndexOrThrow(_cursor, "lud06");
          final int _cursorIndexOfNip05 = CursorUtil.getColumnIndexOrThrow(_cursor, "nip05");
          final int _cursorIndexOfFetchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "fetchedAt");
          final ProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpPicture;
            _tmpPicture = _cursor.getString(_cursorIndexOfPicture);
            final String _tmpBanner;
            _tmpBanner = _cursor.getString(_cursorIndexOfBanner);
            final String _tmpAbout;
            _tmpAbout = _cursor.getString(_cursorIndexOfAbout);
            final String _tmpLud16;
            _tmpLud16 = _cursor.getString(_cursorIndexOfLud16);
            final String _tmpLud06;
            _tmpLud06 = _cursor.getString(_cursorIndexOfLud06);
            final String _tmpNip05;
            _tmpNip05 = _cursor.getString(_cursorIndexOfNip05);
            final long _tmpFetchedAt;
            _tmpFetchedAt = _cursor.getLong(_cursorIndexOfFetchedAt);
            _result = new ProfileEntity(_tmpPubkey,_tmpName,_tmpDisplayName,_tmpPicture,_tmpBanner,_tmpAbout,_tmpLud16,_tmpLud06,_tmpNip05,_tmpFetchedAt);
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
  public Object getByPubkeys(final List<String> pubkeys,
      final Continuation<? super List<ProfileEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM profiles WHERE pubkey IN (");
    final int _inputSize = pubkeys.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : pubkeys) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProfileEntity>>() {
      @Override
      @NonNull
      public List<ProfileEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPubkey = CursorUtil.getColumnIndexOrThrow(_cursor, "pubkey");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPicture = CursorUtil.getColumnIndexOrThrow(_cursor, "picture");
          final int _cursorIndexOfBanner = CursorUtil.getColumnIndexOrThrow(_cursor, "banner");
          final int _cursorIndexOfAbout = CursorUtil.getColumnIndexOrThrow(_cursor, "about");
          final int _cursorIndexOfLud16 = CursorUtil.getColumnIndexOrThrow(_cursor, "lud16");
          final int _cursorIndexOfLud06 = CursorUtil.getColumnIndexOrThrow(_cursor, "lud06");
          final int _cursorIndexOfNip05 = CursorUtil.getColumnIndexOrThrow(_cursor, "nip05");
          final int _cursorIndexOfFetchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "fetchedAt");
          final List<ProfileEntity> _result = new ArrayList<ProfileEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProfileEntity _item_1;
            final String _tmpPubkey;
            _tmpPubkey = _cursor.getString(_cursorIndexOfPubkey);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpPicture;
            _tmpPicture = _cursor.getString(_cursorIndexOfPicture);
            final String _tmpBanner;
            _tmpBanner = _cursor.getString(_cursorIndexOfBanner);
            final String _tmpAbout;
            _tmpAbout = _cursor.getString(_cursorIndexOfAbout);
            final String _tmpLud16;
            _tmpLud16 = _cursor.getString(_cursorIndexOfLud16);
            final String _tmpLud06;
            _tmpLud06 = _cursor.getString(_cursorIndexOfLud06);
            final String _tmpNip05;
            _tmpNip05 = _cursor.getString(_cursorIndexOfNip05);
            final long _tmpFetchedAt;
            _tmpFetchedAt = _cursor.getLong(_cursorIndexOfFetchedAt);
            _item_1 = new ProfileEntity(_tmpPubkey,_tmpName,_tmpDisplayName,_tmpPicture,_tmpBanner,_tmpAbout,_tmpLud16,_tmpLud06,_tmpNip05,_tmpFetchedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
