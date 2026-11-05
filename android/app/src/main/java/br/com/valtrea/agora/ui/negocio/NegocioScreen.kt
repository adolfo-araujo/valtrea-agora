package br.com.valtrea.agora.ui.negocio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.valtrea.agora.data.Estabelecimento
import br.com.valtrea.agora.data.EstabelecimentoRequest
import br.com.valtrea.agora.data.StatusEstabelecimento
import br.com.valtrea.agora.data.ValtreaApi
import br.com.valtrea.agora.data.mensagemAmigavel
import br.com.valtrea.agora.ui.valtreaViewModel
import kotlinx.coroutines.launch

class NegocioViewModel(private val api: ValtreaApi) : ViewModel() {
    var lista by mutableStateOf<List<Estabelecimento>?>(null)
        private set
    var emailConfirmado by mutableStateOf(true)
        private set
    var erro by mutableStateOf<String?>(null)
        private set
    var criando by mutableStateOf(false)

    fun carregar() {
        viewModelScope.launch {
            runCatching { api.perfil() }.onSuccess { emailConfirmado = it.emailConfirmado }
            runCatching { api.meusEstabelecimentos() }
                .onSuccess { lista = it; erro = null }
                .onFailure { erro = it.mensagemAmigavel() }
        }
    }

    suspend fun criar(pedido: EstabelecimentoRequest): String = api.criarEstabelecimento(pedido).id
}

@Composable
fun Situacao(e: Estabelecimento) {
    val cor = when (e.status) {
        StatusEstabelecimento.APROVADO -> MaterialTheme.colorScheme.primary
        StatusEstabelecimento.RECUSADO -> MaterialTheme.colorScheme.error
        StatusEstabelecimento.EM_ANALISE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(e.status.rotulo, color = cor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    when (e.status) {
        StatusEstabelecimento.EM_ANALISE -> Text(
            "Avisamos por e-mail quando terminarmos. Você já pode publicar horários.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusEstabelecimento.RECUSADO -> Text(
            "Motivo: ${e.motivoRecusa}. Corrija e salve para voltar à análise.",
            style = MaterialTheme.typography.bodySmall,
        )
        StatusEstabelecimento.APROVADO -> Unit
    }
}

@Composable
fun NegocioScreen(aoAbrir: (String) -> Unit, contentPadding: PaddingValues) {
    val vm = valtreaViewModel { NegocioViewModel(it.api) }
    LaunchedEffect(Unit) { vm.carregar() }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Meu negócio", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Cadastre o seu salão, barbearia ou outro serviço e publique os horários que ficaram vagos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        vm.erro?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!vm.emailConfirmado) item { Text("Confirme seu e-mail em Conta para cadastrar um estabelecimento.") }
        items(vm.lista.orEmpty(), key = { it.id }) { e ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClickLabel = "Abrir agenda", role = Role.Button) { aoAbrir(e.id) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${e.tipo.rotulo} · ${e.bairro}, ${e.cidade} - ${e.uf}", style = MaterialTheme.typography.bodySmall)
                Text(e.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Situacao(e)
            }
        }
        val lista = vm.lista
        if (lista != null && lista.size < 3 && vm.emailConfirmado) {
            item {
                Button(onClick = { vm.criando = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Cadastrar estabelecimento")
                }
            }
        }
    }

    if (vm.criando) {
        DialogoEstabelecimento(inicial = null, aoCancelar = { vm.criando = false }, aoSalvar = { pedido ->
            val id = vm.criar(pedido)
            vm.criando = false
            aoAbrir(id)
        })
    }
}
