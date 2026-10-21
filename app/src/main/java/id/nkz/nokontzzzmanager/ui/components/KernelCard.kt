package id.nkz.nokontzzzmanager.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import id.nkz.nokontzzzmanager.data.model.KernelDetailInfo
import id.nkz.nokontzzzmanager.data.model.KernelInfo
import id.nkz.nokontzzzmanager.ui.dialog.KernelDetailDialog
import id.nkz.nokontzzzmanager.R

@Composable
fun KernelCard(
    k: KernelInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Section with MD3 styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.kernel),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.kernel),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.system_information),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick info grid with MD3 cards
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Process kernel version to extract clean version info
                val shortenedVersion = shortenKernelVersion(k.version)

                // State untuk menyimpan detail info yang sedang ditampilkan
                var detailInfo by remember { mutableStateOf<KernelDetailInfo?>(null) }
                
                // Tampilkan dialog jika ada detail info
                detailInfo?.let { info ->
                    KernelDetailDialog(
                        detailInfo = info,
                        onDismiss = { detailInfo = null }
                    )
                }

                // Total 7 items in the list for rounding logic
                val totalItems = 7

                // 1. Kernel Version
                val kernelVersionTitle = stringResource(id = R.string.kernel_version)
                CompactInfoCardWithCustomShape(
                    label = stringResource(R.string.version),
                    value = shortenedVersion,
                    icon = Icons.Filled.Memory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = kernelVersionTitle,
                            value = k.version,
                            icon = Icons.Filled.Memory
                        )
                    }
                )

                // 2. GKI Type
                val kernelTypeString = stringResource(id = getKernelTypeResByVersion(k.version))
                val kernelTypeTitle = stringResource(id = R.string.kernel_type)
                CompactInfoCardWithCustomShape(
                    label = kernelTypeTitle,
                    value = kernelTypeString,
                    icon = Icons.Filled.Computer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = kernelTypeTitle,
                            value = kernelTypeString,
                            icon = Icons.Filled.Computer
                        )
                    }
                )

                // 3. Build Fingerprint
                val buildFingerprintTitle = stringResource(id = R.string.build_fingerprint)
                CompactInfoCardWithCustomShape(
                    label = stringResource(id = R.string.build),
                    value = k.fingerprint.substringAfterLast("/"),
                    icon = Icons.Outlined.Fingerprint,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = buildFingerprintTitle,
                            value = k.fingerprint,
                            icon = Icons.Outlined.Fingerprint
                        )
                    }
                )

                // 4. WireGuard Version
                val wireguardTitle = stringResource(id = R.string.wireguard_version)
                val wireguardValue = k.wireguardVersion ?: stringResource(id = R.string.common_na)
                CompactInfoCardWithCustomShape(
                    label = wireguardTitle,
                    value = wireguardValue,
                    icon = Icons.Filled.Shield,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = wireguardTitle,
                            value = wireguardValue,
                            icon = Icons.Filled.Shield
                        )
                    }
                )

                // 5. I/O Scheduler
                val ioSchedulerTitle = stringResource(id = R.string.io_scheduler)
                CompactInfoCardWithCustomShape(
                    label = stringResource(R.string.sched),
                    value = k.scheduler,
                    icon = Icons.Filled.Settings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = ioSchedulerTitle,
                            value = k.scheduler,
                            icon = Icons.Filled.Settings
                        )
                    }
                )

                // 6. ABI
                val abiTitle = stringResource(id = R.string.abi)
                CompactInfoCardWithCustomShape(
                    label = abiTitle,
                    value = k.abi,
                    icon = Icons.Filled.Computer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = abiTitle,
                            value = k.abi,
                            icon = Icons.Filled.Computer
                        )
                    }
                )

                // 7. KernelSU (Bottom item)
                val kernelsuTitle = stringResource(id = R.string.kernelsu)
                CompactInfoCardWithCustomShape(
                    label = kernelsuTitle,
                    value = when {
                        k.kernelSuStatus.contains("Version", ignoreCase = true) -> "✓ " + k.kernelSuStatus.substringAfter("Version ").take(8)
                        k.kernelSuStatus.contains("Active", ignoreCase = true) -> {
                            val inside = k.kernelSuStatus.substringAfter("(", "").substringBefore(")")
                            if (inside.isNotBlank()) "✓ $inside" else stringResource(id = R.string.ksu_active)
                        }
                        k.kernelSuStatus.contains("Detected", ignoreCase = true) -> stringResource(id = R.string.ksu_detected)
                        else -> stringResource(id = R.string.ksu_not_found)
                    },
                    icon = Icons.Filled.AdminPanelSettings,
                    valueColor = getKernelSuColor(k.kernelSuStatus),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp, 4.dp, 16.dp, 16.dp),
                    onCardClick = {
                        detailInfo = KernelDetailInfo(
                            title = kernelsuTitle,
                            value = k.kernelSuStatus,
                            icon = Icons.Filled.AdminPanelSettings
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (value.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCardWithCustomShape(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    valueColor: Color? = null,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (value.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
private fun getSelinuxColor(status: String): Color {
    return when (status.lowercase()) {
        "enforcing" -> MaterialTheme.colorScheme.primary
        "permissive" -> MaterialTheme.colorScheme.tertiary
        "disabled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun getKernelSuColor(status: String): Color {
    return when {
        status.contains("Version", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        status.contains("Active", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        status.contains("Detected", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        status.contains("Not Detected", ignoreCase = true) -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurface
    }
}

private fun shortenKernelVersion(version: String): String {
    if (version.contains("4.19.404R", ignoreCase = true) && 
        version.contains("vyn@zorin", ignoreCase = true)) {
        return "4.19.404R"
    }

    if (version.contains("perf+", ignoreCase = true) && 
        version.contains("rohmanurip@Github", ignoreCase = true)) {
         val versionRegex = """Linux version ([\d.]+)""".toRegex()
         val match = versionRegex.find(version)
         val ver = match?.groupValues?.get(1) ?: ""
         return if (ver.isNotEmpty()) "$ver-perf+" else "perf+"
    }

    val versionRegex = """Linux version ([\d.]+)-([^ ]+)""".toRegex()
    val matchResult = versionRegex.find(version)
    
    return if (matchResult != null) {
        val versionNumber = matchResult.groupValues[1]
        val kernelName = matchResult.groupValues[2]
        "$versionNumber-$kernelName"
    } else {
        val hashIndex = version.indexOf(" #")
        val parenIndex = version.indexOf(" (")
        val endIndex = when {
            hashIndex != -1 && parenIndex != -1 -> minOf(hashIndex, parenIndex)
            hashIndex != -1 -> hashIndex
            parenIndex != -1 -> parenIndex
            else -> version.length
        }
        version.take(endIndex).trim()
    }
}

private fun getKernelTypeResByVersion(version: String): Int {
    Log.d("KernelCard", "getKernelTypeByVersion called with version: '$version'")
    
    val versionRegex = """Linux version ([\d.]+)""".toRegex()
    val matchResult = versionRegex.find(version)
    
    if (matchResult != null) {
        val versionNumber = matchResult.groupValues[1]
        Log.d("KernelCard", "Extracted version number: '$versionNumber'")
        
        val cleanVersionNumber = versionNumber.trim()
            .replace(Regex("""[^\d.]"""), "")
            .replace("..", ".")
            .replace(Regex("""\.{2,}"""), ".")
            .replace(Regex("""[^\x00-\x7F]"""), "")
            .trim('.', '.')
        
        val finalVersionNumber = if (cleanVersionNumber.count { it == '.' } > 1) {
            val firstDotIndex = cleanVersionNumber.indexOf('.')
            val secondDotIndex = cleanVersionNumber.indexOf('.', firstDotIndex + 1)
            if (secondDotIndex != -1) {
                cleanVersionNumber.take(secondDotIndex)
            } else {
                cleanVersionNumber
            }
        } else {
            cleanVersionNumber
        }
        
        val versionParts = finalVersionNumber.split(".").map { it.toIntOrNull() ?: 0 }
        
        val result = when {
            versionParts.size >= 2 && (versionParts[0] < 4 || (versionParts[0] == 4 && versionParts[1] < 19)) -> {
                R.string.legacy
            }
            versionParts.size >= 2 && versionParts[0] == 4 && versionParts[1] == 19 -> {
                R.string.non_gki
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] == 4 -> {
                R.string.gki1
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] > 4 && versionParts[1] < 10 -> {
                R.string.gki1
            }
            versionParts.size >= 2 && (versionParts[0] > 5 || (versionParts[0] == 5 && versionParts[1] >= 10)) -> {
                R.string.gki2
            }
            else -> {
                R.string.common_unknown_value
            }
        }
        
        return result
    }
    
    val simpleVersionRegex = """(\d+\.\d+)""".toRegex()
    val simpleMatch = simpleVersionRegex.find(version)
    
    if (simpleMatch != null) {
        val versionNumber = simpleMatch.groupValues[1]
        
        val cleanVersionNumber = versionNumber.trim()
            .replace(Regex("""[^\d.]"""), "")
            .replace("..", ".")
            .replace(Regex("""\.{2,}"""), ".")
            .replace(Regex("""[^\x00-\x7F]"""), "")
            .trim('.', '.')
        
        val finalVersionNumber = if (cleanVersionNumber.count { it == '.' } > 1) {
            val firstDotIndex = cleanVersionNumber.indexOf('.')
            val secondDotIndex = cleanVersionNumber.indexOf('.', firstDotIndex + 1)
            if (secondDotIndex != -1) {
                cleanVersionNumber.take(secondDotIndex)
            } else {
                cleanVersionNumber
            }
        } else {
            cleanVersionNumber
        }
        
        val versionParts = finalVersionNumber.split(".").map { it.toIntOrNull() ?: 0 }
        
        val result = when {
            versionParts.size >= 2 && (versionParts[0] < 4 || (versionParts[0] == 4 && versionParts[1] < 19)) -> {
                R.string.legacy
            }
            versionParts.size >= 2 && versionParts[0] == 4 && versionParts[1] == 19 -> {
                R.string.non_gki
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] == 4 -> {
                R.string.gki1
            }
            versionParts.size >= 2 && versionParts[0] == 5 && versionParts[1] > 4 && versionParts[1] < 10 -> {
                R.string.gki1
            }
            versionParts.size >= 2 && (versionParts[0] > 5 || (versionParts[0] == 5 && versionParts[1] >= 10)) -> {
                R.string.gki2
            }
            else -> {
                R.string.common_unknown_value
            }
        }
        
        return result
    }
    
    return R.string.common_unknown_value
}
