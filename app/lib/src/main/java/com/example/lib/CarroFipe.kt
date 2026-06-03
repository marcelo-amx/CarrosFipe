package com.example.lib

import com.google.gson.annotations.SerializedName

data class CarroFipe(
    @SerializedName("fipeCode") val id: String,
    @SerializedName("modelCode") val code: String,
    @SerializedName("brandValue") val brand: String,
    @SerializedName("modelValue") val model: String,
    @SerializedName("yearCode") val yearCode: String
) {

    val year get() = yearCode.split("-").first().toInt()
}