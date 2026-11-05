package br.com.valtrea.agora;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.valtrea.agora.retencao.RetencaoService;
import br.com.valtrea.agora.usuario.Termos;

/** Senha forte, "Baixar meus dados", aceite dos Termos, exclusão de conta e rotina de retenção. */
class LgpdIntegracaoTest extends IntegracaoBase {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	RetencaoService retencao;

	private ResponseEntity<Void> excluir(String token, String senha) {
		return http.method(HttpMethod.DELETE).uri("/api/v1/eu").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("senha", senha)).retrieve().toBodilessEntity();
	}

	private boolean existe(String tabela, UUID id) {
		return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE id = ?", Integer.class, id) == 1;
	}

	@Test
	void cadastroPedeSoNomeEmailESenhaComLetrasENumeros() {
		var semNumero = http.post().uri("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Senha Fraca", "email", emailUnico("fraca"), "senha", "somenteletras")).retrieve()
				.toBodilessEntity();
		assertThat(semNumero.getStatusCode().value()).isEqualTo(400);

		String email = emailUnico("dupla");
		cadastrar("Primeira", email);
		var repetido = http.post().uri("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Segunda", "email", email.toUpperCase(), "senha", "senha-forte-123")).retrieve()
				.toBodilessEntity();
		assertThat(repetido.getStatusCode().value()).isEqualTo(409);
	}

	@Test
	void baixarMeusDadosTrazTudoMenosSenhaECodigos() {
		String email = emailUnico("olga");
		String olga = cadastrarConfirmado("Olga Dados", email);
		String dono = cadastrarConfirmado("Davi Dono", emailUnico("davi"));
		Estabelecimento barbearia = estabelecimentoAprovado(dono, "Barbearia do Davi", cidadeUnica());
		UUID horario = publicarHorarios(dono, barbearia.id(), daqui(5)).getFirst().id();
		assertThat(reservar(olga, horario).getStatusCode().value()).isEqualTo(201);
		Estabelecimento meu = cadastrarEstabelecimento(olga, "Salão da Olga", cidadeUnica()).getBody();

		ResponseEntity<String> resposta = http.get().uri("/api/v1/eu/dados").header("Authorization", "Bearer " + olga)
				.retrieve().toEntity(String.class);
		assertThat(resposta.getStatusCode().value()).isEqualTo(200);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition")).contains("valtrea-agora-meus-dados-");
		assertThat(resposta.getBody()).contains(email, "Barbearia do Davi", "Chego 5 min antes", meu.id().toString(),
				"termosVersao");
		assertThat(resposta.getBody()).doesNotContain("senha", "$2a$", "codigo_hash", "token_hash");
	}

	@Test
	void contaNovaJaAceitaOsTermosEAntigaPrecisaAceitar() {
		String token = cadastrar("Quela Termos", emailUnico("quela")).accessToken();
		Perfil perfil = perfil(token);
		assertThat(perfil.termosAceitos()).isTrue();

		jdbc.update("UPDATE usuarios SET termos_versao = NULL, termos_aceitos_em = NULL WHERE id = ?", perfil.id());
		assertThat(perfil(token).termosAceitos()).isFalse();
		assertThat(post("/api/v1/eu/termos", token, Map.of("versao", "2000-01-01")).getStatusCode().value())
				.isEqualTo(400);
		assertThat(post("/api/v1/eu/termos", token, Map.of("versao", Termos.VERSAO)).getStatusCode().value())
				.isEqualTo(200);
		assertThat(perfil(token).termosAceitos()).isTrue();
	}

	@Test
	void excluirContaApagaTudoNaHora() {
		String email = emailUnico("rita");
		String rita = cadastrarConfirmado("Rita Sai", email);
		UUID idRita = perfil(rita).id();
		Estabelecimento salao = estabelecimentoAprovado(rita, "Salão da Rita", cidadeUnica());
		UUID horarioDela = publicarHorarios(rita, salao.id(), daqui(3)).getFirst().id();
		String dono = cadastrarConfirmado("Saulo Dono", emailUnico("saulo"));
		Estabelecimento barbearia = estabelecimentoAprovado(dono, "Barbearia do Saulo", cidadeUnica());
		UUID horarioDele = publicarHorarios(dono, barbearia.id(), daqui(4)).getFirst().id();
		UUID reserva = reservar(rita, horarioDele).getBody().id();

		assertThat(excluir(rita, "senha-errada-1").getStatusCode().value()).isEqualTo(403);
		assertThat(excluir(rita, "senha-forte-123").getStatusCode().value()).isEqualTo(204);

		assertThat(existe("usuarios", idRita)).isFalse();
		assertThat(existe("estabelecimentos", salao.id())).isFalse();
		assertThat(existe("horarios", horarioDela)).isFalse();
		assertThat(existe("reservas", reserva)).isFalse();
		assertThat(publico("/estabelecimentos/" + salao.id()).getStatusCode().value()).isEqualTo(404);
		assertThat(emails.para(email).getLast().getSubject()).contains("excluída");
		assertThat(login(email, "senha-forte-123").getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void retencaoApagaSoOQueNaoPrecisaMaisSerGuardado() {
		UUID parado = perfil(cadastrar("Rui Parado", emailUnico("rui")).accessToken()).id();
		UUID recente = perfil(cadastrar("Tais Recente", emailUnico("tais")).accessToken()).id();
		jdbc.update("UPDATE usuarios SET criado_em = now() - interval '31 days' WHERE id = ?", parado);

		String vera = cadastrarConfirmado("Vera Dona", emailUnico("vera"));
		Estabelecimento salao = estabelecimentoAprovado(vera, "Salão da Vera", cidadeUnica());
		List<HorarioDono> h = publicarHorarios(vera, salao.id(), daqui(2), daqui(3));
		jdbc.update("UPDATE horarios SET inicio = now() - interval '91 days' WHERE id = ?", h.get(0).id());
		jdbc.update("UPDATE horarios SET inicio = now() - interval '10 days' WHERE id = ?", h.get(1).id());

		Estabelecimento recusadoVelho = cadastrarEstabelecimento(vera, "Recusado Velho", cidadeUnica()).getBody();
		Estabelecimento recusadoNovo = cadastrarEstabelecimento(vera, "Recusado Novo", cidadeUnica()).getBody();
		jdbc.update("UPDATE estabelecimentos SET status = 'RECUSADO', revisado_em = now() - interval '31 days' WHERE id = ?",
				recusadoVelho.id());
		jdbc.update("UPDATE estabelecimentos SET status = 'RECUSADO', revisado_em = now() WHERE id = ?",
				recusadoNovo.id());

		retencao.executar(Instant.now());

		assertThat(existe("usuarios", parado)).isFalse();
		assertThat(existe("usuarios", recente)).isTrue();
		assertThat(existe("horarios", h.get(0).id())).isFalse();
		assertThat(existe("horarios", h.get(1).id())).isTrue();
		assertThat(existe("estabelecimentos", recusadoVelho.id())).isFalse();
		assertThat(existe("estabelecimentos", recusadoNovo.id())).isTrue();
		assertThat(existe("estabelecimentos", salao.id())).isTrue();
	}

}
