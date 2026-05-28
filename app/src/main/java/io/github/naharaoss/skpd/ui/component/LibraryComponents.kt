package io.github.naharaoss.skpd.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import kotlinx.coroutines.delay

@Composable
fun LibraryCard(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit = {},
    metadata: @Composable () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        shape = CardDefaults.shape,
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        tonalElevation = when {
            selected -> 8.dp
            else -> 1.dp
        },
        shadowElevation = when {
            !enabled -> 0.dp
            selected -> 2.dp
            else -> 0.dp
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            preview()

            Box(Modifier.padding(16.dp)) {
                metadata()
            }
        }
    }
}

@Composable
fun LibraryDocumentPreview() {
    Box(Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .background(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        )
    )
}

@Composable
fun LibraryCardMetadata(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge) {
            Box(modifier = Modifier.basicMarquee()) {
                title()
            }
        }

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
                subtitle()
            }
        }
    }
}

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by rememberSaveable { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<Exception?>(null) }
    val focusRequester = remember { FocusRequester() }

    FancyDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.create_new_folder_24px),
                contentDescription = "New folder"
            )
        },
        title = { Text("New folder") },
        buttons = {
            TextButton(
                enabled = !creating,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
            TextButton(
                enabled = folderName.isNotEmpty() && !creating,
                onClick = { creating = true }
            ) {
                Text("Confirm")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FancyDialogText {
                Text("Enter the name of new folder to create.")
            }

            TextField(
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                enabled = !creating,
                label = { Text("Folder name") },
                value = folderName,
                onValueChange = { folderName = it },
                isError = lastError != null,
                supportingText = {
                    val error = lastError
                    if (error != null) Text(error.message ?: "An error occurred")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(creating) {
        if (creating) {
            delay(1000)
            creating = false
        }
    }
}

@Composable
fun RenameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialName: String
) {
    var newName by remember(initialName) { mutableStateOf(initialName) }
    var renaming by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<Exception?>(null) }
    val focusRequester = remember { FocusRequester() }

    FancyDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.edit_24px),
                contentDescription = "Rename"
            )
        },
        title = { Text("Rename") },
        buttons = {
            TextButton(
                enabled = !renaming,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
            TextButton(
                enabled = !renaming,
                onClick = {}
            ) {
                Text("Confirm")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FancyDialogText {
                Text("Enter new name to rename.")
            }

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !renaming,
                label = { Text("New name") },
                value = newName,
                onValueChange = { newName = it },
                isError = lastError != null,
                supportingText = {
                    val error = lastError
                    if (error != null) Text(error.message ?: "An error occurred")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun NewDocumentDialog(onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }

    FancyDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.note_add_24px),
                contentDescription = "New sketch"
            )
        },
        title = { Text("New sketch") },
        buttons = {
            TextButton(
                enabled = !creating,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
            TextButton(
                enabled = !creating,
                onClick = {
                    // TODO: Create new document
                }
            ) {
                Text("Confirm")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        }
    }
}
