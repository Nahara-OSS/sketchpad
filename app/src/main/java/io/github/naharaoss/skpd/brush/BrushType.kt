package io.github.naharaoss.skpd.brush

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.utils.GLFramebuffer

/**
 * **Separation of tile processing and tile rendering**: The processing and rendering stages are
 * separated because the processing stage is meant to be used inside event handler, and the
 * rendering stage is meant to be called once per frame or when pasting to canvas.
 *
 * @param [P] Preset type, which must be immutable (usually the type is a data class/record)
 */
interface BrushType<P : BrushType.Preset> {
    /**
     * Default brush preset.
     */
    val defaultPreset: P

    val allParameters: List<ParameterInfo<P>>

    /**
     * Create a new brush renderer.
     *
     * This will be called every time the brush type is changed, along with closing the old
     * renderer associated with previous brush type.
     */
    @WorkerThread
    fun createRenderer(): Renderer<P>

    interface ParameterInfo<P : Preset> {
        val parameter: String
        @get:StringRes val nameRes: Int
        @get:DrawableRes val iconRes: Int
        val min: Float
        val max: Float
        val centered: Boolean
        @Composable fun formatValue(value: Float): String
        fun forwardMapToSlider(value: Float): Float
        fun backwardMapToSlider(slider: Float): Float
        fun getDynamic(preset: P): Dynamic
        fun replaceDynamic(preset: P, dynamic: Dynamic): P

        @Suppress("UNCHECKED_CAST")
        fun getDynamicTypeErased(preset: Preset) = getDynamic(preset as P)

        @Suppress("UNCHECKED_CAST")
        fun replaceDynamicTypeErased(preset: Preset, dynamic: Dynamic): Preset = replaceDynamic(preset as P, dynamic)
    }

    interface Preset {
        val type: BrushType<*>
    }

    /**
     * Interface for brush renderers.
     *
     * This interface should only be used inside rendering thread. Each brush renderer should only
     * be associated with a single OpenGL context.
     */
    interface Renderer<P : Preset> : AutoCloseable {
        /**
         * Brush type.
         */
        val type: BrushType<P>

        /**
         * Set new brush preset.
         *
         * Called when user changed the brush preset. The renderer should reconfigure its internal
         * states when this method is called. It is guaranteed to be called at least once before
         * [beginStroke].
         */
        fun usePreset(preset: P)

        /**
         * Set new brush base color.
         *
         * Called when user changed the color. It is guaranteed to be called at least one before
         * [beginStroke]. It may be called before or after [usePreset].
         */
        fun useColor(color: Color)

        /**
         * Begin new stroke.
         *
         * This method is called when user started drawing, right before [consumeInput] is called.
         * The renderer should reset or clear its internal states when this method is called.
         */
        fun beginStroke()

        /**
         * Consume next input event.
         *
         * This method is called once before calling [consumeTile] on each tile that is overlapping
         * the returned rectangle from this method.
         *
         * @return The returned value is the bounds of affected area on the canvas. This will be
         * used to detect and select the tiles that are overlapping the affected area for further
         * processing.
         */
        fun consumeInput(input: StylusInput): Rect

        /**
         * Consume the tile.
         *
         * This method should be rendering the tile content to internal framebuffer associated with
         * the tile.
         *
         * @param [tileKey] Unique key, meant to be used with [Map]
         * @param [tileRect] The rectangle area of the tile on canvas
         */
        fun consumeTile(tileKey: Any, tileRect: Rect)

        /**
         * Render the tile to framebuffer.
         *
         * @param [tileKey] Unique tile key which was provided from [consumeTile]
         * @param [tileRect] The rectangle area of the tile on canvas
         * @param [framebuffer] The OpenGL framebuffer
         * @param [transform] Transformation matrix that transforms tile's rectangle to framebuffer
         * clip space
         */
        fun renderTile(
            tileKey: Any,
            tileRect: Rect,
            framebuffer: GLFramebuffer,
            transform: Matrix
        )

        /**
         * Finish current stroke.
         *
         * This method is called when user stopped drawing. Implement this method to clear internal
         * states and save resources.
         */
        fun endStroke()
    }

    @Suppress("UNCHECKED_CAST")
    @WorkerThread
    fun createTypeErasedRenderer(): Renderer<Preset> {
        val renderer = createRenderer()
        return object : Renderer<Preset> {
            override val type: BrushType<Preset> get() = renderer.type as BrushType<Preset>
            override fun usePreset(preset: Preset) = renderer.usePreset(preset as P)
            override fun useColor(color: Color) = renderer.useColor(color)
            override fun beginStroke() = renderer.beginStroke()
            override fun consumeInput(input: StylusInput): Rect = renderer.consumeInput(input)
            override fun consumeTile(tileKey: Any, tileRect: Rect) = renderer.consumeTile(tileKey, tileRect)
            override fun renderTile(tileKey: Any, tileRect: Rect, framebuffer: GLFramebuffer, transform: Matrix) = renderer.renderTile(tileKey, tileRect, framebuffer, transform)
            override fun endStroke() = renderer.endStroke()
            override fun close() = renderer.close()
        }
    }
}