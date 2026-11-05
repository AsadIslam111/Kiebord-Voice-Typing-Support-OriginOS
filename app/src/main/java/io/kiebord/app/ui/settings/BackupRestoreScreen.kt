package io.kiebord.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.feature.ime.clipboard.BornoClipboardBackupHelper
import io.kiebord.feature.ime.clipboard.SandboxedClipboardManager
import io.kiebord.data.dictionary.PersonalDictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupRestoreScreen(
    clipboardManager: SandboxedClipboardManager,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val colors = LocalSettingsColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val personalDictionaryRepo = remember { PersonalDictionaryRepository.getInstance(context) }

    var pendingError by remember { mutableStateOf<String?>(null) }

    // File picker for importing Borno or Kiebord clipboard backups
    val importClipboardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val items = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BornoClipboardBackupHelper.parseBornoBackup(stream)
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    if (items.isEmpty()) {
                        pendingError = "No valid clipboard items found in backup file."
                    } else {
                        val res = clipboardManager.importParsedClips(items, replaceExisting = false)
                        val msg = if (res.importedCount > 0) {
                            "Imported ${res.importedCount} items" +
                                    if (res.updatedPinnedCount > 0) " (${res.updatedPinnedCount} pinned updated)" else ""
                        } else if (res.updatedPinnedCount > 0) {
                            "Updated ${res.updatedPinnedCount} pinned items"
                        } else {
                            "All ${res.totalFound} items already exist in clipboard"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pendingError = "Failed to read backup file: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    // File creator for exporting current clipboard
    val exportClipboardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val result = context.contentResolver.openOutputStream(uri)?.use { stream ->
                    clipboardManager.exportClipboardBackup(stream)
                }
                withContext(Dispatchers.Main) {
                    if (result != null && result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        Toast.makeText(context, "Exported $count clipboard items", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = result?.exceptionOrNull()?.localizedMessage ?: "Write error"
                        Toast.makeText(context, "Export failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportDictionaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val result = context.contentResolver.openOutputStream(uri)?.use { stream ->
                    personalDictionaryRepo.exportToJson(stream)
                }
                withContext(Dispatchers.Main) {
                    if (result != null && result.isSuccess) {
                        val count = result.getOrNull() ?: 0
                        Toast.makeText(context, "Exported $count dictionary entries", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = result?.exceptionOrNull()?.localizedMessage ?: "Write error"
                        Toast.makeText(context, "Export failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importDictionaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val result = context.contentResolver.openInputStream(uri)?.use { stream ->
                    personalDictionaryRepo.importFromJson(stream, replaceExisting = false)
                }
                withContext(Dispatchers.Main) {
                    if (result != null && result.isSuccess) {
                        val res = result.getOrNull()!!
                        val msg = if (res.importedCount > 0) {
                            "Imported ${res.importedCount} words" +
                                    if (res.skippedDuplicates > 0) " (${res.skippedDuplicates} duplicates skipped)" else ""
                        } else if (res.skippedDuplicates > 0) {
                            "All ${res.totalFound} words already exist in personal dictionary"
                        } else {
                            "Import completed (${res.totalFound} found)"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        val error = result?.exceptionOrNull()?.localizedMessage ?: "Invalid dictionary file"
                        pendingError = "Import failed: $error"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pendingError = "Failed to import dictionary: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    if (pendingError != null) {
        AlertDialog(
            onDismissRequest = { pendingError = null },
            containerColor = colors.dialogBackground,
            title = {
                Text(
                    text = "Backup Notice",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = pendingError!!,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { pendingError = null }) {
                    Text("OK", color = colors.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        SettingsCategoryHeader(title = "Borno Keyboard")

        SettingsGroup {
            SettingsNavigationItem(
                title = "Import Borno Clipboard Backup",
                subtitle = "Import from .bornoclip or .clip backup file",
                icon = Icons.Outlined.FileDownload,
                position = GroupPosition.ALONE,
                onClick = {
                    importClipboardLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCategoryHeader(title = "Clipboard")

        SettingsGroup {
            SettingsNavigationItem(
                title = "Export Clipboard Backup",
                subtitle = "Save clipboard history to .bornoclip file",
                icon = Icons.Outlined.FileUpload,
                position = GroupPosition.TOP,
                onClick = {
                    val defaultName = "kiebord_clipboard_backup_${System.currentTimeMillis()}.bornoclip"
                    exportClipboardLauncher.launch(defaultName)
                }
            )

            SettingsNavigationItem(
                title = "Restore Clipboard Backup",
                subtitle = "Import from .bornoclip or .clip backup file",
                icon = Icons.Outlined.Restore,
                position = GroupPosition.BOTTOM,
                onClick = {
                    importClipboardLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCategoryHeader(title = "Personal Dictionary")

        SettingsGroup {
            SettingsNavigationItem(
                title = "Export Personal Dictionary",
                subtitle = "Save custom words and shortcut mappings to .bornopdb file",
                icon = Icons.Outlined.FileUpload,
                position = GroupPosition.TOP,
                onClick = {
                    val defaultName = "kiebord_personal_dict_${System.currentTimeMillis()}.bornopdb"
                    exportDictionaryLauncher.launch(defaultName)
                }
            )

            SettingsNavigationItem(
                title = "Restore Personal Dictionary",
                subtitle = "Import words and shortcuts from backup file",
                icon = Icons.Outlined.Restore,
                position = GroupPosition.BOTTOM,
                onClick = {
                    importDictionaryLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json"))
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
