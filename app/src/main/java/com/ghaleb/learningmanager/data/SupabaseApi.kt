package com.ghaleb.learningmanager.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SessionDto(
    val id: String,
    val date: String,
    val subject: String,
    val topics: List<String> = emptyList(),
    val summary: String? = null,
    val light: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MasteryDto(
    val subject: String,
    val topic: String,
    val level: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Read-only PostgREST client. The phone only ever holds the anon key. */
class SupabaseApi(private val baseUrl: String, private val anonKey: String) {

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest {
            header("apikey", anonKey)
            // Legacy anon keys are JWTs and must also go in Authorization. New publishable keys are not
            // JWTs and are rejected there, so only send it for the legacy form.
            if (anonKey.startsWith("eyJ")) header(HttpHeaders.Authorization, "Bearer $anonKey")
        }
    }

    suspend fun sessions(): List<SessionDto> =
        paged("sessions", "id,date,subject,topics,summary,light,created_at", "date.asc,created_at.asc")

    suspend fun mastery(): List<MasteryDto> =
        paged("mastery", "subject,topic,level,updated_at", "subject.asc,topic.asc")

    // Supabase caps a response at 1000 rows, so page until a short page comes back.
    private suspend inline fun <reified T> paged(table: String, select: String, order: String): List<T> {
        val all = ArrayList<T>()
        var offset = 0
        while (true) {
            val page: List<T> = client.get("${baseUrl.trimEnd('/')}/rest/v1/$table") {
                parameter("select", select)
                parameter("order", order)
                parameter("limit", PAGE)
                parameter("offset", offset)
            }.body()
            all += page
            if (page.size < PAGE) return all
            offset += PAGE
        }
    }

    private companion object {
        const val PAGE = 1000
    }
}
