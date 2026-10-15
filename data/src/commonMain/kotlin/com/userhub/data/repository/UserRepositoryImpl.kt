package com.userhub.data.repository

import com.userhub.data.local.UserLocalDataSource
import com.userhub.data.remote.CreateUserRequest
import com.userhub.data.remote.GoRestApi
import com.userhub.data.remote.UserDto
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.time.Clock

class UserRepositoryImpl(
    private val api: GoRestApi,
    private val localDataSource: UserLocalDataSource,
    private val clock: Clock = Clock.System,
) : UserRepository {

    override suspend fun getUsers(): UsersResult = withContext(Dispatchers.Default) {
        try {
            val users = api.fetchLastPageUsers().reversed()
            // persist for offline support
            localDataSource.saveUsers(users, clock.now().toEpochMilliseconds())
            UsersResult.Success(users, lastSyncMillis = null, localDataSource.createdTimes())
        } catch (e: IOException) {
            fallbackToCache()
        } catch (e: ResponseException) {
            fallbackToCache()
        } catch (e: JsonConvertException) {
            fallbackToCache()
        }
    }

    private fun fallbackToCache(): UsersResult {
        val cached = localDataSource.getUsers()
        return if (cached.isEmpty()) {
            UsersResult.NoInternet
        } else {
            UsersResult.Success(
                cached.map { it.user },
                lastSyncMillis = cached.maxOf { it.cachedAt },
                createdAtMillis = localDataSource.createdTimes()
            )
        }
    }

    override suspend fun createUser(name: String, email: String): HttpResponse =
        withContext(Dispatchers.Default) {
            val response = api.createUser(
                CreateUserRequest(name = name, email = email, gender = "male", status = "active")
            )
            // Ktor keeps the body in memory, so the ViewModel can still read it after this
            localDataSource.markCreated(
                response.body<UserDto>().id,
                clock.now().toEpochMilliseconds()
            )
            response
        }

    override suspend fun deleteUser(id: Long): HttpResponse =
        withContext(Dispatchers.Default) {
            api.deleteUser(id).also { localDataSource.forgetCreated(id) }
        }
}
