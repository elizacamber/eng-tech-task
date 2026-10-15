package com.userhub.data.repository

import com.userhub.data.remote.UserDto
import io.ktor.client.statement.HttpResponse

sealed interface UsersResult {
    data class Success(
        val users: List<UserDto>,
        val lastSyncMillis: Long?,
        // only users created in this app have a real creation time
        val createdAtMillis: Map<Long, Long> = emptyMap(),
    ) : UsersResult
    data object NoInternet : UsersResult
}

interface UserRepository {
    suspend fun getUsers(): UsersResult
    suspend fun createUser(name: String, email: String): HttpResponse
    suspend fun deleteUser(id: Long): HttpResponse
}
