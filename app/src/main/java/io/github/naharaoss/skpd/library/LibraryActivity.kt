package io.github.naharaoss.skpd.library

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.document.DocumentActivity
import io.github.naharaoss.skpd.resource.LibraryItem
import io.github.naharaoss.skpd.settings.SettingsActivity
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.library.ui.LibraryCard
import io.github.naharaoss.skpd.library.ui.LibraryCardMetadata
import io.github.naharaoss.skpd.library.ui.LibraryDocumentPreview
import io.github.naharaoss.skpd.library.ui.NewDocumentDialog
import io.github.naharaoss.skpd.library.ui.NewFolderDialog
import io.github.naharaoss.skpd.ui.component.ElapsedText
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import javax.inject.Inject
import io.github.naharaoss.skpd.library.ui.DeleteDialog
import io.github.naharaoss.skpd.utils.toggle

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
            val snackBar = remember { SnackbarHostState() }
            var backStack by rememberSerializable { mutableStateOf(listOf(LibraryItem.Root)) }
            var selectedItems by rememberSerializable { mutableStateOf(emptySet<LibraryItem>()) }
            var fabMenu by remember { mutableStateOf(false) }
            var newFolderDialog by remember { mutableStateOf(false) }
            var newDocumentDialog by remember { mutableStateOf(false) }
            var deleteDialog by remember { mutableStateOf(false) }
            val fadeMotion = tween<Float>(durationMillis = 300, easing = LinearEasing)
            val slideMotion = tween<IntOffset>(300, easing = FastOutSlowInEasing)

            BackHandler(fabMenu) {
                fabMenu = false
            }

            BackHandler(selectedItems.isNotEmpty()) {
                selectedItems = emptySet()
            }

            SketchpadTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val topAppBarColor = if (selectedItems.isEmpty()) Color.Unspecified else MaterialTheme.colorScheme.primaryContainer

                        LargeFlexibleTopAppBar(
                            title = { Text(stringResource(R.string.title_main)) },
                            subtitle = {
                                val last = backStack.last()

                                when {
                                    selectedItems.isNotEmpty() -> Text("${selectedItems.size} selected items")
                                    last == LibraryItem.Root -> Text("Local sketches")
                                    else -> Text(last.name)
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                AnimatedContent(
                                    targetState = backStack.size > 1,
                                    transitionSpec = {
                                        val enter = fadeIn() + slideInVertically { it }
                                        val exit = fadeOut() + slideOutVertically { it }
                                        (enter togetherWith exit).using(SizeTransform(clip = false))
                                    }
                                ) { show ->
                                    if (show) {
                                        TooltipIconButton(
                                            painter = painterResource(R.drawable.arrow_back_24px),
                                            description = "Navigate up",
                                            onClick = { backStack = backStack.dropLast(1) }
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
                            visible = selectedItems.isEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            FloatingActionButtonMenu(
                                modifier = Modifier.padding(bottom = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()),
                                expanded = fabMenu,
                                button = {
                                    ToggleFloatingActionButton(
                                        checked = fabMenu,
                                        onCheckedChange = { fabMenu = it }
                                    ) {
                                        val rotation by animateFloatAsState(
                                            targetValue = if (fabMenu) 45f else 0f,
                                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                                        )

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
                                        newDocumentDialog = true
                                    },
                                    icon = { Icon(painterResource(R.drawable.note_add_24px), null) },
                                    text = { Text("New sketch") }
                                )
                                FloatingActionButtonMenuItem(
                                    onClick = {
                                        fabMenu = false
                                        newFolderDialog = true
                                    },
                                    icon = { Icon(painterResource(R.drawable.create_new_folder_24px), null) },
                                    text = { Text("New folder") }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        AnimatedContent(
                            targetState = selectedItems.isNotEmpty(),
                            transitionSpec = {
                                val enter = slideInVertically { it }
                                val exit = slideOutVertically { -it }
                                enter togetherWith exit
                            }
                        ) { show ->
                            when (show) {
                                true -> FlexibleBottomAppBar {
                                    TextButton({ deleteDialog = true }) {
                                        Icon(painterResource(R.drawable.delete_24px), "Delete")
                                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                        Text("Delete")
                                    }
                                }

                                false -> Spacer(Modifier.fillMaxWidth())
                            }
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackBar)
                    }
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        backStack = backStack,
                        onBack = { backStack = backStack.dropLast(1) },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        transitionSpec = {
                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / 4 }
                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / -4 }
                            enter togetherWith exit
                        },
                        popTransitionSpec = {
                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it / -4 }
                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it / 4 }
                            enter togetherWith exit
                        },
                        predictivePopTransitionSpec = { edge ->
                            val edgeMul = when (edge) {
                                NavigationEvent.EDGE_LEFT -> -1
                                NavigationEvent.EDGE_RIGHT -> +1
                                else -> -1
                            }

                            val enter = fadeIn(fadeMotion) + slideInHorizontally(slideMotion) { it * edgeMul / 4 }
                            val exit = fadeOut(fadeMotion) + slideOutHorizontally(slideMotion) { it * edgeMul / -4 }
                            enter togetherWith exit
                        },
                        entryProvider = entryProvider {
                            entry<LibraryItem.Folder> { key ->
                                val folderViewModel = hiltViewModel(creationCallback = { factory: LibraryFolderViewModel.Factory -> factory.create(key) })
                                val content by folderViewModel.content.collectAsState()

                                AnimatedContent(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    targetState = content,
                                    contentKey = { it != null },
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { content ->
                                    when {
                                        content == null -> Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LoadingIndicator()
                                        }

                                        content.isEmpty() -> Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text("Empty folder")
                                            Text("Tap on + to create")
                                        }

                                        else -> {
                                            val folders = content.filterIsInstance<LibraryItem.Folder>()
                                            val documents = content.filterIsInstance<LibraryItem.Document>()

                                            LazyVerticalGrid(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                columns = GridCells.Adaptive(160.dp)
                                            ) {
                                                items(folders, key = { it.id }) { folder ->
                                                    LibraryCard(
                                                        selected = selectedItems.contains(folder),
                                                        metadata = {
                                                            LibraryCardMetadata(
                                                                title = { Text(folder.name) },
                                                                subtitle = {
                                                                    Text("Folder")
                                                                    ElapsedText(folder.lastModified)
                                                                }
                                                            )
                                                        },
                                                        onClick = {
                                                            if (selectedItems.isNotEmpty()) {
                                                                selectedItems = selectedItems.toggle(folder)
                                                                return@LibraryCard
                                                            }

                                                            backStack = backStack + folder
                                                        },
                                                        onLongClick = { selectedItems = selectedItems.toggle(folder) }
                                                    )
                                                }

                                                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                                                    // Nothing
                                                }

                                                items(documents, key = { it.id }) { document ->
                                                    LibraryCard(
                                                        selected = selectedItems.contains(document),
                                                        preview = { LibraryDocumentPreview() },
                                                        metadata = {
                                                            LibraryCardMetadata(
                                                                title = { Text(document.name) },
                                                                subtitle = {
                                                                    Text("Sketch")
                                                                    ElapsedText(document.lastModified)
                                                                }
                                                            )
                                                        },
                                                        onClick = {
                                                            if (selectedItems.isNotEmpty()) {
                                                                selectedItems = selectedItems.toggle(document)
                                                                return@LibraryCard
                                                            }

                                                            val intent = Intent(this@LibraryActivity, DocumentActivity::class.java)
                                                            intent.putExtra(DocumentActivity.EXTRA_DOCUMENT_ID, document.id)
                                                            startActivity(intent)
                                                        },
                                                        onLongClick = { selectedItems = selectedItems.toggle(document) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (newFolderDialog) {
                                    NewFolderDialog(
                                        onDismiss = { newFolderDialog = false },
                                        onConfirm = {
                                            folderViewModel.createFolder(it)
                                            newFolderDialog = false
                                        }
                                    )
                                }

                                if (newDocumentDialog) {
                                    NewDocumentDialog(
                                        onDismiss = { newDocumentDialog = false },
                                        onConfirm = { name, size ->
                                            val document = folderViewModel.createDocument(name, size)
                                            newDocumentDialog = false

                                            val intent = Intent(this@LibraryActivity, DocumentActivity::class.java)
                                            intent.putExtra(DocumentActivity.EXTRA_DOCUMENT_ID, document.id)
                                            startActivity(intent)
                                        }
                                    )
                                }

                                if (deleteDialog) {
                                    DeleteDialog(
                                        items = selectedItems,
                                        onDismiss = { deleteDialog = false },
                                        onConfirm = {
                                            val items = selectedItems
                                            for (item in items) folderViewModel.deleteItem(item)
                                            selectedItems = emptySet()
                                            deleteDialog = false

                                            snackBar.showSnackbar(
                                                message = when {
                                                    items.size == 1 -> "Deleted ${items.first().name}"
                                                    else -> "Deleted ${items.size} items"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}