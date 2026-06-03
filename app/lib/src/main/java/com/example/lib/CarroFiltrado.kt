package com.example.lib

import com.google.gson.annotations.SerializedName
import java.util.Date

data class CarroFiltrado(
    @SerializedName("id_wp") val id: Int,
    @SerializedName("titulo_atual") val title: String,
    @SerializedName("marca_atual") val brand: String,
    @SerializedName("modelo_atual") val model: String,
    @SerializedName("ano_inicio_atual") val nullableYearStart: Int?,
    @SerializedName("ano_fim_atual") val nullableYearEnd: Int?,
    @SerializedName("slug") val slug: String,
    @SerializedName("fipe_codes") var fipeCodes: List<String> = emptyList(),
) {

    val yearStart get() = nullableYearStart ?: 0
    val yearEnd get() = if(nullableYearEnd == null || nullableYearEnd == 0) 2026 else nullableYearEnd
}