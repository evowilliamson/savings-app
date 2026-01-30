package com.khaohom.savings.data.remote

import com.khaohom.savings.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Backend API service
 */
interface BackendApiService {
    @GET("api/assets")
    suspend fun getAssets(): List<Asset>
    
    @GET("api/payments")
    suspend fun getPayments(): List<Transaction>
    
    @GET("api/current-exchange-rate")
    suspend fun getCurrentExchangeRate(): ExchangeRateResponse
    
    @GET("health")
    suspend fun healthCheck(): Map<String, Any>
}

/**
 * CoinGecko API service
 */
interface CoinGeckoApiService {
    @GET("simple/price")
    suspend fun getPrices(
        @Query("ids") ids: String = "bitcoin,tether-gold",
        @Query("vs_currencies") vsCurrencies: String = "usd"
    ): CoinGeckoPriceResponse
}

/**
 * Exchange Rate API service (backup)
 */
interface ExchangeRateApiService {
    @GET("latest/USD")
    suspend fun getExchangeRates(): Map<String, Any>
}
