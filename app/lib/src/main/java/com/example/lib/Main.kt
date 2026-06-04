package com.example.lib

import com.example.lib.FuzzyMatcher.isAlikeTo
import java.text.Normalizer

var carrosEncontrados: Int = 0
val carrosFipe = LeitorJson.carregarFipe()
val carrosFiltrados = LeitorJson.carregarFiltrados().sortedBy { it.brand }

fun main() {
    lerCarros()
}

fun lerCarros(): MutableList<CarroFiltrado> {
    println("Iniciando processamento...")
    println("Fipe: ${carrosFipe.size} registros")
    println("Filtrados: ${carrosFiltrados.size} registros")

    val carros: MutableList<CarroFiltrado> = mutableListOf()

    carrosFiltrados.forEach { filtrado ->
        // Filtramos a FIPE pela marca, que já está normalizada
        val brandFipeMatches = carrosFipe.filter { it.brand.equals(filtrado.brand, true) }

        if (brandFipeMatches.isNotEmpty()) {
            processarCarro(filtrado, brandFipeMatches)?.let {
                carros.add(it)
            }
        }
    }
    return carros

    // Após processar todos, garantimos que cada código FIPE pertença apenas ao melhor match
    garantirCarrosUnicos(carros)

    println("Carros filtrados com algum código: ${carros.count { it.fipeCodes.isNotEmpty() }}")
    println("Total de códigos FIPE vinculados: ${carros.sumOf { it.fipeCodes.size }}")
    println("Processamento finalizado.")
}

private fun processarCarro(filtrado: CarroFiltrado, brandFipeMatches: List<CarroFipe>): CarroFiltrado? {
    val fipeCodes: MutableList<String> = mutableListOf()

    // Lida com modelos separados por vírgula
    if (filtrado.model.contains(",")) {
        filtrado.model.split(",").forEach { subModelo ->
            fipeCodes.addAll(
                brandFipeMatches.pegarCodigosFipe(subModelo.trim(), filtrado.yearStart, filtrado.yearEnd)
            )
        }
    } else {
        fipeCodes.addAll(
            brandFipeMatches.pegarCodigosFipe(filtrado.model, filtrado.yearStart, filtrado.yearEnd)
        )
    }

    filtrado.fipeCodes = fipeCodes.distinct()

    if (filtrado.fipeCodes.isNotEmpty()) {
        println("Encontrado ${filtrado.fipeCodes.size} códigos para ${filtrado.brand} ${filtrado.model} (${filtrado.id})")
        carrosEncontrados++
        return filtrado
    }
    return null
}

private fun List<CarroFipe>.pegarCodigosFipe(filtradoModelo: String, anoInicio: Int, anoFim: Int): List<String> {
    return this.filter { fipe ->
        // Validação de ano e de modelo flexível (token-based)
        fipe.verifyValidYear(anoInicio, anoFim) && fipe.model.isFlexibleMatch(filtradoModelo)
    }.map { it.id }
}

/**
 * Verifica se o modelo da FIPE contém todos os componentes do modelo filtrado de forma flexível.
 */
private fun String.isFlexibleMatch(otherString: String): Boolean {
    val fipeTokens = this.tokenize()
    val filtradoTokens = otherString.tokenize()

    if (filtradoTokens.isEmpty()) return false

    // Cada token do filtrado deve ser encontrado ou estar contido em algum token da FIPE
    return filtradoTokens.all { fToken ->
        fipeTokens.any { fipeToken ->
            // Match exato
            if (fipeToken == fToken) return@any true

            // Match de prefixo para letras (ex: "prem" -> "premium", "gran" -> "grand", "d" -> "diesel")
            if (fToken.all { it.isLetter() } && fipeToken.all { it.isLetter() }) {
                if (fipeToken.startsWith(fToken)) return@any true
            }

            // Match para números (ex: "116" filtrado deve bater com "116" em "116ia" fipe)
            // Note: tokenize() já separa 116ia em ["116", "ia"], então o match será exato no "116"

            false
        }
    }
}

/**
 * Tokenização inteligente:
 * - Remove pontos/vírgulas em números (6.000 -> 6000)
 * - Separa letras de números colados (116i -> 116 i)
 * - Remove acentos
 */
private fun String.tokenize(): List<String> {
    var text = this.lowercase().removeAccents()

    // Unifica números: "6.000" -> "6000"
    text = text.replace(Regex("(\\d)[.,](\\d)"), "$1$2")

    // Separa letras de números: "116i" -> "116 i", "D60" -> "D 60"
    text = text.replace(Regex("(\\d)([a-zA-Z])"), "$1 $2")
    text = text.replace(Regex("([a-zA-Z])(\\d)"), "$1 $2")

    val regex = Regex("([a-zA-Z]+|[0-9]+)")
    return regex.findAll(text)
        .map { it.value }
        .filter { it.isNotBlank() }
        .toList()
}

fun CarroFipe.verifyValidYear(anoInicio: Int, anoFim: Int): Boolean {
    return (anoInicio..anoFim).contains(year)
}

fun String.removeAccents(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
}

/**
 * Garante que cada código FIPE seja atribuído apenas ao carro filtrado que melhor se encaixa.
 */
private fun garantirCarrosUnicos(carros: List<CarroFiltrado>) {
    println("Garantindo exclusividade dos códigos FIPE...")
    val fipeById = carrosFipe.associateBy { it.id }
    val fipeToCarros = mutableMapOf<String, MutableList<CarroFiltrado>>()

    // Mapeia quais carros estão disputando cada código FIPE
    carros.forEach { carro ->
        carro.fipeCodes.forEach { code ->
            fipeToCarros.getOrPut(code) { mutableListOf() }.add(carro)
        }
    }

    fipeToCarros.forEach { (code, assignedCarros) ->
        if (assignedCarros.size > 1) {
            val fipe = fipeById[code] ?: return@forEach

            // Critério de desempate para encontrar o melhor match
            val bestCar = assignedCarros.maxByOrNull { carro ->
                val fipeModel = fipe.model.lowercase().removeAccents()
                val filtradoModel = carro.model.lowercase().removeAccents()
                
                // Score base do FuzzyMatcher
                val score = fipe.model.isAlikeTo(carro.model)
                
                // Bônus se o modelo filtrado for o início exato do modelo FIPE (ex: "RS Q3" em "RS Q3 2.5...")
                val startBonus = if (fipeModel.startsWith(filtradoModel)) 0.6 else 0.0
                
                // Bônus se o título completo do filtrado também for similar
                val titleScore = fipe.model.isAlikeTo(carro.title) * 0.4
                
                score + startBonus + titleScore
            }

            // Remove este código de todos os carros que perderam a disputa
            assignedCarros.forEach { carro ->
                if (carro != bestCar) {
                    carro.fipeCodes = carro.fipeCodes.filter { it != code }
                }
            }
        }
    }
}