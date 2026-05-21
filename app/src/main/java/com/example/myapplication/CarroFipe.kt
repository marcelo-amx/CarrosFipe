package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class CarroFipe(
    @SerializedName("fipeCode") val id: String,
    @SerializedName("modelCode") val code: String,
    @SerializedName("brandValue") val brand: String,
    @SerializedName("modelValue") val model: String,
)