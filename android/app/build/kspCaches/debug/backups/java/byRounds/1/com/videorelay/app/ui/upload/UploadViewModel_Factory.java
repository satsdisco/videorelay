package com.videorelay.app.ui.upload;

import android.content.Context;
import com.videorelay.app.data.blossom.BlossomUploader;
import com.videorelay.app.data.nostr.RelayPool;
import com.videorelay.app.data.repository.RelayRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class UploadViewModel_Factory implements Factory<UploadViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<BlossomUploader> blossomUploaderProvider;

  private final Provider<RelayPool> relayPoolProvider;

  private final Provider<RelayRepository> relayRepositoryProvider;

  public UploadViewModel_Factory(Provider<Context> contextProvider,
      Provider<BlossomUploader> blossomUploaderProvider, Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.blossomUploaderProvider = blossomUploaderProvider;
    this.relayPoolProvider = relayPoolProvider;
    this.relayRepositoryProvider = relayRepositoryProvider;
  }

  @Override
  public UploadViewModel get() {
    return newInstance(contextProvider.get(), blossomUploaderProvider.get(), relayPoolProvider.get(), relayRepositoryProvider.get());
  }

  public static UploadViewModel_Factory create(Provider<Context> contextProvider,
      Provider<BlossomUploader> blossomUploaderProvider, Provider<RelayPool> relayPoolProvider,
      Provider<RelayRepository> relayRepositoryProvider) {
    return new UploadViewModel_Factory(contextProvider, blossomUploaderProvider, relayPoolProvider, relayRepositoryProvider);
  }

  public static UploadViewModel newInstance(Context context, BlossomUploader blossomUploader,
      RelayPool relayPool, RelayRepository relayRepository) {
    return new UploadViewModel(context, blossomUploader, relayPool, relayRepository);
  }
}
