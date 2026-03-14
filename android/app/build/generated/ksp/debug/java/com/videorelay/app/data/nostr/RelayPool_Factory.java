package com.videorelay.app.data.nostr;

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
public final class RelayPool_Factory implements Factory<RelayPool> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public RelayPool_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public RelayPool get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static RelayPool_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new RelayPool_Factory(okHttpClientProvider);
  }

  public static RelayPool newInstance(OkHttpClient okHttpClient) {
    return new RelayPool(okHttpClient);
  }
}
