package ru.kubsu.borshchevyk

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import ru.kubsu.borshchevyk.core.network.client.TokenProvider
import javax.inject.Inject

@HiltAndroidApp
class BorshchevykApplication : Application(), ImageLoaderFactory {
    
    @Inject
    lateinit var tokenProvider: TokenProvider

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlString = request.url.toString()
                
                // Rewrite /avatars/ to /api/v1/media/avatars/
                var newUrl = request.url
                if (urlString.startsWith("https://borshchevik.su/avatars/")) {
                    newUrl = request.url.newBuilder()
                        .encodedPath(request.url.encodedPath.replace("/avatars/", "/api/v1/media/avatars/"))
                        .build()
                }

                var newRequest = request.newBuilder().url(newUrl).build()
                
                // Add Authorization header for our backend
                if (newUrl.host == "borshchevik.su") {
                    val token = tokenProvider.getAccessTokenSync()
                    if (token != null) {
                        newRequest = newRequest.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    }
                }
                
                chain.proceed(newRequest)
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                
                // Strip Authorization header if redirecting to S3 to prevent "Only one auth mechanism allowed"
                if (host.contains("s3") || host.contains("amazonaws")) {
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(request)
                }
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false) // Force caching of S3 presigned URLs despite restrictive headers
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
