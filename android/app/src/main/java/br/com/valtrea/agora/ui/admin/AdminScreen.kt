package br.com.valtrea.agora.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.MetricasAdmin
import br.com.valtrea.agora.data.ParaAnalisar
import br.com.valtrea.agora.data.RecusaRequest
import br.com.valtrea.agora.data.SemanaAdmin
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.componentes.BotaoVoltar
import br.com.valtrea.agora.ui.valtreaViewModel
import kotlinx.coroutines.launch

class AdminViewModel(private val api: ValtreaApi) : ViewModel() {
    var metricas by mutableStateOf<MetricasAdmin?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var fila by mutableStateOf<List<ParaAnalisar>?>(null)
        private set
    var avisoAnalise by mutableStateOf<String?>(null)
        private set

    /** [motivo] nulo = aprovar. */
    fun decidir(item: ParaAnalisar, motivo: String?) {
        viewModelScope.launch {
            runCatching { if (motivo == null) api.aprovar(item.id) else api.recusar(item.id, RecusaRequest(motivo)) }
                .onSuccess {
                    fila = fila?.filterNot { it.id == item.id }
                    avisoAnalise = if (motivo == null) "\"${item.nome}\" aprovado: já aparece na busca."
                    else "\"${item.nome}\" recusado. O dono foi avisado."
                }
                .onFailure { avisoAnalise = it.mensagemAmigavel() }
        }
    }

    fun carregar() {
        if (carregando) return
        carregando = true
        erro = null
        viewModelScope.launch {
            runCatching { api.metricasAdmin() }
                .onSuccess { metricas = it }
                .onFailure { erro = it.mensagemAmigavel() }
            runCatching { api.filaAnalise() }.onSuccess { fila = it }
            carregando = false
        }
    }
}

/** Painel de administração: números de uso agregados. Só aparece para contas de administração. */
@Composable
fun AdminScreen(aoVoltar: () -> Unit, contentPadding: PaddingValues) {
    val vm = valtreaViewModel { AdminViewModel(it.api) }
    LaunchedEffect(Unit) { vm.carregar() }
    PainelAdmin(
        metricas = vm.metricas,
        carregando = vm.carregando,
        erro = vm.erro,
        aoAtualizar = vm::carregar,
        aoVoltar = aoVoltar,
        contentPadding = contentPadding,
        moderacao = { FilaAnalise(vm.fila, vm.avisoAnalise, vm::decidir) },
    )
}

/** Estabelecimentos novos, corrigidos ou denunciados. Mostra os dados do negócio e os motivos, nunca quem denunciou. */
@Composable
private fun FilaAnalise(fila: List<ParaAnalisar>?, aviso: String?, aoDecidir: (ParaAnalisar, String?) -> Unit) {
    var recusando by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf("") }
    Secao("Análise de estabelecimentos")
    Text(
        "Confira se o endereço e o telefone existem antes de aprovar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    if (fila?.isEmpty() == true) Text("Nada esperando análise.")
    fila?.forEach { item ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(item.nome, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(
                "${item.tipo.rotulo} · dono: ${item.dono}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${item.endereco} · ${item.bairro}, ${item.cidade} - ${item.uf} · ${item.telefone}")
            item.descricao?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (item.denuncias.isNotEmpty()) {
                Text(
                    "Denúncias: " + item.denuncias.entries.joinToString("; ") { "${it.key.rotulo} (${it.value})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (recusando == item.id) {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= 300) motivo = it },
                    label = { Text("Motivo (vai para o dono)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { aoDecidir(item, motivo.trim()); recusando = null; motivo = "" }, enabled = motivo.trim().length >= 5) {
                        Text("Recusar")
                    }
                    TextButton(onClick = { recusando = null }) { Text("Voltar") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { aoDecidir(item, null) }) { Text("Aprovar") }
                    TextButton(onClick = { recusando = item.id }) {
                        Text("Recusar…", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** A tela sem a parte de rede, para os testes conseguirem mostrar números conhecidos. */
@Composable
fun PainelAdmin(
    metricas: MetricasAdmin?,
    carregando: Boolean,
    erro: String?,
    aoAtualizar: () -> Unit,
    aoVoltar: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    moderacao: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BotaoVoltar(aoVoltar, Modifier.offset(x = (-12).dp))
        Text(
            "Administração",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Só números somados de todas as contas. Nenhum nome, e-mail ou reserva de alguém aparece aqui.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (metricas == null && carregando) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        metricas?.let { m ->
            Secao("Contas")
            Numeros(
                "Total" to m.contas.total.toString(),
                "Com e-mail confirmado" to m.contas.confirmadas.toString(),
                "Novas em 7 dias" to m.contas.novas7Dias.toString(),
                "Novas em 30 dias" to m.contas.novas30Dias.toString(),
                "Conectadas em algum aparelho" to m.contas.comSessaoAtiva.toString(),
            )
            Secao("Uso")
            Numeros(
                "Estabelecimentos aprovados" to m.uso.aprovados.toString(),
                "Esperando análise" to m.uso.emAnalise.toString(),
                "Horários vagos futuros" to m.uso.horariosLivres.toString(),
                "Reservas em 30 dias" to m.uso.reservas30Dias.toString(),
                "Reservas marcadas" to m.uso.reservasFuturas.toString(),
                "Atendidas" to m.uso.atendidas.toString(),
                "Não compareceram" to m.uso.naoCompareceu.toString(),
                "Canceladas em 30 dias" to m.uso.canceladas30Dias.toString(),
            )
            Secao("Cadastros por semana")
            GraficoSemanas(m.cadastrosPorSemana)
            Text(
                "Atualizado em ${m.geradoEm.take(16).replace('T', ' ')} (UTC).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        moderacao()

        OutlinedButton(
            onClick = aoAtualizar,
            enabled = !carregando,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (carregando) "Atualizando…" else "Atualizar") }
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun Numeros(vararg itens: Pair<String, String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        itens.forEach { (rotulo, valor) ->
            // O leitor de tela lê a linha inteira de uma vez ("Ativas: 12").
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clearAndSetSemantics { contentDescription = "$rotulo: $valor" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Barras simples; os mesmos números também são lidos pelo leitor de tela. */
@Composable
private fun GraficoSemanas(semanas: List<SemanaAdmin>) {
    val maximo = (semanas.maxOfOrNull { it.cadastros } ?: 0L).coerceAtLeast(1L)
    val descricao = semanas.joinToString("; ") { "semana de ${dataCurta(it.semana)}: ${it.cadastros}" }
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = "Cadastros por semana: $descricao" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        semanas.forEach { semana ->
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(semana.cadastros.toString(), style = MaterialTheme.typography.labelSmall)
                val fracao = semana.cadastros.toFloat() / maximo
                Box(
                    Modifier
                        .width(18.dp)
                        .fillMaxHeight(0.7f * fracao + 0.02f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(dataCurta(semana.semana), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** "2026-11-02" → "02/11". */
private fun dataCurta(iso: String): String =
    if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}" else iso
