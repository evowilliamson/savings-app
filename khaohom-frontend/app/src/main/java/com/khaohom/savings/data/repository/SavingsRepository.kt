package com.khaohom.savings.data.repository

import com.khaohom.savings.data.model.*
import com.khaohom.savings.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.khaohom.savings.utils.DateUtils

/**
 * Main repository for data operations
 */
class SavingsRepository {
    
    private val backendApi = RetrofitClient.backendApi
    private val coinGeckoApi = RetrofitClient.coinGeckoApi
    
    /**
     * Get all assets
     */
    suspend fun getAssets(): Result<List<Asset>> = withContext(Dispatchers.IO) {
        try {
            val assets = backendApi.getAssets()
            Result.success(assets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all transactions
     */
    suspend fun getTransactions(): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val transactions = backendApi.getPayments()
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current exchange rate
     */
    suspend fun getExchangeRate(): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getCurrentExchangeRate()
            Result.success(response.rate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current crypto/gold prices
     */
    suspend fun getCurrentPrices(): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val response = coinGeckoApi.getPrices()
            val prices = mutableMapOf<String, Double>()
            
            response.bitcoin?.usd?.let { prices["BTC"] = it }
            response.tetherGold?.usd?.let { prices["GOLD"] = it }
            prices["USD"] = 1.0 // USD is always 1.0
            
            Result.success(prices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get aggregated asset holdings
     */
    suspend fun getAssetHoldings(
        exchangeRate: Double,
        currentPrices: Map<String, Double>
    ): Result<List<AssetHolding>> = withContext(Dispatchers.IO) {
        try {
            val assets = backendApi.getAssets()
            val transactions = backendApi.getPayments()
            
            // Group transactions by asset
            val holdings = transactions.groupBy { it.assetName }
                .map { (assetName, txs) ->
                    val asset = assets.find { it.assetName == assetName }
                    val totalAmount = txs.sumOf { it.amount }
                    val currentPrice = currentPrices[assetName] ?: 0.0
                    val currentValueUsd = totalAmount * currentPrice
                    val currentValueThb = currentValueUsd * exchangeRate
                    
                    AssetHolding(
                        assetName = assetName,
                        displayName = asset?.displayName ?: assetName,
                        totalAmount = totalAmount,
                        currentValueUsd = currentValueUsd,
                        currentValueThb = currentValueThb,
                        cagrPercent = asset?.cagrPercent ?: 0.0
                    )
                }
                .sortedByDescending { it.currentValueUsd }
            
            Result.success(holdings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate portfolio summary
     */
    suspend fun getPortfolioSummary(
        exchangeRate: Double,
        currentPrices: Map<String, Double>
    ): Result<PortfolioSummary> = withContext(Dispatchers.IO) {
        try {
            val transactions = backendApi.getPayments()
            val holdings = getAssetHoldings(exchangeRate, currentPrices).getOrThrow()
            
            val totalValueUsd = holdings.sumOf { it.currentValueUsd }
            val totalValueThb = totalValueUsd * exchangeRate
            val totalCostUsd = transactions.maxOfOrNull { it.usdCumulative } ?: 0.0
            val profitUsd = totalValueUsd - totalCostUsd
            val profitPercent = if (totalCostUsd > 0) (profitUsd / totalCostUsd) * 100 else 0.0
            
            // Calculate APY based on time since first transaction
            val apyPercent = calculateAPY(transactions, totalValueUsd, totalCostUsd)
            
            Result.success(
                PortfolioSummary(
                    totalValueUsd = totalValueUsd,
                    totalValueThb = totalValueThb,
                    totalCostUsd = totalCostUsd,
                    profitUsd = profitUsd,
                    profitPercent = profitPercent,
                    apyPercent = apyPercent
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate APY (Annual Percentage Yield)
     */
    private fun calculateAPY(
        transactions: List<Transaction>,
        currentValue: Double,
        totalCost: Double
    ): Double {
        if (transactions.isEmpty() || totalCost <= 0) return 0.0
        
        val firstDate = transactions.minByOrNull { it.transactionDate }?.transactionDate ?: return 0.0
        val today = LocalDate.now()
        val firstDateParsed = DateUtils.parseDate(firstDate)
        
        val daysBetween = ChronoUnit.DAYS.between(firstDateParsed, today)
        if (daysBetween <= 0) return 0.0
        
        val years = daysBetween / 365.0
        if (years <= 0) return 0.0
        
        val totalReturn = currentValue / totalCost
        val apy = (Math.pow(totalReturn, 1.0 / years) - 1) * 100
        
        return if (apy.isFinite()) apy else 0.0
    }
    
    /**
     * Get chart data points
     */
    suspend fun getChartData(exchangeRate: Double): Result<List<ChartDataPoint>> = withContext(Dispatchers.IO) {
        try {
            val transactions = backendApi.getPayments()
                .sortedBy { it.transactionDate }
            
            val chartData = transactions.map { tx ->
                ChartDataPoint(
                    date = tx.transactionDate,
                    valueUsd = tx.usdCumulative,
                    valueThb = tx.usdCumulative * exchangeRate
                )
            }
            
            Result.success(chartData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate future projections
     */
    suspend fun getProjections(
        years: Int,
        currentPrices: Map<String, Double>
    ): Result<List<AssetProjection>> = withContext(Dispatchers.IO) {
        try {
            val assets = backendApi.getAssets()
            val transactions = backendApi.getPayments()
            
            val projections = transactions.groupBy { it.assetName }
                .map { (assetName, txs) ->
                    val asset = assets.find { it.assetName == assetName }
                    val totalAmount = txs.sumOf { it.amount }
                    val currentPrice = currentPrices[assetName] ?: 0.0
                    val currentValueUsd = totalAmount * currentPrice
                    val cagr = (asset?.cagrPercent ?: 0.0) / 100.0
                    val futureValueUsd = currentValueUsd * Math.pow(1 + cagr, years.toDouble())
                    
                    AssetProjection(
                        assetName = assetName,
                        displayName = asset?.displayName ?: assetName,
                        currentValueUsd = currentValueUsd,
                        futureValueUsd = futureValueUsd,
                        years = years
                    )
                }
                .sortedByDescending { it.currentValueUsd }
            
            Result.success(projections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Health check
     */
    suspend fun healthCheck(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            backendApi.healthCheck()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
