package com.service.api

import com.service.BuildConfig

object NetworkConfig {

    private const val API_HOST = BuildConfig.API_HOST

     val API_BASE_URL = "https://$API_HOST/api/"
     val WEBSOCKET_URL = "wss://$API_HOST/ws/"
}