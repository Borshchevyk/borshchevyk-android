package ru.kubsu.borshchevyk.core.network

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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.dto.RefreshRequest
import ru.kubsu.borshchevyk.core.model.dto.VerifyResponse
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
                            val response = client.post("https://borshchevik.su/api/v1/auth/refresh") {
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
                url("https://borshchevik.su/") 
                contentType(ContentType.Application.Json)
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
    fun bindContactNetworkDataSource(impl: KtorContactNetworkDataSource): ContactNetworkDataSource
}
