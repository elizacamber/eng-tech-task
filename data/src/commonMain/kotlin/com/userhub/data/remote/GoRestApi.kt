package com.userhub.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class GoRestApi(private val client: HttpClient) {

    suspend fun fetchLastPageUsers(): List<UserDto> {
        val firstPage = client.get("${ApiConfig.BASE_URL}/users")
        val lastPage = firstPage.headers["x-pagination-pages"]?.toIntOrNull() ?: 1
        return client.get("${ApiConfig.BASE_URL}/users?page=$lastPage").body()
    }

    suspend fun createUser(request: CreateUserRequest): HttpResponse =
        client.post("${ApiConfig.BASE_URL}/users") {
            setBody(request)
        }

    suspend fun deleteUser(id: Long): HttpResponse =
        client.delete("${ApiConfig.BASE_URL}/users/$id")
}
