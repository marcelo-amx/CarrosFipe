package com.example.lib

import com.example.lib.FuzzyMatcher.isAlikeTo

fun main() {
    val antigo = lerCarros1()
    val novo = lerCarros()

    antigo.subtract(novo.toSet()).apply {
        println("ZERADOS: $size")
        forEach {
            println("${it.brand}-${it.model} - ${it.id}")
        }
    }
}

private fun lerCarros1(): MutableList<CarroFiltrado> {

    println("Iniciando processamento...")

    println("Fipe: ${carrosFipe.size} registros")
    println("Filtrados: ${carrosFiltrados.size} registros")

    val carros: MutableList<CarroFiltrado> = mutableListOf()

    carrosFiltrados.forEach { filtrado ->
        val fipeMatches = carrosFipe.filter { it.brand.equals(filtrado.brand, true) }
        if (fipeMatches.isNotEmpty()) {
            processarCarro(filtrado, fipeMatches)?.let { carros.add(it) }
        }
    }

    println("Carros encontrados: ${carros.size}")
    println("Processamento finalizado.")
    return carros
}

private fun processarCarro(filtrado: CarroFiltrado, brandFipeMatches: List<CarroFipe>): CarroFiltrado? {

    val fipeCodes: MutableList<CarroFipe> = mutableListOf()

    if (filtrado.model.contains(",")) {
        filtrado.model.split(",").forEach {
            fipeCodes.addAll(
                brandFipeMatches.pegarCodigosFipe(
                    it, filtrado.yearStart, filtrado.yearEnd,
                )
            )
        }
    } else {
        fipeCodes.addAll(
            brandFipeMatches.pegarCodigosFipe(
                filtrado.model, filtrado.yearStart, filtrado.yearEnd
            )
        )
    }

    filtrado.fipeCars = fipeCodes
    if (fipeCodes.isNotEmpty()) {
        println("Encontrado ${fipeCodes.size} códigos para ${filtrado.brand}           ${filtrado.model}.   ${filtrado.title}.     ${filtrado.id}")
        carrosEncontrados++
        return filtrado
    }
    return null
}

private fun List<CarroFipe>.pegarCodigosFipe(filtradoModelo: String, anoInicio: Int, anoFim: Int): List<CarroFipe> {
    val fipeCodes: MutableList<CarroFipe> = mutableListOf()

    forEach { fipe ->
        val isYearValidForCar = fipe.verifyValidYear(anoInicio, anoFim)
        if (fipe.model.hasAllCharacters(filtradoModelo)) {
            if ((fipeCodes.find { it.id == fipe.id } == null) && isYearValidForCar) fipeCodes.add(fipe)
        }
    }
    return fipeCodes
}

private fun String.containsOrder(otherString: String): Boolean {
    val lettersOnlyPrimary =
        lowercase().removeAccents().filter { it.isLetter() }
    val lettersOnlySecondary =
        otherString.lowercase().removeAccents().filter { it.isLetter() }

    val numbersOnlyPrimary = lowercase().removeAccents().filter { it.isDigit() }
    val numbersOnlySecondary = otherString.lowercase().removeAccents().filter { it.isDigit() }

    val containsNumberOrder = numbersOnlyPrimary.contains(numbersOnlySecondary)
    val containsLettersOrder = lettersOnlyPrimary.contains(lettersOnlySecondary)

    return containsNumberOrder || containsLettersOrder
}

fun String.hasAllCharacters(needed: String): Boolean {
    val containerCounts = lowercase().removeAccents().groupingBy { it }.eachCount()
    val neededCounts = needed.lowercase().removeAccents().groupingBy { it }.eachCount()

    return neededCounts.all { (char, count) ->
        containerCounts.getOrDefault(char, 0) >= count
    }
}


private fun garantirCarrosUnicos(carros: MutableList<CarroFiltrado>) {
    carros.forEach { carro1 ->
        carros.forEach { carro2 ->
            if (carro1.id != carro2.id) {
                val commonFipeCodes = carro1.fipeCars.intersect(carro2.fipeCars.toSet())
                if (commonFipeCodes.isNotEmpty()) {
                    commonFipeCodes.forEach { code ->
                        carrosFipe.find { it.id == code.id }?.let { fipe ->
                            var score1 = fipe.model.isAlikeTo(carro1.model)
                            var score2 = fipe.model.isAlikeTo(carro2.model)

                            if (!fipe.verifyValidYear(carro1.yearStart, carro1.yearEnd)) score1 =
                                0.0
                            if (!fipe.verifyValidYear(carro2.yearStart, carro2.yearEnd)) score2 =
                                0.0

                            if (score1 >= score2) {
                                carro2.fipeCars =
                                    carro2.fipeCars.toMutableList().apply { remove(code) }
                            } else {
                                carro1.fipeCars =
                                    carro1.fipeCars.toMutableList().apply { remove(code) }
                            }
                        }
                    }
                }
            }
        }
        if (carro1.fipeCars.isEmpty()) {
            println("Zerado  ${carro1.brand}           ${carro1.model}.   ${carro1.title}.     ${carro1.id}")
        }
    }
}