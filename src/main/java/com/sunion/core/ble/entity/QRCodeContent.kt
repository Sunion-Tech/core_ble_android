package com.sunion.core.ble.entity

import com.google.gson.annotations.SerializedName

data class QRCodeContent(
    @SerializedName("T") val t: String,
    @SerializedName("K") val k: String,
    @SerializedName("A") val a: String,
    @SerializedName("M") val m: String,
    @SerializedName("S") val s: String?,
    @SerializedName("F") val f: String?,
    @SerializedName("L") val l: String?,
)