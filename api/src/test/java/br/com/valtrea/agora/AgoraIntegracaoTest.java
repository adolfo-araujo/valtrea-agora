package br.com.valtrea.agora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** O fluxo principal: o estabelecimento é aprovado, publica horários, o cliente reserva; cancelamentos e análise. */
class AgoraIntegracaoTest extends IntegracaoBase {

	@Autowired
	JdbcTemplate jdbc;

	private List<HorarioDono> agenda(String token, UUID estabelecimento) {
		return http.get().uri("/api/v1/estabelecimentos/{id}/agenda", estabelecimento)
				.header("Authorization", "Bearer " + token).retrieve()
				.body(new ParameterizedTypeReference<List<HorarioDono>>() {
				});
	}

	private ResponseEntity<Void> cancelar(String token, UUID reserva) {
		return post("/api/v1/reservas/" + reserva + "/cancelar", token, Map.of());
	}

	private String admin() {
		String email = emailUnico("admin");
		String token = cadastrarConfirmado("Ana Admin", email);
		jdbc.update("UPDATE usuarios SET papel = 'ADMINISTRADOR' WHERE email = ?", email);
		return token;
	}

	@Test
	void estabelecimentoSoApareceDepoisDeAprovado() {
		String emailDono = emailUnico("dani");
		String dani = cadastrarConfirmado("Dani Souza", emailDono);
		String cidade = cidadeUnica();
		Estabelecimento salao = cadastrarEstabelecimento(dani, "Salão da Dani", cidade).getBody();
		assertThat(salao.status()).isEqualTo("EM_ANALISE");
		publicarHorarios(dani, salao.id(), daqui(3));

		// Em análise: não aparece em nenhuma busca nem pelo endereço direto.
		assertThat(publico("/estabelecimentos?busca=" + cidade).getBody()).isEqualTo("[]");
		assertThat(publico("/horarios?busca=" + cidade).getBody()).isEqualTo("[]");
		assertThat(publico("/estabelecimentos/" + salao.id()).getStatusCode().value()).isEqualTo(404);

		// A administração vê na fila, recusa com motivo; o dono corrige e volta para a fila; aí aprova.
		String admin = admin();
		assertThat(http.get().uri("/api/v1/admin/estabelecimentos").header("Authorization", "Bearer " + dani).retrieve()
				.toBodilessEntity().getStatusCode().value()).isEqualTo(403);
		String fila = get("/api/v1/admin/estabelecimentos", admin, String.class);
		assertThat(fila).contains(salao.id().toString(), "\"dono\":\"Dani\"").doesNotContain(emailDono, "Souza");
		assertThat(post("/api/v1/admin/estabelecimentos/" + salao.id() + "/recusar", admin,
				Map.of("motivo", "Confira o endereço.")).getStatusCode().value()).isEqualTo(204);
		assertThat(emails.para(emailDono).getLast().getText()).contains("Confira o endereço.");
		assertThat(get("/api/v1/eu/estabelecimentos", dani, String.class)).contains("RECUSADO", "Confira o endereço.");

		Map<String, Object> corrigido = Map.of("nome", "Salão da Dani", "tipo", "SALAO", "endereco",
				"Rua das Flores, 120", "bairro", "Centro", "cidade", cidade, "uf", "RJ", "telefone", "(21) 99999-0000");
		assertThat(put("/api/v1/estabelecimentos/" + salao.id(), dani, corrigido).getStatusCode().value()).isEqualTo(200);
		assertThat(get("/api/v1/eu/estabelecimentos", dani, String.class)).contains("EM_ANALISE");
		assertThat(post("/api/v1/admin/estabelecimentos/" + salao.id() + "/aprovar", admin, Map.of()).getStatusCode()
				.value()).isEqualTo(204);
		assertThat(emails.para(emailDono).getLast().getSubject()).contains("já aparece");

		// Aprovado: aparece na busca sem acento e mostra os dados do negócio, nada do dono.
		ResponseEntity<String> busca = publico("/horarios?uf=rj&busca=" + cidade.toLowerCase());
		assertThat(busca.getBody()).contains("Salão da Dani", "Corte masculino").doesNotContain(emailDono, "Dani Souza");
		assertThat(publico("/estabelecimentos/" + salao.id()).getBody()).contains("Rua das Flores, 120",
				"(21) 99999-0000");
	}

	@Test
	void clienteReservaEOEstabelecimentoVeSoOPrimeiroNome() {
		String emailDono = emailUnico("beto");
		String beto = cadastrarConfirmado("Beto Barbeiro", emailDono);
		Estabelecimento barbearia = estabelecimentoAprovado(beto, "Barbearia do Beto", cidadeUnica());
		List<HorarioDono> publicados = publicarHorarios(beto, barbearia.id(), daqui(2), daqui(3), daqui(4));
		assertThat(publicados).hasSize(3).allMatch(h -> h.status().equals("LIVRE"));
		UUID h1 = publicados.get(0).id();

		String emailCarla = emailUnico("carla");
		String carla = cadastrarConfirmado("Carla Mendes Lima", emailCarla);
		ResponseEntity<Reserva> reserva = reservar(carla, h1);
		assertThat(reserva.getStatusCode().value()).isEqualTo(201);
		assertThat(reserva.getBody().status()).isEqualTo("CONFIRMADA");
		assertThat(reserva.getBody().estabelecimento()).containsEntry("endereco", "Rua das Flores, 100");

		// O dono recebe o e-mail com o primeiro nome e vê na agenda só o que a cliente informou.
		assertThat(emails.para(emailDono).getLast().getText()).contains("Carla").doesNotContain("Mendes", emailCarla);
		HorarioDono naAgenda = agenda(beto, barbearia.id()).getFirst();
		assertThat(naAgenda.status()).isEqualTo("RESERVADO");
		assertThat(naAgenda.reserva().cliente()).isEqualTo("Carla");
		assertThat(naAgenda.reserva().telefone()).isEqualTo("(21) 98888-7777");
		assertThat(get("/api/v1/estabelecimentos/" + barbearia.id() + "/agenda", beto, String.class))
				.doesNotContain(emailCarla, "Mendes");

		// O horário reservado some da busca; outra pessoa não consegue reservar o mesmo.
		assertThat(publico("/estabelecimentos/" + barbearia.id()).getBody()).doesNotContain(h1.toString());
		String davi = cadastrarConfirmado("Davi Cliente", emailUnico("davi"));
		assertThat(reservar(davi, h1).getStatusCode().value()).isEqualTo(409);
		// Nem o dono no próprio estabelecimento, nem quem não é dono vê a agenda.
		assertThat(reservar(beto, publicados.get(1).id()).getStatusCode().value()).isEqualTo(409);
		assertThat(http.get().uri("/api/v1/estabelecimentos/{id}/agenda", barbearia.id())
				.header("Authorization", "Bearer " + davi).retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(404);

		// A cliente cancela: o horário volta a ficar livre e o dono é avisado.
		assertThat(cancelar(carla, reserva.getBody().id()).getStatusCode().value()).isEqualTo(204);
		assertThat(emails.para(emailDono).getLast().getSubject()).startsWith("Reserva cancelada");
		assertThat(agenda(beto, barbearia.id()).getFirst().status()).isEqualTo("LIVRE");
		ResponseEntity<Reserva> deNovo = reservar(davi, h1);
		assertThat(deNovo.getStatusCode().value()).isEqualTo(201);

		// O estabelecimento cancela: a cliente é avisada e o horário não volta para a busca.
		String emailDavi = emails.enviados.stream().flatMap(m -> List.of(m.getTo()).stream())
				.filter(e -> e.startsWith("davi-")).findFirst().orElseThrow();
		assertThat(cancelar(beto, deNovo.getBody().id()).getStatusCode().value()).isEqualTo(204);
		assertThat(emails.para(emailDavi).getLast().getSubject()).startsWith("Reserva cancelada");
		assertThat(agenda(beto, barbearia.id()).getFirst().status()).isEqualTo("CANCELADO");
		assertThat(reservar(carla, h1).getStatusCode().value()).isEqualTo(409);

		// Horário com reserva não pode ser apagado; livre pode.
		UUID h2 = publicados.get(1).id();
		reservar(carla, h2);
		assertThat(http.delete().uri("/api/v1/horarios/{id}", h2).header("Authorization", "Bearer " + beto).retrieve()
				.toBodilessEntity().getStatusCode().value()).isEqualTo(409);
		assertThat(http.delete().uri("/api/v1/horarios/{id}", publicados.get(2).id())
				.header("Authorization", "Bearer " + beto).retrieve().toBodilessEntity().getStatusCode().value())
				.isEqualTo(204);
	}

	@Test
	void doisCliquesAoMesmoTempoSoUmReserva() throws Exception {
		String dono = cadastrarConfirmado("Edu Dono", emailUnico("edu"));
		Estabelecimento salao = estabelecimentoAprovado(dono, "Salão do Edu", cidadeUnica());
		UUID horario = publicarHorarios(dono, salao.id(), daqui(5)).getFirst().id();
		List<String> clientes = List.of(cadastrarConfirmado("Fe Um", emailUnico("fe")),
				cadastrarConfirmado("Gui Dois", emailUnico("gui")), cadastrarConfirmado("Hel Tres", emailUnico("hel")));

		List<CompletableFuture<Integer>> tentativas = clientes.stream()
				.map(t -> CompletableFuture.supplyAsync(() -> reservar(t, horario).getStatusCode().value())).toList();
		List<Integer> resultados = tentativas.stream().map(CompletableFuture::join).toList();
		assertThat(resultados).containsOnly(201, 409).filteredOn(c -> c == 201).hasSize(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM reservas WHERE horario_id = ?", Integer.class, horario))
				.isEqualTo(1);
	}

	@Test
	void ateTresReservasFuturasPorPessoa() {
		String dono = cadastrarConfirmado("Ivo Dono", emailUnico("ivo"));
		Estabelecimento salao = estabelecimentoAprovado(dono, "Salão do Ivo", cidadeUnica());
		List<HorarioDono> h = publicarHorarios(dono, salao.id(), daqui(1), daqui(2), daqui(3), daqui(4));
		String jo = cadastrarConfirmado("Jo Apressada", emailUnico("jo"));
		for (int i = 0; i < 3; i++) {
			assertThat(reservar(jo, h.get(i).id()).getStatusCode().value()).isEqualTo(201);
		}
		assertThat(reservar(jo, h.get(3).id()).getStatusCode().value()).isEqualTo(409);
		assertThat(get("/api/v1/eu/reservas", jo, String.class)).contains("Salão do Ivo");
	}

	@Test
	void horariosForaDoPrazoEPresencaSoDepoisDoHorario() {
		String dono = cadastrarConfirmado("Lu Dona", emailUnico("lu"));
		Estabelecimento salao = estabelecimentoAprovado(dono, "Salão da Lu", cidadeUnica());
		ResponseEntity<String> passado = http.post().uri("/api/v1/estabelecimentos/{id}/horarios", salao.id())
				.header("Authorization", "Bearer " + dono).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("servico", "Escova", "duracaoMin", 40, "inicios", List.of(daqui(-1)))).retrieve()
				.toEntity(String.class);
		assertThat(passado.getStatusCode().value()).isEqualTo(400);
		ResponseEntity<String> longe = http.post().uri("/api/v1/estabelecimentos/{id}/horarios", salao.id())
				.header("Authorization", "Bearer " + dono).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("servico", "Escova", "duracaoMin", 40, "inicios", List.of(daqui(24 * 61)))).retrieve()
				.toEntity(String.class);
		assertThat(longe.getStatusCode().value()).isEqualTo(400);

		UUID horario = publicarHorarios(dono, salao.id(), daqui(2)).getFirst().id();
		String max = cadastrarConfirmado("Max Cliente", emailUnico("max"));
		UUID reserva = reservar(max, horario).getBody().id();
		assertThat(post("/api/v1/reservas/" + reserva + "/status", dono, Map.of("status", "NAO_COMPARECEU"))
				.getStatusCode().value()).as("antes do horário").isEqualTo(409);
		jdbc.update("UPDATE horarios SET inicio = now() - interval '1 hour' WHERE id = ?", horario);
		assertThat(cancelar(max, reserva).getStatusCode().value()).as("o cliente não cancela depois").isEqualTo(409);
		assertThat(post("/api/v1/reservas/" + reserva + "/status", max, Map.of("status", "ATENDIDA")).getStatusCode()
				.value()).as("só o dono marca").isEqualTo(404);
		assertThat(post("/api/v1/reservas/" + reserva + "/status", dono, Map.of("status", "ATENDIDA")).getStatusCode()
				.value()).isEqualTo(204);
		assertThat(get("/api/v1/eu/reservas", max, String.class)).contains("ATENDIDA");
	}

	@Test
	void tresDenunciasTiramDaBuscaAteAAnalise() {
		String emailDono = emailUnico("nina");
		String nina = cadastrarConfirmado("Nina Dona", emailDono);
		String cidade = cidadeUnica();
		Estabelecimento salao = estabelecimentoAprovado(nina, "Salão da Nina", cidade);
		publicarHorarios(nina, salao.id(), daqui(6));
		assertThat(post("/api/v1/estabelecimentos/" + salao.id() + "/denuncia", nina, Map.of("motivo", "FALSO"))
				.getStatusCode().value()).isEqualTo(409);

		String[] clientes = { cadastrarConfirmado("Oto Um", emailUnico("oto")),
				cadastrarConfirmado("Pia Dois", emailUnico("pia")), cadastrarConfirmado("Rai Tres", emailUnico("rai")) };
		post("/api/v1/estabelecimentos/" + salao.id() + "/denuncia", clientes[0], Map.of("motivo", "GOLPE"));
		post("/api/v1/estabelecimentos/" + salao.id() + "/denuncia", clientes[0], Map.of("motivo", "GOLPE"));
		post("/api/v1/estabelecimentos/" + salao.id() + "/denuncia", clientes[1], Map.of("motivo", "GOLPE"));
		assertThat(publico("/estabelecimentos/" + salao.id()).getStatusCode().value()).isEqualTo(200);
		post("/api/v1/estabelecimentos/" + salao.id() + "/denuncia", clientes[2], Map.of("motivo", "NAO_RESPEITA"));

		assertThat(publico("/estabelecimentos/" + salao.id()).getStatusCode().value()).isEqualTo(404);
		assertThat(publico("/horarios?busca=" + cidade).getBody()).isEqualTo("[]");
		assertThat(emails.para(emailDono).getLast().getSubject()).contains("revisão");
		String fila = get("/api/v1/admin/estabelecimentos", admin(), String.class);
		assertThat(fila).contains(salao.id().toString(), "\"GOLPE\":2", "\"NAO_RESPEITA\":1")
				.doesNotContain("Oto", "Pia", "Rai");
	}

}
