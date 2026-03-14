package com.videorelay.app.di;

import com.videorelay.app.data.nostr.RelayPool;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NostrModule_ProvideRelayPoolFactory implements Factory<RelayPool> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public NostrModule_ProvideRelayPoolFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public RelayPool get() {
    return provideRelayPool(okHttpClientProvider.get());
  }

  public static NostrModule_ProvideRelayPoolFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new NostrModule_ProvideRelayPoolFactory(okHttpClientProvider);
  }

  public static RelayPool provideRelayPool(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(NostrModule.INSTANCE.provideRelayPool(okHttpClient));
  }
}
