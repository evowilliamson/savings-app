package com.khaohom.savings.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khaohom.savings.data.model.Transaction
import com.khaohom.savings.ui.viewmodel.SavingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val isThb by viewModel.isThb.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transaction History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions yet",
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
                    text = "Date",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Value",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Asset",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Transaction list (oldest first - chronological order)
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(transactions.sortedBy { it.transactionDate }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        isThb = isThb,
                        viewModel = viewModel,
                        onClick = { selectedTransaction = transaction }
                    )
                }
            }
        }
        
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
    
    // Transaction detail dialog
    selectedTransaction?.let { transaction ->
        TransactionDetailDialog(
            transaction = transaction,
            isThb = isThb,
            viewModel = viewModel,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    isThb: Boolean,
    viewModel: SavingsViewModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = try {
                LocalDate.parse(transaction.transactionDate)
                    .format(DateTimeFormatter.ofPattern("MMM dd, yy"))
            } catch (e: Exception) {
                transaction.transactionDate
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f)
        )
        
        // Amount
        Text(
            text = viewModel.formatAmount(transaction.amount, transaction.assetName),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f)
        )
        
        // Value
        Text(
            text = if (isThb)
                viewModel.formatCurrency(transaction.usdValueAtTx * transaction.usdthbRate, true)
            else
                viewModel.formatCurrency(transaction.usdValueAtTx, false),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        
        // Asset
        Text(
            text = transaction.assetName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    isThb: Boolean,
    viewModel: SavingsViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Date", transaction.transactionDate)
                DetailRow("Asset", transaction.assetDisplayName)
                DetailRow("Amount", viewModel.formatAmount(transaction.amount, transaction.assetName))
                
                transaction.price?.let {
                    DetailRow("Price", viewModel.formatCurrency(it, true))
                }
                
                DetailRow(
                    "Value",
                    if (isThb)
                        viewModel.formatCurrency(transaction.usdValueAtTx * transaction.usdthbRate, true)
                    else
                        viewModel.formatCurrency(transaction.usdValueAtTx, false)
                )
                
                DetailRow(
                    "Cumulative",
                    viewModel.formatCurrency(transaction.usdCumulative, false)
                )
                
                transaction.reason?.let {
                    DetailRow("Reason", it)
                }
                
                transaction.status?.let {
                    DetailRow("Status", it)
                }
                
                DetailRow("Exchange Rate", String.format("%.2f", transaction.usdthbRate))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
