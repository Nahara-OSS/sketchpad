package io.github.naharaoss.skpd.resource

import io.github.naharaoss.skpd.document.SketchpadDocumentV1
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.utils.Color
import io.github.naharaoss.skpd.utils.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

class LibraryRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dao: AppDatabase.LibraryDao,
    private val store: ResourceContentStore
) {
    private val _addition = MutableSharedFlow<LibraryItem>()
    private val _removal = MutableSharedFlow<LibraryItem>()
    private val _update = MutableSharedFlow<LibraryItem>()
    val addition = _addition.asSharedFlow()
    val removal = _removal.asSharedFlow()
    val update = _update.asSharedFlow()

    suspend fun getContent(folder: LibraryItem.Folder) = (if (folder != LibraryItem.Root) dao.getChildrenByParentId(folder.id) else dao.getRoot())
        .map {
            when (it.reference) {
                null -> LibraryItem.Folder(it.libraryId, it.name, it.creationTime, it.lastModified)
                else -> LibraryItem.Document(it.libraryId, it.name, it.creationTime, it.lastModified)
            }
        }

    suspend fun getDocumentById(id: Long): LibraryItem.Document {
        val item = dao.getById(id)
        if (item.reference == null) throw Exception("Not a document: $id")
        return LibraryItem.Document(item.libraryId, item.name, item.creationTime, item.lastModified)
    }

    suspend fun createFolder(parent: LibraryItem.Folder, name: String): LibraryItem.Folder {
        val now = Clock.System.now()
        val id = dao.insert(AppDatabase.LibraryItem(
            parentId = if (parent == LibraryItem.Root) null else parent.id,
            name = name,
            creationTime = now,
            lastModified = now,
            reference = null
        ))
        val item = LibraryItem.Folder(
            id = id,
            name = name,
            creationTime = now,
            lastModified = now,
        )
        _addition.emit(item)
        return item
    }

    suspend fun createDocument(
        parent: LibraryItem.Folder,
        name: String,
        size: Size,
        backgroundColor: Color,
        backgroundAlpha: Float
    ): LibraryItem.Document {
        val now = Clock.System.now()
        val reference = UUID.randomUUID().toString()
        val documentRoot = store.referenceRootOf(reference)
        val documentMainPath = File(documentRoot, "main.skpd").toPath()

        withContext(Dispatchers.IO) {
            SketchpadDocumentV1.init(
                channel = FileChannel.open(documentMainPath, CREATE, READ, WRITE),
                tileSizeLog = settingsRepository.settings.value.performance.defaultTileSizeLog,
                size = size,
                backgroundColor = backgroundColor,
                backgroundAlpha = backgroundAlpha
            ).use { document ->
                val layer = document.addLayer()
                document.activeLayer = layer
            }
        }

        val id = dao.insert(AppDatabase.LibraryItem(
            parentId = if (parent == LibraryItem.Root) null else parent.id,
            name = name,
            creationTime = now,
            lastModified = now,
            reference = reference
        ))
        val item = LibraryItem.Document(
            id = id,
            name = name,
            creationTime = now,
            lastModified = now
        )
        _addition.emit(item)
        return item
    }

    suspend fun renameItem(item: LibraryItem, newName: String): LibraryItem {
        val now = Clock.System.now()
        val dbItem = dao.getById(item.id)
        dao.update(dbItem.copy(name = newName, lastModified = now))
        val item = item.copyWith(name = newName, lastModified = now)
        _update.emit(item)
        return item
    }

    suspend fun deleteItem(item: LibraryItem) {
        val dbItem = dao.getById(item.id)

        when (item) {
            is LibraryItem.Folder -> {
                val children = getContent(item)
                for (item in children) deleteItem(item)
            }

            is LibraryItem.Document if (dbItem.reference != null) -> {
                val documentRoot = store.referenceRootOf(dbItem.reference)
                withContext(Dispatchers.IO) { documentRoot.deleteRecursively() }
            }

            else -> {}
        }

        dao.delete(dbItem)
        _removal.emit(item)
    }

    suspend fun openDocument(document: LibraryItem.Document): SketchpadDocumentV1 {
        val reference = dao.getById(document.id).reference ?: throw Exception("Not a document: ${document.id} (${document.name})")
        val documentRoot = store.referenceRootOf(reference)
        val documentMainPath = File(documentRoot, "main.skpd").toPath()
        val channel = withContext(Dispatchers.IO) { FileChannel.open(documentMainPath, READ, WRITE) }
        return SketchpadDocumentV1.load(channel)
    }
}