package dev.havlicektomas.notemark.core.data.networking

import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

suspend inline fun <reified Response : Any> HttpClient.getResult(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<Response, DataError.Network> = safeCall {
    get {
        url(constructRoute(route))
        queryParameters.forEach { (key, value) -> parameter(key, value) }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.postResult(
    route: String,
    body: Request,
): Result<Response, DataError.Network> = safeCall {
    post {
        url(constructRoute(route))
        setBody(body)
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.putResult(
    route: String,
    body: Request,
): Result<Response, DataError.Network> = safeCall {
    put {
        url(constructRoute(route))
        setBody(body)
    }
}

suspend inline fun <reified Response : Any> HttpClient.deleteResult(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<Response, DataError.Network> = safeCall {
    delete {
        url(constructRoute(route))
        queryParameters.forEach { (key, value) -> parameter(key, value) }
    }
}

/** For endpoints that return an empty body (e.g. register → 201 No Content). */
suspend inline fun <reified Request> HttpClient.postEmptyResult(
    route: String,
    body: Request,
): Result<Unit, DataError.Network> = safeCallEmpty {
    post {
        url(constructRoute(route))
        setBody(body)
    }
}

/** For endpoints that return an empty body (e.g. delete → 200 OK, no payload). */
suspend inline fun HttpClient.deleteEmptyResult(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<Unit, DataError.Network> = safeCallEmpty {
    delete {
        url(constructRoute(route))
        queryParameters.forEach { (key, value) -> parameter(key, value) }
    }
}

suspend inline fun safeCallEmpty(
    execute: () -> HttpResponse,
): Result<Unit, DataError.Network> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: IOException) {
        // Connect/socket failures (e.g. airplane mode) surface as IO errors.
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return when (val status = response.status.value) {
        in 200..299 -> Result.Success(Unit)
        else -> Result.Error(statusToError(status))
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse,
): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: IOException) {
        // Connect/socket failures (e.g. airplane mode) surface as IO errors.
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> = when (val status = response.status.value) {
    in 200..299 -> try {
        // Reading the body can fail too (unexpected JSON, an HTML page from a
        // proxy, a dropped connection); it must become an error, not a throw.
        Result.Success(response.body<T>())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: Exception) {
        Result.Error(DataError.Network.SERIALIZATION)
    }
    else -> Result.Error(statusToError(status))
}

fun statusToError(status: Int): DataError.Network = when (status) {
    400 -> DataError.Network.BAD_REQUEST
    401 -> DataError.Network.UNAUTHORIZED
    403 -> DataError.Network.FORBIDDEN
    404 -> DataError.Network.NOT_FOUND
    408 -> DataError.Network.REQUEST_TIMEOUT
    409 -> DataError.Network.CONFLICT
    413 -> DataError.Network.PAYLOAD_TOO_LARGE
    429 -> DataError.Network.TOO_MANY_REQUESTS
    in 500..599 -> DataError.Network.SERVER_ERROR
    else -> DataError.Network.UNKNOWN
}
