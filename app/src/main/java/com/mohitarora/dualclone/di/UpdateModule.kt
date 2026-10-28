package com.mohitarora.dualclone.di

import com.mohitarora.dualclone.update.HttpTransport
import com.mohitarora.dualclone.update.UrlConnectionTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds abstract fun bindTransport(impl: UrlConnectionTransport): HttpTransport
}
