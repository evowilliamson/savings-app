package com.khaohom.savings.data.remote

/**
 * API Configuration
 * 
 * IMPORTANT: Update BASE_URL with your Railway backend URL before building the app
 */
object ApiConfig {
    // Railway backend URL
    const val BASE_URL = "https://savings-app-production.up.railway.app/"
    
    // External API endpoints
    const val COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/"
    const val EXCHANGE_RATE_BASE_URL = "https://api.exchangerate-api.com/v4/"
    
    // Timeouts
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
