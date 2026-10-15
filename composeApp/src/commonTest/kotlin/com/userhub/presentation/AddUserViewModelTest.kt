package com.userhub.presentation

import com.userhub.domain.usecase.AddUserUseCase
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

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

    // the fake goes through a real Ktor MockEngine, which completes off the test scheduler
    private suspend fun awaitResponse() = withContext(Dispatchers.Default) { delay(100) }

    @Test
    fun `submit sends the entered values to the repository`() = runTest {
        val repository = FakeUserRepository()
        val viewModel = viewModel(repository)

        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        assertEquals("Grace Hopper", repository.createdName)
        assertEquals("grace@example.com", repository.createdEmail)
    }

    @Test
    fun `successful creation is exposed in state`() = runTest {
        val viewModel = viewModel(FakeUserRepository())

        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        assertEquals("grace@example.com", viewModel.state.value.createdUser?.email)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `handled result is not delivered again`() = runTest {
        val viewModel = viewModel(FakeUserRepository())
        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        viewModel.onCreatedHandled()

        assertNull(viewModel.state.value.createdUser)
    }

    @Test
    fun `duplicate email is reported on the email field`() = runTest {
        val repository = FakeUserRepository(
            createStatus = HttpStatusCode.UnprocessableEntity,
            createBody = """[{"field":"email","message":"has already been taken"}]"""
        )
        val viewModel = viewModel(repository)

        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        val error = assertIs<AddUserError.InvalidFields>(viewModel.state.value.error)
        assertEquals("has already been taken", error.messages["email"])
        assertNull(viewModel.state.value.createdUser)
    }

    @Test
    fun `unreadable 422 body falls back to a server error`() = runTest {
        val repository = FakeUserRepository(
            createStatus = HttpStatusCode.UnprocessableEntity,
            createBody = """{"unexpected":true}"""
        )
        val viewModel = viewModel(repository)

        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        assertEquals(AddUserError.Server(422), viewModel.state.value.error)
    }

    @Test
    fun `401 is reported as unauthorized`() = runTest {
        val viewModel = viewModel(FakeUserRepository(createStatus = HttpStatusCode.Unauthorized))

        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        assertEquals(AddUserError.Unauthorized, viewModel.state.value.error)
    }

    @Test
    fun `editing clears the server error`() = runTest {
        val repository = FakeUserRepository(
            createStatus = HttpStatusCode.UnprocessableEntity,
            createBody = """[{"field":"email","message":"has already been taken"}]"""
        )
        val viewModel = viewModel(repository)
        viewModel.submit("Grace Hopper", "grace@example.com")
        awaitResponse()

        viewModel.clearError()

        assertNull(viewModel.state.value.error)
    }
}
