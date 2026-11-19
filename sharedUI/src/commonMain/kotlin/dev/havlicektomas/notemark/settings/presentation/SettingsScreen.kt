package dev.havlicektomas.notemark.settings.presentation

import dev.havlicektomas.notemark.core.presentation.designsystem.layout.centeredMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkIcons
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkTopAppBar
import dev.havlicektomas.notemark.core.presentation.designsystem.components.OfflineBanner
import dev.havlicektomas.notemark.core.presentation.designsystem.components.UserAvatar
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toSyncStatusTime
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoot(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var message by remember { mutableStateOf<UiText?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onBack()
            SettingsEvent.NavigateToLanding -> onLoggedOut()
            is SettingsEvent.ShowMessage -> message = event.message
        }
    }

    message?.let { uiText ->
        val text = uiText.asString()
        LaunchedEffect(uiText) {
            snackbarHostState.showSnackbar(text)
            message = null
        }
    }

    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    onAction: (SettingsAction) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NoteMarkTopAppBar(
                title = "Settings",
                onNavigateBack = { onAction(SettingsAction.OnBackClick) },
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .centeredMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                if (!state.isOnline) {
                    OfflineBanner(
                        text = "You are offline. Connect to sync or log out.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                    )
                }

                ProfileCard(username = state.username, initials = state.initials)

                Spacer(Modifier.height(22.dp))

                SectionLabel("Sync")
                SyncNowRow(
                    isSyncing = state.isSyncing,
                    lastSyncedText = state.lastSyncedAt
                        ?.let { "Last synced ${it.toSyncStatusTime().lowercaseFirst()}" }
                        ?: "Never synced",
                    onClick = { onAction(SettingsAction.OnSyncNowClick) },
                )
                AutosyncRow(
                    intervalLabel = state.syncInterval.label(),
                    onClick = { onAction(SettingsAction.OnAutosyncClick) },
                )

                Spacer(Modifier.height(22.dp))

                SectionLabel("Data")
                OutlinedButton(
                    onClick = { onAction(SettingsAction.OnClearDataClick) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = NoteMarkIcons.Trash,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Clear local data", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onAction(SettingsAction.OnLogoutClick) },
                    enabled = !state.isLoggingOut,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (state.isLoggingOut) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = NoteMarkIcons.LogOut,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Log out", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (state.showLogoutDialog) {
        LogoutDialog(
            pendingChanges = state.pendingChanges,
            onSyncAndLogout = { onAction(SettingsAction.OnSyncAndLogout) },
            onLogoutWithoutSync = { onAction(SettingsAction.OnLogoutWithoutSync) },
            onDismiss = { onAction(SettingsAction.OnDismissLogout) },
        )
    }

    if (state.showClearDataDialog) {
        ClearDataDialog(
            onConfirm = { onAction(SettingsAction.OnConfirmClearData) },
            onDismiss = { onAction(SettingsAction.OnDismissClearData) },
        )
    }

    if (state.showAutosyncSheet) {
        AutosyncSheet(
            selected = state.syncInterval,
            onSelect = { onAction(SettingsAction.OnSyncIntervalSelected(it)) },
            onDismiss = { onAction(SettingsAction.OnDismissAutosync) },
        )
    }
}

private fun SyncInterval.label(): String = when (this) {
    SyncInterval.MANUAL_ONLY -> "Manual only"
    SyncInterval.MINUTES_15 -> "15 minutes"
    SyncInterval.MINUTES_30 -> "30 minutes"
    SyncInterval.HOUR_1 -> "1 hour"
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercaseChar() }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SyncNowRow(
    isSyncing: Boolean,
    lastSyncedText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSyncing, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Icon(
                imageVector = NoteMarkIcons.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            Text(
                text = if (isSyncing) "Syncing…" else "Sync now",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = lastSyncedText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutosyncRow(
    intervalLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = NoteMarkIcons.Clock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Automatic sync",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = intervalLabel,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutosyncSheet(
    selected: SyncInterval,
    onSelect: (SyncInterval) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Automatic sync",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            SyncInterval.entries.forEach { interval ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(interval) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RadioButton(
                        selected = interval == selected,
                        onClick = { onSelect(interval) },
                    )
                    Text(
                        text = interval.label(),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    username: String,
    initials: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            UserAvatar(initials = initials, size = 52.dp, fontSize = 19.sp)
            Text(
                text = username,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LogoutDialog(
    pendingChanges: Long,
    onSyncAndLogout: () -> Unit,
    onLogoutWithoutSync: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when {
        pendingChanges == 1L ->
            "You have 1 change that hasn't synced. Sync it before you log out."
        pendingChanges > 1 ->
            "You have $pendingChanges changes that haven't synced. " +
                "Sync them before you log out."
        else -> "You're all caught up. Log out?"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out?") },
        text = { Text(message) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                TextButton(onClick = onSyncAndLogout) {
                    Text("Sync and log out", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onLogoutWithoutSync) {
                    Text("Log out without syncing", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
    )
}

@Composable
private fun ClearDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear local data?") },
        text = {
            Text(
                "This removes synced notes stored on this device. " +
                    "Notes with unsynced changes are kept until they upload.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    NoteMarkTheme {
        SettingsScreen(
            state = SettingsState(username = "Sam Rivera", initials = "SR"),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
        )
    }
}
