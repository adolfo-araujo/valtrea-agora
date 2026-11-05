package br.com.valtrea.agora.ui.negocio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.Estabelecimento
import br.com.valtrea.agora.data.EstabelecimentoRequest
import br.com.valtrea.agora.data.HorarioDono
import br.com.valtrea.agora.data.HorariosRequest
import br.com.valtrea.agora.data.StatusHorario
import br.com.valtrea.agora.data.StatusReserva
import br.com.valtrea.agora.data.StatusReservaRequest
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.componentes.BotaoVoltar
import br.com.valtrea.agora.ui.componentes.detalhes
import br.com.valtrea.agora.ui.componentes.hora
import br.com.valtrea.agora.ui.componentes.porDia
import br.com.valtrea.agora.ui.valtreaViewModel
import java.time.Instant
import kotlinx.coroutines.launch

enum class DialogoAgenda { NENHUM, PUBLICAR, EDITAR, APAGAR }

class AgendaViewModel(private val api: ValtreaApi, private val id: String) : ViewModel() {
    var estabelecimento by mutableStateOf<Estabelecimento?>(null)
        private set
    var agenda by mutableStateOf<List<HorarioDono>?>(null)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set
    var ocupado by mutableStateOf(false)
        private set
    var dialogo by mutableStateOf(DialogoAgenda.NENHUM)
    var cancelando by mutableStateOf<HorarioDono?>(null)

    fun carregar() = tarefa {
        estabelecimento = api.meusEstabelecimentos().firstOrNull { it.id == id }
        agenda = api.agenda(id)
    }

    suspend fun publicar(pedido: HorariosRequest) {
        agenda = api.publicarHorarios(id, pedido)
        aviso = "Horários publicados."
        dialogo = DialogoAgenda.NENHUM
    }

    suspend fun editar(pedido: EstabelecimentoRequest) {
        estabelecimento = api.editarEstabelecimento(id, pedido)
        aviso = "Dados salvos."
        dialogo = DialogoAgenda.NENHUM
    }

    fun apagarHorario(h: HorarioDono) = acao("Horário apagado.") { api.apagarHorario(h.id) }

    fun cancelarReserva(h: HorarioDono) {
        cancelando = null
        acao("Reserva cancelada. Avisamos o cliente.") { api.cancelarReserva(h.reserva!!.id) }
    }

    fun marcar(h: HorarioDono, status: StatusReserva) =
        acao(if (status == StatusReserva.ATENDIDA) "Marcado como atendido." else "Marcado como não compareceu.") {
            api.marcarReserva(h.reserva!!.id, StatusReservaRequest(status))
        }

    fun apagar(aoTerminar: () -> Unit) = tarefa {
        api.apagarEstabelecimento(id)
        dialogo = DialogoAgenda.NENHUM
        aoTerminar()
    }

    private fun acao(mensagem: String, bloco: suspend () -> Unit) = tarefa {
        bloco()
        aviso = mensagem
        agenda = api.agenda(id)
    }

    private fun tarefa(bloco: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        erro = null
        viewModelScope.launch {
            runCatching { bloco() }.onFailure { erro = it.mensagemAmigavel() }
            ocupado = false
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendaScreen(id: String, aoVoltar: () -> Unit, contentPadding: PaddingValues) {
    val vm = valtreaViewModel { AgendaViewModel(it.api, id) }
    LaunchedEffect(id) { vm.carregar() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BotaoVoltar(aoVoltar, Modifier.offset(x = (-12).dp))
        vm.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val e = vm.estabelecimento ?: return@Column
        Text(e.nome, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Situacao(e)
        vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Button(
            onClick = { vm.dialogo = DialogoAgenda.PUBLICAR },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Publicar horários vagos") }

        Text("Agenda", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val agenda = vm.agenda.orEmpty()
        if (vm.agenda?.isEmpty() == true) Text("Nenhum horário publicado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val agora = Instant.now()
        agenda.porDia { it.inicio }.forEach { (dia, itens) ->
            Text(dia, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
            itens.forEach { h ->
                val passou = !Instant.parse(h.inicio).isAfter(agora)
                val r = h.reserva
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val situacao = when (h.status) {
                                StatusHorario.LIVRE -> "livre"
                                StatusHorario.RESERVADO -> "reservado"
                                StatusHorario.CANCELADO -> "cancelado"
                            }
                            Text("${h.inicio.hora()} · $situacao", fontWeight = FontWeight.Bold)
                            Text(detalhes(h.servico, h.duracaoMin, h.precoCentavos), style = MaterialTheme.typography.bodySmall)
                        }
                        if (h.status == StatusHorario.LIVRE) {
                            TextButton(onClick = { vm.apagarHorario(h) }, enabled = !vm.ocupado) {
                                Text("Apagar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (r != null) {
                        Text("${r.cliente} · ${r.status.rotulo}", fontWeight = FontWeight.SemiBold)
                        r.telefone?.let { Text("Telefone: $it") }
                        r.observacao?.let { Text("Observação: $it") }
                        if (r.status == StatusReserva.CONFIRMADA && !passou) {
                            TextButton(onClick = { vm.cancelando = h }, enabled = !vm.ocupado) {
                                Text("Cancelar reserva", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        val marcavel = r.status in setOf(StatusReserva.CONFIRMADA, StatusReserva.ATENDIDA, StatusReserva.NAO_COMPARECEU)
                        if (passou && marcavel) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { vm.marcar(h, StatusReserva.ATENDIDA) },
                                    enabled = !vm.ocupado && r.status != StatusReserva.ATENDIDA,
                                ) { Text("Atendido") }
                                OutlinedButton(
                                    onClick = { vm.marcar(h, StatusReserva.NAO_COMPARECEU) },
                                    enabled = !vm.ocupado && r.status != StatusReserva.NAO_COMPARECEU,
                                ) { Text("Não veio") }
                            }
                        }
                    }
                }
            }
        }

        Text("Dados do estabelecimento", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
        Text("${e.endereco} · ${e.bairro}, ${e.cidade} - ${e.uf} · ${e.telefone}", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { vm.dialogo = DialogoAgenda.EDITAR }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Editar") }
        TextButton(onClick = { vm.dialogo = DialogoAgenda.APAGAR }) {
            Text("Apagar estabelecimento", color = MaterialTheme.colorScheme.error)
        }
    }

    vm.cancelando?.let { h ->
        AlertDialog(
            onDismissRequest = { vm.cancelando = null },
            title = { Text("Cancelar esta reserva?") },
            text = { Text("O cliente recebe um e-mail e o horário não volta para a busca.") },
            confirmButton = { TextButton(onClick = { vm.cancelarReserva(h) }) { Text("Cancelar reserva", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { vm.cancelando = null }) { Text("Voltar") } },
        )
    }
    val e = vm.estabelecimento ?: return
    when (vm.dialogo) {
        DialogoAgenda.PUBLICAR -> DialogoHorarios(aoCancelar = { vm.dialogo = DialogoAgenda.NENHUM }, aoPublicar = vm::publicar)
        DialogoAgenda.EDITAR -> DialogoEstabelecimento(e, aoCancelar = { vm.dialogo = DialogoAgenda.NENHUM }, aoSalvar = vm::editar)
        DialogoAgenda.APAGAR -> AlertDialog(
            onDismissRequest = { vm.dialogo = DialogoAgenda.NENHUM },
            title = { Text("Apagar ${e.nome}?") },
            text = { Text("Apaga os horários e as reservas. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = { vm.apagar(aoVoltar) }, enabled = !vm.ocupado) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { vm.dialogo = DialogoAgenda.NENHUM }) { Text("Cancelar") } },
        )
        DialogoAgenda.NENHUM -> Unit
    }
}
