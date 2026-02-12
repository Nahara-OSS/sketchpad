package io.github.naharaoss.skpd

import io.github.naharaoss.skpd.engine.brush.Brush
import io.github.naharaoss.skpd.impl.brush.dab.DabBrush

data class AppSettings(
    val general: General,
    val toolbar: Toolbar,
    val performance: Performance
) {
    companion object {
        /**
         * Default app settings.
         */
        val Default = AppSettings(
            general = General(
                sliderMode = General.SliderMode.Logarithmic,
            ),
            toolbar = Toolbar(
                side = Toolbar.Side.DockedTop,
                darkTheme = true,
                primary = listOf(
                    Toolbar.BrushPreset("builtin:pencil"),
                    Toolbar.BrushPreset("builtin:ink"),
                    Toolbar.BrushPreset("builtin:fill")
                ),
                secondary = listOf(
                    Toolbar.ColorSampler,
                    Toolbar.ColorPicker
                )
            ),
            performance = Performance(
                lowLatency = Performance.LowLatency.SizeThreshold(80f),
                tileSize = 128,
                compressTiles = true
            )
        )
    }

    data class General(
        val sliderMode: SliderMode
    ) {
        enum class SliderMode {
            Linear,
            Logarithmic,
        }
    }

    data class Toolbar(
        /**
         * Which side to place the toolbar.
         */
        val side: Side,

        /**
         * Whether to force dark theme on toolbar. Typically used together with [Side.DockedTop] to
         * hide the camera cutout.
         */
        val darkTheme: Boolean,

        /**
         * A list of items to place on the start of toolbar. The start of toolbar is typically left
         * (if docked to top or bottom) or top (if docked to left or right).
         */
        val primary: List<Item>,

        /**
         * A list of items to place on the end of toolbar. The end of toolbar is typically right (if
         * docked to top or bottom) or bottom (if docked to left or right).
         */
        val secondary: List<Item>
    ) {
        enum class Side {
            /**
             * Dock the toolbar to the top as top app bar.
             */
            DockedTop,

            /**
             * Dock the toolbar to the bottom as docked horizontal toolbar.
             */
            DockedBottom,

            /**
             * Dock the toolbar to the left as floating vertical toolbar.
             */
            FloatingLeft,

            /**
             * Dock the toolbar to the right as floating vertical toolbar.
             */
            FloatingRight,
        }

        sealed interface Item

        /**
         * Pick color in popup.
         */
        object ColorPicker : Item

        /**
         * Pick color on canvas.
         */
        object ColorSampler : Item

        /**
         * Select brush preset by preset ID.
         */
        data class BrushPreset(val id: String) : Item
    }

    data class Performance(
        val lowLatency: LowLatency,
        val tileSize: Int,
        val compressTiles: Boolean
    ) {
        sealed interface LowLatency {
            val useLowLatencyView: Boolean

            fun checkEnabled(brush: Brush): Boolean

            /**
             * Always disable low latency mode.
             */
            object Disabled : LowLatency {
                override val useLowLatencyView: Boolean get() = false

                override fun checkEnabled(brush: Brush) = false
            }

            /**
             * Only enable low latency mode when the brush base size is smaller than threshold.
             */
            data class SizeThreshold(val size: Float) : LowLatency {
                override val useLowLatencyView: Boolean get() = true

                override fun checkEnabled(brush: Brush): Boolean {
                    return when {
                        brush is DabBrush && brush.size.base >= size -> false
                        else -> true
                    }
                }
            }

            /**
             * Always enable low latency mode.
             */
            object AlwaysEnabled : LowLatency {
                override val useLowLatencyView: Boolean get() = true

                override fun checkEnabled(brush: Brush) = true
            }
        }
    }
}
