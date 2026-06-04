package io.github.naharaoss.skpd.brush.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.resource.BrushResource
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFlexBoxApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrushListScreen(
    modifier: Modifier = Modifier,
    viewModel: BrushListViewModel,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    onBrushSelect: (BrushResource) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val brushes by viewModel.brushes.collectAsState()
    var addTagDialog by remember { mutableStateOf(false) }
    var addBrushDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Brush editor") },
                subtitle = { Text("Select brush to edit") },
                navigationIcon = {
                    TooltipIconButton(
                        painter = painterResource(R.drawable.arrow_back_24px),
                        description = "Go back",
                        onClick = onBack
                    )
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            targetState = brushes,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it != null }
        ) { brushes ->
            if (brushes != null) {
                BrushPicker(
                    modifier = Modifier.fillMaxSize(),
                    padding = PaddingValues(16.dp),
                    compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact,
                    brushes = brushes,
                    onAddTag = { addTagDialog = true },
                    onAddBrush = { addBrushDialog = true },
                    onBrushSelect = onBrushSelect
                )
            } else {
                var showIndicator by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(200)
                    showIndicator = true
                }

                AnimatedVisibility(
                    modifier = Modifier.fillMaxSize(),
                    visible = showIndicator,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        }
    }

    if (addTagDialog) {
        TagEditDialog(
            processing = false,
            title = { Text("Add tag") },
            initialName = "",
            initialIcon = null,
            onDismissRequest = { addTagDialog = false },
            onConfirm = { name, icon ->
            }
        )
    }

    if (addBrushDialog) {
        var processing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        BrushMetadataEditDialog(
            processing = processing,
            title = { Text("Add brush") },
            initialName = "",
            initialIcon = null,
            onDismissRequest = { addBrushDialog = false },
            onConfirm = { name, icon ->
                scope.launch {
                    processing = true
                    val brush = viewModel.createBrush(name, icon)
                    onBrushSelect(brush)
                    addBrushDialog = false
                }
            }
        )
    }
}