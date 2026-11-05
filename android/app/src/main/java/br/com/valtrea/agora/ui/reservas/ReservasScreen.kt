package br.com.valtrea.agora.ui.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.ReservaCliente
import br.com.valtrea.agora.data.StatusReserva
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.componentes.detalhes
import br.com.valtrea.agora.ui.componentes.quando
import br.com.valtrea.agora.ui.valtreaViewModel
import java.time.Instant
import kotlinx.coroutines.launch

class ReservasViewModel(private val api: ValtreaApi) : ViewModel() {
    var lista by mutableStateOf<List<ReservaCliente>?>(null)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set
    var cancelando by mutableStateOf<ReservaCliente?>(null)

    fun carregar() {
        viewModelScope.launch {
            runCatching { api.minhasReservas() }
                .onSuccess { lista = it; erro = null }
                .onFailure { erro = it.mensagemAmigavel() }
        }
    }

    fun cancelar(r: ReservaCliente) {
        cancelando = null
        viewModelScope.launch {
            runCatching { api.cancelarReserva(r.id) }
                .onSuccess { aviso = "Reserva cancelada. Avisamos o estabelecimento."; carregar() }
                .onFailure { erro = it.mensagemAmigavel() }
        }
    }
}

private fun ReservaCliente.futura() = status == StatusReserva.CONFIRMADA && Instant.parse(inicio).isAfter(Instant.now())

@Composable
fun ReservasScreen(contentPadding: PaddingValues) {
    val vm = valtreaViewModel { ReservasViewModel(it.api) }
    LaunchedEffect(Unit) { vm.carregar() }
    val proximas = vm.lista.orEmpty().filter { it.futura() }.reversed()
    val anteriores = vm.lista.orEmpty().filterNot { it.futura() }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Minhas reservas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
        vm.aviso?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        vm.erro?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (vm.lista?.isEmpty() == true) {
            item { Text("Nenhuma reserva ainda. Veja os horários vagos na primeira aba.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (proximas.isNotEmpty()) {
            item { Text("Próximas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
            items(proximas, key = { it.id }) { r -> Cartao(r, aoCancelar = { vm.cancelando = r }) }
        }
        if (anteriores.isNotEmpty()) {
            item { Text("Anteriores e canceladas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
            items(anteriores, key = { it.id }) { r -> Cartao(r, aoCancelar = null) }
        }
        item {
            Text(
                "Até 3 reservas marcadas ao mesmo tempo. As reservas são apagadas 90 dias depois do horário.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    vm.cancelando?.let { r ->
        AlertDialog(
            onDismissRequest = { vm.cancelando = null },
            title = { Text("Cancelar esta reserva?") },
            text = { Text("O horário volta a ficar livre para outra pessoa, e o estabelecimento recebe um e-mail.") },
            confirmButton = { TextButton(onClick = { vm.cancelar(r) }) { Text("Cancelar reserva", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { vm.cancelando = null }) { Text("Voltar") } },
        )
    }
}

@Composable
private fun Cartao(r: ReservaCliente, aoCancelar: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(r.status.rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(r.inicio.quando(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(detalhes(r.servico, r.duracaoMin, r.precoCentavos))
        Text(r.estabelecimento.nome, fontWeight = FontWeight.SemiBold)
        Text(
            "${r.estabelecimento.endereco} · ${r.estabelecimento.bairro}, ${r.estabelecimento.cidade} - ${r.estabelecimento.uf}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Telefone: ${r.estabelecimento.telefone}", style = MaterialTheme.typography.bodySmall)
        aoCancelar?.let {
            TextButton(onClick = it) { Text("Cancelar reserva", color = MaterialTheme.colorScheme.error) }
        }
    }
}
