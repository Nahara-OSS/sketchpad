package io.github.naharaoss.skpd.brush.impl

import android.opengl.GLES30
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.Dynamic
import io.github.naharaoss.skpd.brush.Sensor
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.brush.lerp
import io.github.naharaoss.skpd.utils.Color
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.GLProgram
import io.github.naharaoss.skpd.utils.GLShader
import io.github.naharaoss.skpd.utils.GLTexture2D
import io.github.naharaoss.skpd.utils.Graph
import io.github.naharaoss.skpd.utils.Matrix4
import io.github.naharaoss.skpd.utils.union
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Stamp-based brush family.
 *
 * The idea of stamp-based brush is simple: Stick a bunch of images along the stroke path, each with
 * some spacing amount.
 */
object StampBrush : BrushType<StampBrush.Preset> {
    override val defaultPreset: Preset = Preset(
        tip = Preset.BrushTip.Circle(
            falloff = Graph(markers = listOf(
                Graph.Marker(target = Graph.ControlPoint(0.0f, 1f)),
                Graph.Marker(target = Graph.ControlPoint(0.5f, 1f)),
                Graph.Marker(target = Graph.ControlPoint(1.0f, 0f))
            )),
            scaleX = 1f,
            scaleY = 1f,
        ),
        spacing = 1f,
        count = Dynamic(base = 1f),
        size = Dynamic(
            base = 18f,
            modifiers = listOf(
                Dynamic.Modifier(
                    id = UUID.nameUUIDFromBytes("StampBrush/Size/Pressure".toByteArray()).toString(),
                    sensor = Sensor.Pressure,
                    operation = Dynamic.Operation.Multiplicative(1f),
                    graph = Graph(markers = listOf())
                )
            )
        ),
        opacity = Dynamic(
            base = 1f,
            modifiers = listOf(
                Dynamic.Modifier(
                    id = UUID.nameUUIDFromBytes("StampBrush/Opacity/Pressure".toByteArray()).toString(),
                    sensor = Sensor.Pressure,
                    operation = Dynamic.Operation.Multiplicative(1f),
                    graph = Graph(markers = listOf())
                )
            )
        ),
        flow = Dynamic(base = 1f),
        offsetX = Dynamic(base = 0f),
        offsetY = Dynamic(base = 0f),
        rotation = Dynamic(base = 0f)
    )

    val countParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String get() = "count"
        override val nameRes: Int get() = R.string.parameter_stamp_count
        override val iconRes: Int get() = R.drawable.workspaces_24px
        override val min: Float get() = 1f
        override val max: Float get() = 100f
        override val centered: Boolean = false
        @Composable override fun formatValue(value: Float): String = "${value.roundToInt()} stamps"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.count
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(count = dynamic)
    }

    val sizeParameter = object : BrushType.ParameterInfo<Preset> {
        private val sliderMapExponent = 0.1f
        override val parameter: String get() = "size"
        override val nameRes: Int get() = R.string.parameter_stamp_size
        override val iconRes: Int get() = R.drawable.edit_24px
        override val min: Float get() = 1f
        override val max: Float get() = 1000f
        override val centered: Boolean = false
        @Composable override fun formatValue(value: Float): String = "${if (value < 10) "%.2f".format(value) else value.roundToInt()} pixels"
        override fun forwardMapToSlider(value: Float): Float = (value / max).pow(sliderMapExponent)
        override fun backwardMapToSlider(slider: Float): Float = slider.pow(1f / sliderMapExponent) * max
        override fun getDynamic(preset: Preset): Dynamic = preset.size
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(size = dynamic)
    }

    val opacityParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String get() = "opacity"
        override val nameRes: Int get() = R.string.parameter_stamp_opacity
        override val iconRes: Int get() = R.drawable.opacity_24px
        override val min: Float get() = 0f
        override val max: Float get() = 1f
        override val centered: Boolean = false
        @Composable override fun formatValue(value: Float): String = "${(value * 100).roundToInt()}%"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.opacity
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(opacity = dynamic)
    }

    val flowParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String = "flow"
        override val nameRes: Int = R.string.parameter_stamp_flow
        override val iconRes: Int = R.drawable.animation_24px
        override val min: Float = 0f
        override val max: Float = 1f
        override val centered: Boolean = false
        @Composable override fun formatValue(value: Float): String = "${(value * 100).roundToInt()}%"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.flow
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(flow = dynamic)
    }

    val rotationParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String = "rotation"
        override val nameRes: Int = R.string.parameter_stamp_rotation
        override val iconRes: Int = R.drawable.rotate_left_24px
        override val min: Float = 0f
        override val max: Float = 360f
        override val centered: Boolean = false
        @Composable override fun formatValue(value: Float): String = "${value.roundToInt()}\u00B0"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.rotation
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(rotation = dynamic)
    }

    val offsetXParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String = "offset-x"
        override val nameRes: Int = R.string.parameter_stamp_offset_x
        override val iconRes: Int = R.drawable.adjust_24px
        override val min: Float = -100f
        override val max: Float = +100f
        override val centered: Boolean = true
        @Composable override fun formatValue(value: Float): String = "${if (value < 10) "%.2f".format(value) else value.roundToInt()} pixels"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.offsetX
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(offsetX = dynamic)
    }

    val offsetYParameter = object : BrushType.ParameterInfo<Preset> {
        override val parameter: String = "offset-y"
        override val nameRes: Int = R.string.parameter_stamp_offset_y
        override val iconRes: Int = R.drawable.adjust_24px
        override val min: Float = -100f
        override val max: Float = +100f
        override val centered: Boolean = true
        @Composable override fun formatValue(value: Float): String = "${if (value < 10) "%.2f".format(value) else value.roundToInt()} pixels"
        override fun forwardMapToSlider(value: Float): Float = value
        override fun backwardMapToSlider(slider: Float): Float = slider
        override fun getDynamic(preset: Preset): Dynamic = preset.offsetY
        override fun replaceDynamic(preset: Preset, dynamic: Dynamic): Preset = preset.copy(offsetY = dynamic)
    }

    override val allParameters: List<BrushType.ParameterInfo<Preset>> = listOf(
        countParameter,
        sizeParameter,
        opacityParameter,
        flowParameter,
        rotationParameter,
        offsetXParameter,
        offsetYParameter
    )

    data class Preset(
        /**
         * Brush tip texture.
         */
        val tip: BrushTip,

        /**
         * Spacing between each stamp.
         *
         * Zero value (`0`) is not allowed. Positive value is for fixed spacing and negative value
         * is for dynamic spacing.
         *
         * - **Fixed spacing**: The spacing is measured in pixels.
         *
         * - **Dynamic spacing**: The spacing is determined by taking the base value of [size]
         * (which is [Dynamic.base]) and scales it with negate of the spacing value.
         */
        val spacing: Float,

        /**
         * Stamp count per iteration.
         *
         * This parameter controls the number of stamps to apply per iteration. The value will be
         * rounded to nearest integer to determine the number of stamps. [Sensor.DabJitter] sensor
         * produces a random value on each stamp, while the rest of [Sensor] keeps the same value
         * for entire iteration.
         *
         * This parameter is typically used for making spray-like brushes by combining high stamp
         * count and jitter on offsets and rotation.
         */
        val count: Dynamic,

        /**
         * Size of the brush tip.
         *
         * The size is measured in pixels on canvas. Note that the dimension of brush tip texture
         * only defines the resolution/quality of brush tip, not the actual size of it. The size
         * must be a positive value (so no negative and zero values are allowed).
         */
        val size: Dynamic,

        /**
         * Opacity of the brush tip.
         *
         * The opacity calculation is a bit involved: there is an opacity map, which is basically
         * just a depth map. Late depth testing is used to determine the final opacity of the pixel,
         * and the opacity is the maximum value of existing pixel on the depth map and brush tip.
         */
        val opacity: Dynamic,

        /**
         * Flow of the brush tip.
         *
         * Flow is basically the "stacking opacity" of the brush tip. In stamp-based brush engine,
         * there are 2 different maps: the opacity map (represented as depth map) and the color map.
         * Flow controls the opacity of the tip when pasting on color map. Then the color map is
         * multiplied by opacity map to create the final brush stroke.
         */
        val flow: Dynamic,

        /**
         * Rotation of brush tip.
         *
         * This rotates the brush tip around its origin, which is directly under the stylus' tip.
         * The value is in degree (so the [Dynamic.forInput] must report a value in degree).
         * Typically used together with [Sensor.Rotation] or [Sensor.Azimuth].
         */
        val rotation: Dynamic,

        /**
         * X offset of brush tip.
         */
        val offsetX: Dynamic,

        /**
         * Y offset of brush tip.
         */
        val offsetY: Dynamic,
    ) : BrushType.Preset {
        override val type: BrushType<*> get() = StampBrush

        /**
         * Brush tip texture.
         */
        sealed interface BrushTip {
            /**
             * Simple brush tip.
             *
             * Brush tip of this kind will be rendered in high resolution.
             */
            sealed interface Simple : BrushTip {
                /**
                 * Alpha falloff graph.
                 *
                 * This graph produces the alpha value, running from the center to the edge of brush
                 * tip. The graph will be converted into 1D grayscale texture and then used inside
                 * the renderer.
                 */
                val falloff: Graph

                /**
                 * The scale along X axis.
                 */
                val scaleX: Float

                /**
                 * The scale along Y axis.
                 */
                val scaleY: Float

                fun copyToSimple(
                    falloff: Graph = this.falloff,
                    scaleX: Float = this.scaleX,
                    scaleY: Float = this.scaleY
                ): Simple
            }

            data class Square(
                override val falloff: Graph,
                override val scaleX: Float,
                override val scaleY: Float
            ) : Simple {
                override fun copyToSimple(
                    falloff: Graph,
                    scaleX: Float,
                    scaleY: Float
                ): Simple = copy(
                    falloff = falloff,
                    scaleX = scaleX,
                    scaleY = scaleY
                )
            }

            data class Circle(
                override val falloff: Graph,
                override val scaleX: Float,
                override val scaleY: Float
            ) : Simple {
                override fun copyToSimple(
                    falloff: Graph,
                    scaleX: Float,
                    scaleY: Float
                ): Simple = copy(
                    falloff = falloff,
                    scaleX = scaleX,
                    scaleY = scaleY
                )
            }
        }
    }

    data class Stamp(
        val x: Float,
        val y: Float,
        val size: Float,
        val flow: Float,
        val opacity: Float,
        val rotation: Float
    ) {
        val bounds get() = Rect(center = Offset(x, y), radius = size / 2f)

        fun putToBuffer(buffer: ByteBuffer) {
            buffer.putFloat(x)
            buffer.putFloat(y)
            buffer.putFloat(size)
            buffer.putFloat(flow)
            buffer.putFloat(opacity)
            buffer.putFloat(rotation * PI.toFloat() / 180f)
        }

        companion object {
            const val STRIDE = 4 * 6
            const val POSITION_OFFSET = 4 * 0
            const val SIZE_OFFSET = 4 * 2
            const val FLOW_OFFSET = 4 * 3
            const val OPACITY_OFFSET = 4 * 4
            const val ROTATION_OFFSET = 4 * 5
        }
    }

    @WorkerThread
    private class InternalTile(tileRect: Rect) : AutoCloseable {
        val colorTexture = GLTexture2D()
        val depthTexture = GLTexture2D()
        val framebuffer = GLFramebuffer(tileRect.width.toInt(), tileRect.height.toInt())

        init {
            colorTexture.bind {
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_RGBA,
                    tileRect.width.toInt(),
                    tileRect.height.toInt(),
                    0,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    null
                )

                minFilter = GLTexture2D.Filter.Nearest
                magFilter = GLTexture2D.Filter.Nearest
                wrapS = GLTexture2D.WrapMode.Clamp
                wrapT = GLTexture2D.WrapMode.Clamp
            }

            depthTexture.bind {
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_DEPTH_COMPONENT16,
                    tileRect.width.toInt(),
                    tileRect.height.toInt(),
                    0,
                    GLES30.GL_DEPTH_COMPONENT,
                    GLES30.GL_UNSIGNED_SHORT,
                    null
                )

                minFilter = GLTexture2D.Filter.Nearest
                magFilter = GLTexture2D.Filter.Nearest
                wrapS = GLTexture2D.WrapMode.Clamp
                wrapT = GLTexture2D.WrapMode.Clamp
            }

            framebuffer.bind {
                attach(GLFramebuffer.Attachment.Color(0), colorTexture)
                attach(GLFramebuffer.Attachment.Depth, depthTexture)
                ensureCompleted()
                setClearColor(0f, 0f, 0f, 0f)
                setClearDepth(0f)
                clear(GLFramebuffer.ClearType.Color, GLFramebuffer.ClearType.Depth)
            }
        }

        override fun close() {
            colorTexture.close()
            depthTexture.close()
            framebuffer.close()
        }
    }

    @WorkerThread
    private data class SimpleBrushTipProgram(
        val program: GLProgram,
        val uTransform: Int = GLES30.glGetUniformLocation(program.id, "uTransform"),
        val uColor: Int = GLES30.glGetUniformLocation(program.id, "uColor"),
        val uFalloffGraph: Int = GLES30.glGetUniformLocation(program.id, "uFalloffGraph"),
        val iPosition: Int = GLES30.glGetAttribLocation(program.id, "iPosition"),
        val iSize: Int = GLES30.glGetAttribLocation(program.id, "iSize"),
        val iFlow: Int = GLES30.glGetAttribLocation(program.id, "iFlow"),
        val iOpacity: Int = GLES30.glGetAttribLocation(program.id, "iOpacity"),
        val iRotation: Int = GLES30.glGetAttribLocation(program.id, "iRotation")
    ) : AutoCloseable {
        constructor(distance: String) : this(GLProgram(
            GLShader(GLShader.Type.Vertex, """
                #version 300 es
                precision highp float;
                
                const vec2 QUAD_POSITIONS[4] = vec2[4](
                    vec2(-0.5,  0.5),
                    vec2( 0.5,  0.5),
                    vec2(-0.5, -0.5),
                    vec2( 0.5, -0.5)
                );
                
                const vec2 QUAD_UVS[4] = vec2[4](
                    vec2(0.0, 1.0),
                    vec2(1.0, 1.0),
                    vec2(0.0, 0.0),
                    vec2(1.0, 0.0)
                );
                
                uniform mat4 uTransform;
                in vec2 iPosition;
                in float iSize;
                in float iFlow;
                in float iOpacity;
                in float iRotation;
                out float fFlow;
                out float fOpacity;
                out vec2 fUV;
                
                void main() {
                    mat2 rotation = mat2(-sin(iRotation), cos(iRotation), cos(iRotation), sin(iRotation));
                    gl_Position = uTransform * vec4(iPosition + (rotation * QUAD_POSITIONS[gl_VertexID]) * iSize, 0.0, 1.0);
                    fFlow = iFlow;
                    fOpacity = iOpacity;
                    fUV = QUAD_UVS[gl_VertexID];
                }
            """.trimIndent()),
            GLShader(GLShader.Type.Fragment, """
                #version 300 es
                precision mediump float;
                
                uniform vec4 uColor;
                uniform sampler2D uFalloffGraph;
                in float fFlow;
                in float fOpacity;
                in vec2 fUV;
                layout(location = 0) out vec4 color;
                
                void main() {
                    float dist = $distance;
                    float value = texture(uFalloffGraph, vec2(dist, 0.0)).r * step(dist, 1.0);
                    color = uColor * value * fFlow;
                    gl_FragDepth = value * fOpacity;
                }
            """.trimIndent())
        ))

        /**
         * Use the brush program.
         *
         * It is recommended to keep alpha channel of brush color to `1.0` and use opacity/flow
         * parameter in brush preset instead.
         *
         * @param [falloff] Falloff graph lookup table
         * @param [stamps] Byte buffer to the stamp instance buffer
         * @param [count] Number to stamps
         * @param [transform] Transform matrix
         * @param [color] Brush color
         */
        fun draw(
            falloff: GLTexture2D,
            stamps: Buffer,
            count: Int,
            transform: Matrix4,
            color: Color
        ) {
            val (r, g, b) = color.toRgb()

            program.use {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                falloff.bind()

                GLES30.glUniformMatrix4fv(uTransform, 1, false, transform.toFloatArray(), 0)
                GLES30.glUniform4f(uColor, r, g, b, 1f)
                GLES30.glUniform1i(uFalloffGraph, 0)

                GLES30.glEnableVertexAttribArray(iPosition)
                GLES30.glVertexAttribPointer(iPosition, 2, GLES30.GL_FLOAT, false, Stamp.STRIDE, stamps.position(Stamp.POSITION_OFFSET))
                GLES30.glVertexAttribDivisor(iPosition, 1)
                GLES30.glEnableVertexAttribArray(iSize)
                GLES30.glVertexAttribPointer(iSize, 1, GLES30.GL_FLOAT, false, Stamp.STRIDE, stamps.position(Stamp.SIZE_OFFSET))
                GLES30.glVertexAttribDivisor(iSize, 1)
                GLES30.glEnableVertexAttribArray(iFlow)
                GLES30.glVertexAttribPointer(iFlow, 1, GLES30.GL_FLOAT, false, Stamp.STRIDE, stamps.position(Stamp.FLOW_OFFSET))
                GLES30.glVertexAttribDivisor(iFlow, 1)
                GLES30.glEnableVertexAttribArray(iOpacity)
                GLES30.glVertexAttribPointer(iOpacity, 1, GLES30.GL_FLOAT, false, Stamp.STRIDE, stamps.position(Stamp.OPACITY_OFFSET))
                GLES30.glVertexAttribDivisor(iOpacity, 1)
                GLES30.glEnableVertexAttribArray(iRotation)
                GLES30.glVertexAttribPointer(iRotation, 1, GLES30.GL_FLOAT, false, Stamp.STRIDE, stamps.position(Stamp.ROTATION_OFFSET))
                GLES30.glVertexAttribDivisor(iRotation, 1)

                GLES30.glColorMask(false, false, false, false)
                GLES30.glEnable(GLES30.GL_DEPTH_TEST)
                GLES30.glDepthFunc(GLES30.GL_GEQUAL)
                GLES30.glDepthMask(true)
                GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, count)
                GLES30.glDisable(GLES30.GL_DEPTH_TEST)

                GLES30.glColorMask(true, true, true, true)
                GLES30.glEnable(GLES30.GL_BLEND)
                GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
                GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)
                GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, count)
                GLES30.glDisable(GLES30.GL_BLEND)

                GLES30.glVertexAttribDivisor(iPosition, 0)
                GLES30.glVertexAttribDivisor(iSize, 0)
                GLES30.glVertexAttribDivisor(iFlow, 0)
                GLES30.glVertexAttribDivisor(iOpacity, 0)
                GLES30.glVertexAttribDivisor(iRotation, 0)
                GLES30.glDisableVertexAttribArray(iPosition)
                GLES30.glDisableVertexAttribArray(iSize)
                GLES30.glDisableVertexAttribArray(iFlow)
                GLES30.glDisableVertexAttribArray(iOpacity)
                GLES30.glDisableVertexAttribArray(iRotation)
            }
        }

        override fun close() {
            program.close()
        }
    }

    override fun createRenderer(): BrushType.Renderer<Preset> = object : BrushType.Renderer<Preset> {
        override val type: BrushType<Preset> get() = this@StampBrush

        val squareBrushProgram = SimpleBrushTipProgram("max(abs(fUV.x * 2.0 - 1.0), abs(fUV.y * 2.0 - 1.0))")
        val circleBrushProgram = SimpleBrushTipProgram("distance(fUV, vec2(0.5, 0.5)) * 2.0")

        val mergeProgram = GLProgram(
            GLShader(GLShader.Type.Vertex, """
                #version 300 es
                precision highp float;
                
                const vec4 QUAD_POSITIONS[4] = vec4[4](
                    vec4(-1.0,  1.0, 0.0, 1.0),
                    vec4( 1.0,  1.0, 0.0, 1.0),
                    vec4(-1.0, -1.0, 0.0, 1.0),
                    vec4( 1.0, -1.0, 0.0, 1.0)
                );
                
                const vec2 QUAD_UVS[4] = vec2[4](
                    vec2(0.0, 1.0),
                    vec2(1.0, 1.0),
                    vec2(0.0, 0.0),
                    vec2(1.0, 0.0)
                );
                
                uniform mat4 uTransform;
                out vec2 fUV;
                
                void main() {
                    gl_Position = uTransform * QUAD_POSITIONS[gl_VertexID];
                    fUV = QUAD_UVS[gl_VertexID];
                }
            """.trimIndent()),
            GLShader(GLShader.Type.Fragment, """
                #version 300 es
                precision mediump float;
                
                uniform sampler2D uColorTexture;
                uniform sampler2D uDepthTexture;
                in vec2 fUV;
                out vec4 color;
                
                void main() {
                    vec4 sampled = texture(uColorTexture, fUV);
                    float opacity = texture(uDepthTexture, fUV).r;
                    color = sampled * opacity;
                }
            """.trimIndent())
        )
        val uMergeProgramTransform = GLES30.glGetUniformLocation(mergeProgram.id, "uTransform")
        val uMergeProgramColorTexture = GLES30.glGetUniformLocation(mergeProgram.id, "uColorTexture")
        val uMergeProgramDepthTexture = GLES30.glGetUniformLocation(mergeProgram.id, "uDepthTexture")

        lateinit var preset: Preset
        var color: Color = Color.Black
        var falloffGraph: GLTexture2D? = null
        var lastInput: StylusInput? = null
        val tiles = mutableMapOf<Any, InternalTile>()
        var stampCount = 0
        var stamps = ByteBuffer.allocateDirect(16384).order(ByteOrder.nativeOrder())

        // 16384 (16k) was selected because Android 15 introduced 16k page size support, and we just
        // use 16k for efficiency.

        override fun usePreset(preset: Preset) {
            this.preset = preset

            if (preset.tip is Preset.BrushTip.Simple) {
                val resolution = 124
                val buffer = ByteBuffer.allocateDirect(4 * resolution).order(ByteOrder.nativeOrder())
                preset.tip.falloff.plotToBuffer(buffer, resolution)
                buffer.flip()
                val falloffGraph = GLTexture2D()
                falloffGraph.bind {
                    GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RED, resolution, 1, 0, GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, buffer)
                    minFilter = GLTexture2D.Filter.Linear
                    magFilter = GLTexture2D.Filter.Linear
                    wrapS = GLTexture2D.WrapMode.Clamp
                    wrapT = GLTexture2D.WrapMode.Clamp
                }
                this.falloffGraph?.close()
                this.falloffGraph = falloffGraph
            } else {
                this.falloffGraph?.close()
                this.falloffGraph = null
            }
        }

        override fun useColor(color: Color) {
            this.color = color
        }

        override fun beginStroke() {
            lastInput = null
            tiles.onEach { it.value.close() }.clear()
        }

        override fun consumeInput(input: StylusInput): Rect {
            val count = preset.count.forInput(input).roundToInt()
            val spacing = if (preset.spacing > 0f) preset.spacing else preset.size.base * (-preset.spacing)
            val lastInput = lastInput
            var rect = Rect(center = Offset(x = input.x, y = input.y), radius = 0f)

            fun StylusInput.toStamp() = Stamp(
                x = x + preset.offsetX.forInput(this),
                y = y + preset.offsetY.forInput(this),
                size = preset.size.forInput(this),
                flow = preset.flow.forInput(this),
                opacity = preset.opacity.forInput(this),
                rotation = preset.rotation.forInput(this),
            )

            fun pushStamp(stamp: Stamp) {
                if (stamps.remaining() < Stamp.STRIDE) {
                    val oldCapacity = stamps.capacity()
                    Log.d("StampBrush", "Resizing stamp instance buffer from $oldCapacity to ${oldCapacity * 2}")

                    val newStamps = ByteBuffer.allocateDirect(oldCapacity * 2).order(ByteOrder.nativeOrder())
                    stamps.flip()
                    newStamps.put(stamps)
                    stamps = newStamps
                }

                stamp.putToBuffer(stamps)
                stampCount++
            }

            stampCount = 0
            stamps.clear()

            if (lastInput != null) {
                var remainingDistance = lastInput distanceTo input
                var lastInput: StylusInput = lastInput

                while (remainingDistance >= spacing) {
                    val interpolated = lerp(lastInput, input, spacing / remainingDistance)
                    remainingDistance -= spacing

                    (1..count).forEach { _ ->
                        val stamp = interpolated.toStamp()
                        rect = rect.union(stamp.bounds)
                        pushStamp(stamp)
                    }

                    lastInput = interpolated
                }

                this.lastInput = lastInput
            } else {
                val stamp = input.toStamp()
                rect = rect.union(stamp.bounds)
                pushStamp(stamp)
                this.lastInput = input
            }

            stamps.flip()
            return rect
        }

        override fun consumeTile(tileKey: Any, tileRect: Rect) {
            if (stampCount == 0) return

            val tile = tiles.getOrPut(tileKey, { InternalTile(tileRect) })
            val transform = Matrix4.fromAndroidx(Matrix().apply {
                scale(x = 2f / tileRect.width, y = -2f / tileRect.height)
                translate(x = tileRect.width / -2f, y = tileRect.height / -2f)
                translate(x = -tileRect.left, y = -tileRect.top)
            })

            tile.framebuffer.bind {
                when (preset.tip) {
                    is Preset.BrushTip.Circle -> circleBrushProgram.draw(falloffGraph!!, stamps, stampCount, transform, color)
                    is Preset.BrushTip.Square -> squareBrushProgram.draw(falloffGraph!!, stamps, stampCount, transform, color)
                }
            }
        }

        override fun renderTile(tileKey: Any, tileRect: Rect, framebuffer: GLFramebuffer, transform: Matrix4) {
            val tile = tiles[tileKey] ?: return

            framebuffer.bind {
                mergeProgram.use {
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                    tile.colorTexture.bind()
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                    tile.depthTexture.bind()
                    GLES30.glUniform1i(uMergeProgramColorTexture, 0)
                    GLES30.glUniform1i(uMergeProgramDepthTexture, 1)
                    GLES30.glUniformMatrix4fv(uMergeProgramTransform, 1, false, transform.toFloatArray(), 0)
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
                }
            }
        }

        override fun endStroke() {
            tiles.onEach { it.value.close() }.clear()
        }

        override fun close() {
            tiles.onEach { it.value.close() }.clear()
            squareBrushProgram.close()
            circleBrushProgram.close()
            mergeProgram.close()
            falloffGraph?.close()
        }
    }
}