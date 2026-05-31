package io.github.naharaoss.skpd.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.InputProcessor
import io.github.naharaoss.skpd.brush.Sensor
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme

class InputTestActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalGridApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val stroke = remember { mutableStateListOf<StylusInput>() }
            var lastAction by remember { mutableStateOf<InputProcessor.Action?>(null) }
            var canvasTransform by remember { mutableStateOf(Matrix()) }
            val boxColor = MaterialTheme.colorScheme.tertiary
            val strokeColor = MaterialTheme.colorScheme.primary
            val eventColor = MaterialTheme.colorScheme.secondary

            SketchpadTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        LargeFlexibleTopAppBar(
                            title = {Text(stringResource(R.string.title_input_test)) },
                            navigationIcon = {
                                TooltipIconButton(
                                    painter = painterResource(R.drawable.arrow_back_24px),
                                    description = "Go back",
                                    onClick = ::finish
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    Canvas(Modifier.padding(innerPadding).fillMaxSize()) {
                        withTransform({
                            translate(size.width / 2f, size.height / 2f)
                            transform(canvasTransform)
                        }) {
                            drawRect(
                                topLeft = Offset(size.width / -2f, size.height / -2f),
                                size = size,
                                color = boxColor,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        val path = Path()
                        var isFirst = true

                        for (input in stroke) {
                            if (isFirst) {
                                isFirst = false
                                path.moveTo(input.x, input.y)
                            } else {
                                path.lineTo(input.x, input.y)
                            }

                            drawRect(
                                topLeft = Offset(input.x - 2.dp.toPx(), input.y - 2.dp.toPx()),
                                size = Size(4.dp.toPx(), 4.dp.toPx()),
                                color = eventColor
                            )
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    Column(Modifier
                        .padding(innerPadding)
                        .padding(16.dp)) {
                        val lastAction = lastAction

                        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                            Text("Finger drawing: On \u2022 Touch slop: 10px")

                            Spacer(Modifier.height(8.dp))

                            when (lastAction) {
                                null -> {
                                    Text("Interact this area to begin")
                                }

                                is InputProcessor.Action.Stylus -> {
                                    Text(if (!lastAction.eraser) "Pen input" else "Eraser input")
                                    Text("Action: ${lastAction.kind}")

                                    Spacer(Modifier.height(8.dp))

                                    Text("Event \u2022 Time: ${"%.2f".format(lastAction.input.time)} seconds")
                                    Text("Event \u2022 X: ${"%.2f".format(lastAction.input.x)}")
                                    Text("Event \u2022 Y: ${"%.2f".format(lastAction.input.y)}")
                                    Text("Event \u2022 Velocity: ${"%.2f".format(lastAction.input.velocity)} px/s")
                                    Text("Event \u2022 Pressure: ${"%.2f".format(lastAction.input.pressure * 100f)}%")
                                    Text("Event \u2022 Altitude: ${"%.2f".format(lastAction.input.altitude)}\u00B0")
                                    Text("Event \u2022 Azimuth: ${"%.2f".format(lastAction.input.azimuth)}\u00B0")
                                    Text("Event \u2022 Rotation: NOT IMPLEMENTED")
                                    Text("Event \u2022 Stroke jitter: ${"%.4f".format(lastAction.input.strokeJitter)}")

                                    Spacer(Modifier.height(8.dp))

                                    for (sensor in Sensor.AllDefaults) {
                                        val name = stringResource(sensor.nameRes)
                                        val value = sensor.forInput(lastAction.input)
                                        Text("Sensor \u2022 $name: ${"%.2f".format(value * 100)}%")
                                    }
                                }

                                is InputProcessor.Action.Transform -> {
                                    Text("Transform canvas")

                                    Spacer(Modifier.height(8.dp))

                                    Text("Transforming matrix:")
                                    Grid(config = { repeat(4) { column(50.dp) } }) {
                                        for (value in lastAction.matrix.values) {
                                            Text("%.2f".format(value))
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text("Canvas transformation matrix:")
                                    Grid(config = { repeat(4) { column(50.dp) } }) {
                                        for (value in canvasTransform.values) {
                                            Text("%.2f".format(value))
                                        }
                                    }
                                }

                                is InputProcessor.Action.TapGesture -> {
                                    Text("Tap gesture")

                                    when (lastAction.fingers) {
                                        1 -> Text("1 finger")
                                        else -> Text("${lastAction.fingers} fingers")
                                    }
                                }

                                else -> {
                                    Text("Unknown action: ${lastAction::class.simpleName}")
                                }
                            }
                        }
                    }

                    AndroidView(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        factory = ::TestInputView
                    ) {
                        it.onActions = { actions ->
                            actions.lastOrNull()?.let { lastAction = it }

                            for (action in actions) {
                                when (action) {
                                    is InputProcessor.Action.Stylus -> {
                                        if (action.kind == InputProcessor.Action.Stylus.Kind.Down) stroke.clear()
                                        stroke.add(action.input)
                                    }

                                    is InputProcessor.Action.Transform -> {
                                        val newTransform = Matrix(canvasTransform.values.clone())
                                        newTransform *= action.matrix
                                        canvasTransform = newTransform
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class TestInputView(context: Context) : View(context) {
        private val processor = object : InputProcessor(true, 10f) {
            override fun requestUnbufferedDispatch(event: MotionEvent) {
                this@TestInputView.requestUnbufferedDispatch(event)
            }
        }

        var onActions: ((List<InputProcessor.Action>) -> Unit)? = null

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event == null) return super.onTouchEvent(event)
            val actions = processor.updateState(event)
            onActions?.invoke(actions)
            return true;
        }
    }
}