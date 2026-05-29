package io.github.naharaoss.skpd.document.graphics

import android.opengl.GLES30
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.Color
import io.github.naharaoss.skpd.utils.Allocator
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.GLTexture2D
import java.nio.Buffer

@WorkerThread
class TileTexture(private val tileSize: Int, buffer: Buffer?) : AutoCloseable {
    val texture = GLTexture2D()
    val framebuffer = GLFramebuffer()

    init {
        texture.bind {
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA,
                tileSize,
                tileSize,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                buffer
            )
            wrapS = GLTexture2D.WrapMode.Clamp
            wrapT = GLTexture2D.WrapMode.Clamp
            minFilter = GLTexture2D.Filter.Linear
            magFilter = GLTexture2D.Filter.Nearest
        }

        framebuffer.bind {
            attach(GLFramebuffer.Attachment.Color(0), texture)
            ensureCompleted()

            if (buffer == null) {
                setClearColor(Color.Transparent)
                clear(GLFramebuffer.ClearType.Color)
            }
        }
    }

    override fun close() {
        texture.close()
        framebuffer.close()
    }

    companion object {
        fun createAllocator(tileSize: Int) = object : Allocator<TileTexture, Buffer?>() {
            override fun onResourceCreate(params: Buffer?): TileTexture {
                return TileTexture(tileSize, params)
            }

            override fun onResourceRecall(resource: TileTexture) {
            }

            override fun onResourceRecycle(params: Buffer?, resource: TileTexture) {
                if (params == null) {
                    resource.framebuffer.bind {
                        setClearColor(Color.Transparent)
                        clear(GLFramebuffer.ClearType.Color)
                    }
                } else {
                    resource.texture.bind {
                        GLES30.glTexImage2D(
                            GLES30.GL_TEXTURE_2D,
                            0,
                            GLES30.GL_RGBA,
                            tileSize,
                            tileSize,
                            0,
                            GLES30.GL_RGBA,
                            GLES30.GL_UNSIGNED_BYTE,
                            params
                        )
                    }
                }
            }
        }
    }
}