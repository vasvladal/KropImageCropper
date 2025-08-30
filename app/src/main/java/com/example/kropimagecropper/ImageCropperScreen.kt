package com.example.kropimagecropper

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🎯 REAL KROP LIBRARY IMPORTS
import com.attafitamim.krop.core.crop.*
import com.attafitamim.krop.ui.ImageCropperDialog

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream



@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCropperScreen(navController: NavController) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveLocation by remember { mutableStateOf<String?>(null) }

    // 🎯 USING REAL KROP LIBRARY
    val imageCropper = rememberImageCropper()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Permission handling for image selection
    val readPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // Permission handling for saving images
    val writePermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    )

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    // ✅ Convert ImageBitmap to Android Bitmap (simple and safe)
    fun imageBitmapToBitmap(imageBitmap: ImageBitmap): Bitmap {
        return imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    }

    // ✅ Save to both gallery and app scans folder
    fun saveImageToScans() {
        scope.launch {
            try {
                croppedImage?.let { imageBitmap ->
                    withContext(Dispatchers.IO) {
                        val bitmap = imageBitmapToBitmap(imageBitmap)

                        // Create scans directory
                        val scansDir = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "Scans"
                        )
                        if (!scansDir.exists()) {
                            scansDir.mkdirs()
                        }

                        // Save to app scans folder
                        val fileName = "Scan_${System.currentTimeMillis()}.jpg"
                        val scanFile = File(scansDir, fileName)

                        FileOutputStream(scanFile).use { outputStream ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                                throw Exception("Failed to save scan")
                            }
                        }

                        // Save to gallery
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            saveImageToGalleryQ(bitmap, context)
                        } else {
                            if (writePermissionState.status.isGranted) {
                                saveImageToGalleryLegacy(bitmap, context)
                            } else {
                                throw Exception("Storage permission required")
                            }
                        }

                        withContext(Dispatchers.Main) {
                            saveLocation = scanFile.absolutePath
                            showSaveSuccess = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showSaveSuccess = false
                                saveLocation = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveError = "Failed to save scan: ${e.message}"
                    scope.launch {
                        kotlinx.coroutines.delay(3000)
                        saveError = null
                    }
                }
            }
        }
    }

    // Start cropping when image is selected
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            when (val result = imageCropper.crop(uri, context)) {
                is CropResult.Success -> {
                    croppedImage = result.bitmap
                    selectedImageUri = null
                }
                is CropResult.Cancelled -> {
                    selectedImageUri = null
                }
                is CropError -> {
                    selectedImageUri = null
                    saveError = "Error cropping image"
                }
            }
        }
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.largePadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.mediumPadding))

            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.largePadding),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.mediumPadding)
            ) {
                Column(Modifier.padding(Dimens.largePadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.iconLarge)
                        )
                        Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Document Scanner",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Crop and scan your documents",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        OutlinedButton(onClick = { navController.navigate("scans") }) {
                            Icon(Icons.Default.Folder, null, Modifier.size(Dimens.iconSmall))
                            Spacer(modifier = Modifier.width(Dimens.smallPadding))
                            Text("My Scans")
                        }
                    }
                }
            }

            // 🎯 KROP CROPPER DIALOG
            imageCropper.cropState?.let { cropState ->
                ImageCropperDialog(
                    state = cropState,
                    style = createKropStyle()
                )
            }

            // Loading indicator
            imageCropper.loadingStatus?.let { status ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.largePadding),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.extraLargePadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Dimens.largePadding))
                        Text(
                            text = when (status) {
                                CropperLoading.PreparingImage -> "Loading image..."
                                CropperLoading.SavingResult -> "Processing crop..."
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Success message
            if (showSaveSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.largePadding),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(Dimens.largePadding)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                            Text(
                                text = "Scan saved successfully!",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        saveLocation?.let { location ->
                            Spacer(modifier = Modifier.height(Dimens.smallPadding))
                            Text(
                                text = "Saved to: ${File(location).name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Error message
            saveError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.largePadding),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Row(Modifier.padding(Dimens.largePadding), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Main content area
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (croppedImage != null) {
                    // Show cropped image
                    Card(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        elevation = CardDefaults.cardElevation(Dimens.mediumPadding)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(Dimens.largePadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📄 Scanned Document",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = Dimens.largePadding)
                            )

                            Image(
                                bitmap = croppedImage!!,
                                contentDescription = "Scanned document",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(Dimens.cornerRadiusMedium))
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(Dimens.cornerRadiusMedium)
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(Dimens.largePadding))

                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.mediumPadding)) {
                                Button(
                                    onClick = { saveImageToScans() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimens.buttonMedium),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Save, null, Modifier.size(Dimens.iconSmall))
                                    Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                                    Text("Save as Scan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.smallPadding)) {
                                    OutlinedButton(onClick = { croppedImage = null }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Refresh, null, Modifier.size(Dimens.iconSmall))
                                        Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                                        Text("Scan Another")
                                    }
                                    OutlinedButton(onClick = { navController.navigate("scans") }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Folder, null, Modifier.size(Dimens.iconSmall))
                                        Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                                        Text("View Scans")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Image selection UI
                    Card(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        elevation = CardDefaults.cardElevation(Dimens.mediumPadding)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(Dimens.extraLargePadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                modifier = Modifier.size(Dimens.cardMedium),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(Dimens.cornerRadiusLarge)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.DocumentScanner,
                                        null,
                                        Modifier.size(Dimens.iconLarge),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start Scanning",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(Dimens.mediumPadding))
                                Text(
                                    text = "Select a document or photo to crop and save as a scan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        if (readPermissionState.status.isGranted) {
                                            imagePickerLauncher.launch("image/*")
                                        } else {
                                            readPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimens.buttonMedium),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, Modifier.size(Dimens.iconSmall))
                                    Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                                    Text("Select Document", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }

                                if (!readPermissionState.status.isGranted) {
                                    Spacer(modifier = Modifier.height(Dimens.largePadding))
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                                        Text(
                                            text = "📱 Storage permission is required to select images",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(Dimens.mediumPadding),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.mediumPadding),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(Dimens.mediumPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.iconSmall))
                        Spacer(modifier = Modifier.width(Dimens.mediumPadding))
                        Text(
                            text = "Powered by Krop v${getKropVersion()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.smallPadding))
                    Text(
                        text = "📁 Tap 'My Scans' to manage your documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ✅ Move all helpers OUTSIDE the @Composable

@Composable
fun createKropStyle() = cropperStyle(
    backgroundColor = Color.Black,
    rectColor = MaterialTheme.colorScheme.primary,
    rectStrokeWidth = 4.dp,
    overlay = Color.Black.copy(alpha = 0.7f),
    touchRad = 28.dp,
    autoZoom = true,
    aspects = listOf(
        AspectRatio(1, 1),
        AspectRatio(4, 3),
        AspectRatio(3, 4),
        AspectRatio(16, 9),
        AspectRatio(9, 16),
        AspectRatio(3, 2),
    ),
    shapes = listOf(
        RectCropShape,
        CircleCropShape,
        RoundRectCropShape(15),
        RoundRectCropShape(25),
        TriangleCropShape
    ),
    guidelines = CropperStyleGuidelines(
        count = 2,
        color = Color.White.copy(alpha = 0.6f),
        width = 1.5.dp
    )
)

fun getKropVersion(): String = "0.2.0"

suspend fun saveImageToGalleryQ(bitmap: Bitmap, context: Context): Uri? = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocumentScans")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

    try {
        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw Exception("Failed to compress bitmap")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}

suspend fun saveImageToGalleryLegacy(bitmap: Bitmap, context: Context): Uri? = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

    try {
        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw Exception("Failed to compress bitmap")
            }
        }
        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
}