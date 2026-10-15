package com.userhub.data.remote

import com.userhub.domain.model.DataError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException

// Turns an HTTP error response into a DataError, so no Ktor type leaves the data layer.
internal suspend fun ResponseException.toDataError(): DataError =
    when (response.status) {
        HttpStatusCode.Unauthorized -> DataError.Unauthorized
        HttpStatusCode.UnprocessableEntity -> {
            // GoRest 422 body: [{"field":"email","message":"has already been taken"}]
            val fields = try {
                response.body<List<FieldErrorDto>>().associate { it.field to it.message }
            } catch (e: JsonConvertException) {
                emptyMap()
            } catch (e: NoTransformationFoundException) {
                emptyMap()
            }
            if (fields.isEmpty()) DataError.Server(response.status.value)
            else DataError.InvalidFields(fields)
        }

        else -> DataError.Server(response.status.value)
    }
