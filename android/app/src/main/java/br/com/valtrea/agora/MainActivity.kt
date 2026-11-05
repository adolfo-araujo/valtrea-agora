package br.com.valtrea.agora

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.valtrea.agora.data.EstadoSessao
import br.com.valtrea.agora.ui.admin.AdminScreen
import br.com.valtrea.agora.ui.confirmar.ConfirmarEmailScreen
import br.com.valtrea.agora.ui.conta.ContaScreen
import br.com.valtrea.agora.ui.entrar.EntrarScreen
import br.com.valtrea.agora.ui.horarios.EstabelecimentoScreen
import br.com.valtrea.agora.ui.horarios.HorariosScreen
import br.com.valtrea.agora.ui.negocio.AgendaScreen
import br.com.valtrea.agora.ui.negocio.NegocioScreen
import br.com.valtrea.agora.ui.reservas.ReservasScreen
import br.com.valtrea.agora.ui.theme.ValtreaTheme
import kotlinx.serialization.Serializable

@Serializable data object RotaHorarios
@Serializable data class RotaEstabelecimento(val id: String)
@Serializable data object RotaReservas
@Serializable data object RotaNegocio
@Serializable data class RotaAgenda(val id: String)
@Serializable data object RotaConta
@Serializable data object RotaConfirmarEmail
@Serializable data object RotaAdmin

private data class Aba(val rota: Any, val rotulo: String, val icone: ImageVector)

private val abas = listOf(
    Aba(RotaHorarios, "Horários", Icons.Default.AccessTime),
    Aba(RotaReservas, "Reservas", Icons.Default.EventAvailable),
    Aba(RotaNegocio, "Negócio", Icons.Default.Storefront),
    Aba(RotaConta, "Conta", Icons.Default.Person),
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val sessao = (application as ValtreaApp).sessao
        installSplashScreen().setKeepOnScreenCondition { sessao.estado.value == EstadoSessao.Carregando }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirAcessoARedeLocalEmDebug()
        setContent {
            ValtreaTheme {
                val estado by sessao.estado.collectAsStateWithLifecycle()
                // O Surface pinta o fundo do tema e define a cor do texto (claro no modo escuro, escuro no claro).
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (estado) {
                        EstadoSessao.Carregando -> Unit
                        EstadoSessao.Deslogado -> EntrarScreen()
                        EstadoSessao.Logado -> AppLogado()
                    }
                }
            }
        }
    }

    /** A API de desenvolvimento roda no computador, que o Android 17+ trata como rede local. */
    private fun pedirAcessoARedeLocalEmDebug() {
        val permissao = "android.permission.ACCESS_LOCAL_NETWORK"
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37 &&
            checkSelfPermission(permissao) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permissao), 0)
        }
    }
}

@Composable
private fun AppLogado() {
    val nav = rememberNavController()
    val entradaAtual by nav.currentBackStackEntryAsState()

    fun irPara(rota: Any) {
        nav.navigate(rota) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun voltar() {
        nav.popBackStack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                abas.forEach { aba ->
                    NavigationBarItem(
                        selected = entradaAtual?.destination?.hasRoute(aba.rota::class) == true,
                        onClick = { irPara(aba.rota) },
                        icon = { Icon(aba.icone, contentDescription = null) },
                        label = { Text(aba.rotulo) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = RotaHorarios) {
            composable<RotaHorarios> {
                HorariosScreen(
                    aoAbrir = { id -> nav.navigate(RotaEstabelecimento(id)) },
                    aoConfirmarEmail = { nav.navigate(RotaConfirmarEmail) },
                    contentPadding = padding,
                )
            }
            composable<RotaEstabelecimento> { entrada ->
                EstabelecimentoScreen(entrada.toRoute<RotaEstabelecimento>().id, aoVoltar = { voltar() }, contentPadding = padding)
            }
            composable<RotaReservas> { ReservasScreen(contentPadding = padding) }
            composable<RotaNegocio> {
                NegocioScreen(aoAbrir = { id -> nav.navigate(RotaAgenda(id)) }, contentPadding = padding)
            }
            composable<RotaAgenda> { entrada ->
                AgendaScreen(entrada.toRoute<RotaAgenda>().id, aoVoltar = { voltar() }, contentPadding = padding)
            }
            composable<RotaConta> {
                ContaScreen(contentPadding = padding, aoAbrirAdmin = { nav.navigate(RotaAdmin) })
            }
            composable<RotaConfirmarEmail> {
                ConfirmarEmailScreen(aoConcluir = { voltar() }, aoVoltar = { voltar() }, contentPadding = padding)
            }
            composable<RotaAdmin> { AdminScreen(aoVoltar = { voltar() }, contentPadding = padding) }
        }
    }
}
