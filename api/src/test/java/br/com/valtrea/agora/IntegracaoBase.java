package br.com.valtrea.agora;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe a API com um PostgreSQL real e fala com ela por HTTP, como o app faria. O banco é um só para todas as classes
 * de teste (cada teste usa e-mails e estabelecimentos próprios), e os e-mails ficam numa caixa falsa em vez de sair por SMTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"valtrea.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
		"valtrea.site.url=https://agora.exemplo.com", "valtrea.limites.geral=1000000",
		"valtrea.limites.autenticacao=1000000", "valtrea.limites.cadastro-e-emails=1000000",
		"valtrea.limites.acoes=1000000", "valtrea.retencao.atraso-inicial-ms=86400000",
		"valtrea.admin-emails=Chefe-Agora@Exemplo.com" })
@Import(IntegracaoBase.Emails.class)
abstract class IntegracaoBase {

	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	@TestConfiguration
	static class Emails {
		@Bean
		CaixaDeEmails caixaDeEmails() {
			return new CaixaDeEmails();
		}
	}

	/** Guarda os e-mails em memória. Substitui o JavaMailSender de verdade. */
	static class CaixaDeEmails extends JavaMailSenderImpl {
		final List<SimpleMailMessage> enviados = new CopyOnWriteArrayList<>();

		@Override
		public void send(SimpleMailMessage... mensagens) {
			enviados.addAll(List.of(mensagens));
		}

		List<SimpleMailMessage> para(String email) {
			return enviados.stream().filter(m -> List.of(m.getTo()).contains(email)).toList();
		}

		String ultimoCodigo(String email) {
			List<SimpleMailMessage> recebidos = para(email);
			assertThat(recebidos).as("e-mails para " + email).isNotEmpty();
			Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(recebidos.getLast().getText());
			assertThat(m.find()).isTrue();
			return m.group(1);
		}
	}

	record Tokens(String accessToken, String refreshToken, Instant expiraEm) {
	}

	record Perfil(UUID id, String nome, String email, boolean emailConfirmado, boolean termosAceitos, boolean admin) {
	}

	/** Tipos com caixa: a mesma classe lê as respostas de erro (sem esses campos) nos testes que esperam 4xx. */
	record Estabelecimento(UUID id, String nome, String tipo, String endereco, String cidade, String uf, String telefone,
			String status, String motivoRecusa) {
	}

	record ReservaDono(UUID id, String cliente, String telefone, String observacao, String status) {
	}

	record HorarioDono(UUID id, Instant inicio, Integer duracaoMin, String servico, Integer precoCentavos, String status,
			ReservaDono reserva) {
	}

	record Reserva(UUID id, String status, Instant inicio, String servico, Map<String, Object> estabelecimento) {
	}

	@Value("${local.server.port}")
	int porta;

	@Autowired
	CaixaDeEmails emails;

	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbcBase;

	org.springframework.jdbc.core.JdbcTemplate jdbc() {
		return jdbcBase;
	}

	RestClient http;

	@BeforeEach
	void configurarHttp() {
		http = RestClient.builder().baseUrl("http://localhost:" + porta)
				.defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
				}).build();
	}

	static String emailUnico(String nome) {
		return nome + "-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.com";
	}

	Tokens cadastrar(String nome, String email) {
		return http.post().uri("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", nome, "email", email, "senha", "senha-forte-123"))
				.retrieve().body(Tokens.class);
	}

	/** Cadastra e confirma o e-mail com o código que chegou na caixa. Devolve o token de acesso. */
	String cadastrarConfirmado(String nome, String email) {
		String token = cadastrar(nome, email).accessToken();
		assertThat(confirmarEmail(token, emails.ultimoCodigo(email)).getStatusCode().value()).isEqualTo(204);
		return token;
	}

	ResponseEntity<Void> confirmarEmail(String token, String codigo) {
		return post("/api/v1/auth/confirmar-email", token, Map.of("codigo", codigo));
	}

	ResponseEntity<Tokens> login(String email, String senha) {
		return http.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "senha", senha)).retrieve().toEntity(Tokens.class);
	}

	Perfil perfil(String token) {
		return http.get().uri("/api/v1/eu").header("Authorization", "Bearer " + token).retrieve().body(Perfil.class);
	}

	/** Uma cidade própria para cada teste, para as buscas não se misturarem. */
	static String cidadeUnica() {
		return "Cidade " + UUID.randomUUID().toString().substring(0, 8);
	}

	ResponseEntity<Estabelecimento> cadastrarEstabelecimento(String token, String nome, String cidade) {
		return http.post().uri("/api/v1/estabelecimentos").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", nome, "tipo", "BARBEARIA", "descricao", "Corte e barba", "endereco",
						"Rua das Flores, 100", "bairro", "Centro", "cidade", cidade, "uf", "RJ", "telefone",
						"(21) 99999-0000"))
				.retrieve().toEntity(Estabelecimento.class);
	}

	/** Cadastra e já aprova direto no banco (a aprovação pela administração tem teste próprio). */
	Estabelecimento estabelecimentoAprovado(String token, String nome, String cidade) {
		ResponseEntity<Estabelecimento> r = cadastrarEstabelecimento(token, nome, cidade);
		assertThat(r.getStatusCode().value()).isEqualTo(201);
		jdbc().update("UPDATE estabelecimentos SET status = 'APROVADO' WHERE id = ?", r.getBody().id());
		return r.getBody();
	}

	List<HorarioDono> publicarHorarios(String token, UUID estabelecimentoId, Instant... inicios) {
		ResponseEntity<List<HorarioDono>> r = http.post().uri("/api/v1/estabelecimentos/{id}/horarios", estabelecimentoId)
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("servico", "Corte masculino", "duracaoMin", 30, "precoCentavos", 4500, "inicios",
						List.of(inicios)))
				.retrieve().toEntity(new org.springframework.core.ParameterizedTypeReference<List<HorarioDono>>() {
				});
		assertThat(r.getStatusCode().value()).isEqualTo(201);
		return r.getBody();
	}

	ResponseEntity<Reserva> reservar(String token, UUID horarioId) {
		return http.post().uri("/api/v1/horarios/{id}/reserva", horarioId).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("telefone", "(21) 98888-7777", "observacao", "Chego 5 min antes"))
				.retrieve().toEntity(Reserva.class);
	}

	static Instant daqui(long horas) {
		return Instant.now().plus(java.time.Duration.ofHours(horas)).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
	}

	ResponseEntity<String> publico(String rota) {
		return http.get().uri("/api/v1/publico" + rota).retrieve().toEntity(String.class);
	}

	ResponseEntity<Void> put(String rota, String token, Object corpo) {
		return http.put().uri(rota).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.body(corpo).retrieve().toBodilessEntity();
	}

	<T> T get(String rota, String token, Class<T> tipo) {
		return http.get().uri(rota).header("Authorization", "Bearer " + token).retrieve().body(tipo);
	}

	ResponseEntity<Void> post(String rota, String token, Object corpo) {
		var pedido = http.post().uri(rota).contentType(MediaType.APPLICATION_JSON).body(corpo);
		if (token != null) {
			pedido = pedido.header("Authorization", "Bearer " + token);
		}
		return pedido.retrieve().toBodilessEntity();
	}

}
