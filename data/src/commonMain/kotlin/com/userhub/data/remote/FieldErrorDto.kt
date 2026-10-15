package com.userhub.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One entry of GoRest's 422 body, e.g. {"field":"email","message":"has already been taken"}
@Serializable
internal data class FieldErrorDto(
    @SerialName("field") val field: String,
    @SerialName("message") val message: String
)
