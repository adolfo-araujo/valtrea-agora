package br.com.valtrea.agora.ui.negocio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.valtrea.agora.data.Estabelecimento
import br.com.valtrea.agora.data.EstabelecimentoRequest
import br.com.valtrea.agora.data.HorariosRequest
import br.com.valtrea.agora.data.Tipo
import br.com.valtrea.agora.ui.componentes.DialogoComEnvio
import br.com.valtrea.agora.ui.componentes.Escolha
import br.com.valtrea.agora.ui.componentes.UFS
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun Texto(rotulo: String, valor: String, max: Int, dica: String? = null, teclado: KeyboardType = KeyboardType.Text, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= max) aoMudar(it) },
        label = { Text(rotulo) },
        supportingText = if (dica != null) { { Text(dica) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = max <= 160,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Cadastro e edição do estabelecimento. [inicial] nulo = novo. */
@Composable
fun DialogoEstabelecimento(inicial: Estabelecimento?, aoCancelar: () -> Unit, aoSalvar: suspend (EstabelecimentoRequest) -> Unit) {
    var nome by remember { mutableStateOf(inicial?.nome ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: Tipo.SALAO) }
    var endereco by remember { mutableStateOf(inicial?.endereco ?: "") }
    var bairro by remember { mutableStateOf(inicial?.bairro ?: "") }
    var cidade by remember { mutableStateOf(inicial?.cidade ?: "") }
    var uf by remember { mutableStateOf(inicial?.uf ?: "RJ") }
    var telefone by remember { mutableStateOf(inicial?.telefone ?: "") }
    var descricao by remember { mutableStateOf(inicial?.descricao ?: "") }
    val pronto = nome.trim().length >= 2 && endereco.trim().length >= 5 && bairro.trim().length >= 2 &&
        cidade.trim().length >= 2 && telefone.count { it.isDigit() } >= 8
    DialogoComEnvio(
        titulo = if (inicial == null) "Novo estabelecimento" else "Editar estabelecimento",
        confirmar = if (inicial == null) "Enviar para análise" else "Salvar",
        podeEnviar = pronto,
        aoCancelar = aoCancelar,
        enviar = {
            aoSalvar(
                EstabelecimentoRequest(nome.trim(), tipo, descricao.trim().ifBlank { null }, endereco.trim(), bairro.trim(),
                    cidade.trim(), uf, telefone.trim()),
            )
        },
    ) {
        Texto("Nome do estabelecimento", nome, 80) { nome = it }
        Escolha("Tipo", Tipo.entries, tipo, { it.rotulo }) { tipo = it }
        Texto("Endereço (rua e número)", endereco, 160) { endereco = it }
        Texto("Bairro", bairro, 80) { bairro = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = cidade,
                onValueChange = { if (it.length <= 80) cidade = it },
                label = { Text("Cidade") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Escolha("Estado", UFS, uf, { it }, Modifier.width(110.dp)) { uf = it }
        }
        Texto("Telefone do estabelecimento", telefone, 20, "Com DDD. Aparece para os clientes.", KeyboardType.Phone) { telefone = it }
        Texto("Descrição (opcional)", descricao, 500, "Serviços e diferenciais. Sem pedir pagamento antecipado.") { descricao = it }
        if (inicial == null) {
            Text(
                "Todo cadastro passa por análise antes de aparecer. Avisamos por e-mail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val formatoDia = DateTimeFormatter.ofPattern("EEE, dd/MM", Locale.forLanguageTag("pt-BR"))
private val HORA = Regex("^(\\d{1,2})[:h](\\d{2})$")

/** Converte "14:00, 15:30" do dia escolhido em instantes (no fuso do celular). Nulo se algum estiver errado. */
fun inicios(dia: LocalDate, texto: String): List<String>? {
    val partes = texto.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
    return partes.map { parte ->
        val m = HORA.matchEntire(parte) ?: return null
        val (h, min) = m.destructured
        if (h.toInt() > 23 || min.toInt() > 59) return null
        dia.atTime(h.toInt(), min.toInt()).atZone(ZoneId.systemDefault()).toInstant().toString()
    }
}

@Composable
fun DialogoHorarios(aoCancelar: () -> Unit, aoPublicar: suspend (HorariosRequest) -> Unit) {
    val dias = remember { (0L..13L).map { LocalDate.now().plusDays(it) } }
    var dia by remember { mutableStateOf(dias.first()) }
    var horas by remember { mutableStateOf("") }
    var servico by remember { mutableStateOf("") }
    var duracao by remember { mutableStateOf("30") }
    var valor by remember { mutableStateOf("") }
    val lista = inicios(dia, horas)
    val centavos = valor.trim().takeIf { it.isNotEmpty() }?.replace(".", "")?.replace(',', '.')?.toDoubleOrNull()?.let { (it * 100).toInt() }
    val minutos = duracao.toIntOrNull()
    DialogoComEnvio(
        titulo = "Publicar horários vagos",
        confirmar = if ((lista?.size ?: 0) > 1) "Publicar ${lista!!.size}" else "Publicar",
        podeEnviar = !lista.isNullOrEmpty() && servico.trim().length >= 2 && minutos != null && minutos in 10..480 &&
            (valor.isBlank() || centavos != null),
        aoCancelar = aoCancelar,
        enviar = { aoPublicar(HorariosRequest(servico.trim(), minutos!!, centavos, lista!!)) },
    ) {
        Escolha("Dia", dias, dia, { if (it == dias.first()) "Hoje" else if (it == dias[1]) "Amanhã" else formatoDia.format(it) }) { dia = it }
        Texto("Horários vagos", horas, 200, "Separe por vírgula: 14:00, 15:30. Até 20 de cada vez.") { horas = it }
        if (lista == null && horas.isNotBlank()) {
            Text("Escreva os horários assim: 14:00, 15:30", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Texto("Serviço", servico, 80, "Ex.: Corte masculino") { servico = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = duracao,
                onValueChange = { if (it.length <= 3) duracao = it.filter(Char::isDigit) },
                label = { Text("Duração (min)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = valor,
                onValueChange = { if (it.length <= 10) valor = it },
                label = { Text("Preço (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
