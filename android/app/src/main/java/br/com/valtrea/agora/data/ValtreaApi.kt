package br.com.valtrea.agora.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ValtreaApi {

    @POST("api/v1/auth/cadastro")
    suspend fun cadastrar(@Body pedido: CadastroRequest): TokenResponse

    @POST("api/v1/auth/login")
    suspend fun entrar(@Body pedido: LoginRequest): TokenResponse

    @POST("api/v1/auth/sair")
    suspend fun sair(@Body pedido: RenovarRequest)

    @POST("api/v1/auth/confirmar-email")
    suspend fun confirmarEmail(@Body pedido: CodigoRequest)

    @POST("api/v1/auth/reenviar-confirmacao")
    suspend fun reenviarConfirmacao()

    @POST("api/v1/auth/esqueci-senha")
    suspend fun esqueciSenha(@Body pedido: EsqueciSenhaRequest)

    @POST("api/v1/auth/redefinir-senha")
    suspend fun redefinirSenha(@Body pedido: RedefinirSenhaRequest)

    @GET("api/v1/eu")
    suspend fun perfil(): Perfil

    @PATCH("api/v1/eu")
    suspend fun atualizarPerfil(@Body pedido: AtualizarPerfilRequest): Perfil

    @POST("api/v1/eu/termos")
    suspend fun aceitarTermos(@Body pedido: AceitarTermosRequest): Perfil

    /** "Baixar meus dados": arquivo JSON com cadastro, reservas, estabelecimentos e denúncias (LGPD). */
    @GET("api/v1/eu/dados")
    suspend fun meusDados(): okhttp3.ResponseBody

    @HTTP(method = "DELETE", path = "api/v1/eu", hasBody = true)
    suspend fun excluirConta(@Body pedido: ExcluirContaRequest)

    @GET("api/v1/admin/metricas")
    suspend fun metricasAdmin(): MetricasAdmin

    @GET("api/v1/admin/estabelecimentos")
    suspend fun filaAnalise(): List<ParaAnalisar>

    @POST("api/v1/admin/estabelecimentos/{id}/aprovar")
    suspend fun aprovar(@Path("id") id: String)

    @POST("api/v1/admin/estabelecimentos/{id}/recusar")
    suspend fun recusar(@Path("id") id: String, @Body pedido: RecusaRequest)

    @GET("api/v1/publico/horarios")
    suspend fun horariosLivres(@Query("uf") uf: String?, @Query("tipo") tipo: Tipo?, @Query("busca") busca: String?): List<HorarioPublico>

    @GET("api/v1/estabelecimentos/{id}")
    suspend fun estabelecimento(@Path("id") id: String): EstabelecimentoDetalhe

    @POST("api/v1/estabelecimentos/{id}/denuncia")
    suspend fun denunciar(@Path("id") id: String, @Body pedido: DenunciaRequest)

    @POST("api/v1/horarios/{id}/reserva")
    suspend fun reservar(@Path("id") id: String, @Body pedido: ReservaRequest): ReservaCliente

    @GET("api/v1/eu/reservas")
    suspend fun minhasReservas(): List<ReservaCliente>

    @POST("api/v1/reservas/{id}/cancelar")
    suspend fun cancelarReserva(@Path("id") id: String)

    @POST("api/v1/reservas/{id}/status")
    suspend fun marcarReserva(@Path("id") id: String, @Body pedido: StatusReservaRequest)

    @GET("api/v1/eu/estabelecimentos")
    suspend fun meusEstabelecimentos(): List<Estabelecimento>

    @POST("api/v1/estabelecimentos")
    suspend fun criarEstabelecimento(@Body pedido: EstabelecimentoRequest): Estabelecimento

    @PUT("api/v1/estabelecimentos/{id}")
    suspend fun editarEstabelecimento(@Path("id") id: String, @Body pedido: EstabelecimentoRequest): Estabelecimento

    @DELETE("api/v1/estabelecimentos/{id}")
    suspend fun apagarEstabelecimento(@Path("id") id: String)

    @GET("api/v1/estabelecimentos/{id}/agenda")
    suspend fun agenda(@Path("id") id: String): List<HorarioDono>

    @POST("api/v1/estabelecimentos/{id}/horarios")
    suspend fun publicarHorarios(@Path("id") id: String, @Body pedido: HorariosRequest): List<HorarioDono>

    @DELETE("api/v1/horarios/{id}")
    suspend fun apagarHorario(@Path("id") id: String)
}
