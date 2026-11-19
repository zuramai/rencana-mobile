package com.zuramai.rencana.data

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient

/**
 * Coil loader for backend images. It reuses [api]'s Ktor client, so the bearer token (and refresh) applies, but
 * only to URLs on the backend host (`ApiClient` attaches the token by host). No disk cache: tickets stay out of
 * the file system.
 */
fun createAuthImageLoader(context: PlatformContext, api: ApiClient): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = api.http)) }
        .diskCache(null)
        .build()

/** Coil loader with a credential-free client, for third-party images such as Google avatars. No disk cache. */
fun createPublicImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = HttpClient())) }
        .diskCache(null)
        .build()
