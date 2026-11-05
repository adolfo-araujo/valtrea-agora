package br.com.valtrea.agora.ui.horarios

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.DenunciaRequest
import br.com.valtrea.agora.data.EstabelecimentoDetalhe
import br.com.valtrea.agora.data.HorarioLivre
import br.com.valtrea.agora.data.MotivoDenuncia
import br.com.valtrea.agora.data.ReservaRequest
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.componentes.BotaoVoltar
import br.com.valtrea.agora.ui.componentes.DialogoComEnvio
import br.com.valtrea.agora.ui.componentes.detalhes
import br.com.valtrea.agora.ui.componentes.hora
import br.com.valtrea.agora.ui.componentes.porDia
import br.com.valtrea.agora.ui.componentes.quando
import br.com.valtrea.agora.ui.valtreaViewModel
import kotlinx.coroutines.launch

class EstabelecimentoViewModel(private val api: ValtreaApi, private val id: String) : ViewModel() {
    var dados by mutableStateOf<EstabelecimentoDetalhe?>(null)
        private set
    var emailConfirmado by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set
    var reservando by mutableStateOf<HorarioLivre?>(null)
    var denunciando by mutableStateOf(false)

    fun carregar() {
        viewModelScope.launch {
            runCatching { api.perfil() }.onSuccess { emailConfirmado = it.emailConfirmado }
            runCatching { api.estabelecimento(id) }
                .onSuccess { dados = it; erro = null }
                .onFailure { erro = it.mensagemAmigavel() }
        }
    }

    suspend fun reservar(horario: HorarioLivre, telefone: String, observacao: String) {
        api.reservar(horario.id, ReservaRequest(telefone.trim().ifBlank { null }, observacao.trim().ifBlank { null }))
        reservando = null
        aviso = "Reserva feita para ${horario.inicio.quando()}! Ela está em Reservas."
        dados = api.estabelecimento(id)
    }

    suspend fun denunciar(motivo: MotivoDenuncia) {
        api.denunciar(id, DenunciaRequest(motivo))
        denunciando = false
        dados = dados?.copy(denunciei = true)
        aviso = "Denúncia enviada. Obrigado por avisar."
    }
}

@Composable
fun EstabelecimentoScreen(id: String, aoVoltar: () -> Unit, contentPadding: PaddingValues) {
    val vm = valtreaViewModel { EstabelecimentoViewModel(it.api, id) }
    val contexto = LocalContext.current
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
        vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        val d = vm.dados ?: return@Column
        val e = d.estabelecimento

        Text(e.tipo.rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(e.nome, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        e.descricao?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("${e.endereco} · ${e.bairro}, ${e.cidade} - ${e.uf}")
        TextButton(onClick = {
            contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + e.telefone.filter { it.isDigit() || it == '+' })))
        }) { Text("Ligar: ${e.telefone}") }

        Text("Horários vagos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (d.horarios.isEmpty()) Text("Nenhum horário vago nos próximos 14 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (d.meu) {
            Text("Este estabelecimento é seu. Gerencie a agenda em Negócio.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        d.horarios.porDia { it.inicio }.forEach { (dia, itens) ->
            Text(dia, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
            itens.forEach { h ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(h.inicio.hora(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(detalhes(h.servico, h.duracaoMin, h.precoCentavos), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!d.meu) {
                        Button(
                            onClick = { vm.reservando = h },
                            enabled = vm.emailConfirmado,
                            modifier = Modifier.semantics { contentDescription = "Reservar ${h.servico} às ${h.inicio.hora()}" },
                        ) { Text("Reservar") }
                    }
                }
            }
        }
        if (!vm.emailConfirmado && !d.meu) {
            Text("Confirme seu e-mail em Conta para reservar.", style = MaterialTheme.typography.bodySmall)
        }
        if (vm.emailConfirmado && !d.meu && !d.denunciei) {
            TextButton(onClick = { vm.denunciando = true }) {
                Text("Denunciar este estabelecimento", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    vm.reservando?.let { h ->
        var telefone by remember(h.id) { mutableStateOf("") }
        var observacao by remember(h.id) { mutableStateOf("") }
        DialogoComEnvio(
            titulo = "Reservar",
            confirmar = "Confirmar reserva",
            podeEnviar = true,
            aoCancelar = { vm.reservando = null },
            enviar = { vm.reservar(h, telefone, observacao) },
        ) {
            Text("${h.servico}, ${h.inicio.quando()}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = telefone,
                onValueChange = { if (it.length <= 20) telefone = it },
                label = { Text("Seu telefone (opcional)") },
                supportingText = { Text("Só o estabelecimento vê, para falar com você sobre esta reserva.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = observacao,
                onValueChange = { if (it.length <= 200) observacao = it },
                label = { Text("Observação (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "O pagamento é no local. Nunca pague adiantado nem por Pix para garantir o horário.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (vm.denunciando) {
        var motivo by remember { mutableStateOf<MotivoDenuncia?>(null) }
        DialogoComEnvio(
            titulo = "Denunciar",
            confirmar = "Enviar denúncia",
            podeEnviar = motivo != null,
            aoCancelar = { vm.denunciando = false },
            enviar = { vm.denunciar(motivo!!) },
        ) {
            Text(
                "Com 3 denúncias, o estabelecimento sai da busca até a nossa revisão. Ninguém fica sabendo que foi você.",
                style = MaterialTheme.typography.bodySmall,
            )
            Column(Modifier.selectableGroup()) {
                MotivoDenuncia.entries.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = motivo == m, onClick = { motivo = m }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = motivo == m, onClick = null)
                        Text(m.rotulo, Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
