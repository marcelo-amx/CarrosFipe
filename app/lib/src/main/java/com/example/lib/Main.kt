package com.example.lib

var zeroCount: Int = 0

fun main() {
    lerCarros()
}

private fun lerCarros() {

    println("Iniciando processamento...")
    val carrosFipe = LeitorJson.carregarFipe()
    val carrosFiltrados = LeitorJson.carregarFiltrados()

    println("Fipe: ${carrosFipe.size} registros")
    println("Filtrados: ${carrosFiltrados.size} registros")

    carrosFiltrados.forEach { filtrado ->
        val fipeMatches = carrosFipe.filter { it.brand.equals(filtrado.brand, true) }

        if(fipeMatches.isNotEmpty()) {
            processarCarro(filtrado, fipeMatches)
        }
    }
    println("Sem nada: $zeroCount")
    println("Processamento finalizado.")
}

private fun processarCarro(filtrado: CarroFiltrado, brancFipeMatches: List<CarroFipe>) {
    val fipeCodes: MutableList<String> = mutableListOf()
    brancFipeMatches.forEach { fipe ->
        if (fipe.model.hasAllCharacters(filtrado.model)) {
            fipeCodes.add(fipe.id)
        }
    }

    filtrado.fipeCodes = fipeCodes
    if (fipeCodes.isEmpty()) {
        println("Encontrado ${fipeCodes.size} códigos para ${filtrado.brand}      ${filtrado.model}")
        if(!filtrado.model.contains(",")) zeroCount++
    }
}

fun String.hasAllCharacters(needed: String): Boolean {
    val containerCounts = lowercase().groupingBy { it }.eachCount()
    val neededCounts = needed.lowercase().groupingBy { it }.eachCount()

    return neededCounts.all { (char, count) ->
        containerCounts.getOrDefault(char, 0) >= count
    }
}