package com.example.lib

import com.example.lib.FuzzyMatcher.isAlikeTo
import java.text.Normalizer

var carrosEncontrados: Int = 0
val carrosFipe = LeitorJson.carregarFipe()
val carrosFiltrados = LeitorJson.carregarFiltrados().sortedBy { it.brand }

fun main() {
    val carros = lerCarros()
    carros.forEach {
        it.fipeCodes = it.fipeCars.map { it.id }
    }
    LeitorJson.salvarFiltrados(carros)
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

    println("Carros filtrados com algum código: ${carros.count { it.fipeCars.isNotEmpty() }}")
    println("Total de códigos FIPE vinculados: ${carros.sumOf { it.fipeCars.size }}")
    println("Processamento finalizado.")
    //Verificar esses casos
    // Verificar também o Audi TT e BMW M135i
    val x = carros.filter { it.fipeCars.isEmpty() }.map { it.model }
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

    filtrado.fipeCars = fipeCodes.distinctBy { it.id }

    if (filtrado.fipeCars.isNotEmpty()) {
        println("Encontrado ${filtrado.fipeCars.size} códigos para ${filtrado.brand} ${filtrado.model} (${filtrado.id})")
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

    // 1. Regra para identificadores alfanuméricos (ex: A3, S3, Q3, X5, M135i)
    // Exigimos match estrito ou de prefixo longo para evitar que "M" bata em "M135i"
    val hasAlphanumeric = filtradoTokens.any { it.length <= 6 && it.any { c -> c.isDigit() } && it.any { c -> c.isLetter() } }
    if (hasAlphanumeric) {
        val allTokensMatchStrictly = filtradoTokens.all { fToken ->
            fipeTokens.any { fipeToken ->
                fipeToken == fToken || (fipeToken.startsWith(fToken) && fToken.length >= 2)
            }
        }
        if (allTokensMatchStrictly) return true
        // Se falhou no match estrito de ID, não deixa passar para o genérico se for um modelo curto
        if (filtradoCombined.length <= 5) return false
    }

    // 2. Se os modelos são idênticos ou um contém o outro quando normalizados
    // Bloqueamos strings muito curtas aqui para evitar falsos positivos como "tt" em "quattro"
    if (filtradoCombined.length > 2 && (fipeCombined.contains(filtradoCombined) || filtradoCombined.contains(fipeCombined))) return true

    // 3. Verificação de cada componente do modelo filtrado individualmente
    return filtradoTokens.all { fToken ->
        // Match exato com algum token da Fipe
        if (fipeTokens.contains(fToken)) return@all true

        // Match de prefixo
        if (fipeTokens.any { fipeToken ->
            // Caso 1: Fipe tem o termo mais completo (ex: filtrado "116" -> fipe "116i")
            if (fipeToken.startsWith(fToken) && (fToken.length >= 3 || fToken.all { it.isDigit() })) return@any true
            
            // Caso 2: Filtrado tem o termo mais completo (ex: filtrado "constellation" -> fipe "constel")
            // Bloqueia filtrado "m135i" -> fipe "m" exigindo que o prefixo da fipe seja significativo
            if (fToken.startsWith(fipeToken) && fipeToken.length >= 3) return@any true
            
            false
        }) return@all true

        // Match em palavra composta (ex: "cargo" em "eurocargo") ou tokens separados
        if (fToken.length >= 2 && fipeCombined.contains(fToken)) {
            // Proteção contra falsos positivos de 2 letras (ex: "tt" em "quattro")
            if (fToken.length == 2) {
                // Se o token de 2 letras é o início de algum token da Fipe (ex: "A3" em "A3sb"), aceitamos.
                val isPrefixOfAny = fipeTokens.any { it.startsWith(fToken) }
                if (isPrefixOfAny) return@all true
                
                // Se o token está "escondido" no meio de uma única palavra da Fipe e não é o início dela,
                // recusamos (ex: "tt" dentro de "quattro").
                val isInternalToAny = fipeTokens.any { it.contains(fToken) && !it.startsWith(fToken) }
                if (isInternalToAny) return@all false
                
                // Se não é interno a nenhuma palavra mas está no combined, é uma junção (ex: "s"+"s" = "ss"), aceitamos.
            }
            return@all true
        }

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

fun String.normalize(): String {
    return lowercase().removeAccents().replace("-", " ")
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
        carro.fipeCars.forEach { code ->
            fipeToCarros.getOrPut(code) { mutableListOf() }.add(carro)
        }
    }

    fipeToCarros.forEach { (code, assignedCarros) ->
        if (assignedCarros.size > 1) {
            val fipe = fipeById[code.id] ?: return@forEach

            // Critério de desempate para encontrar o melhor match, considerando submodelos separados por vírgula
            val winningResult = assignedCarros.flatMap { carro ->
                val subModels = if (carro.model.contains(",")) carro.model.split(",").map { it.trim() } else listOf(carro.model)
                subModels.map { subModel ->
                    val fipeModel = fipe.model.lowercase().removeAccents()
                    val filtradoModel = subModel.lowercase().removeAccents()
                    val fipeTokens = fipe.model.tokenize()
                    val filtradoTokens = subModel.tokenize()

                    // Score base do FuzzyMatcher
                    var score = fipe.model.isAlikeTo(subModel)

                    // Bônus se o modelo filtrado for o início exato do modelo FIPE (ex: "RS Q3" em "RS Q3 2.5...")
                    if (fipeModel.startsWith(filtradoModel)) score += 0.4

                    // 1. Bônus por densidade de informação:
                    // Quanto mais tokens o modelo filtrado tem em comum com a FIPE, melhor.
                    // Agora aceita matches parciais (prefixos) para lidar com "s44" vs "s44t"
                    val matchingTokensCount = filtradoTokens.count { ft -> 
                        fipeTokens.any { fipeToken -> 
                            fipeToken == ft || 
                            (ft.length >= 3 && fipeToken.startsWith(ft)) || 
                            (fipeToken.length >= 3 && ft.startsWith(fipeToken))
                        }
                    }
                    score += (matchingTokensCount * 1.5)

                    // 2. Bônus de Especificidade: 
                    // Modelos mais detalhados (mais tokens) tendem a ser melhores matches se os tokens batem.
                    score += (filtradoTokens.size * 0.4)

                    // 3. Bônus para Palavras-Chave Técnicas (Hib, Diesel, Turbo, etc)
                    // Se o carro filtrado especifica algo técnico que a FIPE tem, ele é o match correto.
                    val technicalKeywords = listOf("hib", "hibrido", "hybrid", "diesel", "turbo", "flex", "eletrico", "electric", "4x4", "quattro")
                    filtradoTokens.forEach { ft ->
                        if (technicalKeywords.contains(ft) && fipeTokens.contains(ft)) {
                            score += 2.0
                        }
                    }

                    // 3. Bônus de Match Completo:
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
                    
                    Triple(carro, subModel, score + titleScore)
                }
            }.maxByOrNull { it.third }

            // Remove este código de todos os carros que possuem um modelo diferente do vencedor.
            // Isso permite que anos diferentes do MESMO modelo (ex: Gol 2004 e Gol 2005) 
            // mantenham o mesmo código FIPE, mas separa modelos distintos (ex: Tiggo 7 e Tiggo 7 Pro).
            winningResult?.let { (_, bestSubModel, _) ->
                val normalizedBestModel = bestSubModel.normalize()
                assignedCarros.forEach { carro ->
                    val carSubModels = if (carro.model.contains(",")) carro.model.split(",").map { it.trim() } else listOf(carro.model)
                    val hasSameModel = carSubModels.any { it.normalize() == normalizedBestModel }
                    if (!hasSameModel) {
                        carro.fipeCars = carro.fipeCars.filter { it.id != code.id }
                    }
                }
            }
        }
    }
}