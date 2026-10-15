package com.userhub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.userhub.data.remote.FieldErrorDto
import com.userhub.data.remote.UserDto
import com.userhub.domain.usecase.AddUserUseCase
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

sealed interface AddUserError {
    data object Offline : AddUserError
    data object Unauthorized : AddUserError

    // 422: the server rejected specific fields, keyed by field name ("email", "name")
    data class InvalidFields(val messages: Map<String, String>) : AddUserError
    data class Server(val status: Int) : AddUserError
    data object UnreadableResponse : AddUserError
}

data class AddUserState(
    val isSubmitting: Boolean = false,
    val error: AddUserError? = null,
    // Set when the server confirms the user. Kept in state rather than sent to a callback,
    // so it survives rotation; the screen clears it with onCreatedHandled().
    val createdUser: UserDto? = null,
)

class AddUserViewModel(private val addUser: AddUserUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AddUserState())
    val state: StateFlow<AddUserState> = _state.asStateFlow()

    fun submit(name: String, email: String) {
        viewModelScope.launch {
            _state.value = AddUserState(isSubmitting = true)
            val result = try {
                AddUserState(createdUser = addUser(name, email).body())
            } catch (e: IOException) {
                AddUserState(error = AddUserError.Offline)
            } catch (e: ResponseException) {
                AddUserState(error = e.toAddUserError())
            } catch (e: JsonConvertException) {
                AddUserState(error = AddUserError.UnreadableResponse)
            }
            _state.value = result
        }
    }

    // The screen has added the user to the feed; don't deliver it again.
    fun onCreatedHandled() {
        _state.value = _state.value.copy(createdUser = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

private suspend fun ResponseException.toAddUserError(): AddUserError =
    when (response.status) {
        HttpStatusCode.Unauthorized -> AddUserError.Unauthorized
        HttpStatusCode.UnprocessableEntity -> {
            val fields = try {
                response.body<List<FieldErrorDto>>().associate { it.field to it.message }
            } catch (e: JsonConvertException) {
                emptyMap()
            } catch (e: NoTransformationFoundException) {
                emptyMap()
            }
            if (fields.isEmpty()) AddUserError.Server(response.status.value)
            else AddUserError.InvalidFields(fields)
        }

        else -> AddUserError.Server(response.status.value)
    }
