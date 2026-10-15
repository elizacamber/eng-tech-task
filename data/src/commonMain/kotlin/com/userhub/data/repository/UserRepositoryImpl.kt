package com.userhub.data.repository

import com.userhub.data.local.UserLocalDataSource
import com.userhub.data.remote.CreateUserRequest
import com.userhub.data.remote.GoRestApi
import com.userhub.data.remote.toDataError
import com.userhub.data.remote.toDomain
import com.userhub.domain.model.DataError
import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.repository.UserRepository
import com.userhub.domain.repository.UsersResult
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
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
            val users = api.fetchLastPageUsers().reversed().map { it.toDomain() }
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

    override suspend fun createUser(name: String, email: String): DataResult<User> =
        withContext(Dispatchers.Default) {
            try {
                val created = api.createUser(
                    CreateUserRequest(
                        name = name,
                        email = email,
                        gender = "male",
                        status = "active"
                    )
                ).toDomain()
                val now = clock.now().toEpochMilliseconds()
                // cache it too, so an offline relaunch still shows the new user
                localDataSource.addUser(created, now)
                localDataSource.markCreated(created.id, now)
                DataResult.Success(created)
            } catch (e: IOException) {
                DataResult.Failure(DataError.Offline)
            } catch (e: ResponseException) {
                DataResult.Failure(e.toDataError())
            } catch (e: JsonConvertException) {
                DataResult.Failure(DataError.UnreadableResponse)
            }
        }

    override suspend fun deleteUser(id: Long): DataResult<Unit> =
        withContext(Dispatchers.Default) {
            try {
                api.deleteUser(id)
                forgetLocally(id)
                DataResult.Success(Unit)
            } catch (e: IOException) {
                DataResult.Failure(DataError.Offline)
            } catch (e: ResponseException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    // already gone on the server, which is what the caller wanted
                    forgetLocally(id)
                    DataResult.Success(Unit)
                } else {
                    DataResult.Failure(e.toDataError())
                }
            }
        }

    private fun forgetLocally(id: Long) {
        localDataSource.removeUser(id)
        localDataSource.forgetCreated(id)
    }
}
