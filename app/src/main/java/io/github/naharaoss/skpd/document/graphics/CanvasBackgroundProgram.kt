package io.github.naharaoss.skpd.document.graphics

import android.opengl.GLES30
import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import io.github.naharaoss.skpd.utils.GLProgram
import io.github.naharaoss.skpd.utils.GLShader
import io.github.naharaoss.skpd.utils.Matrix4
import io.github.naharaoss.skpd.utils.Size

@WorkerThread
class CanvasBackgroundProgram : AutoCloseable {
    private val vertexShader = GLShader(GLShader.Type.Vertex, """
        #version 300 es
        precision highp float;
        
        const vec2 QUAD_POSITIONS[4] = vec2[4](
            vec2(-0.5,  0.5),
            vec2( 0.5,  0.5),
            vec2(-0.5, -0.5),
            vec2( 0.5, -0.5)
        );
        
        uniform mat4 uWorldToClip;
        uniform mat4 uCanvasTransform;
        uniform vec2 uCanvasSize;
        
        void main() {
            gl_Position = uWorldToClip * uCanvasTransform * vec4(QUAD_POSITIONS[gl_VertexID] * uCanvasSize, 0.0, 1.0);
        }
    """.trimIndent())

    private val fragmentShader = GLShader(GLShader.Type.Fragment, """
        #version 300 es
        precision mediump float;
        
        uniform vec4 uColor;
        layout (location = 0) out vec4 color;
        
        void main() {
            color = uColor;
        }
    """.trimIndent())

    private val program = GLProgram(vertexShader, fragmentShader)

    private val uWorldToClip = program.uniformLocationOf("uWorldToClip")
    private val uCanvasTransform = program.uniformLocationOf("uCanvasTransform")
    private val uCanvasSize = program.uniformLocationOf("uCanvasSize")
    private val uColor = program.uniformLocationOf("uColor")

    fun draw(
        viewport: Rect,
        canvasTransform: Matrix4,
        canvasSize: Size.Sized,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        program.use {
            uWorldToClip?.let {
                val worldToClip = Matrix4.Identity.copy(m00 = 2f / viewport.width, m11 = -2f / viewport.height)
                GLES30.glUniformMatrix4fv(it, 1, false, worldToClip.toFloatArray(), 0)
            }

            uCanvasTransform?.let {
                GLES30.glUniformMatrix4fv(it, 1, false, canvasTransform.toFloatArray(), 0)
            }

            uCanvasSize?.let {
                GLES30.glUniform2f(it, canvasSize.width.toFloat(), canvasSize.height.toFloat())
            }

            uColor?.let {
                GLES30.glUniform4f(it, r, g, b, a)
            }

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
    }

    override fun close() {
        vertexShader.close()
        fragmentShader.close()
    }
}