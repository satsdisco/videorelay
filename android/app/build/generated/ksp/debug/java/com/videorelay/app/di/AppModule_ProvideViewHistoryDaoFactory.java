package com.videorelay.app.di;

import com.videorelay.app.data.db.AppDatabase;
import com.videorelay.app.data.db.ViewHistoryDao;
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
public final class AppModule_ProvideViewHistoryDaoFactory implements Factory<ViewHistoryDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideViewHistoryDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ViewHistoryDao get() {
    return provideViewHistoryDao(dbProvider.get());
  }

  public static AppModule_ProvideViewHistoryDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideViewHistoryDaoFactory(dbProvider);
  }

  public static ViewHistoryDao provideViewHistoryDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideViewHistoryDao(db));
  }
}
