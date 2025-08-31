package com.example.kropimagecropper

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AppNavigationMenu(
    currentRoute: String,
    navController: NavController,
    scanCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Scanner Tab
            NavigationTab(
                icon = Icons.Default.DocumentScanner,
                label = stringResource(R.string.scanner_tab),
                isActive = currentRoute == "cropper",
                onClick = {
                    if (currentRoute != "cropper") {
                        navController.navigate("cropper") {
                            popUpTo("cropper") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // My Scans Tab
            NavigationTab(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.my_scans_tab),
                badge = if (scanCount > 0) scanCount.toString() else null,
                isActive = currentRoute == "scans",
                onClick = {
                    if (currentRoute != "scans") {
                        navController.navigate("scans")
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavigationTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    val backgroundColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else Color.Transparent

    val contentColor = if (isActive)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )

                // Badge for scan count
                badge?.let { badgeText ->
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun QuickActionMenu(
    onScanDocument: () -> Unit,
    onViewScans: () -> Unit,
    onCreatePdf: () -> Unit,
    scanCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scan Document
                QuickActionButton(
                    icon = Icons.Default.DocumentScanner,
                    label = stringResource(R.string.new_scan),
                    onClick = onScanDocument,
                    modifier = Modifier.weight(1f),
                    isPrimary = true
                )

                // View Scans
                QuickActionButton(
                    icon = Icons.Default.Folder,
                    label = stringResource(R.string.view_scans),
                    badge = if (scanCount > 0) scanCount.toString() else null,
                    onClick = onViewScans,
                    modifier = Modifier.weight(1f),
                    enabled = scanCount > 0
                )

                // Create PDF
                QuickActionButton(
                    icon = Icons.Default.PictureAsPdf,
                    label = stringResource(R.string.create_pdf),
                    onClick = onCreatePdf,
                    modifier = Modifier.weight(1f),
                    enabled = scanCount > 0
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true
) {
    val buttonColors = if (isPrimary) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(72.dp),
            colors = buttonColors,
            enabled = enabled
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                    badge?.let {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(72.dp),
            colors = buttonColors,
            enabled = enabled
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                    badge?.let {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun AppStatsCard(
    scanCount: Int,
    totalSize: String,
    lastScanDate: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.Description,
                value = scanCount.toString(),
                label = stringResource(R.string.scans)
            )

            StatItem(
                icon = Icons.Default.Storage,
                value = totalSize,
                label = stringResource(R.string.storage)
            )

            StatItem(
                icon = Icons.Default.Schedule,
                value = lastScanDate ?: stringResource(R.string.never),
                label = stringResource(R.string.last_scan)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}