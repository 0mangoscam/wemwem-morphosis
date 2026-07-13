package com.mangoscam.wemwem.morphosis.engine

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.mangoscam.wemwem.morphosis.audio.AudioEngine

class MorphosisSurfaceView(
    context: Context,
    audioEngine: AudioEngine,
    controller: MorphosisController,
) : GLSurfaceView(context) {
    private val morphosisRenderer = MorphosisRenderer(context, audioEngine, controller)

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(morphosisRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
        val y = (1f - event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                morphosisRenderer.setTouch(x, y, true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                morphosisRenderer.setTouch(x, y, false)
        }
        return true
    }
}
