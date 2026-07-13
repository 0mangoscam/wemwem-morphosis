package com.mangoscam.wemwem.morphosis.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Minimal in-place radix-2 FFT for microphone analysis. */
internal class Radix2Fft(private val size: Int) {
    private val levels = Integer.numberOfTrailingZeros(size)
    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)

    init {
        require(size > 1 && size and (size - 1) == 0) { "FFT size must be a power of two" }
        for (i in cosTable.indices) {
            val angle = 2.0 * PI * i / size
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    fun transform(real: FloatArray, imag: FloatArray) {
        require(real.size == size && imag.size == size)

        for (i in 0 until size) {
            val j = Integer.reverse(i) ushr (32 - levels)
            if (j > i) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal

                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
        }

        var blockSize = 2
        while (blockSize <= size) {
            val half = blockSize / 2
            val tableStep = size / blockSize
            var start = 0
            while (start < size) {
                var tableIndex = 0
                for (j in start until start + half) {
                    val l = j + half
                    val tReal = real[l] * cosTable[tableIndex] + imag[l] * sinTable[tableIndex]
                    val tImag = -real[l] * sinTable[tableIndex] + imag[l] * cosTable[tableIndex]

                    real[l] = real[j] - tReal
                    imag[l] = imag[j] - tImag
                    real[j] += tReal
                    imag[j] += tImag
                    tableIndex += tableStep
                }
                start += blockSize
            }
            blockSize = blockSize shl 1
        }
    }
}
