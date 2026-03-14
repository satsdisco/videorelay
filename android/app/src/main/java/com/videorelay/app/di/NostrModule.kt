package com.videorelay.app.di

import com.videorelay.app.data.nostr.RelayPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NostrModule {

    @Provides
    @Singleton
    fun provideRelayPool(okHttpClient: OkHttpClient): RelayPool {
        return RelayPool(okHttpClient)
    }
}
