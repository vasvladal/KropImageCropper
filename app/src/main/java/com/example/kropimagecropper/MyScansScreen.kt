// File: MyScansScreen.kt
package com.example.kropimagecropper

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.ui.res.stringResource
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

// Save PDF to custom app folder
suspend fun createPdfAndSave(context: Context, files: List<File>) {
    if (files.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.no_files_selected), Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val pdfDocument = PdfDocument()
        val sortedFiles = files.sortedBy { it.name }

        for ((index, file) in sortedFiles.withIndex()) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate scaling to fit PDF page
            val scale = 595f / options.outWidth.coerceAtLeast(1)
            val scaledWidth = (options.outWidth * scale).toInt()
            val scaledHeight = (options.outHeight * scale).toInt()

            val realOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(options, scaledWidth, scaledHeight)
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, realOptions) ?: continue

            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)

                // Calculate scaling to fit the page
                val scaleX = 595f / bitmap.width
                val scaleY = 842f / bitmap.height
                val scale = scaleX.coerceAtMost(scaleY)

                val matrix = Matrix().apply {
                    postScale(scale, scale)
                }

                page.canvas.drawBitmap(bitmap, matrix, null)
                pdfDocument.finishPage(page)
            } catch (e: Exception) {
                android.util.Log.e("PDF_CREATION", "Error creating page ${index + 1}: ${e.message}")
                continue
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

        // Check if we have any pages
        if (pdfDocument.pages.size == 0) {
            Toast.makeText(context, context.getString(R.string.no_valid_pages), Toast.LENGTH_SHORT).show()
            pdfDocument.close()
            return
        }

        // Create custom directory with app name
        val appName = context.getString(R.string.app_name) // Make sure you have this string resource
        val customDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            appName
        ).apply {
            if (!exists()) mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val pdfFile = File(customDir, "Scans_${timestamp}.pdf")

        try {
            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
            }
            // Verify file was created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                val successMessage = context.getString(R.string.pdf_saved, pdfFile.name)
                Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()

                // Add logging
                android.util.Log.i("PDF_SAVE", "PDF successfully saved: ${pdfFile.absolutePath}")
                android.util.Log.i("PDF_SAVE", "File size: ${pdfFile.length()} bytes")

                // Optional: Scan the file to make it visible in gallery/file managers
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(pdfFile)
                context.sendBroadcast(mediaScanIntent)
            } else {
                val errorMessage = context.getString(R.string.failed_to_save_pdf)
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

                // Add logging for the failure case
                android.util.Log.e("PDF_SAVE", "PDF file was not created or is empty")
                android.util.Log.e("PDF_SAVE", "File exists: ${pdfFile.exists()}")
                android.util.Log.e("PDF_SAVE", "File size: ${pdfFile.length()} bytes")
            }
        } catch (e: Exception) {
            android.util.Log.e("PDF_SAVE", "Error saving PDF: ${e.message}")
            android.util.Log.e("PDF_SAVE", "Error stack trace: ${e.stackTraceToString()}")
            Toast.makeText(context, context.getString(R.string.failed_to_save_pdf), Toast.LENGTH_LONG).show()
        }

        pdfDocument.close()

    } catch (e: Exception) {
        android.util.Log.e("PDF_CREATION", "PDF creation failed: ${e.message}")
        Toast.makeText(context, context.getString(R.string.pdf_creation_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
    }
}

// Helper function to calculate sample size
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
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
                Toast.makeText(context, context.getString(R.string.error_creating_pdf), Toast.LENGTH_SHORT).show()
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

        shareLauncher.launch(Intent.createChooser(intent, context.getString(R.string.share_pdf)))

    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.failed_to_share_pdf, e.message ?: ""), Toast.LENGTH_LONG).show()
    }
}

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
        loadScans(scanDir) { loadedScans: List<File> ->
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
                            if (selectedScans.isNotEmpty()) stringResource(R.string.selected_count, selectedScans.size)
                            else stringResource(R.string.select_scans)
                        } else stringResource(R.string.my_scans),
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
                            contentDescription = if (isSelectMode) stringResource(R.string.cancel) else stringResource(R.string.back)
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
                                        contentDescription = stringResource(R.string.share),
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
                                        contentDescription = stringResource(R.string.select),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // More menu
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.menu),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.select_multiple)) },
                                        leadingIcon = { Icon(Icons.Default.Checklist, null) },
                                        onClick = { showMenu = false; isSelectMode = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_all)) },
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_scan))
                }
            }
        }
    ) { paddingValues ->

        // DELETE DIALOG
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_scans)) },
                text = {
                    Text(
                        stringResource(R.string.delete_confirmation, selectedScans.size)
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
                            // FIX: Use context.getString() instead of stringResource()
                            Toast.makeText(context, context.getString(R.string.scans_deleted), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // PDF CREATION DIALOG (Save to app)
        if (showPdfCreationDialog) {
            AlertDialog(
                onDismissRequest = { showPdfCreationDialog = false },
                title = { Text(stringResource(R.string.create_pdf)) },
                text = { Text(stringResource(R.string.create_pdf_confirmation, selectedScans.size)) },
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
                        Text(stringResource(R.string.create_pdf))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfCreationDialog = false }) {
                        Text(stringResource(R.string.cancel))
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
                    text = stringResource(R.string.no_scans_yet),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_scans_description),
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
                    Text(stringResource(R.string.start_scanning))
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
                        onSelectionChange = { selected: Boolean ->
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
fun SelectionBottomBar(
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
                    text = if (selectedCount == totalCount) stringResource(R.string.select_none) else stringResource(R.string.select_all),
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
                        contentDescription = stringResource(R.string.create_pdf),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf))
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
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
fun ScanItem(
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
                    contentDescription = stringResource(R.string.scan_preview),
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
                        text = stringResource(R.string.file_size_kb, (file.length() / 1024).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}