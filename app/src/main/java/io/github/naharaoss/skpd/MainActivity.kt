package io.github.naharaoss.skpd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.naharaoss.skpd.impl.brush.dab.DabBrush
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import io.github.naharaoss.skpd.view.ScratchpadLowLatencyView
import io.github.naharaoss.skpd.view.ScratchpadView

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            var brush by remember { mutableStateOf(DabBrush.Pencil) }

            SketchpadTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomAppBar {
                            Row(
                                Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                ToggleButton(
                                    modifier = Modifier.weight(1f),
                                    checked = brush == DabBrush.Pencil,
                                    onCheckedChange = { brush = DabBrush.Pencil },
                                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                ) {
                                    Text("Pencil")
                                }
                                ToggleButton(
                                    modifier = Modifier.weight(1f),
                                    checked = brush == DabBrush.Default,
                                    onCheckedChange = { brush = DabBrush.Default },
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                ) {
                                    Text("Ink")
                                }
                            }
                            IconButton({}) {
                                Icon(
                                    painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        AndroidView(
                            factory = { ScratchpadLowLatencyView(it) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            it.brushPreset = brush
                        }
                    }
                }
            }
        }
    }
}
