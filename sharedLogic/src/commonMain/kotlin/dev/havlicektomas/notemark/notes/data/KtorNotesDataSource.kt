package dev.havlicektomas.notemark.notes.data

import dev.havlicektomas.notemark.core.data.networking.deleteEmptyResult
import dev.havlicektomas.notemark.core.data.networking.getResult
import dev.havlicektomas.notemark.core.data.networking.putResult
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import io.ktor.client.HttpClient

/** Talks to the NoteMark notes endpoints. All requests are bearer-authed by the client plugin. */
class KtorNotesDataSource(
    private val httpClient: HttpClient,
) {
    /** `page=-1` asks the backend for every note in one response. */
    suspend fun getNotes(): Result<NotesResponse, DataError.Network> = httpClient.getResult(
        route = "/api/notes",
        queryParameters = mapOf("page" to -1),
    )

    suspend fun upsertNote(
        request: NoteRequest,
    ): Result<NoteResponse, DataError.Network> = httpClient.putResult(
        route = "/api/notes",
        body = request,
    )

    suspend fun deleteNote(
        id: String,
    ): EmptyResult<DataError.Network> = httpClient.deleteEmptyResult(
        route = "/api/notes/$id",
    )
}
