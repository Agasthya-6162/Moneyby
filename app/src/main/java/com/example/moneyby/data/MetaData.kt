package com.example.moneyby.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MetaData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String,
    val deviceName: String = android.os.Build.MODEL
)
