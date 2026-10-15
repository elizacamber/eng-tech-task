package com.userhub.data

import com.userhub.data.local.CachedUser
import com.userhub.data.remote.ApiConfig
import com.userhub.data.remote.GoRestApi
import com.userhub.data.remote.UserDto
import com.userhub.data.remote.createHttpClient
import com.userhub.data.repository.UserRepositoryImpl
import com.userhub.data.repository.UsersResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class UserRepositoryImplTest {

    private val usersJson = """
        [
          {"id":1,"name":"Ada Lovelace","email":"ada@example.com","gender":"female","status":"active"},
          {"id":2,"name":"Alan Turing","email":"alan@example.com","gender":"male","status":"active"}
        ]
    """.trimIndent()

    private fun successEngine() = MockEngine {
        respond(
            content = usersJson,
            status = HttpStatusCode.OK,
            headers = headersOf(
                HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                "x-pagination-pages" to listOf("3")
            )
        )
    }

    private fun createdEngine() = MockEngine {
        respond(
            content = """{"id":42,"name":"New","email":"new@example.com","gender":"male","status":"active"}""",
            status = HttpStatusCode.Created,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }

    private val cachedAda = CachedUser(
        UserDto(7, "Ada", "ada@example.com", "female", "active"), 1_700_000_000_000L
    )

    private fun fixedClock(millis: Long) = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(millis)
    }

    @Test
    fun `returns users from the last page`() = runTest {
        val repository = UserRepositoryImpl(
            GoRestApi(createHttpClient(successEngine())),
            FakeLocalDataSource()
        )

        val result = repository.getUsers()

        assertIs<UsersResult.Success>(result)
        assertEquals(2, result.users.size)
    }

    @Test
    fun `caches users after a successful fetch`() = runTest {
        val local = FakeLocalDataSource()
        val repository = UserRepositoryImpl(GoRestApi(createHttpClient(successEngine())), local)

        repository.getUsers()

        assertEquals(2, local.savedUsers.size)
        assertTrue(local.savedUsers.any { it.email == "ada@example.com" })
    }

    @Test
    fun `falls back to cached users when the network fails`() = runTest {
        val cached = listOf(
            CachedUser(
                UserDto(7, "Cached User", "cached@example.com", "male", "active"),
                1_700_000_000_000L
            )
        )
        val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
        val repository = UserRepositoryImpl(
            GoRestApi(createHttpClient(engine)),
            FakeLocalDataSource(cached)
        )

        val result = repository.getUsers()

        assertIs<UsersResult.Success>(result)
        assertEquals("cached@example.com", result.users.single().email)
        assertEquals(1_700_000_000_000L, result.lastSyncMillis)
    }

    @Test
    fun `uses the public GoRest base url`() {
        assertTrue(ApiConfig.BASE_URL.startsWith("https://"))
    }

    @Test
    fun `returns NoInternet when network fails and cache is empty`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
        val repository = UserRepositoryImpl(
            GoRestApi(createHttpClient(engine)),
            FakeLocalDataSource()
        )

        val result = repository.getUsers()

        assertIs<UsersResult.NoInternet>(result)
    }

    @Test
    fun `propagates cancellation thrown by the network call`() = runTest {
        val engine = MockEngine { throw CancellationException("cancelled") }
        val repository =
            UserRepositoryImpl(GoRestApi(createHttpClient(engine)), FakeLocalDataSource())

        assertFailsWith<CancellationException> { repository.getUsers() }
    }

    @Test
    fun `caches users with the clock's epoch time`() = runTest {
        val local = FakeLocalDataSource()
        val repository = UserRepositoryImpl(
            GoRestApi(createHttpClient(successEngine())), local, fixedClock(1_700_000_000_000L)
        )

        repository.getUsers()

        assertEquals(1_700_000_000_000L, local.getUsers().first().cachedAt)
    }

    @Test
    fun `remembers when a user was created in the app`() = runTest {
        val local = FakeLocalDataSource()
        val repository = UserRepositoryImpl(
            GoRestApi(createHttpClient(createdEngine())),
            local,
            fixedClock(1_700_000_000_000L)
        )

        repository.createUser("New", "new@example.com")

        assertEquals(mapOf(42L to 1_700_000_000_000L), local.createdTimes())
    }

    @Test
    fun `created user is added to the offline cache`() = runTest {
        val local = FakeLocalDataSource()
        val repository =
            UserRepositoryImpl(GoRestApi(createHttpClient(createdEngine())), local, fixedClock(1L))

        repository.createUser("New", "new@example.com")

        assertEquals(listOf(42L), local.getUsers().map { it.user.id })
    }

    @Test
    fun `deleted user is removed from the offline cache`() = runTest {
        val local = FakeLocalDataSource(listOf(cachedAda))
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }
        val repository = UserRepositoryImpl(GoRestApi(createHttpClient(engine)), local)

        repository.deleteUser(7)

        assertTrue(local.getUsers().isEmpty())
    }

    @Test
    fun `user already deleted on the server is removed from the cache`() = runTest {
        val local = FakeLocalDataSource(listOf(cachedAda))
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        val repository = UserRepositoryImpl(GoRestApi(createHttpClient(engine)), local)

        assertFailsWith<ResponseException> { repository.deleteUser(7) }
        assertTrue(local.getUsers().isEmpty())
    }

    @Test
    fun `failed delete keeps the cached user`() = runTest {
        val local = FakeLocalDataSource(listOf(cachedAda))
        val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
        val repository = UserRepositoryImpl(GoRestApi(createHttpClient(engine)), local)

        assertFailsWith<ResponseException> { repository.deleteUser(7) }
        assertEquals(listOf(7L), local.getUsers().map { it.user.id })
    }
}
