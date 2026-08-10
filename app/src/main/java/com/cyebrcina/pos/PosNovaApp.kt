package com.cyebrcina.pos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.cyebrcina.pos.core.image.Base64ImageFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PosNovaApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(Base64ImageFetcher.Factory()) }
            .build()
}
