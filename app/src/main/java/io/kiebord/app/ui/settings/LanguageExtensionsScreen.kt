package io.kiebord.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.langpack.LanguagePackMetadata
import io.kiebord.data.langpack.LanguagePackRepository
import io.kiebord.data.langpack.PackCatalogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LanguageExtensionsScreen(
    pendingImportUri: Uri? = null,
    onClearPendingImportUri: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalSettingsColors.current
    val repository = remember(context) { LanguagePackRepository.getInstance(context) }

    var installedPacks by remember { mutableStateOf<List<LanguagePackMetadata>>(emptyList()) }
    var catalogEntries by remember { mutableStateOf<List<PackCatalogEntry>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var activeDownloadPackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var packToDelete by remember { mutableStateOf<LanguagePackMetadata?>(null) }
    var packForInfo by remember { mutableStateOf<LanguagePackMetadata?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        installedPacks = withContext(Dispatchers.IO) {
            repository.getInstalledPacks()
        }
    }

    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        scope.launch {
            val installResult = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        repository.installPack(stream).getOrThrow()
                    } ?: error("Failed to open file stream")
                }
            }
            installResult.onSuccess { meta ->
                Toast.makeText(context, "${meta.displayName} installed successfully", Toast.LENGTH_SHORT).show()
                refreshTrigger++
            }.onFailure { err ->
                Toast.makeText(context, "Install failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
            onClearPendingImportUri()
        }
    }

    LaunchedEffect(Unit) {
        isLoadingCatalog = true
        val result = repository.fetchCatalog()
        isLoadingCatalog = false
        result.onSuccess { catalogEntries = it }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val installResult = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            repository.installPack(stream).getOrThrow()
                        } ?: error("Failed to open file stream")
                    }
                }
                installResult.onSuccess { meta ->
                    Toast.makeText(context, "${meta.displayName} installed successfully", Toast.LENGTH_SHORT).show()
                    refreshTrigger++
                }.onFailure { err ->
                    Toast.makeText(context, "Install failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (packToDelete != null) {
        val target = packToDelete!!
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            containerColor = colors.dialogBackground,
            title = {
                Text(
                    text = "Uninstall ${target.displayName}?",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove the extension pack and its layouts.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = target
                        packToDelete = null
                        scope.launch {
                            val deleted = withContext(Dispatchers.IO) {
                                repository.uninstallPack(toDelete.id)
                            }
                            if (deleted) {
                                Toast.makeText(context, "${toDelete.displayName} removed", Toast.LENGTH_SHORT).show()
                                refreshTrigger++
                            }
                        }
                    }
                ) {
                    Text("Uninstall", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    if (packForInfo != null) {
        val target = packForInfo!!
        AlertDialog(
            onDismissRequest = { packForInfo = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = target.displayName,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InfoDialogSection(title = "Extension Details", colors = colors) {
                        InfoDialogRow(label = "ID", value = target.id, colors = colors)
                        InfoDialogRow(label = "Version", value = "v${target.version}", colors = colors)
                        InfoDialogRow(
                            label = "Language",
                            value = if (target.locale.isNotEmpty() && target.locale != target.language) {
                                "${target.language} (${target.locale})"
                            } else {
                                target.language
                            },
                            colors = colors
                        )
                        if (target.author.isNotBlank()) {
                            InfoDialogRow(label = "Author", value = target.author, colors = colors)
                        }
                        if (target.license.isNotBlank()) {
                            InfoDialogRow(label = "License", value = target.license, colors = colors)
                        }
                        val credit = target.credit
                        if (!credit.isNullOrBlank()) {
                            InfoDialogRow(label = "Credit", value = credit, colors = colors)
                        }
                    }

                    if (target.layouts.isNotEmpty()) {
                        InfoDialogSection(title = "Layouts (${target.layouts.size})", colors = colors) {
                            Text(
                                text = target.layouts.joinToString(", "),
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    val dictCredit = target.dictionaryCredit
                    val dictLicense = target.dictionaryLicense
                    val dictSource = target.dictionarySource
                    val dictFile = target.dictionaryFile
                    if (dictFile != null || !dictCredit.isNullOrBlank()) {
                        InfoDialogSection(title = "Dictionary & Wordlist", colors = colors) {
                            if (!dictCredit.isNullOrBlank()) {
                                InfoDialogRow(label = "Credit", value = dictCredit, colors = colors)
                            }
                            if (!dictLicense.isNullOrBlank()) {
                                InfoDialogRow(label = "License", value = dictLicense, colors = colors)
                            }
                            if (!dictSource.isNullOrBlank()) {
                                InfoDialogRow(label = "Source", value = dictSource, colors = colors)
                            }
                            if (!dictFile.isNullOrBlank()) {
                                InfoDialogRow(label = "File", value = dictFile, colors = colors)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { packForInfo = null }) {
                    Text("Close", color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SettingsCategoryHeader("Sideload Pack")

        SettingsGroup {
            SettingsNavigationItem(
                title = "Install from File",
                subtitle = "Select a .bornolang pack from device storage",
                icon = Icons.Outlined.FileOpen,
                position = GroupPosition.ALONE,
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategoryHeader("Installed Extensions")

        if (installedPacks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No extension packs installed yet",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            SettingsGroup {
                installedPacks.forEachIndexed { index, pack ->
                    val pos = when {
                        installedPacks.size == 1 -> GroupPosition.ALONE
                        index == 0 -> GroupPosition.TOP
                        index == installedPacks.size - 1 -> GroupPosition.BOTTOM
                        else -> GroupPosition.MIDDLE
                    }
                    val shape = getGroupItemShape(pos)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape),
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.iconBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                val details = buildList {
                                    if (pack.author.isNotEmpty()) add("By ${pack.author}")
                                    if (pack.layouts.isNotEmpty()) add("${pack.layouts.size} layout(s)")
                                    if (pack.dictionaryFile != null) add("Dictionary")
                                }.joinToString(" • ")

                                if (details.isNotEmpty()) {
                                    Text(
                                        text = details,
                                        fontSize = 13.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { packForInfo = pack },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Pack Info",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { packToDelete = pack },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Uninstall",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategoryHeader("Available from Catalog")

        if (isLoadingCatalog) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            }
        } else if (catalogEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to load catalog. Check your internet connection.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            SettingsGroup {
                catalogEntries.forEachIndexed { index, entry ->
                    val isInstalled = installedPacks.any { it.id == entry.id }
                    val isDownloading = activeDownloadPackId == entry.id
                    val pos = when {
                        catalogEntries.size == 1 -> GroupPosition.ALONE
                        index == 0 -> GroupPosition.TOP
                        index == catalogEntries.size - 1 -> GroupPosition.BOTTOM
                        else -> GroupPosition.MIDDLE
                    }
                    val shape = getGroupItemShape(pos)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape),
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.iconBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = if (isInstalled) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = entry.description.ifEmpty { "By ${entry.author}" },
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isInstalled) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Installed",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Installed",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.accent
                                    )
                                }
                            } else if (isDownloading) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    color = colors.accent,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Button(
                                    onClick = {
                                        activeDownloadPackId = entry.id
                                        downloadProgress = 0f
                                        scope.launch {
                                            val dlResult = repository.downloadAndInstall(entry) { p ->
                                                downloadProgress = p
                                            }
                                            activeDownloadPackId = null
                                            dlResult.onSuccess { meta ->
                                                Toast.makeText(context, "${meta.displayName} downloaded & installed", Toast.LENGTH_SHORT).show()
                                                refreshTrigger++
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.cardBackground
                                    ),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Get",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoDialogSection(
    title: String,
    colors: SettingsColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accent
        )
        content()
    }
}

@Composable
private fun InfoDialogRow(
    label: String,
    value: String,
    colors: SettingsColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}

