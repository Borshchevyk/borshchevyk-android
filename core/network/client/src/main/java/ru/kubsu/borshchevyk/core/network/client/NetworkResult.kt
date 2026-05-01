package ru.kubsu.borshchevyk.core.network.client

/**
 * Represents the outcome of a network operation.
 * Encapsulates success with data, HTTP errors, or low-level network exceptions.
 *
 * @param T The type of data expected on success.
 */
sealed interface NetworkResult<out T> {
    /**
     * Represents a successful network operation.
     *
     * @property data The parsed response data.
     */
    data class Success<out T>(val data: T) : NetworkResult<T>

    /**
     * Represents an HTTP error response from the server.
     *
     * @property code The HTTP status code.
     * @property message The error message returned by the server, if any.
     */
    data class Error(val code: Int, val message: String?) : NetworkResult<Nothing>

    /**
     * Represents a low-level network exception or client-side failure (e.g., timeout, parsing error).
     *
     * @property e The underlying exception that caused the failure.
     */
    data class Exception(val e: Throwable) : NetworkResult<Nothing>
}

/**
 * Transforms the data of a [NetworkResult.Success] using the provided [transform] function.
 * If the result is an error or exception, it propagates unchanged.
 *
 * @param transform The mapping function to apply to the successful data.
 * @return A new [NetworkResult] containing the transformed data, or the original error/exception.
 */
inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> {
    return when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data))
        is NetworkResult.Error -> NetworkResult.Error(code, message)
        is NetworkResult.Exception -> NetworkResult.Exception(e)
    }
}

/**
 * Extracts the data from a successful network response, or throws an exception.
 *
 * @return The underlying data [T].
 * @throws java.lang.Exception if the result is a network error.
 * @throws Throwable if the result is an underlying exception.
 */
fun <T> NetworkResult<T>.getOrThrow(): T {
    return when (this) {
        is NetworkResult.Success -> data
        is NetworkResult.Error -> throw java.lang.Exception("Network Error $code: $message")
        is NetworkResult.Exception -> throw e
    }
}
