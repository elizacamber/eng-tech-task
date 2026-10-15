package com.userhub.presentation

import com.userhub.domain.model.DataError
import com.userhub.domain.usecase.AddUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

// The fake returns immediately and Main is unconfined, so submit() finishes before it returns.
@OptIn(ExperimentalCoroutinesApi::class)
class AddUserViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeUserRepository) =
        AddUserViewModel(AddUserUseCase(repository))

    private val duplicateEmail = DataError.InvalidFields(mapOf("email" to "has already been taken"))

    @Test
    fun `submit sends the entered values to the repository`() = runTest {
        val repository = FakeUserRepository()
        val viewModel = viewModel(repository)

        viewModel.submit("Grace Hopper", "grace@example.com")

        assertEquals("Grace Hopper", repository.createdName)
        assertEquals("grace@example.com", repository.createdEmail)
    }

    @Test
    fun `successful creation is exposed in state`() = runTest {
        val viewModel = viewModel(FakeUserRepository())

        viewModel.submit("Grace Hopper", "grace@example.com")

        assertEquals("grace@example.com", viewModel.state.value.createdUser?.email)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `handled result is not delivered again`() = runTest {
        val viewModel = viewModel(FakeUserRepository())
        viewModel.submit("Grace Hopper", "grace@example.com")

        viewModel.onCreatedHandled()

        assertNull(viewModel.state.value.createdUser)
    }

    @Test
    fun `duplicate email is reported on the email field`() = runTest {
        val viewModel = viewModel(FakeUserRepository(createError = duplicateEmail))

        viewModel.submit("Grace Hopper", "grace@example.com")

        val error = assertIs<DataError.InvalidFields>(viewModel.state.value.error)
        assertEquals("has already been taken", error.messages["email"])
        assertNull(viewModel.state.value.createdUser)
    }

    @Test
    fun `editing clears the server error`() = runTest {
        val viewModel = viewModel(FakeUserRepository(createError = duplicateEmail))
        viewModel.submit("Grace Hopper", "grace@example.com")

        viewModel.clearError()

        assertNull(viewModel.state.value.error)
    }
}
