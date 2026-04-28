package ru.kubsu.borshchevyk.core.network.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
            
            defaultRequest {
                url("https://borshchevik.su/")
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
