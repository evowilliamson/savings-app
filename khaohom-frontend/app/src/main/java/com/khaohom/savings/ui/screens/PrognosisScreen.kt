package com.khaohom.savings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.khaohom.savings.ui.viewmodel.SavingsViewModel

@Composable
fun PrognosisScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val projections by viewModel.projections.collectAsState()
    val isThb by viewModel.isThb.collectAsState()
    val exchangeRate by viewModel.exchangeRate.collectAsState()
    val projectionYears by viewModel.projectionYears.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    
    var yearsInput by remember { mutableStateOf(projectionYears.toString()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Future Value Prognosis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Years input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projection Period (years):",
                style = MaterialTheme.typography.bodyMedium
            )
            
            OutlinedTextField(
                value = yearsInput,
                onValueChange = { 
                    yearsInput = it
                    val years = it.toIntOrNull()
                    if (years != null && years in 1..30) {
                        viewModel.updateProjectionYears(years)
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        Divider()
        
        // Projections table
        Text(
            text = "Future Values",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        
        if (projections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No projection data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Current",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "After ${projectionYears}Y",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Asset projections
            projections.forEach { projection ->
                ProjectionRow(
                    projection = projection,
                    isThb = isThb,
                    exchangeRate = exchangeRate,
                    viewModel = viewModel
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Total row
            val totalCurrent = projections.sumOf { it.currentValueUsd }
            val totalFuture = projections.sumOf { it.futureValueUsd }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = if (isThb)
                        viewModel.formatCurrency(totalCurrent * exchangeRate, true)
                    else
                        viewModel.formatCurrency(totalCurrent, false),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isThb)
                        viewModel.formatCurrency(totalFuture * exchangeRate, true)
                    else
                        viewModel.formatCurrency(totalFuture, false),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Projection chart
            Text(
                text = "Growth Projection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            ProjectionChart(
                projections = projections,
                years = projectionYears,
                isThb = isThb,
                exchangeRate = exchangeRate
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

@Composable
fun ProjectionRow(
    projection: com.khaohom.savings.data.model.AssetProjection,
    isThb: Boolean,
    exchangeRate: Double,
    viewModel: SavingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = projection.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            text = if (isThb)
                viewModel.formatCurrency(projection.currentValueUsd * exchangeRate, true)
            else
                viewModel.formatCurrency(projection.currentValueUsd, false),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isThb)
                viewModel.formatCurrency(projection.futureValueUsd * exchangeRate, true)
            else
                viewModel.formatCurrency(projection.futureValueUsd, false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProjectionChart(
    projections: List<com.khaohom.savings.data.model.AssetProjection>,
    years: Int,
    isThb: Boolean,
    exchangeRate: Double
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                
                // X-axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "Y${value.toInt()}"
                        }
                    }
                }
                
                // Y-axis
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color(0xFFE0E0E0).toArgb()
                }
                axisRight.isEnabled = false
                
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val totalCurrent = projections.sumOf { it.currentValueUsd }
            
            // Calculate total value for each year
            val entries = (0..years).map { year ->
                val totalFuture = projections.sumOf { projection ->
                    val asset = projection
                    val cagr = 0.25 // Default, will be fetched from asset
                    projection.currentValueUsd * Math.pow(1 + cagr, year.toDouble())
                }
                val value = if (isThb) (totalFuture * exchangeRate).toFloat() else totalFuture.toFloat()
                Entry(year.toFloat(), value)
            }
            
            val dataSet = LineDataSet(entries, if (isThb) "Portfolio (THB)" else "Portfolio (USD)").apply {
                color = Color(0xFF1976D2).toArgb()
                setCircleColor(Color(0xFF1976D2).toArgb())
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = Color(0xFF1976D2).toArgb()
                fillAlpha = 30
            }
            
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
