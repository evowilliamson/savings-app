package com.khaohom.savings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaohom.savings.data.model.AssetHolding
import com.khaohom.savings.data.model.PortfolioSummary
import com.khaohom.savings.ui.viewmodel.SavingsViewModel
import com.khaohom.savings.ui.viewmodel.UiState

@Composable
fun AssetsScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val holdings by viewModel.holdings.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isThb by viewModel.isThb.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Assets List
        Text(
            text = "Assets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (uiState is UiState.Error) {
            val message = (uiState as UiState.Error).message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Failed to load assets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (holdings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No assets yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Zen table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isThb) "THB" else "USD",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Asset rows
            holdings.forEach { holding ->
                AssetRow(
                    holding = holding,
                    isThb = isThb,
                    viewModel = viewModel
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary Section
        summary?.let { sum ->
            SummarySection(
                summary = sum,
                isThb = isThb,
                viewModel = viewModel
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
fun AssetRow(
    holding: AssetHolding,
    isThb: Boolean,
    viewModel: SavingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Asset name
        Text(
            text = holding.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        // Amount
        Text(
            text = viewModel.formatAmount(holding.totalAmount, holding.assetName),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        // Value
        Text(
            text = if (isThb) 
                viewModel.formatCurrency(holding.currentValueThb, true)
            else 
                viewModel.formatCurrency(holding.currentValueUsd, false),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummarySection(
    summary: PortfolioSummary,
    isThb: Boolean,
    viewModel: SavingsViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        
        // Total
        SummaryRow(
            label = "Total",
            value = if (isThb) {
                "${viewModel.formatCurrency(summary.totalValueUsd, false)} / ${viewModel.formatCurrency(summary.totalValueThb, true)}"
            } else {
                viewModel.formatCurrency(summary.totalValueUsd, false)
            }
        )
        
        // Cost
        SummaryRow(
            label = "Cost",
            value = viewModel.formatCurrency(summary.totalCostUsd, false)
        )
        
        // Profit
        SummaryRow(
            label = "Profit",
            value = "${viewModel.formatCurrency(summary.profitUsd, false)} (${String.format("%.1f", summary.profitPercent)}%)",
            isProfit = summary.profitUsd >= 0
        )
        
        // APY
        SummaryRow(
            label = "APY %",
            value = "${String.format("%.2f", summary.apyPercent)}%"
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isProfit: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (label == "Profit" && !isProfit) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}
