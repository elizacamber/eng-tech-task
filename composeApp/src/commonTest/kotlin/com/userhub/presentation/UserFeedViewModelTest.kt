package com.userhub.presentation

import com.userhub.data.remote.UserDto
import com.userhub.domain.usecase.DeleteUserUseCase
import com.userhub.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserFeedViewModelTest {

    private val users = listOf(
        UserDto(1, "Ada Lovelace", "ada@example.com", "female", "active"),
        UserDto(2, "Alan Turing", "alan@example.com", "male", "active"),
        UserDto(3, "Grace Hopper", "grace@example.com", "female", "active")
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(repository: FakeUserRepository) = UserFeedViewModel(
        GetUsersUseCase(repository),
        DeleteUserUseCase(repository),
        backgroundScope
    )

    @Test
    fun `loads users from the repository on init`() = runTest {
        val repository = FakeUserRepository(users)

        val viewModel = viewModel(repository)

        assertEquals(1, repository.getUsersCalls)
        assertIs<UserFeedUiState.Content>(viewModel.state.value)
    }

    @Test
    fun `content state exposes every user returned by the repository`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))

        val state = viewModel.state.value
        assertIs<UserFeedUiState.Content>(state)
        assertEquals(3, state.users.size)
        assertTrue(state.users.any { it.email == "grace@example.com" })
    }

    @Test
    fun `delete removes the user from the list`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val target = (viewModel.state.value as UserFeedUiState.Content).users.first()

        viewModel.onDeleteConfirmed(target)

        val state = viewModel.state.value
        assertIs<UserFeedUiState.Content>(state)
        assertEquals(2, state.users.size)
        assertTrue(state.users.none { it.id == target.id })
    }

    @Test
    fun `undo restores the deleted user`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val target = (viewModel.state.value as UserFeedUiState.Content).users.first()
        viewModel.onDeleteConfirmed(target)

        viewModel.onUndo(target)

        val state = viewModel.state.value
        assertIs<UserFeedUiState.Content>(state)
        assertEquals(3, state.users.size)
        assertEquals(target.id, state.users.first().id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `undo means the delete is never sent`() = runTest {
        val repository = FakeUserRepository(users)
        val viewModel = viewModel(repository)
        val target = (viewModel.state.value as UserFeedUiState.Content).users.first()

        viewModel.onDeleteConfirmed(target)
        viewModel.onUndo(target)
        runCurrent()

        assertTrue(repository.deletedIds.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `delete is sent once the undo window closes`() = runTest {
        val repository = FakeUserRepository(users)
        val viewModel = viewModel(repository)
        val target = (viewModel.state.value as UserFeedUiState.Content).users.first()

        viewModel.onDeleteConfirmed(target)
        viewModel.onUndoWindowClosed(target)
        runCurrent()

        assertEquals(listOf(target.id), repository.deletedIds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a second delete sends the first and ignores its late callback`() = runTest {
        val repository = FakeUserRepository(users)
        val viewModel = viewModel(repository)
        val (first, second) = (viewModel.state.value as UserFeedUiState.Content).users

        viewModel.onDeleteConfirmed(first)
        viewModel.onDeleteConfirmed(second)
        viewModel.onUndoWindowClosed(first)   // late callback from the dismissed snackbar
        viewModel.onUndo(second)
        runCurrent()

        assertEquals(listOf(first.id), repository.deletedIds)
        val state = viewModel.state.value as UserFeedUiState.Content
        assertTrue(state.users.any { it.id == second.id })
    }

    @Test
    fun `selecting a user stores its id`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val target = (viewModel.state.value as UserFeedUiState.Content).users[1]

        viewModel.onUserSelected(target)

        assertEquals(target.id, viewModel.selectedUserId.value)
    }

    @Test
    fun `deleting the selected user clears the selection`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val target = (viewModel.state.value as UserFeedUiState.Content).users.first()
        viewModel.onUserSelected(target)

        viewModel.onDeleteConfirmed(target)

        assertNull(viewModel.selectedUserId.value)
    }

    @Test
    fun `deleting another user keeps the selection`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val (selected, other) = (viewModel.state.value as UserFeedUiState.Content).users
        viewModel.onUserSelected(selected)

        viewModel.onDeleteConfirmed(other)

        assertEquals(selected.id, viewModel.selectedUserId.value)
    }

    @Test
    fun `the same created user is only added once`() = runTest {
        val viewModel = viewModel(FakeUserRepository(users))
        val created = UserDto(99, "New User", "new@example.com", "female", "active")

        viewModel.onUserCreated(created)
        viewModel.onUserCreated(created)

        val state = viewModel.state.value as UserFeedUiState.Content
        assertEquals(1, state.users.count { it.id == 99L })
    }
}
