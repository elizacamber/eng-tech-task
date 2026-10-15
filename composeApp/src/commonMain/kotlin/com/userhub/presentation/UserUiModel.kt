package com.userhub.presentation

import com.userhub.data.remote.UserDto
import kotlin.time.Instant

data class UserUiModel(
    val user: UserDto,
    // null for users fetched from GoRest, which has no creation date
    val createdAt: Instant?,
) {
    val id: Long get() = user.id
    val name: String get() = user.name
    val email: String get() = user.email
}
