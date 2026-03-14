package com.videorelay.app.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile VideoDao _videoDao;

  private volatile DownloadDao _downloadDao;

  private volatile ViewHistoryDao _viewHistoryDao;

  private volatile ProfileDao _profileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `videos` (`id` TEXT NOT NULL, `pubkey` TEXT NOT NULL, `title` TEXT NOT NULL, `summary` TEXT NOT NULL, `thumbnail` TEXT NOT NULL, `videoUrl` TEXT NOT NULL, `duration` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, `publishedAt` INTEGER NOT NULL, `tags` TEXT NOT NULL, `zapCount` INTEGER NOT NULL, `isShort` INTEGER NOT NULL, `kind` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnail` TEXT NOT NULL, `videoUrl` TEXT NOT NULL, `localPath` TEXT NOT NULL, `status` TEXT NOT NULL, `progress` INTEGER NOT NULL, `fileSize` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `view_history` (`videoId` TEXT NOT NULL, `watchedAt` INTEGER NOT NULL, `watchedSeconds` INTEGER NOT NULL, `totalSeconds` INTEGER NOT NULL, PRIMARY KEY(`videoId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`pubkey` TEXT NOT NULL, `name` TEXT NOT NULL, `displayName` TEXT NOT NULL, `picture` TEXT NOT NULL, `banner` TEXT NOT NULL, `about` TEXT NOT NULL, `lud16` TEXT NOT NULL, `lud06` TEXT NOT NULL, `nip05` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`pubkey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b35f0b4286ffcf8d426459bf9e036827')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `videos`");
        db.execSQL("DROP TABLE IF EXISTS `downloads`");
        db.execSQL("DROP TABLE IF EXISTS `view_history`");
        db.execSQL("DROP TABLE IF EXISTS `profiles`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsVideos = new HashMap<String, TableInfo.Column>(14);
        _columnsVideos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("pubkey", new TableInfo.Column("pubkey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("thumbnail", new TableInfo.Column("thumbnail", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("videoUrl", new TableInfo.Column("videoUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("duration", new TableInfo.Column("duration", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("durationSeconds", new TableInfo.Column("durationSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("publishedAt", new TableInfo.Column("publishedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("zapCount", new TableInfo.Column("zapCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("isShort", new TableInfo.Column("isShort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("kind", new TableInfo.Column("kind", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVideos.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVideos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVideos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVideos = new TableInfo("videos", _columnsVideos, _foreignKeysVideos, _indicesVideos);
        final TableInfo _existingVideos = TableInfo.read(db, "videos");
        if (!_infoVideos.equals(_existingVideos)) {
          return new RoomOpenHelper.ValidationResult(false, "videos(com.videorelay.app.data.db.VideoEntity).\n"
                  + " Expected:\n" + _infoVideos + "\n"
                  + " Found:\n" + _existingVideos);
        }
        final HashMap<String, TableInfo.Column> _columnsDownloads = new HashMap<String, TableInfo.Column>(9);
        _columnsDownloads.put("videoId", new TableInfo.Column("videoId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("thumbnail", new TableInfo.Column("thumbnail", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("videoUrl", new TableInfo.Column("videoUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("localPath", new TableInfo.Column("localPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("progress", new TableInfo.Column("progress", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("fileSize", new TableInfo.Column("fileSize", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloads = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloads = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDownloads = new TableInfo("downloads", _columnsDownloads, _foreignKeysDownloads, _indicesDownloads);
        final TableInfo _existingDownloads = TableInfo.read(db, "downloads");
        if (!_infoDownloads.equals(_existingDownloads)) {
          return new RoomOpenHelper.ValidationResult(false, "downloads(com.videorelay.app.data.db.DownloadEntity).\n"
                  + " Expected:\n" + _infoDownloads + "\n"
                  + " Found:\n" + _existingDownloads);
        }
        final HashMap<String, TableInfo.Column> _columnsViewHistory = new HashMap<String, TableInfo.Column>(4);
        _columnsViewHistory.put("videoId", new TableInfo.Column("videoId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsViewHistory.put("watchedAt", new TableInfo.Column("watchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsViewHistory.put("watchedSeconds", new TableInfo.Column("watchedSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsViewHistory.put("totalSeconds", new TableInfo.Column("totalSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysViewHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesViewHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoViewHistory = new TableInfo("view_history", _columnsViewHistory, _foreignKeysViewHistory, _indicesViewHistory);
        final TableInfo _existingViewHistory = TableInfo.read(db, "view_history");
        if (!_infoViewHistory.equals(_existingViewHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "view_history(com.videorelay.app.data.db.ViewHistoryEntity).\n"
                  + " Expected:\n" + _infoViewHistory + "\n"
                  + " Found:\n" + _existingViewHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsProfiles = new HashMap<String, TableInfo.Column>(10);
        _columnsProfiles.put("pubkey", new TableInfo.Column("pubkey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("picture", new TableInfo.Column("picture", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("banner", new TableInfo.Column("banner", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("about", new TableInfo.Column("about", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("lud16", new TableInfo.Column("lud16", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("lud06", new TableInfo.Column("lud06", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("nip05", new TableInfo.Column("nip05", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("fetchedAt", new TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProfiles = new TableInfo("profiles", _columnsProfiles, _foreignKeysProfiles, _indicesProfiles);
        final TableInfo _existingProfiles = TableInfo.read(db, "profiles");
        if (!_infoProfiles.equals(_existingProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "profiles(com.videorelay.app.data.db.ProfileEntity).\n"
                  + " Expected:\n" + _infoProfiles + "\n"
                  + " Found:\n" + _existingProfiles);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b35f0b4286ffcf8d426459bf9e036827", "cce180fcfacccaf2029d6e88d7bdc38f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "videos","downloads","view_history","profiles");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `videos`");
      _db.execSQL("DELETE FROM `downloads`");
      _db.execSQL("DELETE FROM `view_history`");
      _db.execSQL("DELETE FROM `profiles`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(VideoDao.class, VideoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DownloadDao.class, DownloadDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ViewHistoryDao.class, ViewHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProfileDao.class, ProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
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
  public VideoDao videoDao() {
    if (_videoDao != null) {
      return _videoDao;
    } else {
      synchronized(this) {
        if(_videoDao == null) {
          _videoDao = new VideoDao_Impl(this);
        }
        return _videoDao;
      }
    }
  }

  @Override
  public DownloadDao downloadDao() {
    if (_downloadDao != null) {
      return _downloadDao;
    } else {
      synchronized(this) {
        if(_downloadDao == null) {
          _downloadDao = new DownloadDao_Impl(this);
        }
        return _downloadDao;
      }
    }
  }

  @Override
  public ViewHistoryDao viewHistoryDao() {
    if (_viewHistoryDao != null) {
      return _viewHistoryDao;
    } else {
      synchronized(this) {
        if(_viewHistoryDao == null) {
          _viewHistoryDao = new ViewHistoryDao_Impl(this);
        }
        return _viewHistoryDao;
      }
    }
  }

  @Override
  public ProfileDao profileDao() {
    if (_profileDao != null) {
      return _profileDao;
    } else {
      synchronized(this) {
        if(_profileDao == null) {
          _profileDao = new ProfileDao_Impl(this);
        }
        return _profileDao;
      }
    }
  }
}
