package com.roshni.games.feature.settings.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import com.roshni.games.feature.settings.domain.model.SettingsDialogType
import com.roshni.games.feature.settings.domain.model.SettingsItemType
import com.roshni.games.feature.settings.domain.model.SettingsNavigationEvent
import com.roshni.games.feature.settings.presentation.components.ConfirmationDialog
import com.roshni.games.feature.settings.presentation.components.SuccessDialog
import com.roshni.games.feature.settings.presentation.viewmodel.SettingsAction
import com.roshni.games.feature.settings.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToParentalControls: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit
) {
    RoshniGamesTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                SettingsTopBar(onNavigateBack = onNavigateBack)
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.settingsState.settingsSections) { section ->
                    SettingsSection(
                        section = section,
                        isExpanded = uiState.expandedSections.contains(section.id),
                        onToggleSection = { viewModel.onAction(SettingsAction.ToggleSection(section.id)) },
                        onAction = viewModel::onAction
                    )
                }

                // Spacer for bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Handle navigation events
        LaunchedEffect(viewModel.navigationEvent) {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    SettingsNavigationEvent.NavigateToProfile -> onNavigateToProfile()
                    SettingsNavigationEvent.NavigateToParentalControlsSetup -> onNavigateToParentalControls()
                    SettingsNavigationEvent.NavigateToLanguageSelection -> onNavigateToLanguageSelection()
                    is SettingsNavigationEvent.ShowDialog -> {
                        when (event.dialogType) {
                            SettingsDialogType.RESET_SETTINGS -> {
                                // Show reset confirmation dialog
                            }
                            SettingsDialogType.DELETE_ACCOUNT -> {
                                // Show delete account confirmation dialog
                            }
                            SettingsDialogType.EXPORT_DATA -> {
                                // Show export dialog
                            }
                            SettingsDialogType.IMPORT_DATA -> {
                                // Show import dialog
                            }
                            SettingsDialogType.PIN_SETUP -> {
                                // Show PIN setup dialog
                            }
                            SettingsDialogType.AGE_RATING_SELECTION -> {
                                // Show age rating selection dialog
                            }
                        }
                    }
                }
            }
        }

        // Show dialogs
        if (uiState.showResetDialog) {
            ConfirmationDialog(
                title = "Reset Settings",
                message = "Are you sure you want to reset all settings to their default values? This action cannot be undone.",
                confirmText = "Reset",
                onConfirm = { viewModel.onAction(SettingsAction.ResetSettings) },
                onDismiss = { viewModel.onAction(SettingsAction.DismissDialogs) }
            )
        }

        if (uiState.showDeleteAccountDialog) {
            ConfirmationDialog(
                title = "Delete Account",
                message = "Are you sure you want to delete your account? This will permanently remove all your data and cannot be undone.",
                confirmText = "Delete",
                onConfirm = { viewModel.onAction(SettingsAction.DeleteAccount) },
                onDismiss = { viewModel.onAction(SettingsAction.DismissDialogs) }
            )
        }

        if (uiState.showExportSuccessDialog) {
            SuccessDialog(
                title = "Export Successful",
                message = "Your data has been exported successfully to: ${uiState.exportFilePath}",
                onDismiss = { viewModel.onAction(SettingsAction.DismissDialogs) }
            )
        }

        if (uiState.showImportSuccessDialog) {
            SuccessDialog(
                title = "Import Successful",
                message = "Your data has been imported successfully.",
                onDismiss = { viewModel.onAction(SettingsAction.DismissDialogs) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SettingsSection(
    section: com.roshni.games.feature.settings.domain.model.SettingsSection,
    isExpanded: Boolean,
    onToggleSection: () -> Unit,
    onAction: (SettingsAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleSection)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section Icon
                Text(
                    text = section.icon ?: "⚙️",
                    fontSize = 24.sp,
                    modifier = Modifier.width(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Section Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    section.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Expand/Collapse Icon
                Icon(
                    if (isExpanded) {
                        androidx.compose.material.icons.Icons.Default.ArrowForward // Rotate this
                    } else {
                        Icons.Default.ArrowForward
                    },
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggleSection)
                )
            }

            // Section Items
            if (isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    section.items.forEach { item ->
                        SettingsItem(
                            item = item,
                            onAction = onAction
                        )

                        // Divider between items (except for last item)
                        if (item != section.items.last()) {
                            androidx.compose.material3.Divider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    item: com.roshni.games.feature.settings.domain.model.SettingsItem,
    onAction: (SettingsAction) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.type != SettingsItemType.BUTTON,
                onClick = {
                    when (item.type) {
                        SettingsItemType.NAVIGATION -> {
                            onAction(SettingsAction.NavigateToSection(item.id))
                        }
                        SettingsItemType.BUTTON -> {
                            when (item.id) {
                                "export_data" -> onAction(SettingsAction.ExportData)
                                "delete_account" -> onAction(SettingsAction.DeleteAccount)
                                "reset_settings" -> onAction(SettingsAction.ResetSettings)
                            }
                        }
                        SettingsItemType.DROPDOWN -> {
                            showDropdownMenu = true
                        }
                        else -> {
                            // Handle other types if needed
                        }
                    }
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Item Control
        when (item.type) {
            SettingsItemType.TOGGLE -> {
                Switch(
                    checked = item.value as? Boolean ?: false,
                    onCheckedChange = { value ->
                        onAction(SettingsAction.UpdateToggleSetting("section", item.id, value))
                    },
                    enabled = item.isEnabled
                )
            }
            SettingsItemType.DROPDOWN -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.value as? String ?: "Select",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Select option",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false },
                    offset = DpOffset(x = 0.dp, y = 4.dp)
                ) {
                    item.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onAction(SettingsAction.UpdateDropdownSetting("section", item.id, option))
                                showDropdownMenu = false
                            },
                            trailingIcon = {
                                if (option == item.value) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
            SettingsItemType.BUTTON -> {
                Text(
                    text = "Action",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            SettingsItemType.NAVIGATION -> {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                // Handle other types if needed
            }
        }
    }
}