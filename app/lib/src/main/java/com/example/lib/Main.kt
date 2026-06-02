package com.example.lib

import java.text.Normalizer

var carrosEncontrados: Int = 0

fun main() {
    lerCarros()
}

private fun lerCarros() {

    println("Iniciando processamento...")
    val carrosFipe = LeitorJson.carregarFipe()
    val carrosFiltrados = LeitorJson.carregarFiltrados().sortedBy { it.brand }

    println("Fipe: ${carrosFipe.size} registros")
    println("Filtrados: ${carrosFiltrados.size} registros")

    carrosFiltrados.forEach { filtrado ->
        val fipeMatches = carrosFipe.filter { it.brand.equals(filtrado.brand, true) }

        if(fipeMatches.isNotEmpty()) {
            processarCarro(filtrado, fipeMatches)
        }
    }
    println("Carros encontrados: $carrosEncontrados")
    println("Processamento finalizado.")
}

private fun processarCarro(filtrado: CarroFiltrado, brandFipeMatches: List<CarroFipe>) {
    val fipeCodes: MutableList<String> = mutableListOf()

    if (filtrado.model.contains(",")) {
        filtrado.model.split(",").forEach {
            fipeCodes.addAll(brandFipeMatches.pegarCodigosFipe(it, filtrado.yearStart, filtrado.yearEnd))
        }
    } else {
        fipeCodes.addAll(brandFipeMatches.pegarCodigosFipe(filtrado.model,filtrado.yearStart, filtrado.yearEnd))
    }

    filtrado.fipeCodes = fipeCodes
    if (fipeCodes.isNotEmpty()) {
        println("Encontrado ${fipeCodes.size} códigos para ${filtrado.brand}      ${filtrado.model}")
        carrosEncontrados++
    }
}

private fun List<CarroFipe>.pegarCodigosFipe(filtradoModelo: String, anoInicio: String, anoFim: String) : List<String> {
    val fipeCodes: MutableList<String> = mutableListOf()

    forEach { fipe ->
        if (fipe.model.hasAllCharacters(filtradoModelo)) {
            if(!fipeCodes.contains(fipe.id)) fipeCodes.add(fipe.id)
        }
    }
    return fipeCodes
}

fun String.hasAllCharacters(needed: String): Boolean {
    val containerCounts = lowercase().removeAccents().groupingBy { it }.eachCount()
    val neededCounts = needed.lowercase().removeAccents().groupingBy { it }.eachCount()

    return neededCounts.all { (char, count) ->
        containerCounts.getOrDefault(char, 0) >= count
    }
}

fun String.removeAccents(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
}