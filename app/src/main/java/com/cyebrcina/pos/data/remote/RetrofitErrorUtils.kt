package com.cyebrcina.pos.data.remote

import com.cyebrcina.pos.data.remote.model.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response

/** Fire Hut's error responses are `{ "error": "..." }` — see openapi.yaml's Unauthorized/400 shapes. */
fun <T> Response<T>.errorMessageOrNull(json: Json): String? {
    val body = errorBody()?.string() ?: return null
    return runCatching { json.decodeFromString(ErrorResponse.serializer(), body).error }.getOrNull()
}

fun <T> Response<T>.errorMessageOrDefault(json: Json): String =
    errorMessageOrNull(json) ?: "Request failed (HTTP ${code()})"
