package br.com.valtrea.agora.data

import kotlinx.serialization.Serializable

// Espelham os DTOs da API (api/src/main/java/br/com/valtrea/agora).

@Serializable
data class CadastroRequest(val nome: String, val email: String, val senha: String)

@Serializable
data class LoginRequest(val email: String, val senha: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val tipo: String,
    val expiraEm: String,
    val refreshToken: String,
)

@Serializable
data class RenovarRequest(val refreshToken: String)

@Serializable
data class CodigoRequest(val codigo: String)

@Serializable
data class EsqueciSenhaRequest(val email: String)

@Serializable
data class RedefinirSenhaRequest(val email: String, val codigo: String, val novaSenha: String)

@Serializable
data class Perfil(
    val id: String,
    val nome: String,
    val email: String,
    val emailConfirmado: Boolean,
    /** Falso quando há Termos novos para aceitar: a tela de horários mostra o aviso. */
    val termosAceitos: Boolean = true,
    /** Conta de administração: a tela Conta mostra o atalho para o painel. */
    val admin: Boolean = false,
)

@Serializable
data class AceitarTermosRequest(val versao: String)

@Serializable
data class AtualizarPerfilRequest(val nome: String)

@Serializable
data class ExcluirContaRequest(val senha: String)

@Serializable
enum class Tipo(val rotulo: String) {
    SALAO("Salão de beleza"),
    BARBEARIA("Barbearia"),
    MANICURE("Manicure e pedicure"),
    ESTETICA("Estética"),
    SOBRANCELHA("Sobrancelha e cílios"),
    MASSAGEM("Massagem"),
    PET("Banho e tosa (pet)"),
    OUTRO("Outro serviço"),
}

@Serializable
enum class StatusEstabelecimento(val rotulo: String) {
    EM_ANALISE("Em análise"),
    APROVADO("Aprovado: aparece na busca"),
    RECUSADO("Precisa de ajuste"),
}

@Serializable
enum class StatusHorario { LIVRE, RESERVADO, CANCELADO }

@Serializable
enum class StatusReserva(val rotulo: String) {
    CONFIRMADA("Confirmada"),
    CANCELADA_CLIENTE("Cancelada pelo cliente"),
    CANCELADA_ESTABELECIMENTO("Cancelada pelo estabelecimento"),
    ATENDIDA("Atendida"),
    NAO_COMPARECEU("Não compareceu"),
}

@Serializable
enum class MotivoDenuncia(val rotulo: String) {
    FALSO("O lugar não existe ou não é o que diz"),
    NAO_RESPEITA("Não respeita as reservas"),
    GOLPE("Pede pagamento antecipado, Pix ou dados"),
    IMPROPRIO("Nome, descrição ou serviço impróprio"),
    OUTRO("Outro motivo"),
}

@Serializable
data class EstabelecimentoRequest(
    val nome: String,
    val tipo: Tipo,
    val descricao: String? = null,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val telefone: String,
)

/** Para o dono, com a situação da análise. */
@Serializable
data class Estabelecimento(
    val id: String,
    val nome: String,
    val tipo: Tipo,
    val descricao: String? = null,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val telefone: String,
    val status: StatusEstabelecimento,
    val motivoRecusa: String? = null,
    val criadoEm: String,
)

@Serializable
data class EstabelecimentoPublico(
    val id: String,
    val nome: String,
    val tipo: Tipo,
    val descricao: String? = null,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val telefone: String,
    val horariosLivres: Long = 0,
)

@Serializable
data class EstabelecimentoResumo(val id: String, val nome: String, val tipo: Tipo, val bairro: String, val cidade: String, val uf: String)

@Serializable
data class HorarioLivre(val id: String, val inicio: String, val duracaoMin: Int, val servico: String, val precoCentavos: Int? = null)

@Serializable
data class EstabelecimentoDetalhe(
    val estabelecimento: EstabelecimentoPublico,
    val horarios: List<HorarioLivre> = emptyList(),
    val denunciei: Boolean = false,
    val meu: Boolean = false,
)

@Serializable
data class HorarioPublico(
    val id: String,
    val inicio: String,
    val duracaoMin: Int,
    val servico: String,
    val precoCentavos: Int? = null,
    val estabelecimento: EstabelecimentoResumo,
)

@Serializable
data class ReservaParaDono(
    val id: String,
    val cliente: String,
    val telefone: String? = null,
    val observacao: String? = null,
    val status: StatusReserva,
)

@Serializable
data class HorarioDono(
    val id: String,
    val inicio: String,
    val duracaoMin: Int,
    val servico: String,
    val precoCentavos: Int? = null,
    val status: StatusHorario,
    val reserva: ReservaParaDono? = null,
)

@Serializable
data class EstabelecimentoDaReserva(
    val id: String,
    val nome: String,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val telefone: String,
)

@Serializable
data class ReservaCliente(
    val id: String,
    val status: StatusReserva,
    val criadoEm: String,
    val inicio: String,
    val duracaoMin: Int,
    val servico: String,
    val precoCentavos: Int? = null,
    val telefone: String? = null,
    val observacao: String? = null,
    val estabelecimento: EstabelecimentoDaReserva,
)

@Serializable
data class HorariosRequest(val servico: String, val duracaoMin: Int, val precoCentavos: Int? = null, val inicios: List<String>)

@Serializable
data class ReservaRequest(val telefone: String? = null, val observacao: String? = null)

@Serializable
data class StatusReservaRequest(val status: StatusReserva)

@Serializable
data class DenunciaRequest(val motivo: MotivoDenuncia)

@Serializable
data class RecusaRequest(val motivo: String)

/** Fila de análise: dados do negócio, o primeiro nome do dono e quantas denúncias de cada motivo. */
@Serializable
data class ParaAnalisar(
    val id: String,
    val nome: String,
    val tipo: Tipo,
    val descricao: String? = null,
    val endereco: String,
    val bairro: String,
    val cidade: String,
    val uf: String,
    val telefone: String,
    val dono: String,
    val criadoEm: String,
    val atualizadoEm: String,
    val denuncias: Map<MotivoDenuncia, Long> = emptyMap(),
)

/** Painel de administração: só números agregados, nenhum dado de outra pessoa. */
@Serializable
data class MetricasAdmin(
    val geradoEm: String,
    val contas: ContasAdmin,
    val uso: UsoAdmin,
    val cadastrosPorSemana: List<SemanaAdmin>,
)

@Serializable
data class ContasAdmin(
    val total: Long,
    val confirmadas: Long,
    val novas7Dias: Long,
    val novas30Dias: Long,
    val comSessaoAtiva: Long,
)

@Serializable
data class UsoAdmin(
    val aprovados: Long,
    val emAnalise: Long,
    val horariosLivres: Long,
    val reservas30Dias: Long,
    val reservasFuturas: Long,
    val atendidas: Long,
    val naoCompareceu: Long,
    val canceladas30Dias: Long,
)

@Serializable
data class SemanaAdmin(val semana: String, val cadastros: Long)

/** Resposta de erro da API no formato ProblemDetail. */
@Serializable
data class Problema(val detail: String? = null, val campos: Map<String, String>? = null)
