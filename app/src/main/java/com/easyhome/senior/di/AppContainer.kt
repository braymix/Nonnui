package com.easyhome.senior.di

import android.content.Context
import com.easyhome.senior.EasyHomeApp
import com.easyhome.senior.data.apps.PackageManagerAppsRepository
import com.easyhome.senior.data.config.DataStoreConfigRepository
import com.easyhome.senior.data.device.AndroidDeviceStatusRepository
import com.easyhome.senior.domain.repository.AppsRepository
import com.easyhome.senior.domain.repository.ConfigRepository
import com.easyhome.senior.domain.repository.DeviceStatusRepository
import com.easyhome.senior.system.ActionLauncher
import com.easyhome.senior.system.ContactResolver
import com.easyhome.senior.system.FeedbackManager
import com.easyhome.senior.system.PermissionChecker

/** Dependency injection manuale: un unico grafo di singleton per processo. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val configRepository: ConfigRepository by lazy { DataStoreConfigRepository(appContext) }
    val appsRepository: AppsRepository by lazy { PackageManagerAppsRepository(appContext) }
    val deviceStatusRepository: DeviceStatusRepository by lazy { AndroidDeviceStatusRepository(appContext) }
    val feedbackManager: FeedbackManager by lazy { FeedbackManager(appContext) }
    val contactResolver: ContactResolver by lazy { ContactResolver(appContext) }
    val actionLauncher: ActionLauncher by lazy { ActionLauncher(appContext, contactResolver) }
    val permissionChecker: PermissionChecker by lazy { PermissionChecker(appContext) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as EasyHomeApp).container
