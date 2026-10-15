package com.userhub.di

import com.userhub.data.db.UserDatabase
import com.userhub.data.local.SqlDelightUserLocalDataSource
import com.userhub.data.local.UserLocalDataSource
import com.userhub.data.remote.GoRestApi
import com.userhub.data.remote.createHttpClient
import com.userhub.data.repository.UserRepository
import com.userhub.data.repository.UserRepositoryImpl
import com.userhub.domain.usecase.AddUserUseCase
import com.userhub.domain.usecase.DeleteUserUseCase
import com.userhub.domain.usecase.GetUsersUseCase
import com.userhub.presentation.AddUserViewModel
import com.userhub.presentation.UserFeedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { createHttpClient(get()) }
    single { GoRestApi(get()) }
    single { UserDatabase(get()) }
    single<UserLocalDataSource> { SqlDelightUserLocalDataSource(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    factory { GetUsersUseCase(get()) }
    factory { AddUserUseCase(get()) }
    factory { DeleteUserUseCase(get()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    viewModel { UserFeedViewModel(get(), get(), get()) }
    viewModel { AddUserViewModel(get()) }
}
