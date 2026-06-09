package io.github.naharaoss.skpd.toolbar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.naharaoss.skpd.resource.BrushRepository
import io.github.naharaoss.skpd.utils.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolbarRepository @Inject constructor(
    private val factory: ToolbarFactoryDataSource,
    private val local: ToolbarLocalDataSource,
    private val brush: BrushRepository,
    @ApplicationScope scope: CoroutineScope
) {
    private val _toolbars = MutableStateFlow<List<Toolbar>>(emptyList())
    val toolbars = _toolbars.asStateFlow()

    init {
        scope.launch {
            _toolbars.value = resolve(local.readToolbars() ?: factory.readFactoryToolbars())
        }
    }

    suspend fun writeToolbars(toolbars: List<Toolbar>) {
        val toolbars = resolve(toolbars)
        _toolbars.value = toolbars
        local.writeToolbars(toolbars)
    }

    suspend fun resolve(toolbars: List<Toolbar>): List<Toolbar> {
        val unresolved = toolbars.any { tb ->
            tb.tools.any { tl -> tl.content is Tool.Brush && tl.content.brushId == null }
        }

        if (unresolved) {
            val usedBrushIds = toolbars.flatMap { toolbar ->
                toolbar.tools
                    .map { it.content }
                    .filterIsInstance<Tool.Brush>()
                    .mapNotNull { it.brushId }
            }

            val availableBrushIds = brush.getAll()
                .map { it.id }
                .filter { !usedBrushIds.contains(it) }
                .toMutableList()

            return toolbars.map { toolbar ->
                toolbar.copy(tools = toolbar.tools.map { tool ->
                    if (tool.content is Tool.Brush && tool.content.brushId == null) tool.copy(content = tool.content.copy(brushId = availableBrushIds.removeFirstOrNull()))
                    else tool
                })
            }
        }

        return toolbars
    }
}

@Singleton
class ToolbarFactoryDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    suspend fun readFactoryToolbars(): List<Toolbar> = listOf(
        Toolbar(
            id = UUID.nameUUIDFromBytes("toolbar 1".toByteArray(Charsets.UTF_8)),
            side = Toolbar.Side.Top,
            position = Toolbar.Position.Start,
            docked = true,
            tools = listOf(
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("exit".toByteArray(Charsets.UTF_8)),
                    content = Tool.Exit
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("exit".toByteArray(Charsets.UTF_8)),
                    content = Tool.Undo
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("exit".toByteArray(Charsets.UTF_8)),
                    content = Tool.Redo
                )
            )
        ),
        Toolbar(
            id = UUID.nameUUIDFromBytes("toolbar 2".toByteArray(Charsets.UTF_8)),
            side = Toolbar.Side.Top,
            position = Toolbar.Position.End,
            docked = true,
            tools = listOf(
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("reset transform".toByteArray(Charsets.UTF_8)),
                    content = Tool.ResetTransform
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("layers".toByteArray(Charsets.UTF_8)),
                    content = Tool.Layers
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("menu".toByteArray(Charsets.UTF_8)),
                    content = Tool.Menu
                )
            )
        ),
        Toolbar(
            id = UUID.nameUUIDFromBytes("toolbar 3".toByteArray(Charsets.UTF_8)),
            side = Toolbar.Side.Left,
            position = Toolbar.Position.Center,
            docked = false,
            tools = listOf(
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("quick brush 1".toByteArray(Charsets.UTF_8)),
                    content = Tool.Brush(null)
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("quick brush 2".toByteArray(Charsets.UTF_8)),
                    content = Tool.Brush(null)
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("quick brush 3".toByteArray(Charsets.UTF_8)),
                    content = Tool.Brush(null)
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("color picker".toByteArray(Charsets.UTF_8)),
                    content = Tool.ColorPicker
                ),
                Toolbar.ToolInfo(
                    id = UUID.nameUUIDFromBytes("color sampler".toByteArray(Charsets.UTF_8)),
                    content = Tool.ColorSampler
                )
            )
        )
    )
}

@Singleton
class ToolbarLocalDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    val file = File(context.filesDir, "toolbars.json")

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readToolbars(): List<Toolbar>? = withContext(Dispatchers.IO) {
        if (!file.exists()) null
        else file.inputStream().use { Json.decodeFromStream(it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun writeToolbars(toolbars: List<Toolbar>) = withContext(Dispatchers.IO) {
        file.outputStream().use { Json.encodeToStream(toolbars, it) }
    }
}