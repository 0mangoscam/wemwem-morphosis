package com.mangoscam.wemwem.morphosis.engine

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.SystemClock
import com.mangoscam.wemwem.morphosis.audio.AudioEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

internal class MorphosisRenderer(
    private val context: Context,
    private val audioEngine: AudioEngine,
    private val controller: MorphosisController,
) : GLSurfaceView.Renderer {
    private val simulationSize = 512
    private val textures = IntArray(2)
    private val framebuffers = IntArray(2)
    private val vertexArray = IntArray(1)
    private val vertexBuffer = IntArray(1)

    private var updateProgram = 0
    private var displayProgram = 0
    private var readIndex = 0
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var startTime = 0L
    private var observedResetToken = -1L

    @Volatile private var touchX = 0.5f
    @Volatile private var touchY = 0.5f
    @Volatile private var touchActive = false

    fun setTouch(x: Float, y: Float, active: Boolean) {
        touchX = x
        touchY = y
        touchActive = active
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        startTime = SystemClock.elapsedRealtime()
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        updateProgram = GlProgram.create(loadAsset("shaders/fullscreen.vert"), loadAsset("shaders/reaction.frag"))
        displayProgram = GlProgram.create(loadAsset("shaders/fullscreen.vert"), loadAsset("shaders/display.frag"))
        createQuad()
        createSimulationTargets()
        resetSimulation()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = max(width, 1)
        viewportHeight = max(height, 1)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (observedResetToken != controller.resetToken) {
            observedResetToken = controller.resetToken
            resetSimulation()
        }

        val time = (SystemClock.elapsedRealtime() - startTime) / 1000f
        val audio = audioEngine.snapshot()
        val parameters = controller.snapshot()

        repeat(3) { step ->
            val writeIndex = 1 - readIndex
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffers[writeIndex])
            GLES30.glViewport(0, 0, simulationSize, simulationSize)
            GLES30.glUseProgram(updateProgram)
            bindQuad()

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[readIndex])
            uniform1i(updateProgram, "uState", 0)
            uniform2f(updateProgram, "uTexel", 1f / simulationSize, 1f / simulationSize)
            uniform1f(updateProgram, "uTime", time + step * 0.013f)
            uniform1f(updateProgram, "uLevel", audio.level)
            uniform1f(updateProgram, "uBass", audio.bass)
            uniform1f(updateProgram, "uMid", audio.mid)
            uniform1f(updateProgram, "uTreble", audio.treble)
            uniform1f(updateProgram, "uPeak", audio.peak)
            uniform1f(updateProgram, "uMatter", parameters.matter)
            uniform1f(updateProgram, "uMutation", parameters.mutation)
            uniform1f(updateProgram, "uDecay", parameters.decay)
            uniform1f(updateProgram, "uMemory", parameters.memory)
            uniform1f(updateProgram, "uSensitivity", parameters.sensitivity)
            uniform2f(updateProgram, "uTouch", touchX, touchY)
            uniform1f(updateProgram, "uTouchActive", if (touchActive) 1f else 0f)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            readIndex = writeIndex
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(displayProgram)
        bindQuad()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[readIndex])
        uniform1i(displayProgram, "uState", 0)
        uniform2f(displayProgram, "uTexel", 1f / simulationSize, 1f / simulationSize)
        uniform2f(displayProgram, "uResolution", viewportWidth.toFloat(), viewportHeight.toFloat())
        uniform1f(displayProgram, "uTime", time)
        uniform1f(displayProgram, "uLevel", audio.level)
        uniform1f(displayProgram, "uDepth", parameters.depth)
        uniform1f(displayProgram, "uMatter", parameters.matter)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun createQuad() {
        val vertices = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)

        GLES30.glGenVertexArrays(1, vertexArray, 0)
        GLES30.glGenBuffers(1, vertexBuffer, 0)
        GLES30.glBindVertexArray(vertexArray[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 2 * 4, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun createSimulationTargets() {
        GLES30.glGenTextures(2, textures, 0)
        GLES30.glGenFramebuffers(2, framebuffers, 0)

        for (i in 0..1) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[i])
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA8,
                simulationSize,
                simulationSize,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                null,
            )

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffers[i])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                textures[i],
                0,
            )
            check(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE) {
                "Morphosis framebuffer is incomplete"
            }
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun resetSimulation() {
        val pixels = ByteBuffer.allocateDirect(simulationSize * simulationSize * 4)
        repeat(simulationSize * simulationSize) {
            pixels.put(255.toByte())
            pixels.put(0.toByte())
            pixels.put(0.toByte())
            pixels.put(255.toByte())
        }
        pixels.position(0)

        for (texture in textures) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
            GLES30.glTexSubImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                0,
                0,
                simulationSize,
                simulationSize,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                pixels,
            )
            pixels.position(0)
        }
        readIndex = 0
    }

    private fun bindQuad() {
        GLES30.glBindVertexArray(vertexArray[0])
    }

    private fun loadAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun uniform1f(program: Int, name: String, value: Float) {
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun uniform1i(program: Int, name: String, value: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun uniform2f(program: Int, name: String, x: Float, y: Float) {
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, name), x, y)
    }
}
