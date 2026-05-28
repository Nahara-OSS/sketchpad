package io.github.naharaoss.skpd.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.naharaoss.skpd.R
import kotlinx.serialization.Serializable

/**
 * Settings for a single toolbar.
 *
 * This is the settings for a single toolbar only. There can be multiple toolbars in the viewport.
 */
@Serializable
data class Toolbar(
    /**
     * Toolbar docking side.
     *
     * The side of the screen that the toolbar will be attached to.
     */
    val side: Side,

    /**
     * Toolbar alignment.
     *
     * The placement of the toolbar on the attached side.
     */
    val align: Align,

    /**
     * Toolbar docked state.
     *
     * Whether the toolbar should be docked, or floating otherwise. When there is at least 1 docked
     * toolbar on certain side, the viewport will be pushed to leave space for that side. Some users
     * might want to dock the toolbars and use dark theme in order to hide the camera cutout.
     */
    val docked: Boolean,

    /**
     * List of tools.
     *
     * List of tools on this toolbar.
     */
    val tools: List<Tool>,
) {
    enum class Orientation {
        Row,
        Column
    }

    enum class Side(val orientation: Orientation) {
        Top(Orientation.Row),
        Bottom(Orientation.Row),
        Start(Orientation.Column),
        End(Orientation.Column)
    }

    enum class Align {
        Start,
        Middle,
        End
    }

    @Serializable
    sealed interface Tool {
        @get:Composable val name: String

        @get:Composable val description: String

        @get:Composable val icon: Painter

        /**
         * Composable for tool editor.
         *
         * Call the [onChange] function to signal that the tool options has been changed.
         */
        @Composable
        fun Editor(onChange: (Tool) -> Unit) {
        }

        /**
         * Brush tool.
         *
         * Defaults to the first brush in the favorite list (or the first one in the library if
         * there are none). Tap to select the brush tool, tap again (or long tap) to open the
         * library to select different brush, as well as editing them.
         */
        object Brush : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_brush)
            override val description: String @Composable get() = stringResource(R.string.tool_brush_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Color picker tool.
         *
         * Open the pop-up to pick color on HSL wheel.
         */
        object ColorPicker : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_color_picker)
            override val description: String @Composable get() = stringResource(R.string.tool_color_picker_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Color sampling tool.
         *
         * Select the tool, then tap the color on canvas to sample.
         */
        object ColorSampler : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_color_sampler)
            override val description: String @Composable get() = stringResource(R.string.tool_color_sampler_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Back tool.
         *
         * Basically just an on-screen back button.
         */
        object Back : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_back)
            override val description: String @Composable get() = stringResource(R.string.tool_back_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.arrow_back_24px)
        }

        /**
         * Undo tool.
         */
        object Undo : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_undo)
            override val description: String @Composable get() = stringResource(R.string.tool_undo_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Redo tool.
         */
        object Redo : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_redo)
            override val description: String @Composable get() = stringResource(R.string.tool_redo_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Layer tool.
         *
         * Show all layers in the pop-up.
         */
        object Layers : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_layers)
            override val description: String @Composable get() = stringResource(R.string.tool_layers_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.edit_24px)
        }

        /**
         * Document menu tool.
         *
         * Show document actions in the pop-up.
         */
        object Menu : Tool {
            override val name: String @Composable get() = stringResource(R.string.tool_menu)
            override val description: String @Composable get() = stringResource(R.string.tool_menu_description)
            override val icon: Painter @Composable get() = painterResource(R.drawable.menu_24px)
        }

        companion object {
            /**
             * Get all tools with default options.
             *
             * Get a list of all tools with default options. This list is meant to be used inside
             * toolbar editor to add new tool.
             */
            val AllTools = listOf(
                Brush,
                ColorPicker,
                ColorSampler,
                Back,
                Undo,
                Redo,
                Layers,
                Menu
            )
        }
    }
}
