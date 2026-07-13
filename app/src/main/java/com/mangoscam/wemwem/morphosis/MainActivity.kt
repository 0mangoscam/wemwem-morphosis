package com.mangoscam.wemwem.morphosis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mangoscam.wemwem.morphosis.audio.AudioEngine
import com.mangoscam.wemwem.morphosis.engine.MorphosisController
import com.mangoscam.wemwem.morphosis.ui.MorphosisApp

class MainActivity : ComponentActivity() {
    private val audioEngine = AudioEngine()
    private val controller = MorphosisController()
    private var microphoneReady by mutableStateOf(false)

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphoneReady = granted
        if (granted) audioEngine.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        microphoneReady = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        setContent {
            MorphosisApp(
                audioEngine = audioEngine,
                controller = controller,
                microphoneReady = microphoneReady,
                onRequestMicrophone = {
                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }

        if (microphoneReady) {
            audioEngine.start()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        if (microphoneReady) audioEngine.start()
    }

    override fun onPause() {
        audioEngine.stop()
        super.onPause()
    }
}
