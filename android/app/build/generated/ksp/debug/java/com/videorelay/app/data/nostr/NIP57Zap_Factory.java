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
public final class NIP57Zap_Factory implements Factory<NIP57Zap> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public NIP57Zap_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public NIP57Zap get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static NIP57Zap_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new NIP57Zap_Factory(okHttpClientProvider);
  }

  public static NIP57Zap newInstance(OkHttpClient okHttpClient) {
    return new NIP57Zap(okHttpClient);
  }
}
