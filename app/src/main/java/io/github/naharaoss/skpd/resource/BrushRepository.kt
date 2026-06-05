package io.github.naharaoss.skpd.resource

import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.utils.ApplicationScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class BrushRepository @Inject constructor(
    private val dao: AppDatabase.BrushDao,
    private val store: ResourceContentStore,
) {
    private val _addition = MutableSharedFlow<BrushItem>()
    private val _removal = MutableSharedFlow<BrushItem>()
    private val _update = MutableSharedFlow<BrushItem>()
    private val _presetUpdate = MutableSharedFlow<BrushItem>()
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
        val dbItem = dao.getById(item.id)
        val brushRoot = store.referenceRootOf(dbItem.reference)
        return withContext(Dispatchers.IO) { brushRoot.loadBrushPresetFromFolder() }
    }

    suspend fun changePreset(item: BrushItem, preset: BrushType.Preset) {
        val dbItem = dao.getById(item.id)
        val brushRoot = store.referenceRootOf(dbItem.reference)
        withContext(Dispatchers.IO) { brushRoot.storeBrushPresetToFolder(preset) }
        _presetUpdate.emit(item)
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