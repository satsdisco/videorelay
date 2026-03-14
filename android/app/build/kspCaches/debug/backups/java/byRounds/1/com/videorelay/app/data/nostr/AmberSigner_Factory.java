package com.videorelay.app.data.nostr;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AmberSigner_Factory implements Factory<AmberSigner> {
  private final Provider<Context> contextProvider;

  public AmberSigner_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AmberSigner get() {
    return newInstance(contextProvider.get());
  }

  public static AmberSigner_Factory create(Provider<Context> contextProvider) {
    return new AmberSigner_Factory(contextProvider);
  }

  public static AmberSigner newInstance(Context context) {
    return new AmberSigner(context);
  }
}
