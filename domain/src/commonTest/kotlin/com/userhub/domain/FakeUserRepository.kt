package com.userhub.domain

import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.repository.UserRepository
import com.userhub.domain.repository.UsersResult

class FakeUserRepository(
    private val users: List<User> = emptyList(),
) : UserRepository {

    var getUsersCalls = 0
        private set
    var createdName: String? = null
        private set
    var createdEmail: String? = null
        private set
    var deletedIds = mutableListOf<Long>()
        private set

    override suspend fun getUsers(): UsersResult {
        getUsersCalls++
        return UsersResult.Success(users, lastSyncMillis = null)
    }

    override suspend fun createUser(name: String, email: String): DataResult<User> {
        createdName = name
        createdEmail = email
        return DataResult.Success(User(99, name, email, "male", "active"))
    }

    override suspend fun deleteUser(id: Long): DataResult<Unit> {
        deletedIds.add(id)
        return DataResult.Success(Unit)
    }
}
