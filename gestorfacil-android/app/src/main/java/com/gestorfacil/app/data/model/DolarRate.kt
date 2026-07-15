package com.gestorfacil.app.data.model

import com.google.gson.annotations.SerializedName

data class DolarResponse(
    @SerializedName("moneda") val moneda: String,
    @SerializedName("fuente") val fuente: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("compra") val compra: Double?,
    @SerializedName("venta") val venta: Double?,
    @SerializedName("promedio") val promedio: Double,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String
)

data class BolivarRate(
    val oficial: Double,
    val paralelo: Double,
    val binance: BinanceRate? = null,
    val euro: Double = 0.0,
    val lastUpdated: String
)

data class BinanceRate(
    val compra: Double,
    val venta: Double,
    val promedio: Double
)

data class BinanceAd(
    @SerializedName("adv") val adv: BinanceAdv
)

data class BinanceAdv(
    @SerializedName("price") val price: String,
    @SerializedName("tradeType") val tradeType: String
)

data class BinanceResponse(
    @SerializedName("data") val data: List<BinanceAd>,
    @SerializedName("success") val success: Boolean
)

// Al Cambio GraphQL
data class AlCambioQuery(
    val query: String
)

data class AlCambioResponse(
    val data: AlCambioData?
)

data class AlCambioData(
    val getBinanceP2PAverages: AlCambioBinanceAverages?,
    val getCountryConversions: AlCambioConversions?
)

data class AlCambioBinanceAverages(
    val sellAverage: Double,
    val buyAverage: Double
)

data class AlCambioConversions(
    val conversionRates: List<AlCambioConversionRate>
)

data class AlCambioConversionRate(
    val type: String,
    val official: Boolean,
    val baseValue: Double,
    val rateCurrency: AlCambioCurrency
)

data class AlCambioCurrency(
    val code: String
)
