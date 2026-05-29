package io.github.naharaoss.skpd.resource

/**
 * Badges system.
 *
 * Badges in Sketchpad are used for identifying whenever user updated the app or perform certain
 * action that should only be triggered once.
 */
object Badges {
    /**
     * App installation badge.
     *
     * This badge is for detecting whether the app was launched for the first time since
     * installation (or after app data wipe). This badge will be used for populating the brush list
     * with factory presets and library with sample artworks.
     */
    const val INSTALLED_SKETCHPAD = "installed-sketchpad"
}