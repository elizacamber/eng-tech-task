package com.userhub.data.remote

import com.userhub.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("gender") val gender: String,
    @SerialName("status") val status: String
)

internal fun UserDto.toDomain() =
    User(id = id, name = name, email = email, gender = gender, status = status)
