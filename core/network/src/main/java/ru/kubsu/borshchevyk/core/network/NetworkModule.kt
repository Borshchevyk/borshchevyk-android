package ru.kubsu.borshchevyk.core.network

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
                        // TODO: Implement actual token refresh logic via auth endpoint
                        tokenProvider.clearTokens()
                        null
                    }
                }
            }
            defaultRequest {
                url("https://borshchevik.su/") 
                contentType(ContentType.Application.Json)
            }
        }
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
