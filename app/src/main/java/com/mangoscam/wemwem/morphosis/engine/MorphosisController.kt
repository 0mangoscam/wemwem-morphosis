package com.mangoscam.wemwem.morphosis.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicReference

class MorphosisController {
    private val atomicParameters = AtomicReference(MorphosisParameters())

    var parameters by mutableStateOf(atomicParameters.get())
        private set

    @Volatile
    var resetToken: Long = 0L
        private set

    fun snapshot(): MorphosisParameters = atomicParameters.get()

    fun update(transform: (MorphosisParameters) -> MorphosisParameters) {
        val updated = transform(atomicParameters.get())
        atomicParameters.set(updated)
        parameters = updated
    }

    fun reset() {
        resetToken += 1L
    }
}
