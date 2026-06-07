package io.github.naharaoss.skpd.document.graphics

import android.opengl.GLES30
import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import io.github.naharaoss.skpd.utils.GLProgram
import io.github.naharaoss.skpd.utils.GLShader
import io.github.naharaoss.skpd.utils.GLTexture2D
import io.github.naharaoss.skpd.utils.Matrix4
import io.github.naharaoss.skpd.utils.TileAddress

@WorkerThread
class TileProgram : AutoCloseable {
    private val vertexShader = GLShader(GLShader.Type.Vertex, """
        #version 300 es
        precision highp float;
        
        const vec2 QUAD_UVS[4] = vec2[4](
            vec2(0.0, 0.0),
            vec2(1.0, 0.0),
            vec2(0.0, 1.0),
            vec2(1.0, 1.0)
        );
        
        uniform float uTileSize;
        uniform mat4 uWorldToClip;
        uniform mat4 uCanvasTransform;
        uniform vec2 uTilePosition;
        out vec2 fUV;
        
        void main() {
            gl_Position = uWorldToClip * uCanvasTransform * vec4((QUAD_UVS[gl_VertexID] + uTilePosition) * uTileSize, 0.0, 1.0);
            fUV = QUAD_UVS[gl_VertexID];
        }
    """.trimIndent())

    private val fragmentShader = GLShader(GLShader.Type.Fragment, """
        #version 300 es
        precision mediump float;
        
        uniform sampler2D uTexture;
        layout (location = 0) out vec4 color;
        in vec2 fUV;
        
        void main() {
            color = texture(uTexture, fUV);
        }
    """.trimIndent())

    private val program = GLProgram(vertexShader, fragmentShader)

    private val uTileSize = program.uniformLocationOf("uTileSize")
    private val uWorldToClip = program.uniformLocationOf("uWorldToClip")
    private val uCanvasTransform = program.uniformLocationOf("uCanvasTransform")
    private val uTilePosition = program.uniformLocationOf("uTilePosition")
    private val uTexture = program.uniformLocationOf("uTexture")

    fun draw(
        source: GLTexture2D,
        tileSize: Int,
        viewport: Rect,
        canvasTransform: Matrix4,
        tileAddress: TileAddress
    ) {
        program.use {
            uTileSize?.let {
                GLES30.glUniform1f(it, tileSize.toFloat())
            }

            uWorldToClip?.let {
                val worldToClip = Matrix4.Identity.copy(m00 = 2f / viewport.width, m11 = -2f / viewport.height)
                GLES30.glUniformMatrix4fv(it, 1, false, worldToClip.toFloatArray(), 0)
            }

            uCanvasTransform?.let {
                GLES30.glUniformMatrix4fv(it, 1, false, canvasTransform.toFloatArray(), 0)
            }

            uTilePosition?.let {
                GLES30.glUniform2f(it, tileAddress.x.toFloat(), tileAddress.y.toFloat())
            }

            uTexture?.let {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                source.bind()
                GLES30.glUniform1i(it, 0)
            }

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
    }

    override fun close() {
        vertexShader.close()
        fragmentShader.close()
        program.close()
    }
}