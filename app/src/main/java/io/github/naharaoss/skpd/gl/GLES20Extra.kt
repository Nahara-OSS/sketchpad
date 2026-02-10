package io.github.naharaoss.skpd.gl

import android.opengl.GLES20

object GLES20Extra {
    fun glGenBuffers() = IntArray(1).also { GLES20.glGenBuffers(1, it, 0) }[0]
    fun glGenFramebuffers() = IntArray(1).also { GLES20.glGenFramebuffers(1, it, 0) }[0]
    fun glGenTextures() = IntArray(1).also { GLES20.glGenTextures(1, it, 0) }[0]
    fun glDeleteBuffer(id: Int) = GLES20.glDeleteBuffers(1, intArrayOf(id), 0)
    fun glDeleteFramebuffers(id: Int) = GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
    fun glDeleteTextures(id: Int) = GLES20.glDeleteTextures(1, intArrayOf(id), 0)
}