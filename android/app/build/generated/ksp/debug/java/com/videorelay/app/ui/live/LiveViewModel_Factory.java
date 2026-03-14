package com.videorelay.app.ui.live;

import com.videorelay.app.data.nostr.RelayPool;
import com.videorelay.app.data.repository.ProfileRepository;
import com.videorelay.app.data.repository.RelayRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LiveViewModel_Factory implements Factory<LiveViewModel> {
  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public LiveViewModel_Factory(Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.relayPoolProvider = relayPoolProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public LiveViewModel get() {
    return newInstance(relayPoolProvider.get(), relayRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static LiveViewModel_Factory create(Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new LiveViewModel_Factory(relayPoolProvider, relayRepositoryProvider, profileRepositoryProvider);
  }

  public static LiveViewModel newInstance(RelayPool relayPool, RelayRepository relayRepository,
      ProfileRepository profileRepository) {
    return new LiveViewModel(relayPool, relayRepository, profileRepository);
  }
}
