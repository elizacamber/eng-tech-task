package com.userhub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.userhub.data.remote.UserDto
import com.userhub.data.repository.UsersResult
import com.userhub.domain.time.TimeProvider
import com.userhub.domain.time.formatRelativeTime
import com.userhub.domain.usecase.DeleteUserUseCase
import com.userhub.domain.usecase.GetUsersUseCase
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

class UserFeedViewModel(
    private val getUsers: GetUsersUseCase,
    private val deleteUser: DeleteUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<UserFeedUiState>(UserFeedUiState.Loading)
    val state: StateFlow<UserFeedUiState> = _state.asStateFlow()

    private var pendingUndo: Pair<Int, UserUiModel>? = null

    init {
        load()
    }

    fun load() {
        _state.value = UserFeedUiState.Loading
        viewModelScope.launch {
            when (val result = getUsers()) {
                is UsersResult.Success -> {
                    val lastSyncLabel = result.lastSyncMillis?.let {
                        "Offline — last updated ${
                            formatRelativeTime(
                                it,
                                TimeProvider.nowEpochMillis()
                            )
                        }"
                    }
                    _state.value = UserFeedUiState.Content(result.users.toUiModels(), lastSyncLabel)
                }

                UsersResult.NoInternet -> _state.value = UserFeedUiState.NoInternet
            }
        }
    }

    fun onUserCreated(user: UserDto) {
        val content = _state.value as? UserFeedUiState.Content ?: return
        _state.value = content.copy(users = listOf(user.toNewUiModel()) + content.users)
    }

    fun onDeleteConfirmed(user: UserUiModel) {
        val content = _state.value as? UserFeedUiState.Content ?: return
        val index = content.users.indexOf(user)
        if (index < 0) return
        pendingUndo = index to user
        _state.value = content.copy(users = content.users - user)
        // fixme use app scope
        viewModelScope.launch {
            try {
                deleteUser(user.id)
            } catch (e: IOException) {
                // server didn't delete this. Restore it.
                restore(index, user)
            } catch (e: ResponseException) {
                // treat 404 as success
                // for any other server error, restore it
                if (e.response.status != HttpStatusCode.NotFound) restore(index, user)
            }
        }
    }

    fun onUndo() {
        val (index, user) = pendingUndo ?: return
        restore(index, user)
    }

    private fun restore(index: Int, user: UserUiModel) {
        val content = _state.value as? UserFeedUiState.Content ?: return
        if (user in content.users) return
        val users = content.users.toMutableList()
        users.add(index.coerceAtMost(users.size), user)
        _state.value = content.copy(users = users)
        pendingUndo = null
    }
}
