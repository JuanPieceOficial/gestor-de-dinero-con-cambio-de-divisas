package com.cryptowallet.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiHttpClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val client: HttpClient by lazy {
        HttpClient(Android) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }
}
