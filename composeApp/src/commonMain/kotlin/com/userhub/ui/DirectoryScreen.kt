package com.userhub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.userhub.presentation.AddUserViewModel
import com.userhub.presentation.UserFeedUiState
import com.userhub.presentation.UserFeedViewModel
import com.userhub.presentation.UserUiModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DirectoryScreen(
    feedViewModel: UserFeedViewModel = koinViewModel(),
    addUserViewModel: AddUserViewModel = koinViewModel()
) {
    val state by feedViewModel.state.collectAsStateWithLifecycle()
    val addState by addUserViewModel.state.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<UserUiModel?>(null) }
    val selectedUserId by feedViewModel.selectedUserId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Delete failures from the ViewModel (the row has already been restored).
    LaunchedEffect(Unit) {
        feedViewModel.errors.collect { snackbarHostState.showSnackbar(it) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp
        val users = (state as? UserFeedUiState.Content)?.users.orEmpty()
        val selectedUser = users.firstOrNull { it.id == selectedUserId }
        // Phone with a user selected: the details replace the list
        val showDetailOnly = !isWide && selectedUser != null

        PlatformBackHandler(enabled = showDetailOnly, onBack = feedViewModel::onSelectionCleared)

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                // only show FAB when we're not on the detail screen
                if (!showDetailOnly) {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add user")
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val current = state) {
                    UserFeedUiState.Loading -> LoadingSkeleton()
                    UserFeedUiState.NoInternet -> ConnectionErrorState(onRetry = feedViewModel::load)
                    is UserFeedUiState.Content -> when {
                        isWide -> Row {
                            UserList(
                                users = current.users,
                                lastSyncLabel = current.lastSyncLabel,
                                onClick = feedViewModel::onUserSelected,
                                onLongPress = { pendingDelete = it },
                                modifier = Modifier.weight(2f)
                            )
                            DetailPanel(
                                user = selectedUser ?: current.users.firstOrNull(),
                                modifier = Modifier.weight(3f)
                            )
                        }

                        selectedUser != null -> DetailPanel(
                            user = selectedUser,
                            onBack = feedViewModel::onSelectionCleared
                        )

                        else -> UserList(
                            users = current.users,
                            lastSyncLabel = current.lastSyncLabel,
                            onClick = feedViewModel::onUserSelected,
                            onLongPress = { pendingDelete = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showAddSheet) {
                NewUserSheet(
                    state = addState,
                    onDismiss = {
                        showAddSheet = false
                        addUserViewModel.clearError()
                    },
                    onSubmit = { name, email ->
                        addUserViewModel.submit(name, email) { created ->
                            feedViewModel.onUserCreated(created)
                            showAddSheet = false
                            scope.launch { snackbarHostState.showSnackbar("User added") }
                        }
                    }
                )
            }

            pendingDelete?.let { user ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("Delete user?") },
                    text = { Text("${user.name} will be removed from the directory.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingDelete = null
                                // only one undo at a time: close the previous snackbar
                                snackbarHostState.currentSnackbarData?.dismiss()
                                feedViewModel.onDeleteConfirmed(user)
                                scope.launch {
                                    var undone = false
                                    try {
                                        // finally runs on Undo, timeout, dismiss, or the screen leaving
                                        undone = snackbarHostState.showSnackbar(
                                            message = "${user.name} deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Long
                                        ) == SnackbarResult.ActionPerformed
                                    } finally {
                                        if (undone) feedViewModel.onUndo(user)
                                        else feedViewModel.onUndoWindowClosed(user)
                                    }
                                }
                            }
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<UserUiModel>,
    lastSyncLabel: String?,
    onClick: (UserUiModel) -> Unit,
    onLongPress: (UserUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
        if (lastSyncLabel != null) {
            item {
                Text(
                    text = lastSyncLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        items(users, key = { it.id }) { user ->
            DirectoryRow(
                user = user,
                onClick = { onClick(user) },
                onLongPress = { onLongPress(user) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
