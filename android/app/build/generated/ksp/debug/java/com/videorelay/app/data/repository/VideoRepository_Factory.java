package com.videorelay.app.data.repository;

import com.videorelay.app.data.db.VideoDao;
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
public final class VideoRepository_Factory implements Factory<VideoRepository> {
  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<VideoDao> videoDaoProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  public VideoRepository_Factory(Provider<RelayPool> relayPoolProvider,
      Provider<VideoDao> videoDaoProvider, Provider<RelayRepository> relayRepositoryProvider) {
    this.relayPoolProvider = relayPoolProvider;
    this.videoDaoProvider = videoDaoProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
  }

  @Override
  public VideoRepository get() {
    return newInstance(relayPoolProvider.get(), videoDaoProvider.get(), relayRepositoryProvider.get());
  }

  public static VideoRepository_Factory create(Provider<RelayPool> relayPoolProvider,
      Provider<VideoDao> videoDaoProvider, Provider<RelayRepository> relayRepositoryProvider) {
    return new VideoRepository_Factory(relayPoolProvider, videoDaoProvider, relayRepositoryProvider);
  }

  public static VideoRepository newInstance(RelayPool relayPool, VideoDao videoDao,
      RelayRepository relayRepository) {
    return new VideoRepository(relayPool, videoDao, relayRepository);
  }
}
