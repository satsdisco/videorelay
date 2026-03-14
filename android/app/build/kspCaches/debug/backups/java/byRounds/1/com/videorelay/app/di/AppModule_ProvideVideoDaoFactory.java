package com.videorelay.app.di;

import com.videorelay.app.data.db.AppDatabase;
import com.videorelay.app.data.db.VideoDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppModule_ProvideVideoDaoFactory implements Factory<VideoDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideVideoDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public VideoDao get() {
    return provideVideoDao(dbProvider.get());
  }

  public static AppModule_ProvideVideoDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideVideoDaoFactory(dbProvider);
  }

  public static VideoDao provideVideoDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideVideoDao(db));
  }
}
