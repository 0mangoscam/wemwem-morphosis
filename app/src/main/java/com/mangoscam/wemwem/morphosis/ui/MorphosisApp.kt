package com.mangoscam.wemwem.morphosis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mangoscam.wemwem.morphosis.audio.AudioEngine
import com.mangoscam.wemwem.morphosis.engine.MorphosisController
import com.mangoscam.wemwem.morphosis.engine.MorphosisParameters
import com.mangoscam.wemwem.morphosis.engine.MorphosisSurfaceView

@Composable
fun MorphosisApp(
    audioEngine: AudioEngine,
    controller: MorphosisController,
    microphoneReady: Boolean,
    onRequestMicrophone: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var controlsVisible by remember { mutableStateOf(true) }
    val surface = remember { MorphosisSurfaceView(context, audioEngine, controller) }

    DisposableEffect(lifecycleOwner, surface) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surface.onResume()
                Lifecycle.Event.ON_PAUSE -> surface.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            surface.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surface.onPause()
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { surface },
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "WEMWEM",
                    color = Color.White,
                    fontSize = 15.sp,
                    letterSpacing = 4.sp,
                )
                Text(
                    text = "MORPHOSIS  //  ${if (microphoneReady) "LISTENING" else "TAP TO ENABLE MIC"}",
                    color = if (microphoneReady) Color.White.copy(alpha = 0.64f) else Color(0xFFFF927D),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.clickable(
                        enabled = !microphoneReady,
                        onClick = onRequestMicrophone,
                    ),
                )
            }

            Text(
                text = if (controlsVisible) "HIDE GENETICS" else "SHOW GENETICS",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .clickable { controlsVisible = !controlsVisible }
                    .padding(8.dp),
            )

            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ControlPanel(
                    parameters = controller.parameters,
                    onChange = controller::update,
                    onReset = controller::reset,
                )
            }
        }
    }
}

@Composable
private fun ControlPanel(
    parameters: MorphosisParameters,
    onChange: ((MorphosisParameters) -> MorphosisParameters) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.80f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("GENETIC / CELL_01", color = Color.White, fontSize = 11.sp, letterSpacing = 1.4.sp)
            Text(
                "ERASE ORGANISM",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable(onClick = onReset)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))

        ParameterSlider("MATTER", parameters.matter) { value -> onChange { it.copy(matter = value) } }
        ParameterSlider("MUTATION", parameters.mutation) { value -> onChange { it.copy(mutation = value) } }
        ParameterSlider("DECAY", parameters.decay) { value -> onChange { it.copy(decay = value) } }
        ParameterSlider("DEPTH", parameters.depth) { value -> onChange { it.copy(depth = value) } }
        ParameterSlider("MEMORY", parameters.memory) { value -> onChange { it.copy(memory = value) } }
        ParameterSlider("SENSITIVITY", parameters.sensitivity) { value -> onChange { it.copy(sensitivity = value) } }
    }
}

@Composable
private fun ParameterSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, modifier = Modifier.width(94.dp))
        Slider(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
        Text(
            text = "%02d".format((value * 99f).toInt()),
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 10.sp,
            modifier = Modifier.width(28.dp),
        )
    }
}
