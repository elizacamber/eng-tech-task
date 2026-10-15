package com.userhub.domain.usecase

import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.repository.UserRepository

class AddUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(name: String, email: String): DataResult<User> =
        repository.createUser(name, email)
}
