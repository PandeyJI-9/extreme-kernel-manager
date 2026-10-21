package id.nkz.nokontzzzmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.database.BenchmarkEntity
import id.nkz.nokontzzzmanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.nkz.nokontzzzmanager.ui.components.MultiLineChart
import id.nkz.nokontzzzmanager.ui.components.SimpleLineChart
import id.nkz.nokontzzzmanager.utils.CompressionUtils
import id.nkz.nokontzzzmanager.viewmodel.BenchmarkDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CaptureAction { SHARE, DOWNLOAD }

@Composable
fun BenchmarkDetailScreen(
    navController: NavController,
    viewModel: BenchmarkDetailViewModel = hiltViewModel()
) {
    val benchmark by viewModel.benchmark.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var captureAction by remember { mutableStateOf<CaptureAction?>(null) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Pre-resolve strings to avoid lint errors and ensure consistency
    val captureFailedTemplate = stringResource(R.string.benchmark_capture_failed)
    val saveSuccessMsg = stringResource(R.string.benchmark_save_success)
    val saveFailedTemplate = stringResource(R.string.benchmark_save_failed)
    val shareChooserTitle = stringResource(R.string.benchmark_share_chooser)

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                saveBitmapToUri(context, pendingBitmap, it, saveSuccessMsg, saveFailedTemplate)
                pendingBitmap = null
            }
        } ?: run {
            pendingBitmap = null
        }
    }
    
    // Handle share trigger from ViewModel
    LaunchedEffect(Unit) {
        viewModel.shareTrigger.collect {
            captureAction = CaptureAction.SHARE
        }
    }

    // Handle download trigger from ViewModel
    LaunchedEffect(Unit) {
        viewModel.downloadTrigger.collect {
            captureAction = CaptureAction.DOWNLOAD
        }
    }
    
    if (captureAction != null) {
        LaunchedEffect(captureAction) {
            try {
                // Give more time for the hidden layout to stabilize
                kotlinx.coroutines.delay(1000)
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                
                when (captureAction) {
                    CaptureAction.SHARE -> {
                        shareBitmap(context, bitmap, "benchmark_${System.currentTimeMillis()}.png", shareChooserTitle)
                        captureAction = null
                    }
                    CaptureAction.DOWNLOAD -> {
                        pendingBitmap = bitmap
                        val fileName = "benchmark_${benchmark?.appName?.replace(" ", "_")}_${System.currentTimeMillis()}.png"
                        createDocumentLauncher.launch(fileName)
                        captureAction = null
                    }
                    else -> { captureAction = null }
                }
            } catch (e: Exception) {
                val errorMsg = captureFailedTemplate.replace("%s", e.message ?: "Unknown error")
                Toast.makeText(context.applicationContext, errorMsg, Toast.LENGTH_SHORT).show()
                captureAction = null
            }
        }
    }
    
    benchmark?.let { b ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main UI with LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    BenchmarkContent(b)
                }
            }

            // Hidden full-height content for capture
            if (captureAction != null) {
                // We use a custom Layout to bypass the parent's fillMaxSize constraints
                // and allow the content to be measured at its full intrinsic height.
                Layout(
                    content = {
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BenchmarkContent(b)
                        }
                    },
                    modifier = Modifier
                        .alpha(0f) // Hide it from the user
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) { measurables, constraints ->
                    // Measure with no height limit
                    val placeables = measurables.map { 
                        it.measure(constraints.copy(minHeight = 0, maxHeight = 15000)) 
                    }
                    val width = placeables.maxOfOrNull { it.width } ?: 0
                    val height = placeables.maxOfOrNull { it.height } ?: 0
                    
                    layout(width, height) {
                        placeables.forEach { it.placeRelative(0, 0) }
                    }
                }
                
                // Loading overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IndeterminateExpressiveLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.benchmark_generating_screenshot), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun BenchmarkContent(b: BenchmarkEntity) {
    // Extract colors again or pass them as parameters. Let's extract for simplicity in refactor.
    val colorFps = MaterialTheme.colorScheme.primary
    val colorLow1 = MaterialTheme.colorScheme.tertiary
    val colorLow01 = MaterialTheme.colorScheme.error
    val colorFrameTime = MaterialTheme.colorScheme.secondary
    val colorCpu = MaterialTheme.colorScheme.secondary
    val colorGpu = MaterialTheme.colorScheme.primary
    val colorTemp = MaterialTheme.colorScheme.error
    val colorCpuTemp = MaterialTheme.colorScheme.tertiary
    val colorGpuFreq = MaterialTheme.colorScheme.secondary
    val colorBatteryPower = MaterialTheme.colorScheme.error
    val colorBatteryLevel = MaterialTheme.colorScheme.primary
    val colorLittle = MaterialTheme.colorScheme.primary
    val colorBig = MaterialTheme.colorScheme.secondary
    val colorPrime = MaterialTheme.colorScheme.tertiary

    val frameIntervals = remember(b.frameTimeDataJson) { decodeJsonList(b.frameTimeDataJson) }
    val fpsOverTime = remember(frameIntervals) { calculateFpsOverTime(frameIntervals) }

    val topCardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val middleCardShape = RoundedCornerShape(8.dp)
    val bottomCardShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    BenchmarkHeader(b)
    Spacer(modifier = Modifier.height(14.dp))

    BenchmarkSummaryCard(b, colorFps, colorLow1, colorLow01, shape = topCardShape)

    // FPS Graph
    ChartCard(
        title = stringResource(R.string.benchmark_fps_stability),
        icon = Icons.Default.Speed,
        data = fpsOverTime,
        lineColor = colorFps,
        unit = "FPS",
        targetValue = 60f,
        shape = middleCardShape
    )

    // Frame Time Graph
    ChartCard(
        title = stringResource(R.string.benchmark_frame_time),
        icon = Icons.Default.Timeline,
        data = frameIntervals,
        lineColor = colorFrameTime,
        unit = "ms",
        targetValue = 16.6f,
        shape = middleCardShape
    )

    // CPU Usage Graph
    ChartCard(
        title = stringResource(R.string.benchmark_cpu_usage),
        icon = Icons.Default.Memory,
        data = decodeJsonList(b.cpuUsageDataJson),
        lineColor = colorCpu,
        unit = "%",
        shape = middleCardShape
    )

    // CPU Clusters Freq Graph
    val little = decodeJsonList(b.cpuFreqLittleDataJson)
    val big = decodeJsonList(b.cpuFreqBigDataJson)
    val prime = decodeJsonList(b.cpuFreqPrimeDataJson)
    
    if (little.isNotEmpty() || big.isNotEmpty() || prime.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = middleCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(text = stringResource(R.string.benchmark_cpu_clusters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                MultiLineChart(
                    dataSets = listOf(little, big, prime),
                    lineColors = listOf(colorLittle, colorBig, colorPrime),
                    labels = listOf("Little", "Big", "Prime"),
                    unit = "MHz"
                )
            }
        }
    }

    // CPU Temp Graph
    ChartCard(
        title = stringResource(R.string.benchmark_cpu_temp),
        icon = Icons.Default.DeviceThermostat,
        data = decodeJsonList(b.cpuTempDataJson),
        lineColor = colorCpuTemp,
        unit = "°C",
        shape = middleCardShape
    )

    // GPU Usage Graph
    ChartCard(
        title = stringResource(R.string.benchmark_gpu_usage),
        icon = Icons.Default.GraphicEq,
        data = decodeJsonList(b.gpuUsageDataJson),
        lineColor = colorGpu,
        unit = "%",
        shape = middleCardShape
    )

    // GPU Freq Graph
    ChartCard(
        title = stringResource(R.string.benchmark_gpu_freq),
        icon = Icons.Default.FlashOn,
        data = decodeJsonList(b.gpuFreqDataJson),
        lineColor = colorGpuFreq,
        unit = "MHz",
        shape = middleCardShape
    )

    // Battery Power Graph
    ChartCard(
        title = stringResource(R.string.benchmark_battery_power),
        icon = Icons.Default.BatteryChargingFull,
        data = decodeJsonList(b.batteryPowerDataJson),
        lineColor = colorBatteryPower,
        unit = "W",
        shape = middleCardShape
    )

    // Battery Level Graph
    ChartCard(
        title = stringResource(R.string.benchmark_battery_level),
        icon = Icons.Default.BatteryFull,
        data = decodeJsonList(b.batteryLevelDataJson),
        lineColor = colorBatteryLevel,
        unit = "%",
        shape = middleCardShape
    )

    // Temperature Graph
    ChartCard(
        title = stringResource(R.string.benchmark_battery_temp),
        icon = Icons.Default.Thermostat,
        data = decodeJsonList(b.tempDataJson),
        lineColor = colorTemp,
        unit = "°C",
        shape = bottomCardShape
    )
}


@Composable
fun BenchmarkHeader(benchmark: BenchmarkEntity) {
    val context = LocalContext.current
    val appIcon = remember(benchmark.packageName) {
        try {
            context.packageManager.getApplicationIcon(benchmark.packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    val date = remember(benchmark.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(benchmark.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        appIcon?.let { drawable ->
            Image(
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } ?: Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column {
            Text(
                text = benchmark.appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BenchmarkSummaryCard(
    benchmark: BenchmarkEntity,
    colorFps: Color,
    colorLow1: Color,
    colorLow01: Color,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_avg_fps), "${benchmark.avgFps.toInt()}", colorFps)
                StatItem(stringResource(R.string.benchmark_1low_fps), "${benchmark.fps1Low.toInt()}", colorLow1)
                StatItem(stringResource(R.string.benchmark_01low_fps), "${benchmark.fps01Low.toInt()}", colorLow01)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            val totalSeconds = benchmark.durationMs / 1000
            val formattedDuration = if (totalSeconds >= 60) {
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                stringResource(R.string.duration_format_ms, minutes, seconds)
            } else {
                stringResource(R.string.duration_format_s, totalSeconds)
            }

            val statValueColor = MaterialTheme.colorScheme.onSurface
            
            // FPS Detail Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_max_fps), "${benchmark.maxFps.toInt()}", statValueColor)
                StatItem(stringResource(R.string.benchmark_min_fps), "${benchmark.minFps.toInt()}", statValueColor)
                StatItem(stringResource(R.string.benchmark_fps_variance), String.format("%.1f", benchmark.fpsVariance), statValueColor)
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_avg_cpu), "${benchmark.avgCpuUsage.toInt()}%", statValueColor)
                StatItem(stringResource(R.string.benchmark_avg_gpu), "${benchmark.avgGpuUsage.toInt()}%", statValueColor)
                StatItem(stringResource(R.string.benchmark_avg_temp), String.format("%.1f°C", benchmark.avgTemp), statValueColor)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            // Power & Max Temp Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.benchmark_avg_power), String.format("%.2fW", benchmark.avgPower), statValueColor)
                StatItem(stringResource(R.string.benchmark_max_temp), String.format("%.1f°C", benchmark.maxTemp), statValueColor)
                StatItem(stringResource(R.string.benchmark_duration), formattedDuration, statValueColor)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatItem(stringResource(R.string.benchmark_janks), "${benchmark.jankCount}", statValueColor)
                StatItem(stringResource(R.string.benchmark_big_janks), "${benchmark.bigJankCount}", statValueColor)
            }

        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ChartCard(
    title: String,
    icon: ImageVector,
    data: List<Float>,
    lineColor: Color,
    unit: String,
    targetValue: Float? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = lineColor, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                if (data.isNotEmpty()) {
                    val avgValue = data.average()
                    val formattedAvg = if (unit == "W" || unit == "ms" || unit == "°C") {
                        String.format("%.2f", avgValue)
                    } else {
                        avgValue.toInt().toString()
                    }
                    Text(
                        text = "Avg: $formattedAvg $unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (data.isNotEmpty()) {
                SimpleLineChart(
                    data = data,
                    lineColor = lineColor,
                    fillColor = lineColor.copy(alpha = 0.1f),
                    targetValue = targetValue
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.benchmark_no_data), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun decodeJsonList(json: String?): List<Float> {
    return try {
        if (json != null) {
            val decompressed = CompressionUtils.decompress(json)
            Json.decodeFromString<List<Float>>(decompressed ?: "[]")
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Aggregates raw frame intervals into FPS per second.
 * This provides a standard FPS graph that matches the user's perceived average.
 */
fun calculateFpsOverTime(frameIntervals: List<Float>): List<Float> {
    if (frameIntervals.isEmpty()) return emptyList()
    
    val result = mutableListOf<Float>()
    var currentWindowMs = 0f
    var frameCount = 0
    
    for (interval in frameIntervals) {
        currentWindowMs += interval
        frameCount++
        
        if (currentWindowMs >= 1000f) {
            result.add(frameCount.toFloat())
            // Carry over the overflow to the next window for precision
            currentWindowMs -= 1000f
            frameCount = 0
        }
    }
    
    // Add the last partial second if it's significant
    if (frameCount > 0 && currentWindowMs > 200f) {
        result.add(frameCount * (1000f / currentWindowMs))
    }
    
    return result
}

private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val imageFile = File(cachePath, fileName)
        val stream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun saveBitmapToUri(
    context: Context,
    bitmap: Bitmap?,
    uri: Uri,
    successMsg: String,
    failedTemplate: String
) {
    if (bitmap == null) return
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context.applicationContext, successMsg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                val msg = failedTemplate.replace("%s", e.message ?: "Unknown error")
                Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

