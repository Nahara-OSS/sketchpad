package io.github.naharaoss.skpd.brush

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.utils.Matrix4
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

abstract class InputProcessor(
    var fingerDrawing: Boolean,
    var touchSlop: Float = 0f
) {
    protected abstract val width: Float
    protected abstract val height: Float

    protected abstract fun requestUnbufferedDispatch(event: MotionEvent)

    private interface Subprocessor {
        /**
         * Return an empty list if there is no action to emit but still want to keep the
         * subprocessor. Return `null` to cancel subprocessor.
         */
        fun updateState(event: MotionEvent, pointers: Map<Pointer, Pointer.State>): List<Action>?
    }

    private var currentSubprocessor: Subprocessor? = null
    private val subprocessors = listOf(
        // Stylus handling
        object : Subprocessor {
            private var stylus: Pointer.Stylus? = null
            private var lastInput: StylusInput? = null

            override fun updateState(event: MotionEvent, pointers: Map<Pointer, Pointer.State>): List<Action>? {
                val stylus = ((stylus ?: pointers.keys.find { it is Pointer.Stylus } ?: return null) as Pointer.Stylus).also { stylus = it }
                val state = pointers[stylus] ?: Pointer.State.Move
                val input = StylusInput.fromMotionEvent(lastInput, event, stylus.id, lastInput?.strokeJitter ?: Random.nextFloat())
                val kind = when (state) {
                    Pointer.State.Down -> {
                        requestUnbufferedDispatch(event)
                        lastInput = input
                        Action.Stylus.Kind.Down
                    }
                    Pointer.State.Move -> {
                        lastInput = input
                        Action.Stylus.Kind.Move
                    }
                    Pointer.State.Up -> {
                        lastInput = null
                        this.stylus = null
                        Action.Stylus.Kind.Up
                    }
                }
                return listOf(Action.Stylus(input, kind, stylus.eraser))
            }
        },

        object : Subprocessor {
            private var triggeredDrawing = false
            private var firstInput: StylusInput? = null
            private var lastInput: StylusInput? = null

            override fun updateState(event: MotionEvent, pointers: Map<Pointer, Pointer.State>): List<Action>? {
                val fingerDown = fingerDrawing && pointers.keys.count { it is Pointer.Finger && pointers[it] != Pointer.State.Up } == 1
                val firstInput = firstInput
                val lastInput = lastInput

                if (!fingerDown) {
                    val lastInput = lastInput
                    this.firstInput = null
                    this.lastInput = null

                    if (triggeredDrawing && lastInput != null) {
                        triggeredDrawing = false
                        return listOf(Action.Stylus(lastInput, Action.Stylus.Kind.Up, false))
                    } else {
                        return null
                    }
                }

                val input = StylusInput.fromMotionEvent(lastInput, event, event.getPointerId(0), lastInput?.strokeJitter ?: Random.nextFloat())

                when {
                    firstInput == null -> {
                        requestUnbufferedDispatch(event)
                        this.firstInput = input
                        this.lastInput = input
                        return emptyList()
                    }

                    triggeredDrawing -> {
                        this.lastInput = input
                        return listOf(Action.Stylus(input, Action.Stylus.Kind.Move, false))
                    }

                    lastInput != null && (firstInput distanceTo lastInput) >= 10f -> {
                        triggeredDrawing = true

                        return listOf(
                            Action.Stylus(firstInput, Action.Stylus.Kind.Down, false),
                            Action.Stylus(input, Action.Stylus.Kind.Move, false)
                        )
                    }

                    else -> {
                        this.lastInput = input
                        return emptyList()
                    }
                }
            }
        },

        // Canvas transform/tap gesture handling
        object : Subprocessor {
            private var fingers = emptyMap<Pointer.Finger, Offset>()
            private var maxFingers = 0
            private var triggeredTransform = false

            override fun updateState(event: MotionEvent, pointers: Map<Pointer, Pointer.State>): List<Action>? {
                val fingers = pointers.keys.filterIsInstance<Pointer.Finger>().filter { pointers[it] != Pointer.State.Up }

                if (fingerDrawing && fingers.size == 1) {
                    this.fingers = emptyMap()
                    this.maxFingers = 0
                    this.triggeredTransform = false
                    return null
                }

                if (this.fingers.size != fingers.size) {
                    if (fingers.isEmpty()) {
                        val maxFingers = maxFingers
                        this.maxFingers = 0
                        this.fingers = emptyMap()

                        if (triggeredTransform) {
                            triggeredTransform = false
                            return emptyList()
                        }

                        return listOf(Action.TapGesture(maxFingers))
                    }

                    maxFingers = max(maxFingers, fingers.size)
                    this.fingers = fingers.associateWith {
                        val x = event.getX(event.findPointerIndex(it.id))
                        val y = event.getY(event.findPointerIndex(it.id))
                        Offset(x, y)
                    }
                } else if (fingers.isEmpty()) {
                    return null
                }

                val triggerTransform = !triggeredTransform && fingers.any {
                    val initialPosition = this.fingers[it] ?: return@any false
                    val currentPosition = Offset(event.getX(event.findPointerIndex(it.id)), event.getY(event.findPointerIndex(it.id)))
                    (currentPosition - initialPosition).getDistance() >= touchSlop
                }

                if (triggerTransform) {
                    triggeredTransform = true
                }

                return when (triggeredTransform) {
                    true if (fingers.size == 1) -> {
                        val p1 = fingers[0]
                        val prevPos = this.fingers[p1] ?: return emptyList()
                        val currPos = Offset(event.getX(event.findPointerIndex(p1.id)), event.getY(event.findPointerIndex(p1.id)))
                        val delta = currPos - prevPos
                        this.fingers = this.fingers.mapValues { (key, value) -> if (key == p1) currPos else value }
                        listOf(Action.Transform(Matrix4.Identity.copy(m30 = delta.x, m31 = delta.y)))
                    }
                    true if (fingers.size == 2) -> {
                        val p1 = fingers[0]
                        val p2 = fingers[1]
                        val p1PrevPos = this.fingers[p1] ?: return emptyList()
                        val p2PrevPos = this.fingers[p2] ?: return emptyList()
                        val p1CurrPos = Offset(event.getX(event.findPointerIndex(p1.id)), event.getY(event.findPointerIndex(p1.id)))
                        val p2CurrPos = Offset(event.getX(event.findPointerIndex(p2.id)), event.getY(event.findPointerIndex(p2.id)))
                        val prevCentroid = (p1PrevPos + p2PrevPos) / 2f
                        val currCentroid = (p1CurrPos + p2CurrPos) / 2f
                        val translate = currCentroid - prevCentroid
                        val scale = (p2CurrPos - p1CurrPos).getDistance() / (p2PrevPos - p1PrevPos).getDistance()
                        val rotate = atan2(p2CurrPos.y - p1CurrPos.y, p2CurrPos.x - p1CurrPos.x) - atan2(p2PrevPos.y - p1PrevPos.y, p2PrevPos.x - p1PrevPos.x)

                        this.fingers = this.fingers.mapValues { (key, value) ->
                            when (key) {
                                p1 -> p1CurrPos
                                p2 -> p2CurrPos
                                else -> value
                            }
                        }

                        listOf(Action.Transform(Matrix4.fromAndroidx(Matrix().apply {
                            translate(x = currCentroid.x - width / 2f, y = currCentroid.y - height / 2f)
                            rotateZ(rotate * 180f / PI.toFloat())
                            scale(x = scale, y = scale)
                            translate(x = -(currCentroid.x - width / 2f), y = -(currCentroid.y - height / 2f))
                            translate(x = translate.x, y = translate.y)
                        })))
                    }
                    else -> emptyList()
                }
            }
        }
    )

    fun updateState(event: MotionEvent): List<Action> {
        val pointers = (0..<event.pointerCount).associate { i ->
            val id = event.getPointerId(i)
            val pointer = when (event.getToolType(i)) {
                MotionEvent.TOOL_TYPE_STYLUS -> Pointer.Stylus(id, false)
                MotionEvent.TOOL_TYPE_ERASER -> Pointer.Stylus(id, true)
                else -> Pointer.Finger(id)
            }
            val state = when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN if (event.actionIndex == i) -> Pointer.State.Down
                MotionEvent.ACTION_POINTER_UP if (event.actionIndex == i) -> Pointer.State.Up
                MotionEvent.ACTION_DOWN -> Pointer.State.Down
                MotionEvent.ACTION_UP -> Pointer.State.Up
                else -> Pointer.State.Move
            }
            pointer to state
        }

        val actions = currentSubprocessor?.updateState(event, pointers)

        if (actions == null) {
            for (subprocessor in subprocessors) {
                if (subprocessor == currentSubprocessor) continue
                val actions = subprocessor.updateState(event, pointers) ?: continue
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
        enum class State { Down, Move, Up }
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
        data class Transform(val matrix: Matrix4) : Action

        /**
         * Tap gesture.
         *
         * User is trying to trigger gesture by tapping on the screen with two or more fingers.
         */
        data class TapGesture(val fingers: Int) : Action
    }
}