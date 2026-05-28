package io.github.naharaoss.skpd.settings

import kotlinx.serialization.Serializable

/**
 * Miscellaneous settings.
 *
 * Some settings that just can't fit anywhere else.
 */
@Serializable
data class MiscellaneousSettings(
    /**
     * Enable promotional artworks.
     *
     * For every major release of Nahara's Sketchpad, there will be several artworks bundled with
     * the app and unpacked in the user's library to showcase new features. Some users might find
     * this annoying though, so there is a toggle to disable it. All promotional artworks will be
     * licensed under CC-BY-4.0.
     */
    val promotionalArtworks: Boolean = true,

    /**
     * Keep the screen on.
     *
     * Keep the display always on while the document is visible. May consume a lot of power!
     */
    val keepScreenOn: Boolean = false,
)