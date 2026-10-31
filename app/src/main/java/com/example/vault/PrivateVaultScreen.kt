package com.example.vault

import android.content.Context
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.filemanager.FileManagerHelper
import com.example.trash.TrashItem
import com.example.trash.TrashManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultScreen(
    onOpenMediaItem: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isSetup by remember { mutableStateOf(VaultManager.isVaultSetup(context)) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    // Vault contents
    var vaultFiles by remember { mutableStateOf<List<VaultFileItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var currentAlbum by remember { mutableStateOf("Main") }
    var albumList by remember { mutableStateOf<List<String>>(listOf("Main")) }

    // Private Trash view state
    var showPrivateTrash by remember { mutableStateOf(false) }
    var privateTrashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }

    // Dialog states
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentItemMenu by remember { mutableStateOf<VaultFileItem?>(null) }

    // Auto-lock when activity goes to background (ON_STOP)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                isAuthenticated = false
                enteredPin = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun reloadVault() {
        scope.launch {
            vaultFiles = VaultManager.listVaultFiles(context, currentAlbum)
            albumList = VaultManager.getAlbums(context)
            if (showPrivateTrash) {
                privateTrashItems = TrashManager.listTrash(context, isPrivate = true)
            }
        }
    }

    LaunchedEffect(isAuthenticated, currentAlbum, showPrivateTrash) {
        if (isAuthenticated) {
            reloadVault()
        }
    }

    // SAF Import Launcher
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var importedCount = 0
                withContext(Dispatchers.IO) {
                    for (uri in uris) {
                        try {
                            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}")
                            context.contentResolver.openInputStream(uri)?.use { inStream: java.io.InputStream ->
                                java.io.FileOutputStream(tempFile).use { outStream: java.io.FileOutputStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                            val res = VaultManager.importFile(
                                context = context,
                                sourceFile = tempFile,
                                pin = enteredPin,
                                album = currentAlbum,
                                deleteOriginal = true
                            )
                            if (res.isSuccess) importedCount++
                        } catch (_: Exception) {}
                    }
                }
                reloadVault()
                snackbarHostState.showSnackbar("Encrypted & imported $importedCount file(s) into vault")
            }
        }
    }

    BackHandler {
        if (showPrivateTrash) {
            showPrivateTrash = false
        } else {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAuthenticated) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isAuthenticated) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showPrivateTrash) "Private Trash" else "Private Vault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showPrivateTrash) showPrivateTrash = false else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAuthenticated && !showPrivateTrash) {
                        IconButton(onClick = { showChangePinDialog = true }) {
                            Icon(Icons.Default.Password, contentDescription = "Change PIN")
                        }
                        IconButton(onClick = {
                            showPrivateTrash = true
                            scope.launch {
                                privateTrashItems = TrashManager.listTrash(context, isPrivate = true)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Private Trash")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAuthenticated && !showPrivateTrash) {
                FloatingActionButton(
                    onClick = { importFilePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.testTag("import_vault_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import Files")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("private_vault_container")
        ) {
            if (!isAuthenticated) {
                // AUTHENTICATION / SETUP SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (!isSetup) "Set Up Private Vault" else "Unlock Private Vault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (!isSetup) "Create a 4 to 6 digit security PIN for encrypted storage" else "Enter your PIN to decrypt and access private files",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it.take(6) },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f).testTag("vault_pin_input")
                    )

                    if (!isSetup) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it.take(6) },
                            label = { Text("Confirm PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                    }

                    authError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isSetup) {
                                if (enteredPin.length < 4) {
                                    authError = "PIN must be at least 4 digits"
                                } else if (enteredPin != confirmPin) {
                                    authError = "PINs do not match"
                                } else {
                                    VaultManager.setupVaultPin(context, enteredPin)
                                    isSetup = true
                                    isAuthenticated = true
                                    authError = null
                                }
                            } else {
                                if (VaultManager.verifyPin(context, enteredPin)) {
                                    isAuthenticated = true
                                    authError = null
                                } else {
                                    authError = "Incorrect PIN"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
                    ) {
                        Text(if (!isSetup) "Create Vault" else "Unlock Vault")
                    }
                }
            } else if (showPrivateTrash) {
                // PRIVATE TRASH SCREEN (Independent from normal trash)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Deleted Vault Files (${privateTrashItems.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (privateTrashItems.isNotEmpty()) {
                            TextButton(onClick = {
                                scope.launch {
                                    TrashManager.emptyTrash(context, isPrivate = true)
                                    privateTrashItems = emptyList()
                                    snackbarHostState.showSnackbar("Emptied Private Trash")
                                }
                            }) {
                                Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (privateTrashItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Private Trash is empty", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(privateTrashItems) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.fileName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(item.formattedSize, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            TrashManager.restoreItem(context, item, isPrivate = true)
                                            reloadVault()
                                            snackbarHostState.showSnackbar("Restored to vault")
                                        }
                                    }) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            TrashManager.permanentlyDelete(context, item, isPrivate = true)
                                            reloadVault()
                                            snackbarHostState.showSnackbar("Permanently deleted")
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                // AUTHENTICATED VAULT FILES BROWSER
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Album selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Album: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(albumList) { album ->
                                FilterChip(
                                    selected = currentAlbum == album,
                                    onClick = { currentAlbum = album },
                                    label = { Text(album) }
                                )
                            }
                        }
                        IconButton(onClick = { showNewAlbumDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Album")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (vaultFiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("This vault album is empty", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("Tap the + button below to encrypt and import files", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(vaultFiles) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Decrypt to temp cache and open internally
                                            scope.launch {
                                                val decRes = VaultManager.decryptFileToCache(context, item, enteredPin)
                                                if (decRes.isSuccess) {
                                                    val plain = decRes.getOrNull()!!
                                                    val mediaItem = FileManagerHelper.fileToMediaItem(plain)
                                                    onOpenMediaItem(mediaItem)
                                                } else {
                                                    snackbarHostState.showSnackbar("Decryption failed")
                                                }
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (item.mediaType) {
                                        MediaType.VIDEO -> Icons.Default.Movie
                                        MediaType.AUDIO -> Icons.Default.MusicNote
                                        MediaType.IMAGE -> Icons.Default.Photo
                                        else -> Icons.Default.InsertDriveFile
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.formattedSize} • AES-GCM Encrypted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        // Export to Downloads
                                        scope.launch {
                                            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            val res = VaultManager.exportFile(context, item, enteredPin, destDir)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Exported to Downloads/${res.getOrNull()?.name}")
                                            } else {
                                                snackbarHostState.showSnackbar("Export failed")
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                                    }
                                    IconButton(onClick = {
                                        // Move to Private Trash
                                        scope.launch {
                                            TrashManager.moveToTrash(context, item.encryptedFile, isPrivate = true)
                                            VaultManager.deleteVaultItem(context, item)
                                            reloadVault()
                                            snackbarHostState.showSnackbar("Moved to Private Trash")
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete to Trash", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // New Album Dialog
    if (showNewAlbumDialog) {
        var newAlbumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewAlbumDialog = false },
            title = { Text("New Vault Album", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clean = newAlbumName.trim()
                    if (clean.isNotEmpty()) {
                        if (!albumList.contains(clean)) {
                            albumList = albumList + clean
                        }
                        currentAlbum = clean
                    }
                    showNewAlbumDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAlbumDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var changeError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Vault PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { currentPinInput = it.take(6) },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it.take(6) },
                        label = { Text("New PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    changeError?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (VaultManager.changePin(context, currentPinInput, newPinInput)) {
                        enteredPin = newPinInput
                        showChangePinDialog = false
                        scope.launch { snackbarHostState.showSnackbar("PIN updated successfully") }
                    } else {
                        changeError = "Incorrect current PIN or invalid new PIN"
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }
}
