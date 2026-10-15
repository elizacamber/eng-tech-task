package com.userhub.data.local

import com.userhub.domain.model.User

data class CachedUser(
    val user: User,
    val cachedAt: Long
)

interface UserLocalDataSource {
    fun saveUsers(users: List<User>, timestamp: Long)
    fun getUsers(): List<CachedUser>
    fun markCreated(id: Long, createdAt: Long)
    fun forgetCreated(id: Long)
    fun createdTimes(): Map<Long, Long>
    fun addUser(user: User, timestamp: Long)
    fun removeUser(id: Long)
}
