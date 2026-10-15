package com.userhub.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: String,
    val status: String
)
