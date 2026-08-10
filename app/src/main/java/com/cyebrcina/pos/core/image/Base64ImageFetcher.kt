package com.cyebrcina.pos.core.image

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.ImageLoader

/**
 * Fire Hut's menu/branding API (`GET /api/device/menu`, `imageUrl`/`logoUrl` fields) returns
 * images as inline `data:image/...;base64,...` data URIs — the images live in the database, not
 * as hosted files. Coil's default fetchers (OkHttp/File/ContentUri) don't decode these, so
 * product photos and the store logo would silently fail to load without this. Wrapped in a
 * dedicated [Base64Image] model type (rather than routing raw Strings through Coil's built-in
 * String→Uri mapping) to avoid relying on [android.net.Uri] round-tripping a large base64 blob.
 */
data class Base64Image(val dataUri: String)

/** `null` if [imageUrl] isn't a data URI (blank, or a future hosted-URL field) — passed through unwrapped for Coil's normal handling. */
fun imageModel(imageUrl: String?): Any? {
    if (imageUrl.isNullOrBlank()) return null
    return if (imageUrl.startsWith("data:")) Base64Image(imageUrl) else imageUrl
}

class Base64ImageFetcher(private val model: Base64Image, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val base64 = model.dataUri.substringAfter(",", missingDelimiterValue = "")
        require(base64.isNotEmpty()) { "Malformed data URI: no base64 payload" }
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Could not decode image data")
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Base64Image> {
        override fun create(data: Base64Image, options: Options, imageLoader: ImageLoader): Fetcher = Base64ImageFetcher(data, options)
    }
}
