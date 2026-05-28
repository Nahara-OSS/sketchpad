package io.github.naharaoss.skpd.library

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushEditorActivity
import io.github.naharaoss.skpd.document.DocumentActivity
import io.github.naharaoss.skpd.settings.SettingsActivity
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import io.github.naharaoss.skpd.ui.slideTransition
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import javax.inject.Inject

@AndroidEntryPoint
class LibraryActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !settingsRepository.initialized }

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val backStack = rememberSaveable { mutableStateListOf("root") }
            var fabMenu by remember { mutableStateOf(false) }

            SketchpadTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val topAppBarColor = Color.Unspecified // TODO

                        LargeFlexibleTopAppBar(
                            title = { Text(stringResource(R.string.title_main)) },
                            subtitle = { Text("Local sketches") },
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                AnimatedContent(targetState = backStack.size > 1) { show ->
                                    if (show) {
                                        TooltipIconButton(
                                            painter = painterResource(R.drawable.arrow_back_24px),
                                            description = "Navigate up",
                                            onClick = { backStack.removeLastOrNull() }
                                        )
                                    }
                                }
                            },
                            actions = {
                                TooltipIconButton(
                                    painter = painterResource(R.drawable.settings_24px),
                                    description = "App settings",
                                    onClick = {
                                        val intent =
                                            Intent(this@LibraryActivity, SettingsActivity::class.java)
                                        startActivity(intent)
                                    }
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = topAppBarColor,
                                scrolledContainerColor = topAppBarColor
                            )
                        )
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = true, // TODO
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            FloatingActionButtonMenu(
                                expanded = fabMenu,
                                button = {
                                    ToggleFloatingActionButton(
                                        checked = fabMenu,
                                        onCheckedChange = { fabMenu = it }
                                    ) {
                                        val rotation by animateFloatAsState(if (fabMenu) 45f else 0f)

                                        Icon(
                                            modifier = Modifier.rotate(rotation),
                                            painter = painterResource(R.drawable.add_24px),
                                            contentDescription = "Add new item"
                                        )
                                    }
                                }
                            ) {
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        fabMenu = false
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.note_add_24px),
                                            null
                                        )
                                    },
                                    text = { Text("New sketch") }
                                )
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        fabMenu = false
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.create_new_folder_24px),
                                            null
                                        )
                                    },
                                    text = { Text("New folder") }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        // TODO
                    }
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        transitionSpec = { slideTransition(false).using(SizeTransform(clip = false)) },
                        popTransitionSpec = { slideTransition(true).using(SizeTransform(clip = false)) }
                    ) { key ->
                        NavEntry(key) {
                            Row(Modifier.padding(innerPadding)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(this@LibraryActivity, DocumentActivity::class.java)
                                        startActivity(intent)
                                    }
                                ) {
                                    Text("Open sample document")
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(this@LibraryActivity, BrushEditorActivity::class.java)
                                        startActivity(intent)
                                    }
                                ) {
                                    Text("Open brush editor")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}