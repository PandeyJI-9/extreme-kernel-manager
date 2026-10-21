package id.nkz.nokontzzzmanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import id.nkz.nokontzzzmanager.ui.dialog.RootFilePickerDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.CustomTunableEntity
import id.nkz.nokontzzzmanager.ui.MainActivity
import id.nkz.nokontzzzmanager.viewmodel.CustomTunableUiState
import id.nkz.nokontzzzmanager.viewmodel.CustomTunableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTunableScreen(
    navController: NavController,
    viewModel: CustomTunableViewModel = hiltViewModel()
) {
    val tunables by viewModel.tunables.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTunable by remember { mutableStateOf<CustomTunableEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingTunable by remember { mutableStateOf<CustomTunableEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(tunables, key = { _, item -> item.entity.path }) { index, state ->
                val shape = when {
                    tunables.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    index == tunables.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(8.dp)
                }

                CustomTunableCard(
                    uiState = state,
                    shape = shape,
                    onEdit = {
                        editingTunable = state.entity
                        showAddDialog = true
                    },
                    onDelete = {
                        deletingTunable = state.entity
                        showDeleteDialog = true
                    },
                    onToggleBoot = { enabled ->
                        viewModel.updateTunable(state.entity.path, state.entity.copy(applyOnBoot = enabled))
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(text = stringResource(R.string.add_tunable)) }
        )
    }

    if (showAddDialog) {
        CustomTunableDialog(
            viewModel = viewModel,
            initialTunable = editingTunable,
            onDismiss = {
                showAddDialog = false
                editingTunable = null
            },
            onSave = { path, value, applyOnBoot ->
                editingTunable?.let { tunable ->
                    viewModel.updateTunable(tunable.path, CustomTunableEntity(path, value, applyOnBoot))
                } ?: viewModel.addTunable(path, value, applyOnBoot)
                showAddDialog = false
                editingTunable = null
            },
            onReadValue = { path, callback ->
                viewModel.readCurrentValue(path, callback)
            }
        )
    }

    if (showDeleteDialog) {
        deletingTunable?.let { tunable ->
            ConfirmDeleteDialog(
                tunablePath = tunable.path,
                onConfirm = {
                    viewModel.deleteTunable(tunable)
                    showDeleteDialog = false
                    deletingTunable = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    deletingTunable = null
                }
            )
        }
    }
}

@Composable
fun CustomTunableCard(
    uiState: CustomTunableUiState,
    shape: androidx.compose.ui.graphics.Shape,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBoot: (Boolean) -> Unit
) {
    val tunable = uiState.entity
    val actualValue = uiState.actualValue
    val isMismatch = actualValue != null && actualValue != tunable.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tunable.path,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tunable.value == "0" || tunable.value == "1") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${stringResource(R.string.tunable_target_prefix)} ${tunable.value}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (actualValue != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.tunable_actual_prefix)} $actualValue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = if (isMismatch) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = tunable.applyOnBoot,
                        onCheckedChange = onToggleBoot,
                        modifier = Modifier.scale(0.8f),
                        thumbContent = if (tunable.applyOnBoot) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.set_on_boot),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// Extension to scale Switch
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTunableDialog(
    viewModel: CustomTunableViewModel,
    initialTunable: CustomTunableEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
    onReadValue: (String, (String) -> Unit) -> Unit
) {
    var path by remember { mutableStateOf(initialTunable?.path ?: "") }
    var value by remember { mutableStateOf(initialTunable?.value ?: "") }
    var applyOnBoot by remember { mutableStateOf(initialTunable?.applyOnBoot ?: false) }
    
    var showFilePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val valueReadMsg = stringResource(R.string.tunable_value_read)
    val pathRequiredMsg = stringResource(R.string.tunable_path_required)
    val pathInvalidMsg = stringResource(R.string.tunable_path_invalid)
    val unsafeInputMsg = stringResource(R.string.tunable_unsafe_input)

    fun isInputSafe(input: String): Boolean {
        val unsafeChars = listOf(";", "|", "&&", "$", "`", "\n", "(", ")", ">", "<", "\\")
        return unsafeChars.none { input.contains(it) }
    }

    if (showFilePicker) {
        val fileList by viewModel.fileBrowserList.collectAsStateWithLifecycle()
        val currentBrowserPath by viewModel.currentBrowserPath.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            val initialPath = if (path.startsWith("/")) path.substringBeforeLast('/', "") else "/"
            val startPath = initialPath.ifEmpty { "/" }
            viewModel.loadFileList(startPath)
        }

        RootFilePickerDialog(
            currentPath = currentBrowserPath,
            fileList = fileList,
            onNavigate = { newPath -> viewModel.loadFileList(newPath) },
            onNavigateUp = { viewModel.navigateUp() },
                            onFileSelected = { selectedPath ->
                            path = selectedPath
                            showFilePicker = false
                            onReadValue(selectedPath) { readVal -> 
                                value = readVal
                                Toast.makeText(context, valueReadMsg, Toast.LENGTH_SHORT).show()
                            }
                        },            onDismiss = { showFilePicker = false }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = if (initialTunable == null) stringResource(R.string.add_tunable) else stringResource(R.string.edit_tunable),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = { Text(stringResource(R.string.tunable_path_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showFilePicker = true }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.pick_file))
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text(stringResource(R.string.tunable_value_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (path.isNotEmpty()) {
                                        onReadValue(path) { readVal ->
                                            value = readVal
                                            Toast.makeText(context, valueReadMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.tunable_read_current))
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.set_on_boot),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Switch(
                                    checked = applyOnBoot,
                                    onCheckedChange = { applyOnBoot = it },
                                    thumbContent = if (applyOnBoot) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    } else {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.tunable_root_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (path.isNotBlank() && path.startsWith("/")) {
                                    if (isInputSafe(path) && isInputSafe(value)) {
                                        onSave(path, value, applyOnBoot)
                                    } else {
                                        Toast.makeText(context, unsafeInputMsg, Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, pathInvalidMsg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_save))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    tunablePath: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.delete_tunable_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Text(
                        text = stringResource(R.string.delete_tunable_confirm_msg, tunablePath),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}