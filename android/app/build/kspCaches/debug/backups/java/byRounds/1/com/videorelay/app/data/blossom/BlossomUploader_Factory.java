package com.videorelay.app.data.blossom;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class BlossomUploader_Factory implements Factory<BlossomUploader> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public BlossomUploader_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public BlossomUploader get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static BlossomUploader_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new BlossomUploader_Factory(okHttpClientProvider);
  }

  public static BlossomUploader newInstance(OkHttpClient okHttpClient) {
    return new BlossomUploader(okHttpClient);
  }
}
