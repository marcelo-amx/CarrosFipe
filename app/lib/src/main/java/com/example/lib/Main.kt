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

    val carros1: MutableList<CarroFiltrado> = mutableListOf()
    val carros2: MutableList<CarroFiltrado> = mutableListOf()

    carrosFiltrados.forEach { filtrado ->
        val fipeMatches = carrosFipe.filter { it.brand.equals(filtrado.brand, true) }
        if(fipeMatches.isNotEmpty()) {
            processarCarro(filtrado, fipeMatches, true)?.let { carros1.add(it) }
            processarCarro(filtrado, fipeMatches, false)?.let { carros2.add(it) }
        }
    }

    carros1.forEach {
        if(!carros2.contains(it)) {
            println("Encontrado  códigos para ${it.brand}           ${it.model}.   ${it.title}.     ${it.id}")
        }
    }

    println("Carros encontrados: $carrosEncontrados")
    println("Processamento finalizado.")
}

private fun processarCarro(filtrado: CarroFiltrado, brandFipeMatches: List<CarroFipe>, lala: Boolean): CarroFiltrado? {
    val fipeCodes: MutableList<String> = mutableListOf()

    if (filtrado.model.contains(",")) {
        filtrado.model.split(",").forEach {
            fipeCodes.addAll(brandFipeMatches.pegarCodigosFipe(it, filtrado.yearStart, filtrado.yearEnd, lala))
        }
    } else {
        fipeCodes.addAll(brandFipeMatches.pegarCodigosFipe(filtrado.model,filtrado.yearStart, filtrado.yearEnd, lala))
    }

    filtrado.fipeCodes = fipeCodes
    if (fipeCodes.isNotEmpty()) {
        println("Encontrado ${fipeCodes.size} códigos para ${filtrado.brand}           ${filtrado.model}.   ${filtrado.title}.     ${filtrado.id}")
        carrosEncontrados++
        return filtrado
    }
    return null
}

private fun List<CarroFipe>.pegarCodigosFipe(
    filtradoModelo: String,
    anoInicio: Int,
    anoFim: Int,
    lala: Boolean
) : List<String> {
    val fipeCodes: MutableList<String> = mutableListOf()

    forEach { fipe ->
        if (fipe.model.hasAllCharacters(filtradoModelo)) {
            val isYearValidForCar = fipe.verifyValidYear(anoInicio, anoFim)
            if(!fipeCodes.contains(fipe.id) && isYearValidForCar) fipeCodes.add(fipe.id)
        }
    }
    return fipeCodes
}

fun CarroFipe.verifyValidYear( anoInicio: Int, anoFim: Int): Boolean {
    val isYearValidForCar = (anoInicio..anoFim).contains(year)
    return isYearValidForCar
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