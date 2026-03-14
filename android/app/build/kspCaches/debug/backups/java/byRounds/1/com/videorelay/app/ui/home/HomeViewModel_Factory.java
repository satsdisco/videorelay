package com.videorelay.app.ui.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<VideoRepository> videoRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public HomeViewModel_Factory(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.videoRepositoryProvider = videoRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(videoRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<VideoRepository> videoRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new HomeViewModel_Factory(videoRepositoryProvider, profileRepositoryProvider);
  }

  public static HomeViewModel newInstance(VideoRepository videoRepository,
      ProfileRepository profileRepository) {
    return new HomeViewModel(videoRepository, profileRepository);
  }
}
