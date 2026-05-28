package io.github.naharaoss.skpd.resource

import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.utils.ApplicationScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class BrushRepository @Inject constructor(
    private val databaseProvider: AppDatabaseProvider,
    private val store: ResourceContentStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val database = databaseProvider.database
    private val _brushes = MutableStateFlow<List<BrushResource>?>(null)
    private val lock = ReentrantLock()
    private var brushesLoadTask: CompletableDeferred<List<BrushResource>>? = null
    val brushes = _brushes.asStateFlow()

    suspend fun getBrushes(): List<BrushResource> {
        val existingLoadTask = lock.withLock { brushesLoadTask }
        if (existingLoadTask != null) return existingLoadTask.await()

        val newLoadTask = CompletableDeferred<List<BrushResource>>()
        lock.withLock { brushesLoadTask = newLoadTask }

        val brushes = database.brushDao().getAll().map { wrap(it) }
        _brushes.value = brushes
        newLoadTask.complete(brushes)
        return _brushes.value ?: brushes
    }

    suspend fun createBrush(name: String, icon: String?, preset: BrushType.Preset): BrushResource {
        val reference = UUID.randomUUID().toString()
        val params = AppDatabase.Brush(name = name, icon = icon, reference = reference)
        val id = database.brushDao().insert(params)
        val entity = params.copy(brushId = id)
        val resource = wrap(entity)
        resource.store(preset)
        _brushes.update { (it ?: emptyList()) + resource }
        return resource
    }

    private fun wrap(entity: AppDatabase.Brush) = object : BrushResource(scope, store, entity.brushId, entity.reference, entity.name, entity.icon) {
        override suspend fun onMetadataUpdate(name: String, icon: String?) {
            database.brushDao().update(entity.copy(name = name, icon = icon))
        }

        override suspend fun onDelete() {
            database.brushDao().delete(entity)
            _brushes.update { brushes -> brushes?.filter { it == this } }
        }
    }
}