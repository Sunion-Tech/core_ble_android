package com.sunion.core.ble.entity

import com.google.gson.annotations.SerializedName

data class ProductionGetRequest(
    @SerializedName("code") val code:String,
    @SerializedName("clientToken") val clientToken: String? = null
)

data class ProductionGetResponse(
    @SerializedName("workOrderNumber") val workOrderNumber:String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("model") val model: String,
    @SerializedName("uuid") val uuid: String? = null,
    @SerializedName("token") val token: String,
    @SerializedName("gatewayToken") val gatewayToken: String? = null,
    @SerializedName("key") val key: String,
    @SerializedName("address") val address: String? = null,
    @SerializedName("broadcastName") val broadcastName: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("clientToken") val clientToken: String? = null
)

data class ProductionUpdateRequest(
    @SerializedName("model") val model: String,
    @SerializedName("token") val token: String,
    @SerializedName("gatewayToken") val gatewayToken: String? = null,
    @SerializedName("key") val key: String,
    @SerializedName("clientToken") val clientToken: String? = null
)

data class ProductionUpdateResponse(
    @SerializedName("code") val code:String,
    @SerializedName("clientToken") val clientToken: String? = null
)
