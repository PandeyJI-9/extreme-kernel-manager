package id.nkz.nokontzzzmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

@Composable
fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.2f),
    targetValue: Float? = null // Optional target line (e.g., 16.6ms)
) {
    if (data.isEmpty()) return

    // Calculate max value with some padding, but cap it to avoid extreme spikes flattening the graph
    val rawMax = data.maxOrNull() ?: 1f
    val avg = data.average().toFloat()
    val maxVal = if (targetValue != null) {
        // For Frame Time, we want to see the area around the target (e.g., 16ms to 33ms)
        maxOf(targetValue * 2f, avg * 2f).coerceAtMost(rawMax)
    } else {
        rawMax.coerceAtLeast(1f)
    }
    val minVal = 0f
    
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f).toArgb()
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val textPaint = remember {
        Paint().apply {
            color = onSurfaceColor
            textSize = 24f // approx 10sp
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val labelWidth = 60f
        val width = size.width - labelWidth
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        // Draw Grid and Labels
        val gridCount = 4
        for (i in 0..gridCount) {
            val labelValue = (maxVal * i / gridCount).toInt()
            val y = height - (i.toFloat() / gridCount * height)

            drawLine(
                color = gridLineColor,
                start = Offset(labelWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )

            
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(labelValue.toString(), labelWidth - 10f, y + 10f, textPaint)
            }
        }

        // Draw Target Line if provided (e.g., 16.6ms line)
        targetValue?.let { target ->
            if (target <= maxVal) {
                val targetY = height - ((target - minVal) / (maxVal - minVal) * height)
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(labelWidth, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }

        val path = Path()
        val fillPath = Path()

        data.forEachIndexed { index, value ->
            val x = labelWidth + (index * stepX)
            // Clamp value to maxVal for visualization consistency
            val clampedValue = value.coerceAtMost(maxVal)
            val y = height - ((clampedValue - minVal) / (maxVal - minVal) * height)
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == data.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(path = fillPath, color = fillColor)
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun MultiLineChart(
    dataSets: List<List<Float>>,
    lineColors: List<Color>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    unit: String = ""
) {
    if (dataSets.isEmpty() || dataSets.all { it.isEmpty() }) return

    val maxVal = dataSets.flatten().maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val minVal = 0f
    
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f).toArgb()
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val textPaint = remember {
        Paint().apply {
            color = onSurfaceColor
            textSize = 24f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val labelWidth = 60f
        val legendHeight = 30f
        val width = size.width - labelWidth
        val height = size.height - legendHeight
        val maxPoints = dataSets.maxOf { it.size }
        val stepX = width / (maxPoints - 1).coerceAtLeast(1)

        // Draw Grid and Labels
        val gridCount = 4
        for (i in 0..gridCount) {
            val labelValue = (maxVal * i / gridCount).toInt()
            val y = height - (i.toFloat() / gridCount * height)

            drawLine(
                color = gridLineColor,
                start = Offset(labelWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(labelValue.toString(), labelWidth - 10f, y + 10f, textPaint)
            }
        }

        // Draw Lines
        dataSets.forEachIndexed { setIndex, data ->
            if (data.isEmpty()) return@forEachIndexed
            
            val path = Path()
            val lineColor = lineColors.getOrElse(setIndex) { defaultPrimaryColor }
            
            data.forEachIndexed { index, value ->
                val x = labelWidth + (index * stepX)
                val y = height - ((value - minVal) / (maxVal - minVal) * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
        }
        
        // Draw Legend
        var currentLegendX = labelWidth
        labels.forEachIndexed { index, label ->
            val color = lineColors.getOrElse(index) { defaultPrimaryColor }
            val rectSize = 10.dp.toPx()
            
            drawRect(
                color = color,
                topLeft = Offset(currentLegendX, height + 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize)
            )
            
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    this.color = onSurfaceColor
                    this.textSize = 24f
                    this.textAlign = Paint.Align.LEFT
                }
                canvas.nativeCanvas.drawText(label, currentLegendX + rectSize + 5.dp.toPx(), height + 10.dp.toPx() + rectSize, paint)
                currentLegendX += paint.measureText(label) + rectSize + 30.dp.toPx()
            }
        }
    }
}
