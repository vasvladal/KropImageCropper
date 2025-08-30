package com.example.kropimagecropper

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

// 🎯 REAL KROP LIBRARY IMPORTS
import com.attafitamim.krop.core.crop.*
import com.attafitamim.krop.ui.ImageCropperDialog

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCropperScreen(navController: NavController) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // 🎯 USING REAL KROP LIBRARY
    val imageCropper = rememberImageCropper()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permission handling for image selection
    val readPermissionState = rememberPermissionState(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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

    // FIXED: Proper ImageBitmap to Android Bitmap conversion
    fun imageBitmapToBitmap(imageBitmap: ImageBitmap): Bitmap {
        return try {
            // Try to use the direct conversion method if available
            imageBitmap.asAndroidBitmap()
        } catch (e: Exception) {
            // Fallback method for older versions or if asAndroidBitmap() fails
            val bitmap = Bitmap.createBitmap(
                imageBitmap.width,
                imageBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            // Create a paint object for drawing
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            // Convert ImageBitmap to Android Bitmap properly
            val androidBitmap = Bitmap.createBitmap(
                imageBitmap.width,
                imageBitmap.height,
                Bitmap.Config.ARGB_8888
            )

            // Copy pixel data from ImageBitmap to Android Bitmap
            val pixels = IntArray(imageBitmap.width * imageBitmap.height)
            imageBitmap.readPixels(pixels)
            androidBitmap.setPixels(
                pixels,
                0,
                imageBitmap.width,
                0,
                0,
                imageBitmap.width,
                imageBitmap.height
            )

            // Draw the bitmap with proper scaling
            canvas.drawBitmap(androidBitmap, 0f, 0f, paint)

            bitmap
        }
    }

    // Function to save image to gallery
    fun saveImageToGallery() {
        scope.launch {
            try {
                croppedImage?.let { imageBitmap ->
                    withContext(Dispatchers.IO) {
                        // Convert ImageBitmap to Android Bitmap
                        val bitmap = imageBitmapToBitmap(imageBitmap)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // For Android 10 and above
                            saveImageToGalleryQ(bitmap, context)
                        } else {
                            // For older versions, check permission first
                            if (writePermissionState.status.isGranted) {
                                saveImageToGalleryLegacy(bitmap, context)
                            } else {
                                withContext(Dispatchers.Main) {
                                    writePermissionState.launchPermissionRequest()
                                }
                                return@withContext
                            }
                        }

                        withContext(Dispatchers.Main) {
                            showSaveSuccess = true
                            // Hide success message after 3 seconds
                            launch {
                                kotlinx.coroutines.delay(3000)
                                showSaveSuccess = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveError = "Failed to save image: ${e.message}"
                    // Hide error after 3 seconds
                    launch {
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
            // 🎯 USING KROP'S CROP FUNCTION
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Krop Image Cropper",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Using Version Catalogs + Krop v${getKropVersion()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 🎯 KROP CROPPER DIALOG - THE REAL LIBRARY COMPONENT
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
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
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
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image saved to gallery!",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Error message
        if (saveError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Error: ${saveError}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Content area
        if (croppedImage != null) {
            // Show cropped result
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✨ Cropped Result",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Image(
                        bitmap = croppedImage!!,
                        contentDescription = "Cropped image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                croppedImage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crop Another")
                        }

                        Button(
                            onClick = { saveImageToGallery() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Image")
                        }
                    }
                }
            }
        } else {
            // Show image picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Select an image to crop",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Experience powerful cropping with Krop library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (readPermissionState.status.isGranted) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                readPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Image")
                    }

                    if (!readPermissionState.status.isGranted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Storage permission needed to select images",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Footer with library info and navigation button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Built with Krop v${getKropVersion()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Navigation button to MyScansScreen
                TextButton(
                    onClick = { navController.navigate("scans") }
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "My Scans",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View My Scans")
                }
            }
        }
    }
}

// 🎯 KROP STYLING CONFIGURATION
@Composable
private fun createKropStyle() = cropperStyle(
    backgroundColor = Color.Black,
    rectColor = MaterialTheme.colorScheme.primary,
    rectStrokeWidth = 3.dp,
    overlay = Color.Black.copy(alpha = 0.6f),
    touchRad = 25.dp,
    autoZoom = true,
    aspects = listOf(
        AspectRatio(1, 1),      // Square
        AspectRatio(16, 9),     // Widescreen
        AspectRatio(9, 16),     // Portrait
        AspectRatio(4, 3),      // Classic
        AspectRatio(3, 2),      // Photo,
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
        color = Color.White.copy(alpha = 0.5f),
        width = 1.dp
    )
)

// Helper to get Krop version from BuildConfig (if needed)
private fun getKropVersion(): String = "0.2.0"

// Function to save image to gallery for Android Q and above
private suspend fun saveImageToGalleryQ(bitmap: Bitmap, context: Context): Uri? {
    return withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Krop_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Krop")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw Exception("Failed to compress bitmap")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                return@withContext uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        return@withContext null
    }
}

// Function to save image to gallery for legacy Android versions
private suspend fun saveImageToGalleryLegacy(bitmap: Bitmap, context: Context): Uri? {
    return withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Krop_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw Exception("Failed to compress bitmap")
                    }
                }
                return@withContext uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        return@withContext null
    }
}
