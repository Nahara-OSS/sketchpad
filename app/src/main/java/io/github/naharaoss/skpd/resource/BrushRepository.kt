package io.github.naharaoss.skpd.resource

import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.utils.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class BrushRepository @Inject constructor(
    private val dao: AppDatabase.BrushDao,
    private val store: ResourceContentStore,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    private val _addition = MutableSharedFlow<BrushItem>()
    private val _removal = MutableSharedFlow<BrushItem>()
    private val _update = MutableSharedFlow<BrushItem>()
    private val _presetUpdate = MutableSharedFlow<BrushItem>()
    private val cachedPresets = mutableMapOf<Long, Pair<Int, BrushType.Preset?>>()
    private val cacheRemoveJob = mutableMapOf<Long, Job>()
    private val presetCacheLock = ReentrantLock()
    val addition = _addition.asSharedFlow()
    val removal = _removal.asSharedFlow()
    val update = _update.asSharedFlow()
    val presetUpdate = _presetUpdate.asSharedFlow()

    suspend fun getAll(keyword: String? = null) = (when {
        keyword != null -> dao.searchAll(keyword)
        else -> dao.getAll()
    })
        .map(::wrap)
        .sortedBy { it.name }

    suspend fun getById(id: Long) = dao.getById(id).let {
        BrushItem(
            id = it.brushId,
            name = it.name,
            icon = it.icon
        )
    }

    suspend fun createBrush(name: String, icon: String?, preset: BrushType.Preset): BrushItem {
        val reference = UUID.randomUUID().toString()
        val brushRoot = store.referenceRootOf(reference)
        withContext(Dispatchers.IO) { brushRoot.storeBrushPresetToFolder(preset) }

        val id = dao.insert(AppDatabase.Brush(
            name = name,
            icon = icon,
            reference = reference
        ))
        val item = BrushItem(
            id = id,
            name = name,
            icon = icon
        )
        _addition.emit(item)
        return item
    }

    suspend fun getPreset(item: BrushItem): BrushType.Preset {
        val p = cachedPresets[item.id]
        if (p?.second != null) return p.second!!

        val dbItem = dao.getById(item.id)
        val brushRoot = store.referenceRootOf(dbItem.reference)
        val preset = withContext(Dispatchers.IO) { brushRoot.loadBrushPresetFromFolder() }

        if (p != null) {
            presetCacheLock.withLock {
                cachedPresets[item.id]?.let {
                    cachedPresets[item.id] = it.copy(second = preset)
                }
            }
        }

        return preset
    }

    suspend fun changePreset(item: BrushItem, preset: BrushType.Preset) {
        presetCacheLock.withLock {
            cachedPresets.computeIfPresent(item.id) { _, p -> p.copy(second = preset) }
        }

        val dbItem = dao.getById(item.id)
        val brushRoot = store.referenceRootOf(dbItem.reference)
        withContext(Dispatchers.IO) { brushRoot.storeBrushPresetToFolder(preset) }
        _presetUpdate.emit(item)
    }

    fun cachePreset(item: BrushItem) = object : AutoCloseable {
        init {
            presetCacheLock.withLock {
                val pair = cachedPresets.getOrPut(item.id, { Pair(0, null) })
                cachedPresets[item.id] = pair.copy(first = pair.first + 1)
                cacheRemoveJob.remove(item.id)?.cancel()
            }
        }

        override fun close() {
            val ref = presetCacheLock.withLock {
                val pair = cachedPresets[item.id] ?: return@withLock 0
                val ref = pair.first - 1
                cachedPresets[item.id] = pair.copy(first = ref)
                ref
            }

            if (ref == 0) {
                val job = scope.launch {
                    delay(5000.milliseconds)
                    presetCacheLock.withLock { cachedPresets.remove(item.id) }
                }

                presetCacheLock.withLock {
                    cacheRemoveJob[item.id] = job
                }
            }
        }
    }

    suspend fun renameBrush(item: BrushItem, name: String): BrushItem {
        val dbItem = dao.getById(item.id)
        dao.update(dbItem.copy(name = name))
        val item = item.copy(name = name)
        _update.emit(item)
        return item
    }

    suspend fun deleteBrush(item: BrushItem) {
        val dbItem = dao.getById(item.id)
        val brushRoot = store.referenceRootOf(dbItem.reference)
        withContext(Dispatchers.IO) { brushRoot.deleteRecursively() }
        dao.delete(dbItem)
        _removal.emit(item)
    }

    private fun wrap(dbItem: AppDatabase.Brush) = BrushItem(
        id = dbItem.brushId,
        name = dbItem.name,
        icon = dbItem.icon
    )
}