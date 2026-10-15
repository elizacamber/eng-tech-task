package com.userhub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.userhub.domain.model.DataError
import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.usecase.AddUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddUserState(
    val isSubmitting: Boolean = false,
    val error: DataError? = null,
    // Set when the server confirms the user. Kept in state rather than sent to a callback,
    // so it survives rotation; the screen clears it with onCreatedHandled().
    val createdUser: User? = null,
)

class AddUserViewModel(private val addUser: AddUserUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AddUserState())
    val state: StateFlow<AddUserState> = _state.asStateFlow()

    fun submit(name: String, email: String) {
        viewModelScope.launch {
            _state.value = AddUserState(isSubmitting = true)
            _state.value = when (val result = addUser(name, email)) {
                is DataResult.Success -> AddUserState(createdUser = result.value)
                is DataResult.Failure -> AddUserState(error = result.error)
            }
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
