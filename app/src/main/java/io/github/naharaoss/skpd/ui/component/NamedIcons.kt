package io.github.naharaoss.skpd.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R

const val FavoriteIconName = "Favorite"
const val PencilIconName = "Pencil"
const val PenIconName = "Pen"
const val FountainIconName = "Fountain"
const val BrushIconName = "Brush"
const val HighlighterIconName = "Highlighter"
const val EraserIconName = "Eraser"
const val TextureIconName = "Texture"

fun resourceIdFromNamedIcon(iconId: String) = when (iconId) {
    FavoriteIconName -> R.drawable.star_24px
    PencilIconName -> R.drawable.stylus_pencil_24px
    PenIconName -> R.drawable.stylus_pen_24px
    FountainIconName -> R.drawable.stylus_fountain_pen_24px
    BrushIconName -> R.drawable.stylus_brush_24px
    HighlighterIconName -> R.drawable.stylus_highlighter_24px
    EraserIconName -> R.drawable.ink_eraser_24px
    TextureIconName -> R.drawable.texture_24px
    else -> R.drawable.question_mark_24px
}

val AllIconNames = listOf(
    FavoriteIconName,
    PencilIconName,
    PenIconName,
    FountainIconName,
    BrushIconName,
    HighlighterIconName,
    EraserIconName,
    TextureIconName
)

@Composable
fun IconPicker(
    modifier: Modifier = Modifier,
    icon: String?,
    onIconSelect: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            FlowRow(Modifier.align(Alignment.Center).padding(vertical = 8.dp)) {
                AllIconNames.forEach { iconId ->
                    FilledIconToggleButton(
                        checked = icon == iconId,
                        onCheckedChange = { onIconSelect(iconId) }
                    ) {
                        Icon(
                            painter = painterResource(resourceIdFromNamedIcon(iconId)),
                            contentDescription = iconId
                        )
                    }
                }
            }
        }
    }
}