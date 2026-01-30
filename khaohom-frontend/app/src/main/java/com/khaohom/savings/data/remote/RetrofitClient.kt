package com.khaohom.savings.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client factory
 */
object RetrofitClient {
    
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val backendApi: BackendApiService by lazy {
        createRetrofit(ApiConfig.BASE_URL).create(BackendApiService::class.java)
    }
    
    val coinGeckoApi: CoinGeckoApiService by lazy {
        createRetrofit(ApiConfig.COINGECKO_BASE_URL).create(CoinGeckoApiService::class.java)
    }
    
    val exchangeRateApi: ExchangeRateApiService by lazy {
        createRetrofit(ApiConfig.EXCHANGE_RATE_BASE_URL).create(ExchangeRateApiService::class.java)
    }
}
