package com.mangoscam.wemwem.morphosis.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

class AudioEngine {
    private val running = AtomicBoolean(false)
    private val latest = AtomicReference(AudioFeatures())
    private var worker: Thread? = null
    private var recorder: AudioRecord? = null

    fun snapshot(): AudioFeatures = latest.get()

    @SuppressLint("MissingPermission")
    fun start() {
        if (!running.compareAndSet(false, true)) return

        val sampleRate = 48_000
        val fftSize = 1_024
        val minimumBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = max(minimumBuffer, fftSize * 4)

        val localRecorder = createRecorder(
            sampleRate = sampleRate,
            bufferSize = bufferSize,
            source = MediaRecorder.AudioSource.UNPROCESSED,
        ) ?: createRecorder(
            sampleRate = sampleRate,
            bufferSize = bufferSize,
            source = MediaRecorder.AudioSource.MIC,
        )

        if (localRecorder == null) {
            running.set(false)
            return
        }

        recorder = localRecorder
        worker = thread(name = "WemWemAudio", priority = Thread.MAX_PRIORITY) {
            val pcm = ShortArray(fftSize)
            val real = FloatArray(fftSize)
            val imag = FloatArray(fftSize)
            val fft = Radix2Fft(fftSize)
            var previousPeak = 0f

            try {
                localRecorder.startRecording()
                while (running.get()) {
                    val read = localRecorder.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (read <= 0) continue

                    var sumSquares = 0.0
                    for (i in 0 until fftSize) {
                        val sample = if (i < read) pcm[i] / 32768f else 0f
                        val window = (0.5 - 0.5 * cos(2.0 * PI * i / (fftSize - 1))).toFloat()
                        val value = sample * window
                        real[i] = value
                        imag[i] = 0f
                        sumSquares += sample * sample
                    }

                    fft.transform(real, imag)

                    val rms = sqrt(sumSquares / max(read, 1)).toFloat()
                    val level = compress(rms * 8f)
                    val bass = bandEnergy(real, imag, sampleRate, 40f, 250f)
                    val mid = bandEnergy(real, imag, sampleRate, 250f, 2_000f)
                    val treble = bandEnergy(real, imag, sampleRate, 2_000f, 10_000f)
                    val peak = (level - previousPeak * 0.86f).coerceAtLeast(0f)
                    previousPeak = max(level, previousPeak * 0.92f)

                    val old = latest.get()
                    latest.set(
                        AudioFeatures(
                            level = smooth(old.level, level, 0.24f),
                            bass = smooth(old.bass, bass, 0.18f),
                            mid = smooth(old.mid, mid, 0.2f),
                            treble = smooth(old.treble, treble, 0.24f),
                            peak = smooth(old.peak, peak, 0.35f),
                        ),
                    )
                }
            } finally {
                runCatching { localRecorder.stop() }
                localRecorder.release()
                recorder = null
                running.set(false)
                latest.set(AudioFeatures())
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(sampleRate: Int, bufferSize: Int, source: Int): AudioRecord? =
        runCatching {
            val candidate = AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                candidate
            } else {
                candidate.release()
                null
            }
        }.getOrNull()

    fun stop() {
        running.set(false)
        runCatching { recorder?.stop() }
        worker?.join(400)
        worker = null
    }

    private fun bandEnergy(
        real: FloatArray,
        imag: FloatArray,
        sampleRate: Int,
        lowHz: Float,
        highHz: Float,
    ): Float {
        val binHz = sampleRate.toFloat() / real.size
        val from = (lowHz / binHz).toInt().coerceAtLeast(1)
        val to = (highHz / binHz).toInt().coerceAtMost(real.size / 2 - 1)
        if (to <= from) return 0f

        var sum = 0.0
        for (i in from..to) {
            sum += real[i] * real[i] + imag[i] * imag[i]
        }
        val average = sqrt(sum / (to - from + 1)).toFloat() / real.size
        return compress(average * 28f)
    }

    private fun compress(value: Float): Float {
        val positive = value.coerceAtLeast(0f)
        return (ln(1f + positive * 8f) / ln(9f)).coerceIn(0f, 1f)
    }

    private fun smooth(old: Float, new: Float, amount: Float): Float = old + (new - old) * amount
}
