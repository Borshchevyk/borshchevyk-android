package ru.kubsu.borshchevyk.core.network.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.dto.RefreshRequest
import ru.kubsu.borshchevyk.core.network.dto.VerifyResponse
import ru.kubsu.borshchevyk.core.network.client.BuildConfig
import ru.kubsu.borshchevyk.core.network.client.TokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        json: Json,
        tokenProvider: TokenProvider
    ): HttpClient {
        return HttpClient(OkHttp) {
            expectSuccess = true

            install(HttpTimeout) {
                requestTimeoutMillis = BuildConfig.TIMEOUT_MILLIS
                connectTimeoutMillis = BuildConfig.TIMEOUT_MILLIS
                socketTimeoutMillis = BuildConfig.TIMEOUT_MILLIS
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
                modifyRequest { request ->
                    request.headers.append("X-Retry-Count", retryCount.toString())
                }
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorNetwork", message)
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
            }
            
            install(ContentNegotiation) {
                json(json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenProvider.getAccessToken()
                        val refresh = tokenProvider.getRefreshToken()
                        if (access != null && refresh != null) {
                            BearerTokens(access, refresh)
                        } else {
                            null
                        }
                    }
                    refreshTokens {
                        val refreshToken = tokenProvider.getRefreshToken() ?: return@refreshTokens null
                        try {
                            val response = client.post("api/v1/auth/refresh") {
                                setBody(RefreshRequest(refreshToken = refreshToken))
                            }.body<VerifyResponse>()
                            
                            tokenProvider.saveTokens(response.accessToken, response.refreshToken)
                            BearerTokens(response.accessToken, response.refreshToken)
                        } catch (e: Exception) {
                            tokenProvider.clearTokens()
                            null
                        }
                    }
                    sendWithoutRequest { request ->
                        val host = request.url.host
                        val isS3Request = host.contains("s3.cloud.ru") || host.contains("amazonaws.com")
                        val isAuthRequest = request.url.pathSegments.contains("auth")
                        val isRefreshRequest = request.url.pathSegments.contains("refresh")
                        
                        // DO NOT send tokens for S3 or general Auth requests (login/register)
                        // But DO send them for refresh requests or anything else
                        !isS3Request && (!isAuthRequest || isRefreshRequest)
                    }
                }
            }
            
            defaultRequest {
                url(BuildConfig.BASE_URL)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
