package com.example.kropimagecropper

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    // State management
    val scanDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Scans")
    val scans = remember { mutableStateListOf<File>() }
    val selectedScans = remember { mutableStateListOf<File>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPdfCreationDialog by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }

    // Load scans when entering screen
    LaunchedEffect(Unit) {
        if (scanDir.exists()) {
            scans.clear()
            scans.addAll(
                scanDir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
                    ?.sortedByDescending { it.lastModified() } // Newest first
                    ?: emptyList()
            )
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
                    Column {
                        Text(
                            text = if (isSelectMode) "Select Scans" else "My Scans",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (scans.isNotEmpty()) {
                            Text(
                                text = "${scans.size} scan${if (scans.size != 1) "s" else ""}" +
                                        if (selectedScans.isNotEmpty()) " • ${selectedScans.size} selected" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
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
                            if (isSelectMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSelectMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (scans.isNotEmpty()) {
                        // Select/Deselect all button
                        if (isSelectMode) {
                            TextButton(
                                onClick = {
                                    if (selectedScans.size == scans.size) {
                                        selectedScans.clear()
                                    } else {
                                        selectedScans.clear()
                                        selectedScans.addAll(scans)
                                    }
                                }
                            ) {
                                Text(
                                    if (selectedScans.size == scans.size) "Deselect All" else "Select All"
                                )
                            }
                        }

                        // Create PDF button
                        IconButton(
                            onClick = {
                                if (selectedScans.isNotEmpty()) {
                                    showPdfCreationDialog = true
                                } else if (!isSelectMode) {
                                    isSelectMode = true
                                } else {
                                    Toast.makeText(context, "Select scans first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = if (isSelectMode) selectedScans.isNotEmpty() else true
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Create PDF",
                                tint = if (isSelectMode && selectedScans.isEmpty())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Delete button (only in select mode)
                        if (isSelectMode) {
                            IconButton(
                                onClick = {
                                    if (selectedScans.isNotEmpty()) {
                                        showDeleteDialog = true
                                    } else {
                                        Toast.makeText(context, "Select scans to delete", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = selectedScans.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = if (selectedScans.isEmpty())
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Menu button for other actions
                        if (!isSelectMode) {
                            var showMenu by remember { mutableStateOf(false) }

                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select Multiple") },
                                    onClick = {
                                        showMenu = false
                                        isSelectMode = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Checklist, contentDescription = null)
                                    },
                                    enabled = scans.isNotEmpty()
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete All") },
                                    onClick = {
                                        showMenu = false
                                        selectedScans.clear()
                                        selectedScans.addAll(scans)
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                                    },
                                    enabled = scans.isNotEmpty()
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->

        // DELETE CONFIRMATION DIALOG
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Scans") },
                text = {
                    Text("Are you sure you want to delete ${selectedScans.size} scan${if (selectedScans.size != 1) "s" else ""}? This action cannot be undone.")
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

        // PDF CREATION DIALOG
        if (showPdfCreationDialog) {
            AlertDialog(
                onDismissRequest = { showPdfCreationDialog = false },
                title = { Text("Create PDF") },
                text = {
                    Text("Create a PDF from ${selectedScans.size} selected scan${if (selectedScans.size != 1) "s" else ""}?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                createPdfFromImages(context, selectedScans.toList())
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

        // MAIN CONTENT
        if (scans.isEmpty()) {
            // Empty state with scrolling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.largePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.size(Dimens.cardMedium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(Dimens.cornerRadiusLarge)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconLarge),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.extraLargePadding))

                Text(
                    text = "No Scans Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.mediumPadding))

                Text(
                    text = "Start scanning documents to see them here.\nAll your scans will be organized and ready for PDF export.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.extraLargePadding))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                    Text("Start Scanning")
                }
            }
        } else {
            // Scans grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Dimens.gridItemMinWidth),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(Dimens.mediumPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.mediumPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.mediumPadding)
            ) {
                items(scans, key = { it.absolutePath }) { file ->
                    Box(modifier = Modifier.height(Dimens.gridItemHeight)) {
                        ScanItem(
                            file = file,
                            isSelected = file in selectedScans,
                            isSelectMode = isSelectMode,
                            onSelectionChange = { selected ->
                                if (selected) {
                                    selectedScans.add(file)
                                } else {
                                    selectedScans.remove(file)
                                }
                            },
                            onLongPress = {
                                if (!isSelectMode) {
                                    isSelectMode = true
                                    selectedScans.add(file)
                                }
                            },
                            onTap = {
                                if (isSelectMode) {
                                    if (file in selectedScans) {
                                        selectedScans.remove(file)
                                    } else {
                                        selectedScans.add(file)
                                    }
                                }
                                // Could add preview functionality here later
                            }
                        )
                    }
                }
            }
        }

        // FLOATING ACTION BUTTON for quick actions
        if (scans.isNotEmpty() && !isSelectMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FloatingActionButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimens.largePadding),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add New Scan",
                        tint = Color.White
                    )
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
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onTap() }
            .padding(Dimens.smallPadding),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) Dimens.mediumPadding.value.toInt().dp else Dimens.smallPadding.value.toInt().dp
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = "Scan preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = Dimens.cornerRadiusMedium, topEnd = Dimens.cornerRadiusMedium)),
                    contentScale = ContentScale.Crop
                )

                // Selection indicator
                if (isSelectMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Dimens.mediumPadding)
                            .size(Dimens.iconMedium)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                RoundedCornerShape(Dimens.cornerRadiusMedium)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(Dimens.iconSmall)
                            )
                        }
                    }
                }
            }

            // File info
            Column(
                modifier = Modifier.padding(Dimens.mediumPadding)
            ) {
                Text(
                    text = file.nameWithoutExtension.take(15) + if (file.nameWithoutExtension.length > 15) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(Dimens.smallPadding))

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

// ENHANCED: PDF Creation with better error handling
suspend fun createPdfFromImages(context: Context, files: List<File>) {
    if (files.isEmpty()) {
        Toast.makeText(context, "No files selected", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val pdfDocument = PdfDocument()
        val sortedFiles = files.sortedBy { it.name } // Sort by name for consistent order

        sortedFiles.forEachIndexed { index, file ->
            try {
                val bitmap = BitmapFactory.decodeFile(file.path)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width,
                        bitmap.height,
                        index + 1
                    ).create()

                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle() // Free memory
                } else {
                    throw Exception("Could not decode image: ${file.name}")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error processing ${file.name}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Save PDF
        val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PDFs")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val pdfFile = File(pdfDir, "Scans_${timestamp}.pdf")

        FileOutputStream(pdfFile).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        Toast.makeText(
            context,
            "PDF created: ${pdfFile.name}\nLocation: ${pdfFile.absolutePath}",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}