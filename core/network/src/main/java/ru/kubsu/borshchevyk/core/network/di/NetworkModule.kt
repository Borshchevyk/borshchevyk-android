package ru.kubsu.borshchevyk.core.network.di

import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.dto.RefreshRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyResponse
import ru.kubsu.borshchevyk.core.network.auth.AuthNetworkDataSource
import ru.kubsu.borshchevyk.core.network.auth.KtorAuthNetworkDataSource
import ru.kubsu.borshchevyk.core.network.auth.TokenProvider
import ru.kubsu.borshchevyk.core.network.chat.ChatNetworkDataSource
import ru.kubsu.borshchevyk.core.network.chat.KtorChatNetworkDataSource
import ru.kubsu.borshchevyk.core.network.media.KtorMediaNetworkDataSource
import ru.kubsu.borshchevyk.core.network.media.MediaNetworkDataSource
import ru.kubsu.borshchevyk.core.network.message.KtorMessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.message.MessageNetworkDataSource
import ru.kubsu.borshchevyk.core.network.user.ContactNetworkDataSource
import ru.kubsu.borshchevyk.core.network.user.KtorContactNetworkDataSource
import ru.kubsu.borshchevyk.core.network.user.KtorUserNetworkDataSource
import ru.kubsu.borshchevyk.core.network.user.UserNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.KrossbowWebSocketDataSource
import ru.kubsu.borshchevyk.core.network.websocket.WebSocketDataSource
import ru.kubsu.borshchevyk.core.network.NetworkConstants
import ru.kubsu.borshchevyk.core.network.NetworkMonitor
import ru.kubsu.borshchevyk.core.network.ConnectivityNetworkMonitor
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
        val client = HttpClient(OkHttp) {
            expectSuccess = true

            install(HttpTimeout) {
                requestTimeoutMillis = 60_000L
                connectTimeoutMillis = 60_000L
                socketTimeoutMillis = 60_000L
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorNetwork", message)
                    }
                }
                level = LogLevel.ALL
            }
            
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenProvider.getAccessToken()
                        val refreshToken = tokenProvider.getRefreshToken()
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }
                    
                    refreshTokens {
                        val refreshToken = tokenProvider.getRefreshToken() ?: return@refreshTokens null
                        try {
                            val response = client.post("${NetworkConstants.BASE_URL}/api/v1/auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(RefreshRequest(refreshToken))
                            }.body<VerifyResponse>()
                            
                            tokenProvider.saveTokens(response.accessToken, response.refreshToken)
                            BearerTokens(response.accessToken, response.refreshToken)
                        } catch (e: Exception) {
                            Log.e("KtorNetwork", "Token refresh failed", e)
                            tokenProvider.clearTokens()
                            null
                        }
                    }
                    
                    sendWithoutRequest { request ->
                        !request.url.pathSegments.contains("auth")
                    }
                }
            }
            
            defaultRequest {
                url("${NetworkConstants.BASE_URL}/") 
                contentType(ContentType.Application.Json)
            }
        }

        client.requestPipeline.intercept(HttpRequestPipeline.State) {
            val token = tokenProvider.getAccessToken()
            val host = context.url.host
            // Add Authorization header ONLY if requesting our own backend.
            // S3 pre-signed URLs (upload/download) will fail if we add an Authorization header.
            val isBackEndRequest = host.isEmpty() || host == "borshchevik.su" || host.endsWith(".borshchevik.su")
            val isS3Request = host.contains("s3.cloud.ru") || host.contains("amazonaws.com")
            
            if (token != null && isBackEndRequest && !isS3Request && !context.url.pathSegments.contains("auth")) {
                context.headers.remove(io.ktor.http.HttpHeaders.Authorization)
                context.headers.append(io.ktor.http.HttpHeaders.Authorization, "Bearer $token")
            }
        }

        return client
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface NetworkDataSourceModule {
    @Binds
    @Singleton
    fun bindAuthNetworkDataSource(impl: KtorAuthNetworkDataSource): AuthNetworkDataSource

    @Binds
    @Singleton
    fun bindUserNetworkDataSource(impl: KtorUserNetworkDataSource): UserNetworkDataSource

    @Binds
    @Singleton
    fun bindChatNetworkDataSource(impl: KtorChatNetworkDataSource): ChatNetworkDataSource

    @Binds
    @Singleton
    fun bindMessageNetworkDataSource(impl: KtorMessageNetworkDataSource): MessageNetworkDataSource

    @Binds
    @Singleton
    fun bindContactNetworkDataSource(impl: KtorContactNetworkDataSource): ContactNetworkDataSource

    @Binds
    @Singleton
    fun bindMediaNetworkDataSource(impl: KtorMediaNetworkDataSource): MediaNetworkDataSource

    @Binds
    @Singleton
    fun bindWebSocketDataSource(impl: KrossbowWebSocketDataSource): WebSocketDataSource

    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
