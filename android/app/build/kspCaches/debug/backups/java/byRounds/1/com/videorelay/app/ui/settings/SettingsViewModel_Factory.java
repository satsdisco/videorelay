package com.videorelay.app.ui.settings;

import com.videorelay.app.data.nostr.AmberSigner;
import com.videorelay.app.data.repository.RelayRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<RelayRepository> relayRepositoryProvider;

  private final Provider<AmberSigner> amberSignerProvider;

  public SettingsViewModel_Factory(Provider<RelayRepository> relayRepositoryProvider,
      Provider<AmberSigner> amberSignerProvider) {
    this.relayRepositoryProvider = relayRepositoryProvider;
    this.amberSignerProvider = amberSignerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(relayRepositoryProvider.get(), amberSignerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<RelayRepository> relayRepositoryProvider,
      Provider<AmberSigner> amberSignerProvider) {
    return new SettingsViewModel_Factory(relayRepositoryProvider, amberSignerProvider);
  }

  public static SettingsViewModel newInstance(RelayRepository relayRepository,
      AmberSigner amberSigner) {
    return new SettingsViewModel(relayRepository, amberSigner);
  }
}
