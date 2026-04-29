package ru.kubsu.borshchevyk.core.network.client

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.errors.IOException

suspend inline fun <reified T> safeRequest(
    block: () -> HttpResponse
): NetworkResult<T> {
    return try {
        val response = block()
        NetworkResult.Success(response.body<T>())
    } catch (e: ClientRequestException) {
        // 4xx errors
        NetworkResult.Error(e.response.status.value, e.message)
    } catch (e: ServerResponseException) {
        // 5xx errors
        NetworkResult.Error(e.response.status.value, e.message)
    } catch (e: RedirectResponseException) {
        // 3xx errors
        NetworkResult.Error(e.response.status.value, e.message)
    } catch (e: IOException) {
        // Network errors (No internet, timeout, etc.)
        NetworkResult.Exception(e)
    } catch (e: Exception) {
        // Unknown errors
        NetworkResult.Exception(e)
    }
}
