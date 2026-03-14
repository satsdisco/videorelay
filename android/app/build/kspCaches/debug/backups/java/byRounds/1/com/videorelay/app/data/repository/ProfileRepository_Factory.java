package com.videorelay.app.data.repository;

import com.videorelay.app.data.db.ProfileDao;
import com.videorelay.app.data.nostr.RelayPool;
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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<ProfileDao> profileDaoProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  public ProfileRepository_Factory(Provider<RelayPool> relayPoolProvider,
      Provider<ProfileDao> profileDaoProvider, Provider<RelayRepository> relayRepositoryProvider) {
    this.relayPoolProvider = relayPoolProvider;
    this.profileDaoProvider = profileDaoProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(relayPoolProvider.get(), profileDaoProvider.get(), relayRepositoryProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<RelayPool> relayPoolProvider,
      Provider<ProfileDao> profileDaoProvider, Provider<RelayRepository> relayRepositoryProvider) {
    return new ProfileRepository_Factory(relayPoolProvider, profileDaoProvider, relayRepositoryProvider);
  }

  public static ProfileRepository newInstance(RelayPool relayPool, ProfileDao profileDao,
      RelayRepository relayRepository) {
    return new ProfileRepository(relayPool, profileDao, relayRepository);
  }
}
