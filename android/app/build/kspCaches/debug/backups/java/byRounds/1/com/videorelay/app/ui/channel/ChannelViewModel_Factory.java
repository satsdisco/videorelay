package com.videorelay.app.ui.channel;

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
public final class ChannelViewModel_Factory implements Factory<ChannelViewModel> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<VideoRepository> videoRepositoryProvider;

  public ChannelViewModel_Factory(Provider<ProfileRepository> profileRepositoryProvider,
      Provider<VideoRepository> videoRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.videoRepositoryProvider = videoRepositoryProvider;
  }

  @Override
  public ChannelViewModel get() {
    return newInstance(profileRepositoryProvider.get(), videoRepositoryProvider.get());
  }

  public static ChannelViewModel_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<VideoRepository> videoRepositoryProvider) {
    return new ChannelViewModel_Factory(profileRepositoryProvider, videoRepositoryProvider);
  }

  public static ChannelViewModel newInstance(ProfileRepository profileRepository,
      VideoRepository videoRepository) {
    return new ChannelViewModel(profileRepository, videoRepository);
  }
}
