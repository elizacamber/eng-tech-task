package com.userhub.presentation

import com.userhub.domain.model.DataError
import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.repository.UserRepository
import com.userhub.domain.repository.UsersResult

class FakeUserRepository(
    private val users: List<User> = emptyList(),
    // null = succeed, echoing the created user back like GoRest does on 201
    private val createError: DataError? = null,
    private val deleteError: DataError? = null,
) : UserRepository {

    var getUsersCalls = 0
        private set
    var createdName: String? = null
        private set
    var createdEmail: String? = null
        private set

    // record deletes
    val deletedIds = mutableListOf<Long>()

    override suspend fun getUsers(): UsersResult {
        getUsersCalls++
        return UsersResult.Success(users, lastSyncMillis = null)
    }

    override suspend fun createUser(name: String, email: String): DataResult<User> {
        createdName = name
        createdEmail = email
        return createError?.let { DataResult.Failure(it) }
            ?: DataResult.Success(User(99, name, email, "male", "active"))
    }

    override suspend fun deleteUser(id: Long): DataResult<Unit> {
        deletedIds += id
        return deleteError?.let { DataResult.Failure(it) } ?: DataResult.Success(Unit)
    }
}
