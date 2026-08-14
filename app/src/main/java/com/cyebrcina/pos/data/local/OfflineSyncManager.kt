package com.cyebrcina.pos.data.local

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import com.cyebrcina.pos.core.image.imageModel
import com.cyebrcina.pos.data.repository.MenuRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class OfflineSyncStatus(
    val isSyncing: Boolean = false,
    val totalImages: Int = 0,
    val downloadedImages: Int = 0,
    val error: String? = null,
    /** ISO-8601 instant, from MenuCacheStore — set as soon as this is known (on init and after
     * every sync), independent of [isSyncing]/whether a sync has run this session. */
    val lastSyncedAt: String? = null,
)

/**
 * Backs Settings' "Offline Mode" section: an explicit "Download for Offline Use" action, on top
 * of the automatic caching FireHutMenuRepositoryImpl/MenuCacheStore already do on every regular
 * menu refresh. That automatic path only ever caches whatever the till happened to fetch during
 * normal use — this does a deliberate, complete pass so staff can proactively get a till ready
 * before a known outage (or just confirm offline mode is actually ready) instead of hoping the
 * right things were already cached.
 *
 * The menu JSON itself (categories/products/add-ons) is already fully covered by
 * [MenuCacheStore] — this exists for the other half: product/category **images**. Fire Hut's
 * menu images are either inline base64 data URIs (already embedded in that same JSON, so no
 * extra work needed) or real hosted URLs (`Category.imageUrl`/`Product.imageUrl` pointing at
 * Admin's `/uploads/media/...`, resolved server-side — see Admin's `resolveMediaUrl`) which Coil
 * only ever caches to disk if/when it's actually rendered a given image before. A product whose
 * photo was never scrolled into view on this till would otherwise show a broken image offline
 * even though the item itself is fully browsable/orderable — this forces every image through
 * Coil's loader once, up front, so its disk cache has everything by the time a connection drops.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val menuRepository: MenuRepository,
    private val menuCacheStore: MenuCacheStore,
) {
    private val _status = MutableStateFlow(OfflineSyncStatus())
    val status: StateFlow<OfflineSyncStatus> = _status

    suspend fun refreshLastSyncedAt() {
        _status.value = _status.value.copy(lastSyncedAt = menuCacheStore.lastCachedAt())
    }

    suspend fun syncForOffline() {
        _status.value = OfflineSyncStatus(isSyncing = true, lastSyncedAt = _status.value.lastSyncedAt)

        val refreshResult = menuRepository.refresh()
        if (refreshResult.isFailure) {
            _status.value = _status.value.copy(isSyncing = false, error = refreshResult.exceptionOrNull()?.message ?: "Couldn't reach the server")
            return
        }

        val imageUrls = buildList {
            menuRepository.categories.value.forEach { category ->
                category.imageUrl?.let(::add)
                category.products.forEach { product -> product.imageUrl?.let(::add) }
            }
        }.distinct()

        _status.value = _status.value.copy(totalImages = imageUrls.size, downloadedImages = 0)

        val loader = context.imageLoader
        var downloaded = 0
        for (url in imageUrls) {
            val model = imageModel(url)
            if (model != null) {
                runCatching { loader.execute(ImageRequest.Builder(context).data(model).build()) }
            }
            downloaded++
            _status.value = _status.value.copy(downloadedImages = downloaded)
        }

        _status.value = _status.value.copy(isSyncing = false, lastSyncedAt = menuCacheStore.lastCachedAt())
    }
}
