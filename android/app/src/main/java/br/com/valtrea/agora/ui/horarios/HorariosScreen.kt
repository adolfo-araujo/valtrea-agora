package br.com.valtrea.agora.ui.horarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.AceitarTermosRequest
import br.com.valtrea.agora.data.HorarioPublico
import br.com.valtrea.agora.data.Perfil
import br.com.valtrea.agora.data.Tipo
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.componentes.Escolha
import br.com.valtrea.agora.ui.componentes.Links
import br.com.valtrea.agora.ui.componentes.UFS
import br.com.valtrea.agora.ui.componentes.detalhes
import br.com.valtrea.agora.ui.componentes.hora
import br.com.valtrea.agora.ui.componentes.porDia
import br.com.valtrea.agora.ui.valtreaViewModel
import kotlinx.coroutines.launch

class HorariosViewModel(private val api: ValtreaApi) : ViewModel() {
    var perfil by mutableStateOf<Perfil?>(null)
        private set
    var lista by mutableStateOf<List<HorarioPublico>?>(null)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var carregando by mutableStateOf(false)
        private set
    var uf by mutableStateOf("")
    var busca by mutableStateOf("")
    var tipo by mutableStateOf<Tipo?>(null)

    fun carregarPerfil() {
        viewModelScope.launch { runCatching { api.perfil() }.onSuccess { perfil = it } }
    }

    fun procurar() {
        if (carregando) return
        carregando = true
        viewModelScope.launch {
            runCatching { api.horariosLivres(uf.ifBlank { null }, tipo, busca.trim().ifBlank { null }) }
                .onSuccess { lista = it; erro = null }
                .onFailure { erro = it.mensagemAmigavel() }
            carregando = false
        }
    }

    fun aceitarTermos() {
        viewModelScope.launch {
            runCatching { api.aceitarTermos(AceitarTermosRequest(Links.VERSAO_TERMOS)) }
                .onSuccess { perfil = it }
                .onFailure { erro = it.mensagemAmigavel() }
        }
    }
}

@Composable
fun HorariosScreen(aoAbrir: (String) -> Unit, aoConfirmarEmail: () -> Unit, contentPadding: PaddingValues) {
    val vm = valtreaViewModel { HorariosViewModel(it.api) }
    LaunchedEffect(Unit) {
        vm.carregarPerfil()
        if (vm.lista == null) vm.procurar()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Horários vagos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Dos próximos 7 dias. Reserve e pague no local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        vm.perfil?.let { p ->
            if (!p.termosAceitos) item { AvisoNovosTermos(aoAceitar = vm::aceitarTermos) }
            if (!p.emailConfirmado) item { AvisoConfirmarEmail(aoConfirmarEmail) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Escolha("Estado", listOf("") + UFS, vm.uf, { it.ifBlank { "Todos" } }, Modifier.width(120.dp)) { vm.uf = it }
                    OutlinedTextField(
                        value = vm.busca,
                        onValueChange = { vm.busca = it },
                        label = { Text("Cidade ou bairro") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.procurar() }),
                        modifier = Modifier.weight(1f),
                    )
                }
                Escolha("Serviço", listOf<Tipo?>(null) + Tipo.entries, vm.tipo, { it?.rotulo ?: "Todos" }) { vm.tipo = it }
                Button(onClick = vm::procurar, enabled = !vm.carregando, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(if (vm.carregando) "Procurando…" else "Procurar")
                }
            }
        }
        vm.erro?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        val lista = vm.lista
        if (lista != null && lista.isEmpty()) {
            item {
                Text(
                    "Nenhum horário vago nos próximos 7 dias com esses filtros. Tente outra cidade ou outro serviço.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        lista.orEmpty().porDia { it.inicio }.forEach { (dia, itens) ->
            item(key = "dia-$dia") {
                Text(dia, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            }
            items(itens, key = { it.id }) { h -> CartaoHorario(h) { aoAbrir(h.estabelecimento.id) } }
        }
    }
}

@Composable
private fun CartaoHorario(h: HorarioPublico, aoAbrir: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = "Ver e reservar", role = Role.Button, onClick = aoAbrir)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(h.inicio.hora(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.weight(1f)) {
            Text(h.estabelecimento.nome, fontWeight = FontWeight.Bold)
            Text(detalhes(h.servico, h.duracaoMin, h.precoCentavos), style = MaterialTheme.typography.bodySmall)
            Text(
                "${h.estabelecimento.bairro}, ${h.estabelecimento.cidade} - ${h.estabelecimento.uf}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
