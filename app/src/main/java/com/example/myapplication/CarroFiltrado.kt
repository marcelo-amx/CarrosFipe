package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class CarroFiltrado(
    @SerializedName("id_wp") val id: Int,
    @SerializedName("titulo_atual") val title: String,
    @SerializedName("marca_atual") val brand: String,
    @SerializedName("modelo_atual") val model: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("fipe_codes") var fipeCodes: List<String> = emptyList(),
)