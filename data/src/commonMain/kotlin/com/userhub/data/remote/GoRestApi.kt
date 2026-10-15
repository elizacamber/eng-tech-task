package com.userhub.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class GoRestApi(private val client: HttpClient) {

    internal suspend fun fetchLastPageUsers(): List<UserDto> {
        val firstPage = client.get("${ApiConfig.BASE_URL}/users")
        val lastPage = firstPage.headers["x-pagination-pages"]?.toIntOrNull() ?: 1
        return client.get("${ApiConfig.BASE_URL}/users?page=$lastPage").body()
    }

    internal suspend fun createUser(request: CreateUserRequest): UserDto =
        client.post("${ApiConfig.BASE_URL}/users") {
            setBody(request)
        }.body()

    internal suspend fun deleteUser(id: Long) {
        client.delete("${ApiConfig.BASE_URL}/users/$id")
    }
}
