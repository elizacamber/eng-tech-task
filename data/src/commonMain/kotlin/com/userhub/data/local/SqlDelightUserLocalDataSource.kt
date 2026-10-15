package com.userhub.data.local

import com.userhub.data.db.UserDatabase
import com.userhub.data.remote.UserDto

class SqlDelightUserLocalDataSource(database: UserDatabase) : UserLocalDataSource {

    private val queries = database.userCacheQueries
    private val createdQueries = database.createdUserQueries

    override fun saveUsers(users: List<UserDto>, timestamp: Long) {
        queries.transaction {
            queries.clearAll()
            users.forEach { user ->
                queries.insertUser(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    gender = user.gender,
                    status = user.status,
                    cached_at = timestamp
                )
            }
        }
    }

    override fun getUsers(): List<CachedUser> =
        queries.selectAll().executeAsList().map { row ->
            CachedUser(
                user = UserDto(
                    id = row.id,
                    name = row.name,
                    email = row.email,
                    gender = row.gender,
                    status = row.status
                ),
                cachedAt = row.cached_at
            )
        }

    override fun markCreated(id: Long, createdAt: Long) {
        createdQueries.insertCreated(id, createdAt)
    }

    override fun forgetCreated(id: Long) {
        createdQueries.deleteCreated(id)
    }

    override fun createdTimes(): Map<Long, Long> =
        createdQueries.selectAllCreated().executeAsList().associate { it.id to it.created_at }

    override fun addUser(user: UserDto, timestamp: Long) {
        queries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            gender = user.gender,
            status = user.status,
            cached_at = timestamp
        )
    }

    override fun removeUser(id: Long) {
        queries.deleteUser(id)
    }
}
