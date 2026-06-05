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

    // Após processar todos, garantimos que cada código FIPE pertença apenas ao melhor match
    garantirCarrosUnicos(carros)

    println("Carros filtrados com algum código: ${carros.count { it.fipeCodes.isNotEmpty() }}")
    println("Total de códigos FIPE vinculados: ${carros.sumOf { it.fipeCodes.size }}")
    println("Processamento finalizado.")
    return carros
}

private fun processarCarro(filtrado: CarroFiltrado, brandFipeMatches: List<CarroFipe>): CarroFiltrado? {
    val fipeCodes: MutableList<CarroFipe> = mutableListOf()

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

    filtrado.fipeCodes = fipeCodes.distinctBy { it.id }

    if (filtrado.fipeCodes.isNotEmpty()) {
        println("Encontrado ${filtrado.fipeCodes.size} códigos para ${filtrado.brand} ${filtrado.model} (${filtrado.id})")
        carrosEncontrados++
        return filtrado
    }
    return null
}

private fun List<CarroFipe>.pegarCodigosFipe(filtradoModelo: String, anoInicio: Int, anoFim: Int): List<CarroFipe> {
    return this.filter { fipe ->
        // Validação de ano e de modelo flexível (token-based)
        fipe.verifyValidYear(anoInicio, anoFim) && fipe.model.isFlexibleMatch(filtradoModelo)
    }
}

/**
 * Verifica se o modelo da FIPE contém todos os componentes do modelo filtrado de forma flexível.
 */
private fun String.isFlexibleMatch(otherString: String): Boolean {
    val fipeTokens = this.tokenize()
    val filtradoTokens = otherString.tokenize()

    if (filtradoTokens.isEmpty()) return false

    val fipeCombined = fipeTokens.joinToString("")
    val filtradoCombined = filtradoTokens.joinToString("")

    // 1. Regra para identificadores alfanuméricos curtos (ex: A3, S3, Q3, X5)
    // Agora que a tokenização mantém "a3" unido, verificamos se ele existe como token ou prefixo forte
    val hasAlphanumericShort = filtradoTokens.any { it.length <= 3 && it.any { c -> c.isDigit() } && it.any { c -> c.isLetter() } }
    if (hasAlphanumericShort) {
        val allTokensMatchStrictly = filtradoTokens.all { fToken ->
            fipeTokens.any { fipeToken ->
                fipeToken == fToken || (fipeToken.startsWith(fToken) && fToken.length >= 2)
            }
        }
        if (allTokensMatchStrictly) return true
        // Se falhou no match estrito de ID, não deixa passar para o genérico se for muito curto
        if (filtradoCombined.length <= 3) return false
    }

    // 2. Se os modelos são idênticos ou um contém o outro quando normalizados
    if (fipeCombined.contains(filtradoCombined) || filtradoCombined.contains(fipeCombined)) return true

    // 3. Verificação de cada componente do modelo filtrado individualmente
    return filtradoTokens.all { fToken ->
        // Match exato com algum token da Fipe
        if (fipeTokens.contains(fToken)) return@all true

        // Match de prefixo em AMBAS as direções (ex: "constel" vs "constellation")
        if (fipeTokens.any { fipeToken ->
            val isPrefix = fipeToken.startsWith(fToken) || fToken.startsWith(fipeToken)
            // Só aceita prefixo se for significativo (>= 3 chars) ou se forem números
            isPrefix && (fToken.length >= 3 || fipeToken.length >= 3 || fToken.all { it.isDigit() })
        }) return@all true

        // Match em palavra composta (ex: "cargo" em "eurocargo") ou tokens separados
        if (fToken.length >= 2 && fipeCombined.contains(fToken)) return@all true

        false
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

    // Removemos a separação forçada de letras e números para manter IDs como "a3", "q3", "116i" e "3p" íntegros.
    // O regex abaixo agora captura sequências alfanuméricas completas.
    val regex = Regex("([a-z0-9]+)")
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
    val fipeToCarros = mutableMapOf<CarroFipe, MutableList<CarroFiltrado>>()

    // Mapeia quais carros estão disputando cada código FIPE
    carros.forEach { carro ->
        carro.fipeCodes.forEach { code ->
            fipeToCarros.getOrPut(code) { mutableListOf() }.add(carro)
        }
    }

    fipeToCarros.forEach { (code, assignedCarros) ->
        if (assignedCarros.size > 1) {
            val fipe = fipeById[code.id] ?: return@forEach

            // Critério de desempate para encontrar o melhor match
            val bestCar = assignedCarros.maxByOrNull { carro ->
                val fipeModel = fipe.model.lowercase().removeAccents()
                val filtradoModel = carro.model.lowercase().removeAccents()
                val fipeTokens = fipe.model.tokenize()
                val filtradoTokens = carro.model.tokenize()

                // Score base do FuzzyMatcher
                var score = fipe.model.isAlikeTo(carro.model)

                // Bônus se o modelo filtrado for o início exato do modelo FIPE (ex: "RS Q3" em "RS Q3 2.5...")
                if (fipeModel.startsWith(filtradoModel)) score += 0.6

                // 1. Bônus por densidade de informação:
                // Quanto mais tokens o modelo filtrado tem em comum com a FIPE, melhor.
                // Isso faz o "Tiggo 7 Pro" (3 tokens) ganhar do "Tiggo 7" (2 tokens)
                val matchingTokensCount = filtradoTokens.count { ft -> fipeTokens.contains(ft) }
                score += (matchingTokensCount * 1.0)

                // 2. Bônus de Match Completo:
                // Se o modelo filtrado foi totalmente encontrado na FIPE, ganha bônus extra.
                if (matchingTokensCount == filtradoTokens.size && filtradoTokens.isNotEmpty()) {
                    score += 2.0
                }

                // Penalidade para "parte de token": Se o modelo filtrado é apenas uma parte de um token FIPE
                // (Ex: Filtrado "S3" é parte do token "RS3" da FIPE).
                val isPartialMatch = filtradoTokens.any { ft -> fipeTokens.any { it.contains(ft) && it != ft } }
                if (isPartialMatch) score -= 0.5

                // 3. Penalidade de "Tokens Sobrando" na FIPE:
                // Se a FIPE tem muitos detalhes que o modelo filtrado não capturou, o score cai.
                // O "Tiggo 7 Pro" deixará menos tokens sobrando na FIPE do que o "Tiggo 7".
                val unmatchedFipeTokens = fipeTokens.size - matchingTokensCount
                score -= (unmatchedFipeTokens * 0.2)

                // Bônus se o título completo do filtrado também for similar
                val titleScore = fipe.model.isAlikeTo(carro.title) * 0.4
                
                score + titleScore
            }

            // Remove este código de todos os carros que perderam a disputa
            assignedCarros.forEach { carro ->
                if (carro != bestCar) {
                    carro.fipeCodes = carro.fipeCodes.filter { it.id != code.id }
                }
            }
        }
    }
}