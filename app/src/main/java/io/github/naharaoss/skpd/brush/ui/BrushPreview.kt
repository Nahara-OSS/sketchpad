package io.github.naharaoss.skpd.brush.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.TextureView
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.graphics.BrushPreviewRenderer
import io.github.naharaoss.skpd.brush.graphics.rememberBrushPreviewRenderer
import io.github.naharaoss.skpd.utils.Color

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun BrushPreview(
    modifier: Modifier = Modifier,
    brushPreviewRenderer: BrushPreviewRenderer = rememberBrushPreviewRenderer(),
    preset: BrushType.Preset,
    color: Color? = null
) {
    val (r, g, b) = LocalContentColor.current
    val strokeColor = color ?: Color.Rgb(r, g, b)

    AndroidView(
        modifier = modifier,
        factory = { BrushPreviewView(it, brushPreviewRenderer, preset, strokeColor.toRgb()) }
    ) { view ->
        view.changeParams(preset, strokeColor.toRgb())
    }
}

@SuppressLint("ViewConstructor")
class BrushPreviewView(context: Context, renderer: BrushPreviewRenderer, preset: BrushType.Preset, color: Color.Rgb) : TextureView(context) {
    private val listener = renderer.createSurfaceTextureListener(preset, color)

    init {
        surfaceTextureListener = listener
        alpha = 254f / 255f // TODO: Do something about this trick

        // Compose did some kind of "optimization" where at 100% alpha, it would just cut a hole,
        // which ended up exposing the SurfaceView layer from the behind
    }

    fun changeParams(preset: BrushType.Preset, color: Color.Rgb) = listener.changeParams(preset, color)
}