package com.videorelay.app.data.repository;

import com.videorelay.app.data.db.DownloadDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DownloadRepository_Factory implements Factory<DownloadRepository> {
  private final Provider<DownloadDao> downloadDaoProvider;

  public DownloadRepository_Factory(Provider<DownloadDao> downloadDaoProvider) {
    this.downloadDaoProvider = downloadDaoProvider;
  }

  @Override
  public DownloadRepository get() {
    return newInstance(downloadDaoProvider.get());
  }

  public static DownloadRepository_Factory create(Provider<DownloadDao> downloadDaoProvider) {
    return new DownloadRepository_Factory(downloadDaoProvider);
  }

  public static DownloadRepository newInstance(DownloadDao downloadDao) {
    return new DownloadRepository(downloadDao);
  }
}
