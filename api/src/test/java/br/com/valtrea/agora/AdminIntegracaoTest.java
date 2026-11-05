package br.com.valtrea.agora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Painel de administração: só para administradores e só com números agregados. */
class AdminIntegracaoTest extends IntegracaoBase {

	@Autowired
	JdbcTemplate jdbc;

	record PerfilAdmin(UUID id, boolean admin) {
	}

	private PerfilAdmin perfilAdmin(String token) {
		return http.get().uri("/api/v1/eu").header("Authorization", "Bearer " + token).retrieve()
				.body(PerfilAdmin.class);
	}

	private ResponseEntity<String> metricas(String token) {
		return http.get().uri("/api/v1/admin/metricas").header("Authorization", "Bearer " + token).retrieve()
				.toEntity(String.class);
	}

	@Test
	void pessoaComumNaoVeOPainel() {
		String comum = cadastrarConfirmado("Pessoa Comum", emailUnico("comum"));
		assertThat(perfilAdmin(comum).admin()).isFalse();
		assertThat(metricas(comum).getStatusCode().value()).isEqualTo(403);
		assertThat(http.get().uri("/api/v1/admin/metricas").retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(401);
	}

	@Test
	void emailDaListaVeOPainelDepoisDeConfirmar() {
		String token = cadastrar("Chefe", "chefe-agora@exemplo.com").accessToken();
		assertThat(perfilAdmin(token).admin()).as("sem e-mail confirmado ainda não é admin").isFalse();
		assertThat(confirmarEmail(token, emails.ultimoCodigo("chefe-agora@exemplo.com")).getStatusCode().value())
				.isEqualTo(204);
		assertThat(perfilAdmin(token).admin()).isTrue();
		assertThat(metricas(token).getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void papelAdministradorMostraNumerosAgregadosSemDadosPessoais() {
		String emailAdmin = emailUnico("admin");
		String admin = cadastrarConfirmado("Ana Admin", emailAdmin);
		jdbc.update("UPDATE usuarios SET papel = 'ADMINISTRADOR' WHERE email = ?", emailAdmin);

		String bia = cadastrarConfirmado("Bia Dona", emailUnico("bia"));
		Estabelecimento salao = estabelecimentoAprovado(bia, "Salão Azul da Bia", cidadeUnica());
		UUID horario = publicarHorarios(bia, salao.id(), daqui(8)).getFirst().id();
		String caio = cadastrarConfirmado("Caio Cliente", emailUnico("caio"));
		reservar(caio, horario);

		ResponseEntity<String> resposta = metricas(admin);
		assertThat(resposta.getStatusCode().value()).isEqualTo(200);
		assertThat(resposta.getBody()).contains("\"contas\"", "\"uso\"", "\"cadastrosPorSemana\"");
		assertThat(resposta.getBody()).doesNotContain("Bia", "Caio", "Salão Azul", "@exemplo.com");

		MetricasLidas m = http.get().uri("/api/v1/admin/metricas").header("Authorization", "Bearer " + admin)
				.retrieve().body(MetricasLidas.class);
		assertThat(m.contas().confirmadas()).isGreaterThanOrEqualTo(2);
		assertThat(m.uso().aprovados()).isGreaterThanOrEqualTo(1);
		assertThat(m.uso().reservas30Dias()).isGreaterThanOrEqualTo(1);
		assertThat(m.cadastrosPorSemana()).hasSize(8);
	}

	record MetricasLidas(ContasLidas contas, UsoLido uso, java.util.List<Object> cadastrosPorSemana) {
	}

	record ContasLidas(long total, long confirmadas) {
	}

	record UsoLido(long aprovados, long emAnalise, long reservas30Dias) {
	}

}
