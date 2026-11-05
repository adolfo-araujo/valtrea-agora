package br.com.valtrea.agora.ui.componentes

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", br)
private val formatoDia = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", br)
private val moeda = NumberFormat.getCurrencyInstance(br)

private fun String.local() = Instant.parse(this).atZone(ZoneId.systemDefault())

fun String.hora(): String = runCatching { formatoHora.format(local()) }.getOrDefault("")

/** "Hoje", "Amanhã" ou "quinta-feira, 12 de novembro". */
fun String.nomeDoDia(): String = runCatching {
    val dia = local().toLocalDate()
    val hoje = LocalDate.now()
    when (dia) {
        hoje -> "Hoje"
        hoje.plusDays(1) -> "Amanhã"
        else -> formatoDia.format(dia).replaceFirstChar { it.uppercase() }
    }
}.getOrDefault("")

fun String.quando(): String = "${nomeDoDia()}, ${hora()}"

fun preco(centavos: Int?): String? = centavos?.let { moeda.format(it / 100.0) }

fun detalhes(servico: String, duracaoMin: Int, precoCentavos: Int?): String =
    listOfNotNull(servico, "$duracaoMin min", preco(precoCentavos)).joinToString(" · ")

/** Agrupa uma lista já ordenada pelo horário em pares (nome do dia, itens). */
fun <T> List<T>.porDia(inicio: (T) -> String): List<Pair<String, List<T>>> =
    groupBy { inicio(it).nomeDoDia() }.toList()
