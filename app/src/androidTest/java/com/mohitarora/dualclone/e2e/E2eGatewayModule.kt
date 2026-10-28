package com.mohitarora.dualclone.e2e

import com.mohitarora.dualclone.data.prefs.SharedPrefsAppSettings
import com.mohitarora.dualclone.di.BindingsModule
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// NOTE: BindingsModule is the production module that binds ShizukuGateway and AppSettings.
// NOTE: The test module binds the same two types, with UiAutomation shell access in place of Shizuku.
// NOTE: If BindingsModule changes, mirror its bindings here and nothing else.
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [BindingsModule::class])
abstract class E2eGatewayModule {
    @Binds abstract fun bindGateway(impl: UiAutomationShellGateway): ShizukuGateway
    @Binds abstract fun bindSettings(impl: SharedPrefsAppSettings): AppSettings
}
