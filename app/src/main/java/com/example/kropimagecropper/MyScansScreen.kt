package com.example.kropimagecropper

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class) // Add this annotation
@Composable
fun MyScansScreen(navController: NavController) {
    val context = LocalContext.current
    val scanDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Scans")
    val scans = remember { mutableStateListOf<File>() }
    val selectedScans = remember { mutableStateListOf<File>() }

    // Load scans when entering screen
    LaunchedEffect(Unit) {
        if (scanDir.exists()) {
            scans.clear()
            scans.addAll(scanDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Scans") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            createPdfFromImages(context, selectedScans)
                        },
                        enabled = selectedScans.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    }
                    IconButton(
                        onClick = {
                            selectedScans.forEach {
                                it.delete()
                                scans.remove(it)
                            }
                            selectedScans.clear()
                            Toast.makeText(context, "Deleted selected scans", Toast.LENGTH_SHORT).show()
                        },
                        enabled = selectedScans.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(scans) { file ->
                val isSelected = file in selectedScans

                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            if (isSelected) {
                                selectedScans.remove(file)
                            } else {
                                selectedScans.add(file)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(file),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedScans.add(file) else selectedScans.remove(file)
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(file.name.take(12), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// 🔨 PDF Creation (selected scans only)
fun createPdfFromImages(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    val pdfDocument = PdfDocument()
    files.forEachIndexed { index, file ->
        val bitmap = BitmapFactory.decodeFile(file.path)
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)
    }

    val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MyScans.pdf")
    FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
    pdfDocument.close()

    Toast.makeText(context, "PDF saved: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
}