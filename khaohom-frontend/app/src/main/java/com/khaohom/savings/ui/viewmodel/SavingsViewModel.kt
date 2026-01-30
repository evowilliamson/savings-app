package com.khaohom.savings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaohom.savings.data.model.*
import com.khaohom.savings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main ViewModel for the app
 */
class SavingsViewModel : ViewModel() {
    
    private val repository = SavingsRepository()
    
    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Currency preference (default: USD)
    private val _isThb = MutableStateFlow(false)
    val isThb: StateFlow<Boolean> = _isThb.asStateFlow()
    
    // Data
    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _holdings = MutableStateFlow<List<AssetHolding>>(emptyList())
    val holdings: StateFlow<List<AssetHolding>> = _holdings.asStateFlow()
    
    private val _summary = MutableStateFlow<PortfolioSummary?>(null)
    val summary: StateFlow<PortfolioSummary?> = _summary.asStateFlow()
    
    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()
    
    private val _projections = MutableStateFlow<List<AssetProjection>>(emptyList())
    val projections: StateFlow<List<AssetProjection>> = _projections.asStateFlow()
    
    private val _exchangeRate = MutableStateFlow(31.2) // Default THB rate
    val exchangeRate: StateFlow<Double> = _exchangeRate.asStateFlow()
    
    private val _currentPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val currentPrices: StateFlow<Map<String, Double>> = _currentPrices.asStateFlow()
    
    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated.asStateFlow()
    
    private val _projectionYears = MutableStateFlow(5)
    val projectionYears: StateFlow<Int> = _projectionYears.asStateFlow()
    
    init {
        loadData()
    }
    
    /**
     * Load all data
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // Fetch exchange rate
                val rateResult = repository.getExchangeRate()
                if (rateResult.isSuccess) {
                    _exchangeRate.value = rateResult.getOrNull() ?: 31.2
                }
                
                // Fetch current prices
                val pricesResult = repository.getCurrentPrices()
                if (pricesResult.isSuccess) {
                    _currentPrices.value = pricesResult.getOrNull() ?: emptyMap()
                }
                
                // Fetch assets
                val assetsResult = repository.getAssets()
                if (assetsResult.isFailure) {
                    _uiState.value = UiState.Error(assetsResult.exceptionOrNull()?.message ?: "Failed to load assets")
                    return@launch
                }
                _assets.value = assetsResult.getOrNull() ?: emptyList()
                
                // Fetch transactions
                val transactionsResult = repository.getTransactions()
                if (transactionsResult.isFailure) {
                    _uiState.value = UiState.Error(transactionsResult.exceptionOrNull()?.message ?: "Failed to load transactions")
                    return@launch
                }
                _transactions.value = transactionsResult.getOrNull() ?: emptyList()
                
                // Calculate holdings
                val holdingsResult = repository.getAssetHoldings(
                    _exchangeRate.value,
                    _currentPrices.value
                )
                if (holdingsResult.isFailure) {
                    _uiState.value = UiState.Error(holdingsResult.exceptionOrNull()?.message ?: "Failed to calculate holdings")
                    return@launch
                }
                _holdings.value = holdingsResult.getOrNull() ?: emptyList()
                
                // Calculate summary
                val summaryResult = repository.getPortfolioSummary(
                    _exchangeRate.value,
                    _currentPrices.value
                )
                if (summaryResult.isFailure) {
                    _uiState.value = UiState.Error(summaryResult.exceptionOrNull()?.message ?: "Failed to calculate summary")
                    return@launch
                }
                _summary.value = summaryResult.getOrNull()
                
                // Get chart data
                val chartResult = repository.getChartData(_exchangeRate.value)
                if (chartResult.isFailure) {
                    _uiState.value = UiState.Error(chartResult.exceptionOrNull()?.message ?: "Failed to load chart data")
                    return@launch
                }
                _chartData.value = chartResult.getOrNull() ?: emptyList()
                
                // Get projections
                updateProjections()
                
                // Update timestamp
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                _lastUpdated.value = now.format(formatter)
                
                _uiState.value = UiState.Success
                
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Toggle currency display
     */
    fun toggleCurrency() {
        _isThb.value = !_isThb.value
    }
    
    /**
     * Update projection years
     */
    fun updateProjectionYears(years: Int) {
        _projectionYears.value = years
        updateProjections()
    }
    
    /**
     * Update projections based on current years
     */
    private fun updateProjections() {
        viewModelScope.launch {
            val projectionsResult = repository.getProjections(
                _projectionYears.value,
                _currentPrices.value
            )
            if (projectionsResult.isSuccess) {
                _projections.value = projectionsResult.getOrNull() ?: emptyList()
            }
        }
    }
    
    /**
     * Format currency value
     */
    fun formatCurrency(value: Double, isThb: Boolean = _isThb.value): String {
        return if (isThb) {
            "฿${String.format("%,.0f", value)}"
        } else {
            "$${String.format("%,.2f", value)}"
        }
    }
    
    /**
     * Format asset amount
     */
    fun formatAmount(amount: Double, assetName: String): String {
        return when (assetName) {
            "BTC" -> "฿${String.format("%.5f", amount)}"
            "GOLD" -> String.format("%.5f", amount)
            "USD" -> "$${String.format("%,.2f", amount)}"
            else -> String.format("%.5f", amount)
        }
    }
}

/**
 * UI State sealed class
 */
sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}
