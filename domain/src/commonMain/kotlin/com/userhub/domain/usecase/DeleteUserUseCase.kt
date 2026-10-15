package com.userhub.domain.usecase

import com.userhub.domain.model.DataResult
import com.userhub.domain.repository.UserRepository

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: Long): DataResult<Unit> =
        repository.deleteUser(id)
}
