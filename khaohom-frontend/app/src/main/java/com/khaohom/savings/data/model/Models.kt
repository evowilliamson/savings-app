package com.khaohom.savings.data.model

import com.google.gson.annotations.SerializedName

/**
 * Asset data model
 */
data class Asset(
    @SerializedName("id") val id: Int,
    @SerializedName("asset_name") val assetName: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("cagr_percent") val cagrPercent: Double
)

/**
 * Transaction data model
 */
data class Transaction(
    @SerializedName("id") val id: Int,
    @SerializedName("transaction_date") val transactionDate: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("asset_name") val assetName: String,
    @SerializedName("asset_display_name") val assetDisplayName: String,
    @SerializedName("thb_price") val thbPrice: Double?,
    @SerializedName("usd_value_at_tx") val usdValueAtTx: Double,
    @SerializedName("usd_cumulative") val usdCumulative: Double,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("usdthb_rate") val usdthbRate: Double
)

/**
 * Exchange rate response
 */
data class ExchangeRateResponse(
    @SerializedName("rate") val rate: Double,
    @SerializedName("timestamp") val timestamp: String
)

/**
 * CoinGecko price response
 */
data class CoinGeckoPriceResponse(
    @SerializedName("bitcoin") val bitcoin: PriceData?,
    @SerializedName("tether-gold") val tetherGold: PriceData?
)

data class PriceData(
    @SerializedName("usd") val usd: Double
)

/**
 * Aggregated asset holding
 */
data class AssetHolding(
    val assetName: String,
    val displayName: String,
    val totalAmount: Double,
    val currentValueUsd: Double,
    val currentValueThb: Double,
    val cagrPercent: Double
)

/**
 * Portfolio summary
 */
data class PortfolioSummary(
    val totalValueUsd: Double,
    val totalValueThb: Double,
    val totalCostUsd: Double,
    val profitUsd: Double,
    val profitPercent: Double,
    val apyPercent: Double
)

/**
 * Future projection for an asset
 */
data class AssetProjection(
    val assetName: String,
    val displayName: String,
    val currentValueUsd: Double,
    val futureValueUsd: Double,
    val years: Int
)

/**
 * Chart data point
 */
data class ChartDataPoint(
    val date: String,
    val valueUsd: Double,
    val valueThb: Double
)
