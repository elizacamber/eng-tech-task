package com.userhub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.userhub.data.remote.UserDto
import com.userhub.domain.usecase.AddUserUseCase
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

data class AddUserState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class AddUserViewModel(private val addUser: AddUserUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AddUserState())
    val state: StateFlow<AddUserState> = _state.asStateFlow()

    fun submit(name: String, email: String, onCreated: (UserDto) -> Unit) {
        viewModelScope.launch {
            _state.value = AddUserState(isSubmitting = true)
            try {
                val response = addUser(name, email)
                _state.value = AddUserState()
                onCreated(response.body())
            } catch (e: IOException) {
                _state.value = AddUserState(
                    errorMessage = "No internet connection. Please check your network and try again."
                )
            } catch (e: ResponseException) {
                _state.value = AddUserState(
                    errorMessage = "Couldn't create user (${e.response.status.value}). Please check the details."
                )
            } catch (e: JsonConvertException) {
                _state.value = AddUserState(
                    errorMessage = "User was created, but the response couldn't be read. Pull to refresh."
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
