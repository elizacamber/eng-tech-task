package com.userhub.domain.usecase

import com.userhub.domain.repository.UserRepository
import com.userhub.domain.repository.UsersResult

class GetUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): UsersResult = repository.getUsers()
}
