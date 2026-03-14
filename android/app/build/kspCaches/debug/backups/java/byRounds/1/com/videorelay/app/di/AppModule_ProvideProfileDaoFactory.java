package com.videorelay.app.di;

import com.videorelay.app.data.db.AppDatabase;
import com.videorelay.app.data.db.ProfileDao;
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
public final class AppModule_ProvideProfileDaoFactory implements Factory<ProfileDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideProfileDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ProfileDao get() {
    return provideProfileDao(dbProvider.get());
  }

  public static AppModule_ProvideProfileDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideProfileDaoFactory(dbProvider);
  }

  public static ProfileDao provideProfileDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProfileDao(db));
  }
}
