package io.github.naharaoss.skpd.document.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.document.DocumentAccess

/**
 * Common interface for Android views that can draw on Sketchpad documents.
 *
 * This is the interface for both regular and low latency views. This unified interface makes it
 * easy to swap between regular and low latency graphics mode.
 */
interface DocumentViewInterface {
    /**
     * Color of drawing board background.
     *
     * One can think of drawing board as just a UI background. The canvas also have its own
     * background color as well. If the canvas is infinite, the drawing board background becomes
     * canvas' background.
     */
    var drawingBoardBackground: Color

    /**
     * Finger drawing mode.
     *
     * Allow user drawing with their finger.
     */
    var fingerDrawing: Boolean

    /**
     * Tap maximum distance.
     *
     * The maximum distance from initial touch point before the input processor determine the touch
     * input is a swipe instead of tap.
     */
    var touchSlop: Float

    /**
     * Currently active document.
     *
     * If the value is `null`, the view will only display the drawing board color. User cannot draw
     * on the canvas when the document is not active.
     *
     * The initial value is `null`.
     */
    var document: DocumentAccess?

    /**
     * Currently active layer.
     *
     * The active layer is the one that can be drawn on at current moment. Invisible layer is
     * allowed, but it won't be visible anyway. There must be an active layer in order to use the
     * brush.
     *
     * The initial value is `null`.
     */
    var layer: DocumentAccess.Layer?

    /**
     * Current canvas transformation matrix.
     *
     * The viewport (camera) is always fixed in-place, while the canvas moves around it. For
     * example, if the canvas is moving to the left, the camera may appear as moving to the right.
     *
     * The initial value is an identity matrix.
     */
    var canvasTransform: Matrix

    /**
     * Current brush preset.
     *
     * If the value is `null`, user cannot draw on the view.
     *
     * The initial value is `null`.
     */
    var brushPreset: BrushType.Preset?

    /**
     * Current brush color.
     *
     * The initial color is [Color.Black].
     */
    var brushColor: Color

    /**
     * Callback for transform gesture.
     *
     * This may be assigned with a callback function to transform the [canvasTransform] matrix when
     * user drag, zoom or rotate the canvas with gestures.
     */
    var onTransformGesture: ((Matrix) -> Unit)?

    /**
     * Callback for tap gesture.
     *
     * This may be assigned with a callback function to perform action when user tapped the canvas
     * with up to 5 fingers.
     */
    var onTapGesture: ((fingers: Int) -> Unit)?

    /**
     * Manually trigger document update.
     */
    fun triggerDocumentUpdate()
}