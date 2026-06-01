package com.example.lib

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
    println("Processamento finalizado.")
}

private fun processarCarro(filtrado: CarroFiltrado, fipeMatches: List<CarroFipe>) {
    val fipeCodes: MutableList<String> = mutableListOf()

    fipeMatches.forEach { fipe ->
        if (fipe.model.contains(filtrado.model, ignoreCase = true)) {
            fipeCodes.add(fipe.id)
        }
    }
    filtrado.fipeCodes = fipeCodes
    if (fipeCodes.isEmpty()) {
        println("Encontrado ${fipeCodes.size} códigos para ${filtrado.brand} ${filtrado.model}")
    }
}