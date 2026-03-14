package com.videorelay.app.service;

import com.videorelay.app.data.repository.DownloadRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class DownloadService_MembersInjector implements MembersInjector<DownloadService> {
  private final Provider<DownloadRepository> downloadRepositoryProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public DownloadService_MembersInjector(Provider<DownloadRepository> downloadRepositoryProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.downloadRepositoryProvider = downloadRepositoryProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  public static MembersInjector<DownloadService> create(
      Provider<DownloadRepository> downloadRepositoryProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new DownloadService_MembersInjector(downloadRepositoryProvider, okHttpClientProvider);
  }

  @Override
  public void injectMembers(DownloadService instance) {
    injectDownloadRepository(instance, downloadRepositoryProvider.get());
    injectOkHttpClient(instance, okHttpClientProvider.get());
  }

  @InjectedFieldSignature("com.videorelay.app.service.DownloadService.downloadRepository")
  public static void injectDownloadRepository(DownloadService instance,
      DownloadRepository downloadRepository) {
    instance.downloadRepository = downloadRepository;
  }

  @InjectedFieldSignature("com.videorelay.app.service.DownloadService.okHttpClient")
  public static void injectOkHttpClient(DownloadService instance, OkHttpClient okHttpClient) {
    instance.okHttpClient = okHttpClient;
  }
}
