package com.userhub.data.repository

import com.userhub.data.currentTimeMillis
import com.userhub.data.local.UserLocalDataSource
import com.userhub.data.remote.CreateUserRequest
import com.userhub.data.remote.GoRestApi
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val api: GoRestApi,
    private val localDataSource: UserLocalDataSource
) : UserRepository {

    override suspend fun getUsers(): UsersResult = withContext(Dispatchers.Default) {
        try {
            val users = api.fetchLastPageUsers().reversed()
            // persist for offline support
            localDataSource.saveUsers(users, currentTimeMillis())
            UsersResult.Success(users, lastSyncMillis = null)
        } catch (e: Exception) {
            val cached = localDataSource.getUsers()
            if (cached.isEmpty()) {
                UsersResult.NoInternet
            } else {
                val lastSync = cached.maxOfOrNull { it.cachedAt }
                UsersResult.Success(cached.map { it.user }, lastSyncMillis = lastSync)
            }
        }
    }

    override suspend fun createUser(name: String, email: String): HttpResponse =
        withContext(Dispatchers.Default) {
            api.createUser(
                CreateUserRequest(name = name, email = email, gender = "male", status = "active")
            )
        }

    override suspend fun deleteUser(id: Long): HttpResponse =
        withContext(Dispatchers.Default) {
            api.deleteUser(id)
        }
}
