package io.github.naharaoss.skpd.utils

import android.opengl.GLES30
import android.util.Log
import androidx.graphics.lowlatency.BufferInfo
import java.nio.Buffer

fun checkGLError() {
    val code = GLES30.glGetError()
    if (code != GLES30.GL_NO_ERROR) throw Exception(when (code) {
        GLES30.GL_INVALID_ENUM -> "Invalid GL enum"
        GLES30.GL_INVALID_VALUE -> "Invalid GL value"
        GLES30.GL_INVALID_OPERATION -> "Invalid GL operation"
        GLES30.GL_INVALID_FRAMEBUFFER_OPERATION -> "Invalid GL framebuffer operation"
        GLES30.GL_OUT_OF_MEMORY -> "Out of memory"
        else -> "GL Error: $code"
    })
}

data class GLShader(val id: Int) : AutoCloseable {
    constructor(type: Type, source: String) : this(GLES30.glCreateShader(type.gl)) {
        GLES30.glShaderSource(id, source)
        GLES30.glCompileShader(id)

        if (IntArray(1).also { GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, it, 0) }[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetShaderInfoLog(id)
            Log.e("GLUtils", log)
            GLES30.glDeleteShader(id)
            throw Exception("$type shader failed to compile: $log")
        }
    }

    override fun close() = GLES30.glDeleteShader(id)

    enum class Type(val gl: Int) {
        Vertex(GLES30.GL_VERTEX_SHADER),
        Fragment(GLES30.GL_FRAGMENT_SHADER)
    }
}

data class GLProgram(val id: Int) : AutoCloseable {
    constructor(vararg shaders: GLShader) : this(GLES30.glCreateProgram()) {
        for (shader in shaders) GLES30.glAttachShader(id, shader.id)
        GLES30.glLinkProgram(id)
        for (shader in shaders) GLES30.glDetachShader(id, shader.id)

        if (IntArray(1).also { GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, it, 0) }[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetProgramInfoLog(id)
            GLES30.glDeleteProgram(id)
            throw Exception("Program link failed: $log")
        }
    }

    fun use(block: () -> Unit = {}) {
        GLES30.glUseProgram(id)
        block()
    }

    fun uniformLocationOf(name: String): Int? {
        val location = GLES30.glGetUniformLocation(id, name)
        return if (location != -1) location else null
    }

    fun attributeLocationOf(name: String): Int? {
        val location = GLES30.glGetAttribLocation(id, name)
        return if (location != -1) location else null
    }

    override fun close() = GLES30.glDeleteProgram(id)
}

data class GLTexture2D(val id: Int) : AutoCloseable {
    constructor() : this(IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0])

    fun bind(block: Scope.() -> Unit = {}) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        Scope(this).block()
    }

    override fun close() {
        GLES30.glDeleteTextures(1, intArrayOf(id), 0)
    }

    data class Scope(val texture: GLTexture2D) {
        var minFilter: Filter
            get() = Filter.fromGL(IntArray(1).also { GLES30.glGetTexParameteriv(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, it, 0) }[0])
            set(value) = GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, value.gl)

        var magFilter: Filter
            get() = Filter.fromGL(IntArray(1).also { GLES30.glGetTexParameteriv(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, it, 0) }[0])
            set(value) = GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, value.gl)

        var wrapS: WrapMode
            get() = WrapMode.fromGL(IntArray(1).also { GLES30.glGetTexParameteriv(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, it, 0) }[0])
            set(value) = GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, value.gl)

        var wrapT: WrapMode
            get() = WrapMode.fromGL(IntArray(1).also { GLES30.glGetTexParameteriv(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, it, 0) }[0])
            set(value) = GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, value.gl)
    }

    enum class Filter(val gl: Int) {
        Nearest(GLES30.GL_NEAREST),
        Linear(GLES30.GL_LINEAR);

        companion object {
            fun fromGL(gl: Int) = entries.firstOrNull { it.gl == gl } ?: throw Exception("Unknown value: $gl")
        }
    }

    enum class WrapMode(val gl: Int) {
        Clamp(GLES30.GL_CLAMP_TO_EDGE),
        Mirror(GLES30.GL_MIRRORED_REPEAT),
        Repeat(GLES30.GL_REPEAT);

        companion object {
            fun fromGL(gl: Int) = entries.firstOrNull { it.gl == gl } ?: throw Exception("Unknown value: $gl")
        }
    }
}

data class GLFramebuffer(val id: Int, val width: Int, val height: Int) : AutoCloseable {
    constructor(width: Int, height: Int) : this(
        id = IntArray(1).also { GLES30.glGenFramebuffers(1, it, 0) }[0],
        width = width,
        height = height
    )

    fun bind(target: Target = Target.Framebuffer, block: Scope.() -> Unit = {}) {
        GLES30.glBindFramebuffer(target.gl, id)
        GLES30.glViewport(0, 0, width, height)
        Scope(target, this).block()
    }

    override fun close() {
        if (id == 0) throw Exception("Cannot release default framebuffer")
        GLES30.glDeleteFramebuffers(1, intArrayOf(id), 0)
    }

    companion object {
        /**
         * Create reference to default framebuffer.
         *
         * @param [width] The width of default framebuffer
         * @param [height] The height of default framebuffer
         */
        fun default(width: Int, height: Int): GLFramebuffer = GLFramebuffer(0, width, height)
    }

    data class Scope(val target: Target, val framebuffer: GLFramebuffer) {
        fun attach(type: Attachment, texture: GLTexture2D, level: Int = 0) {
            if (framebuffer.id == 0) throw Exception("Cannot bind to default framebuffer")
            GLES30.glFramebufferTexture2D(target.gl, type.gl, GLES30.GL_TEXTURE_2D, texture.id, level)
        }

        fun ensureCompleted() {
            if (framebuffer.id == 0) return
            val status = GLES30.glCheckFramebufferStatus(target.gl)

            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                val kind = when (status) {
                    GLES30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT"
                    GLES30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"
                    GLES30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS"
                    else -> "$status (unknown)"
                }

                throw Exception("Framebuffer is not complete: $kind")
            }
        }

        fun resetViewport() = GLES30.glViewport(0, 0, framebuffer.width, framebuffer.height)
        fun setViewport(x: Int, y: Int, width: Int, height: Int) = GLES30.glViewport(x, y, width, height)
        fun setClearColor(r: Float, g: Float, b: Float, a: Float) = GLES30.glClearColor(r, g, b, a)
        fun setClearDepth(depth: Float) = GLES30.glClearDepthf(depth)
        fun setClearStencil(stencil: Int) = GLES30.glClearStencil(stencil)
        fun clear(vararg types: ClearType) = GLES30.glClear(types.map { it.gl }.reduce { acc, type -> acc or type })
    }

    sealed interface Attachment {
        val gl: Int

        data class Color(val location: Int) : Attachment {
            override val gl: Int get() = GLES30.GL_COLOR_ATTACHMENT0 + location
        }

        object Depth : Attachment {
            override val gl: Int get() = GLES30.GL_DEPTH_ATTACHMENT
        }
    }

    enum class Target(val gl: Int) {
        Framebuffer(GLES30.GL_FRAMEBUFFER),
    }

    enum class ClearType(val gl: Int) {
        Color(GLES30.GL_COLOR_BUFFER_BIT),
        Depth(GLES30.GL_DEPTH_BUFFER_BIT),
        Stencil(GLES30.GL_STENCIL_BUFFER_BIT)
    }
}

fun BufferInfo.toGLFramebuffer() = GLFramebuffer(frameBufferId, width, height)

data class GLBuffer(val id: Int) : AutoCloseable {
    constructor() : this(IntArray(1).also { GLES30.glGenBuffers(1, it, 0) }[0])

    fun bind(target: Target, block: Scope.() -> Unit = {}) {
        GLES30.glBindBuffer(target.gl, id)
        Scope(target, this).block()
    }

    override fun close() {
        GLES30.glDeleteBuffers(1, intArrayOf(id), 0)
    }

    data class Scope(val target: Target, val buffer: GLBuffer) {
        fun init(size: Int, usage: Usage, buffer: Buffer?) {
            GLES30.glBufferData(target.gl, size, buffer, usage.gl)
        }
    }

    enum class Target(val gl: Int) {
        Vertex(GLES30.GL_ARRAY_BUFFER),
        Index(GLES30.GL_ELEMENT_ARRAY_BUFFER)
    }

    enum class Usage(val gl: Int) {
        StaticDraw(GLES30.GL_STATIC_DRAW),
        DynamicDraw(GLES30.GL_DYNAMIC_DRAW)
    }
}

class GLBlitProgram(flipY: Boolean) : AutoCloseable {
    private val vertexShader = GLShader(GLShader.Type.Vertex, """
        #version 300 es
        precision mediump float;
        
        const vec4 QUAD_POSITIONS[4] = vec4[4](
            vec4(-1.0, ${if (flipY) "-1.0" else "1.0"}, 0.0, 1.0),
            vec4( 1.0, ${if (flipY) "-1.0" else "1.0"}, 0.0, 1.0),
            vec4(-1.0, ${if (flipY) "1.0" else "-1.0"}, 0.0, 1.0),
            vec4( 1.0, ${if (flipY) "1.0" else "-1.0"}, 0.0, 1.0)
        );
        
        const vec2 QUAD_UVS[4] = vec2[4](
            vec2(0.0, 1.0),
            vec2(1.0, 1.0),
            vec2(0.0, 0.0),
            vec2(1.0, 0.0)
        );
        
        out vec2 fUV;
        
        void main() {
            gl_Position = QUAD_POSITIONS[gl_VertexID];
            fUV = QUAD_UVS[gl_VertexID];
        }
    """.trimIndent())

    private val fragmentShader = GLShader(GLShader.Type.Fragment, """
        #version 300 es
        precision mediump float;
        
        uniform sampler2D uTexture;
        in vec2 fUV;
        layout (location = 0) out vec4 color;
        
        void main() {
            color = texture(uTexture, fUV);
        }
    """.trimIndent())

    private val program = GLProgram(vertexShader, fragmentShader)

    private val uTexture = program.uniformLocationOf("uTexture")

    fun blit(texture: GLTexture2D) {
        program.use {
            uTexture?.let {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.id)
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

data class GLBlendState(
    val rgbFunction: Function,
    val rgbSrcFactor: Factor,
    val rgbDstFactor: Factor,
    val alphaFunction: Function = rgbFunction,
    val alphaSrcFactor: Factor = rgbSrcFactor,
    val alphaDstFactor: Factor = rgbDstFactor
) {
    enum class Function(val gl: Int) {
        Add(GLES30.GL_FUNC_ADD),
        Sub(GLES30.GL_FUNC_SUBTRACT),
        SubRev(GLES30.GL_FUNC_REVERSE_SUBTRACT),
        Min(GLES30.GL_MIN),
        Max(GLES30.GL_MAX),
    }

    enum class Factor(val gl: Int) {
        One(GLES30.GL_ONE),
        Zero(GLES30.GL_ZERO),
        SrcAlpha(GLES30.GL_SRC_ALPHA),
        DstAlpha(GLES30.GL_DST_ALPHA),
        OneMinusSrcAlpha(GLES30.GL_ONE_MINUS_SRC_ALPHA),
        OneMinusDstAlpha(GLES30.GL_ONE_MINUS_DST_ALPHA),
        SrcColor(GLES30.GL_SRC_COLOR),
        DstColor(GLES30.GL_DST_COLOR),
        OneMinusSrcColor(GLES30.GL_ONE_MINUS_SRC_COLOR),
        OneMinusDstColor(GLES30.GL_ONE_MINUS_DST_COLOR),
    }

    fun use() {
        GLES30.glBlendEquationSeparate(rgbFunction.gl, alphaFunction.gl)
        GLES30.glBlendFuncSeparate(rgbSrcFactor.gl, rgbDstFactor.gl, alphaSrcFactor.gl, alphaDstFactor.gl)
    }
}

fun BlendMode.toBlendState(): GLBlendState = when (this) {
    BlendMode.SourceOver -> GLBlendState(
        rgbFunction = GLBlendState.Function.Add,
        rgbSrcFactor = GLBlendState.Factor.One,
        rgbDstFactor = GLBlendState.Factor.OneMinusSrcAlpha
    )
    BlendMode.Erase -> GLBlendState(
        rgbFunction = GLBlendState.Function.Add,
        rgbSrcFactor = GLBlendState.Factor.Zero,
        rgbDstFactor = GLBlendState.Factor.OneMinusSrcAlpha
    )
}