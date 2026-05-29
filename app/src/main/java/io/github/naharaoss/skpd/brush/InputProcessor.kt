package io.github.naharaoss.skpd.brush

import android.view.MotionEvent
import androidx.compose.ui.graphics.Matrix
import kotlin.random.Random

abstract class InputProcessor(
    private var fingerDrawing: Boolean,
    private var touchSlop: Float = 0f
) {
    protected abstract fun requestUnbufferedDispatch(event: MotionEvent)

    private interface Subprocessor {
        /**
         * Return an empty list if there is no action to emit but still want to keep the
         * subprocessor. Return `null` to cancel subprocessor.
         */
        fun updateState(event: MotionEvent): List<Action>?
    }

    private var currentSubprocessor: Subprocessor? = null
    private val subprocessors = listOf(
        // Stylus handling
        object : Subprocessor {
            private var stylusId: Int? = null
            private var lastInput: StylusInput? = null

            override fun updateState(event: MotionEvent): List<Action>? {
                val actionId = if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_UP) event.getPointerId(event.actionIndex) else null

                if (stylusId == null) {
                    val stylusIndex = (0..<event.pointerCount).find { event.getToolType(it) == MotionEvent.TOOL_TYPE_STYLUS }
                    val eraserIndex = (0..<event.pointerCount).find { event.getToolType(it) == MotionEvent.TOOL_TYPE_ERASER }
                    val index = stylusIndex ?: eraserIndex
                    if (index == null) return null
                    stylusId = event.getPointerId(index)
                }

                val kind = when {
                    event.actionMasked == MotionEvent.ACTION_DOWN || (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && actionId == stylusId) -> Action.Stylus.Kind.Down
                    event.actionMasked == MotionEvent.ACTION_UP || (event.actionMasked == MotionEvent.ACTION_POINTER_UP && actionId == stylusId) -> Action.Stylus.Kind.Up
                    else -> Action.Stylus.Kind.Move
                }

                val input = StylusInput.fromMotionEvent(lastInput, event, stylusId!!, lastInput?.strokeJitter ?: Random.nextFloat())
                val eraser = event.getToolType(event.findPointerIndex(stylusId!!)) == MotionEvent.TOOL_TYPE_ERASER

                if (kind == Action.Stylus.Kind.Down) {
                    requestUnbufferedDispatch(event)
                }

                if (kind == Action.Stylus.Kind.Up) {
                    lastInput = null
                    stylusId = null
                } else {
                    lastInput = input
                }

                return listOf(Action.Stylus(input, kind, eraser))
            }
        }
    )

    fun updateState(event: MotionEvent): List<Action> {
        val actions = currentSubprocessor?.updateState(event)

        if (actions == null) {
            for (subprocessor in subprocessors) {
                if (subprocessor == currentSubprocessor) continue
                val actions = subprocessor.updateState(event) ?: continue
                currentSubprocessor = subprocessor
                return actions
            }

            currentSubprocessor = null
            return emptyList()
        }

        return actions
    }

    private sealed interface Pointer {
        val id: Int

        data class Stylus(override val id: Int, val eraser: Boolean) : Pointer
        data class Finger(override val id: Int) : Pointer
    }

    sealed interface Action {
        /**
         * Stylus use.
         *
         * User is trying to draw with stylus (or with finger if finger drawing is enabled).
         *
         * @param [input] Stylus input event
         * @param [kind] The kind of input event
         * @param [eraser] Whether the eraser is being used
         */
        data class Stylus(
            val input: StylusInput,
            val kind: Kind,
            val eraser: Boolean
        ) : Action {
            enum class Kind { Down, Move, Up }
        }

        /**
         * Cancel previous draw action.
         */
        object Cancel : Action

        /**
         * Transform the canvas.
         *
         * User is trying to transform (move, rotate or scale) the canvas.
         */
        data class Transform(val matrix: Matrix) : Action

        /**
         * Tap gesture.
         *
         * User is trying to trigger gesture by tapping on the screen with two or more fingers.
         */
        data class TapGesture(val fingers: Int) : Action
    }
}