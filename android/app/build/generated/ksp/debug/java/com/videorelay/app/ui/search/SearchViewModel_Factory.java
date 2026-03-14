package com.videorelay.app.ui.search;

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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public SearchViewModel_Factory(Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.relayPoolProvider = relayPoolProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(relayPoolProvider.get(), relayRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new SearchViewModel_Factory(relayPoolProvider, relayRepositoryProvider, profileRepositoryProvider);
  }

  public static SearchViewModel newInstance(RelayPool relayPool, RelayRepository relayRepository,
      ProfileRepository profileRepository) {
    return new SearchViewModel(relayPool, relayRepository, profileRepository);
  }
}
