package com.khaohom.savings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.khaohom.savings.ui.viewmodel.SavingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ChartScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val chartData by viewModel.chartData.collectAsState()
    val isThb by viewModel.isThb.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Portfolio Value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (chartData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chart data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)
                        setDrawGridBackground(false)
                        
                        // X-axis configuration
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            granularity = 1f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val index = value.toInt()
                                    return if (index >= 0 && index < chartData.size) {
                                        try {
                                            val date = LocalDate.parse(chartData[index].date)
                                            date.format(DateTimeFormatter.ofPattern("MMM yy"))
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    } else {
                                        ""
                                    }
                                }
                            }
                        }
                        
                        // Y-axis configuration
                        axisLeft.apply {
                            setDrawGridLines(true)
                            gridColor = Color(0xFFE0E0E0).toArgb()
                        }
                        axisRight.isEnabled = false
                        
                        // Legend
                        legend.isEnabled = true
                    }
                },
                update = { chart ->
                    // Prepare data entries
                    val entries = chartData.mapIndexed { index, point ->
                        val value = if (isThb) point.valueThb.toFloat() else point.valueUsd.toFloat()
                        Entry(index.toFloat(), value)
                    }
                    
                    // Create dataset
                    val dataSet = LineDataSet(entries, if (isThb) "Value (THB)" else "Value (USD)").apply {
                        color = Color(0xFF1976D2).toArgb()
                        setCircleColor(Color(0xFF1976D2).toArgb())
                        lineWidth = 2f
                        circleRadius = 3f
                        setDrawCircleHole(false)
                        valueTextSize = 9f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        cubicIntensity = 0.2f
                        setDrawFilled(true)
                        fillColor = Color(0xFF1976D2).toArgb()
                        fillAlpha = 30
                    }
                    
                    // Update chart
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Last updated
        lastUpdated?.let {
            Text(
                text = "Last updated: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }
}
