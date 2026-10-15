package com.userhub.data.local

import com.userhub.data.remote.UserDto

data class CachedUser(
    val user: UserDto,
    val cachedAt: Long
)

interface UserLocalDataSource {
    fun saveUsers(users: List<UserDto>, timestamp: Long)
    fun getUsers(): List<CachedUser>
    fun markCreated(id: Long, createdAt: Long)
    fun forgetCreated(id: Long)
    fun createdTimes(): Map<Long, Long>
    fun addUser(user: UserDto, timestamp: Long)
    fun removeUser(id: Long)
}
