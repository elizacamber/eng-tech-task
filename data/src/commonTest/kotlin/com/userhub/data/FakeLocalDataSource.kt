package com.userhub.data

import com.userhub.data.local.CachedUser
import com.userhub.data.local.UserLocalDataSource
import com.userhub.data.remote.UserDto

class FakeLocalDataSource(initial: List<CachedUser> = emptyList()) : UserLocalDataSource {

    private var stored = initial.toMutableList()

    var savedUsers: List<UserDto> = emptyList()
        private set

    override fun saveUsers(users: List<UserDto>, timestamp: Long) {
        savedUsers = users
        stored = users.map { CachedUser(it, timestamp) }.toMutableList()
    }

    override fun getUsers(): List<CachedUser> = stored

    private val created = mutableMapOf<Long, Long>()

    override fun markCreated(id: Long, createdAt: Long) {
        created[id] = createdAt
    }

    override fun forgetCreated(id: Long) {
        created.remove(id)
    }

    override fun createdTimes(): Map<Long, Long> = created.toMap()

    override fun addUser(user: UserDto, timestamp: Long) {
        stored.removeAll { it.user.id == user.id }
        stored.add(0, CachedUser(user, timestamp))
    }

    override fun removeUser(id: Long) {
        stored.removeAll { it.user.id == id }
    }
}
