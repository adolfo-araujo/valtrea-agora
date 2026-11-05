package br.com.valtrea.agora.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.valtrea.agora.data.mensagemAmigavel
import kotlinx.coroutines.launch

val UFS = listOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE",
    "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")

/** Escolha numa lista fechada, com o rótulo sempre visível. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Escolha(
    rotulo: String,
    opcoes: List<T>,
    atual: T,
    nome: (T) -> String,
    modifier: Modifier = Modifier,
    aoEscolher: (T) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }, modifier = modifier) {
        OutlinedTextField(
            value = nome(atual),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(text = { Text(nome(opcao)) }, onClick = { aoEscolher(opcao); aberto = false })
            }
        }
    }
}

/** Diálogo com botão de confirmar que mostra o envio e o erro da API logo acima. */
@Composable
fun DialogoComEnvio(
    titulo: String,
    confirmar: String,
    podeEnviar: Boolean,
    aoCancelar: () -> Unit,
    enviar: suspend () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!enviando) aoCancelar() },
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                conteudo()
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando && podeEnviar,
                onClick = {
                    enviando = true
                    erro = null
                    escopo.launch {
                        runCatching { enviar() }.onFailure { erro = it.mensagemAmigavel() }
                        enviando = false
                    }
                },
            ) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(confirmar)
            }
        },
        dismissButton = { TextButton(onClick = aoCancelar, enabled = !enviando) { Text("Cancelar") } },
    )
}
