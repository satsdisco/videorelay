package com.videorelay.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.videorelay.app.data.blossom.BlossomUploader;
import com.videorelay.app.data.db.AppDatabase;
import com.videorelay.app.data.db.DownloadDao;
import com.videorelay.app.data.db.ProfileDao;
import com.videorelay.app.data.db.VideoDao;
import com.videorelay.app.data.db.ViewHistoryDao;
import com.videorelay.app.data.nostr.AmberSigner;
import com.videorelay.app.data.nostr.RelayPool;
import com.videorelay.app.data.repository.DownloadRepository;
import com.videorelay.app.data.repository.ProfileRepository;
import com.videorelay.app.data.repository.RelayRepository;
import com.videorelay.app.data.repository.VideoRepository;
import com.videorelay.app.di.AppModule_ProvideDatabaseFactory;
import com.videorelay.app.di.AppModule_ProvideDownloadDaoFactory;
import com.videorelay.app.di.AppModule_ProvideOkHttpClientFactory;
import com.videorelay.app.di.AppModule_ProvideProfileDaoFactory;
import com.videorelay.app.di.AppModule_ProvideVideoDaoFactory;
import com.videorelay.app.di.AppModule_ProvideViewHistoryDaoFactory;
import com.videorelay.app.di.NostrModule_ProvideRelayPoolFactory;
import com.videorelay.app.service.DownloadService;
import com.videorelay.app.service.DownloadService_MembersInjector;
import com.videorelay.app.ui.channel.ChannelViewModel;
import com.videorelay.app.ui.channel.ChannelViewModel_HiltModules;
import com.videorelay.app.ui.channel.ChannelViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.channel.ChannelViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.downloads.DownloadsViewModel;
import com.videorelay.app.ui.downloads.DownloadsViewModel_HiltModules;
import com.videorelay.app.ui.downloads.DownloadsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.downloads.DownloadsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.home.HomeViewModel;
import com.videorelay.app.ui.home.HomeViewModel_HiltModules;
import com.videorelay.app.ui.home.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.home.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.live.LiveViewModel;
import com.videorelay.app.ui.live.LiveViewModel_HiltModules;
import com.videorelay.app.ui.live.LiveViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.live.LiveViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.search.SearchViewModel;
import com.videorelay.app.ui.search.SearchViewModel_HiltModules;
import com.videorelay.app.ui.search.SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.search.SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.settings.SettingsViewModel;
import com.videorelay.app.ui.settings.SettingsViewModel_HiltModules;
import com.videorelay.app.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.shorts.ShortsViewModel;
import com.videorelay.app.ui.shorts.ShortsViewModel_HiltModules;
import com.videorelay.app.ui.shorts.ShortsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.shorts.ShortsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.upload.UploadViewModel;
import com.videorelay.app.ui.upload.UploadViewModel_HiltModules;
import com.videorelay.app.ui.upload.UploadViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.upload.UploadViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.videorelay.app.ui.watch.WatchViewModel;
import com.videorelay.app.ui.watch.WatchViewModel_HiltModules;
import com.videorelay.app.ui.watch.WatchViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.videorelay.app.ui.watch.WatchViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

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
public final class DaggerVideoRelayApp_HiltComponents_SingletonC {
  private DaggerVideoRelayApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public VideoRelayApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements VideoRelayApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements VideoRelayApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements VideoRelayApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements VideoRelayApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements VideoRelayApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements VideoRelayApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements VideoRelayApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public VideoRelayApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends VideoRelayApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends VideoRelayApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends VideoRelayApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends VideoRelayApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(9).put(ChannelViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChannelViewModel_HiltModules.KeyModule.provide()).put(DownloadsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DownloadsViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(LiveViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, LiveViewModel_HiltModules.KeyModule.provide()).put(SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SearchViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(ShortsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ShortsViewModel_HiltModules.KeyModule.provide()).put(UploadViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, UploadViewModel_HiltModules.KeyModule.provide()).put(WatchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, WatchViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends VideoRelayApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ChannelViewModel> channelViewModelProvider;

    private Provider<DownloadsViewModel> downloadsViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LiveViewModel> liveViewModelProvider;

    private Provider<SearchViewModel> searchViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<ShortsViewModel> shortsViewModelProvider;

    private Provider<UploadViewModel> uploadViewModelProvider;

    private Provider<WatchViewModel> watchViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.channelViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.downloadsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.liveViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.searchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.shortsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.uploadViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.watchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(9).put(ChannelViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) channelViewModelProvider)).put(DownloadsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) downloadsViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(LiveViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) liveViewModelProvider)).put(SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) searchViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(ShortsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) shortsViewModelProvider)).put(UploadViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) uploadViewModelProvider)).put(WatchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) watchViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.videorelay.app.ui.channel.ChannelViewModel 
          return (T) new ChannelViewModel(singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.videoRepositoryProvider.get());

          case 1: // com.videorelay.app.ui.downloads.DownloadsViewModel 
          return (T) new DownloadsViewModel(singletonCImpl.downloadRepositoryProvider.get());

          case 2: // com.videorelay.app.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.videoRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 3: // com.videorelay.app.ui.live.LiveViewModel 
          return (T) new LiveViewModel(singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.relayRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 4: // com.videorelay.app.ui.search.SearchViewModel 
          return (T) new SearchViewModel(singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.relayRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 5: // com.videorelay.app.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.relayRepositoryProvider.get(), singletonCImpl.amberSignerProvider.get());

          case 6: // com.videorelay.app.ui.shorts.ShortsViewModel 
          return (T) new ShortsViewModel(singletonCImpl.videoRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 7: // com.videorelay.app.ui.upload.UploadViewModel 
          return (T) new UploadViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.blossomUploaderProvider.get(), singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.relayRepositoryProvider.get());

          case 8: // com.videorelay.app.ui.watch.WatchViewModel 
          return (T) new WatchViewModel(singletonCImpl.videoRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.relayRepositoryProvider.get(), singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.viewHistoryDao());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends VideoRelayApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends VideoRelayApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectDownloadService(DownloadService downloadService) {
      injectDownloadService2(downloadService);
    }

    private DownloadService injectDownloadService2(DownloadService instance) {
      DownloadService_MembersInjector.injectDownloadRepository(instance, singletonCImpl.downloadRepositoryProvider.get());
      DownloadService_MembersInjector.injectOkHttpClient(instance, singletonCImpl.provideOkHttpClientProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends VideoRelayApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<RelayPool> provideRelayPoolProvider;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<RelayRepository> relayRepositoryProvider;

    private Provider<ProfileRepository> profileRepositoryProvider;

    private Provider<VideoRepository> videoRepositoryProvider;

    private Provider<DownloadRepository> downloadRepositoryProvider;

    private Provider<AmberSigner> amberSignerProvider;

    private Provider<BlossomUploader> blossomUploaderProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ProfileDao profileDao() {
      return AppModule_ProvideProfileDaoFactory.provideProfileDao(provideDatabaseProvider.get());
    }

    private VideoDao videoDao() {
      return AppModule_ProvideVideoDaoFactory.provideVideoDao(provideDatabaseProvider.get());
    }

    private DownloadDao downloadDao() {
      return AppModule_ProvideDownloadDaoFactory.provideDownloadDao(provideDatabaseProvider.get());
    }

    private ViewHistoryDao viewHistoryDao() {
      return AppModule_ProvideViewHistoryDaoFactory.provideViewHistoryDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 2));
      this.provideRelayPoolProvider = DoubleCheck.provider(new SwitchingProvider<RelayPool>(singletonCImpl, 1));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 3));
      this.relayRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<RelayRepository>(singletonCImpl, 4));
      this.profileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 0));
      this.videoRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<VideoRepository>(singletonCImpl, 5));
      this.downloadRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DownloadRepository>(singletonCImpl, 6));
      this.amberSignerProvider = DoubleCheck.provider(new SwitchingProvider<AmberSigner>(singletonCImpl, 7));
      this.blossomUploaderProvider = DoubleCheck.provider(new SwitchingProvider<BlossomUploader>(singletonCImpl, 8));
    }

    @Override
    public void injectVideoRelayApp(VideoRelayApp videoRelayApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.videorelay.app.data.repository.ProfileRepository 
          return (T) new ProfileRepository(singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.profileDao(), singletonCImpl.relayRepositoryProvider.get());

          case 1: // com.videorelay.app.data.nostr.RelayPool 
          return (T) NostrModule_ProvideRelayPoolFactory.provideRelayPool(singletonCImpl.provideOkHttpClientProvider.get());

          case 2: // okhttp3.OkHttpClient 
          return (T) AppModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 3: // com.videorelay.app.data.db.AppDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.videorelay.app.data.repository.RelayRepository 
          return (T) new RelayRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.videorelay.app.data.repository.VideoRepository 
          return (T) new VideoRepository(singletonCImpl.provideRelayPoolProvider.get(), singletonCImpl.videoDao(), singletonCImpl.relayRepositoryProvider.get());

          case 6: // com.videorelay.app.data.repository.DownloadRepository 
          return (T) new DownloadRepository(singletonCImpl.downloadDao());

          case 7: // com.videorelay.app.data.nostr.AmberSigner 
          return (T) new AmberSigner(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.videorelay.app.data.blossom.BlossomUploader 
          return (T) new BlossomUploader(singletonCImpl.provideOkHttpClientProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
