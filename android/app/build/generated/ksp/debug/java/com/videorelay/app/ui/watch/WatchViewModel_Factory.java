package com.videorelay.app.ui.watch;

import com.videorelay.app.data.db.ViewHistoryDao;
import com.videorelay.app.data.nostr.RelayPool;
import com.videorelay.app.data.repository.ProfileRepository;
import com.videorelay.app.data.repository.RelayRepository;
import com.videorelay.app.data.repository.VideoRepository;
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
public final class WatchViewModel_Factory implements Factory<WatchViewModel> {
  private final Provider<VideoRepository> videoRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<ViewHistoryDao> viewHistoryDaoProvider;

  public WatchViewModel_Factory(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<RelayRepository> relayRepositoryProvider, Provider<RelayPool> relayPoolProvider,
      Provider<ViewHistoryDao> viewHistoryDaoProvider) {
    this.videoRepositoryProvider = videoRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
    this.relayPoolProvider = relayPoolProvider;
    this.viewHistoryDaoProvider = viewHistoryDaoProvider;
  }

  @Override
  public WatchViewModel get() {
    return newInstance(videoRepositoryProvider.get(), profileRepositoryProvider.get(), relayRepositoryProvider.get(), relayPoolProvider.get(), viewHistoryDaoProvider.get());
  }

  public static WatchViewModel_Factory create(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<RelayRepository> relayRepositoryProvider, Provider<RelayPool> relayPoolProvider,
      Provider<ViewHistoryDao> viewHistoryDaoProvider) {
    return new WatchViewModel_Factory(videoRepositoryProvider, profileRepositoryProvider, relayRepositoryProvider, relayPoolProvider, viewHistoryDaoProvider);
  }

  public static WatchViewModel newInstance(VideoRepository videoRepository,
      ProfileRepository profileRepository, RelayRepository relayRepository, RelayPool relayPool,
      ViewHistoryDao viewHistoryDao) {
    return new WatchViewModel(videoRepository, profileRepository, relayRepository, relayPool, viewHistoryDao);
  }
}
