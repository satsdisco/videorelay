package com.videorelay.app.ui.shorts;

import com.videorelay.app.data.repository.ProfileRepository;
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
public final class ShortsViewModel_Factory implements Factory<ShortsViewModel> {
  private final Provider<VideoRepository> videoRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public ShortsViewModel_Factory(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.videoRepositoryProvider = videoRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public ShortsViewModel get() {
    return newInstance(videoRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static ShortsViewModel_Factory create(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new ShortsViewModel_Factory(videoRepositoryProvider, profileRepositoryProvider);
  }

  public static ShortsViewModel newInstance(VideoRepository videoRepository,
      ProfileRepository profileRepository) {
    return new ShortsViewModel(videoRepository, profileRepository);
  }
}
