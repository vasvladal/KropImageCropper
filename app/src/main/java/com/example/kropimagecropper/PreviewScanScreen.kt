package com.example.kropimagecropper

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScanScreen(
    navController: NavController,
    filePath: String
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(filePath)
    val file = remember { File(decodedPath) }
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Get string resources
    val scanNotFound = stringResource(R.string.scan_not_found)
    val scanDeleted = stringResource(R.string.scan_deleted)
    val deleteFailed = stringResource(R.string.delete_failed)

    if (!file.exists()) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, scanNotFound, Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        return
    }

    val sharePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* Handle result if needed */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.preview_scan), style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Delete button
                    IconButton(onClick = {
                        showDeleteDialog = true
                    }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }

                    // Share as PDF (single)
                    IconButton(onClick = {
                        createAndSharePdf(context, listOf(file), sharePdfLauncher)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, null)
                    }
                }
            )
        }
    ) { padding ->

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_scan)) },
                text = { Text(stringResource(R.string.delete_scan_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (file.delete()) {
                                Toast.makeText(context, scanDeleted, Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, deleteFailed, Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
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
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.file_info), style = MaterialTheme.typography.headlineSmall)

                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow(stringResource(R.string.file_name), file.name)
                    InfoRow(stringResource(R.string.file_size), "${(file.length() / 1024).toInt()} KB")
                    InfoRow(stringResource(R.string.file_modified), SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified())))
                    InfoRow(stringResource(R.string.file_location), file.parentFile?.name ?: stringResource(R.string.unknown))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}