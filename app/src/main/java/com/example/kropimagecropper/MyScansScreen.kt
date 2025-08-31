// File: MyScansScreen.kt
package com.example.kropimagecropper

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScansScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launchers
    val sharePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    // State
    val scanDir = getScansDirectory(context)
    val scans = remember { mutableStateListOf<File>() }
    val selectedScans = remember { mutableStateListOf<File>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPdfCreationDialog by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }

    // Load scans
    LaunchedEffect(Unit) {
        loadScans(scanDir) { loadedScans ->
            scans.clear()
            scans.addAll(loadedScans)
        }
    }

    // Clear selection when exiting select mode
    LaunchedEffect(isSelectMode) {
        if (!isSelectMode) {
            selectedScans.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectMode) {
                            if (selectedScans.isNotEmpty()) "${selectedScans.size} selected" else "Select Scans"
                        } else "My Scans",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectMode) {
                            isSelectMode = false
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSelectMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSelectMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (scans.isNotEmpty()) {
                        if (isSelectMode) {
                            // Share button only in top bar during selection
                            if (selectedScans.isNotEmpty()) {
                                IconButton(onClick = {
                                    createAndSharePdf(context, selectedScans.toList(), sharePdfLauncher)
                                }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Row {
                                // Quick select button
                                IconButton(onClick = {
                                    isSelectMode = true
                                }) {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = "Select",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More menu
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Select Multiple") },
                                        leadingIcon = { Icon(Icons.Default.Checklist, null) },
                                        onClick = { showMenu = false; isSelectMode = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete All") },
                                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                        onClick = {
                                            showMenu = false
                                            selectedScans.clear()
                                            selectedScans.addAll(scans)
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectMode) {
                SelectionBottomBar(
                    selectedCount = selectedScans.size,
                    totalCount = scans.size,
                    onSelectAll = {
                        if (selectedScans.size == scans.size) {
                            selectedScans.clear()
                        } else {
                            selectedScans.clear()
                            selectedScans.addAll(scans)
                        }
                    },
                    onDelete = { showDeleteDialog = true },
                    onCreatePdf = { showPdfCreationDialog = true }
                )
            }
        },
        floatingActionButton = {
            if (scans.isNotEmpty() && !isSelectMode) {
                FloatingActionButton(
                    onClick = { navController.popBackStack() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Scan")
                }
            }
        }
    ) { paddingValues ->

        // DELETE DIALOG
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Scans") },
                text = {
                    Text(
                        "Are you sure you want to delete ${selectedScans.size} scan${if (selectedScans.size != 1) "s" else ""}? This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedScans.forEach { file ->
                                if (file.delete()) {
                                    scans.remove(file)
                                }
                            }
                            selectedScans.clear()
                            showDeleteDialog = false
                            isSelectMode = false
                            Toast.makeText(context, "Scans deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // PDF CREATION DIALOG (Save to app)
        if (showPdfCreationDialog) {
            AlertDialog(
                onDismissRequest = { showPdfCreationDialog = false },
                title = { Text("Create PDF") },
                text = { Text("Create a PDF from ${selectedScans.size} selected scan(s)?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                createPdfAndSave(context, selectedScans.toList())
                                showPdfCreationDialog = false
                                isSelectMode = false
                                selectedScans.clear()
                            }
                        }
                    ) {
                        Text("Create PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfCreationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // EMPTY STATE
        if (scans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Scans Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your scanned documents will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DocumentScanner, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Scanning")
                }
            }
        } else {
            // SCAN GRID
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = 8.dp,
                    end = 8.dp,
                    bottom = if (isSelectMode) 80.dp else paddingValues.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(scans, key = { it.absolutePath }) { file ->
                    ScanItem(
                        file = file,
                        isSelected = file in selectedScans,
                        isSelectMode = isSelectMode,
                        onSelectionChange = { selected ->
                            if (selected) {
                                if (file !in selectedScans) selectedScans.add(file)
                            } else {
                                selectedScans.remove(file)
                            }
                        },
                        onLongPress = {
                            if (!isSelectMode) {
                                isSelectMode = true
                                if (file !in selectedScans) selectedScans.add(file)
                            }
                        },
                        onTap = {
                            if (isSelectMode) {
                                if (file in selectedScans) {
                                    selectedScans.remove(file)
                                } else {
                                    selectedScans.add(file)
                                }
                            } else {
                                // Open preview screen - encode the path properly
                                navController.navigate("preview/${Uri.encode(file.absolutePath)}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCreatePdf: () -> Unit
) {
    Surface(
        tonalElevation = 16.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select All/None button
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (selectedCount == totalCount) "Select None" else "Select All",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create PDF button
                FilledTonalButton(
                    onClick = onCreatePdf,
                    enabled = selectedCount > 0,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Create PDF",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }

                // Delete button
                FilledTonalButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun ScanItem(
    file: File,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .widthIn(min = 150.dp)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(file)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "Scan preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Selection overlay
                if (isSelectMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // File info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = file.nameWithoutExtension.take(15) + if (file.nameWithoutExtension.length > 15) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(file.lastModified())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${(file.length() / 1024).toInt()} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// App-private scan directory
fun getScansDirectory(context: Context): File {
    return File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Scans").apply {
        if (!exists()) mkdirs()
    }
}

// Load scans
suspend fun loadScans(dir: File, onResult: (List<File>) -> Unit) {
    if (dir.exists() && dir.isDirectory) {
        val files = dir.listFiles { f ->
            f.isFile && f.extension.lowercase() in listOf("jpg", "jpeg", "png")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        onResult(files)
    } else {
        onResult(emptyList())
    }
}

// Save PDF to app storage
suspend fun createPdfAndSave(context: Context, files: List<File>) {
    if (files.isEmpty()) {
        Toast.makeText(context, "No files selected", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val pdfDocument = PdfDocument()
        val sortedFiles = files.sortedBy { it.name }

        for ((index, file) in sortedFiles.withIndex()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            } catch (e: Exception) {
                pdfDocument.close()
                Toast.makeText(context, "Error on page ${index + 1}", Toast.LENGTH_SHORT).show()
                return
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

        val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PDFs").apply {
            if (!exists()) mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val pdfFile = File(pdfDir, "Scans_${timestamp}.pdf")

        try {
            FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
            Toast.makeText(context, "✅ PDF saved: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_LONG).show()
        }

        pdfDocument.close()

    } catch (e: Exception) {
        Toast.makeText(context, "PDF creation failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// Create and share PDF
fun createAndSharePdf(
    context: Context,
    files: List<File>,
    shareLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    if (files.isEmpty()) return

    try {
        val pdfDocument = PdfDocument()
        val sortedFiles = files.sortedBy { it.name }

        for ((index, file) in sortedFiles.withIndex()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            } catch (e: Exception) {
                pdfDocument.close()
                Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()
                return
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

        val cachePdf = File(context.cacheDir, "shared_scans.pdf")
        FileOutputStream(cachePdf).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cachePdf
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        shareLauncher.launch(Intent.createChooser(intent, "Share PDF"))

    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}