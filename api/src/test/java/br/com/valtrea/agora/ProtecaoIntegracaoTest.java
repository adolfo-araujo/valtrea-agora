package br.com.valtrea.agora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Limites por IP, tamanho máximo do pedido e cabeçalhos de segurança, com limites baixos só nesta classe. */
class ProtecaoIntegracaoTest extends IntegracaoBase {

	@DynamicPropertySource
	static void limitesBaixos(DynamicPropertyRegistry registro) {
		registro.add("valtrea.limites.cadastro-e-emails", () -> "3");
		registro.add("valtrea.limites.autenticacao", () -> "5");
		registro.add("valtrea.limites.acoes", () -> "3");
	}

	/** Cada teste usa um IP próprio (a Cloudflare preenche esse cabeçalho em produção). */
	private static String ipNovo() {
		return "203.0.113." + (1 + (int) (Math.random() * 250)) + "-" + UUID.randomUUID().toString().substring(0, 4);
	}

	private ResponseEntity<String> cadastroDe(String ip) {
		return http.post().uri("/api/v1/auth/cadastro").header("CF-Connecting-IP", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("nome", "Robo Teste", "email", emailUnico("robo"), "senha", "senha-forte-123"))
				.retrieve().toEntity(String.class);
	}

	private ResponseEntity<String> loginDe(String ip) {
		return http.post().uri("/api/v1/auth/login").header("CF-Connecting-IP", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", emailUnico("ninguem"), "senha", "senha-errada-1")).retrieve()
				.toEntity(String.class);
	}

	@Test
	void cadastrosDemaisDoMesmoIpSaoBarrados() {
		String ip = ipNovo();
		for (int i = 0; i < 3; i++) {
			assertThat(cadastroDe(ip).getStatusCode().value()).isEqualTo(201);
		}
		ResponseEntity<String> barrado = cadastroDe(ip);
		assertThat(barrado.getStatusCode().value()).isEqualTo(429);
		assertThat(barrado.getHeaders().getFirst("Retry-After")).isNotBlank();
		assertThat(barrado.getBody()).contains("Muitas tentativas");

		// Outro IP continua podendo se cadastrar.
		assertThat(cadastroDe(ipNovo()).getStatusCode().value()).isEqualTo(201);
	}

	@Test
	void tentativasDeLoginDemaisDoMesmoIpSaoBarradas() {
		String ip = ipNovo();
		for (int i = 0; i < 5; i++) {
			assertThat(loginDe(ip).getStatusCode().value()).isEqualTo(401);
		}
		assertThat(loginDe(ip).getStatusCode().value()).isEqualTo(429);
	}

	@Test
	void pedidoGrandeDemaisERecusado() {
		String enorme = "x".repeat(200 * 1024);
		ResponseEntity<String> resposta = http.post().uri("/api/v1/auth/login").header("CF-Connecting-IP", ipNovo())
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"email\":\"a@a.com\",\"senha\":\"" + enorme + "\"}").retrieve().toEntity(String.class);
		assertThat(resposta.getStatusCode().value()).isEqualTo(413);
	}

	@Test
	void acoesDemaisDoMesmoIpSaoBarradas() {
		String dono = cadastrarConfirmado("Dono Limite", emailUnico("dono-limite"));
		String ip = ipNovo();
		for (int i = 0; i < 3; i++) {
			assertThat(denunciaDe(ip, dono).getStatusCode().value()).isNotEqualTo(429);
		}
		assertThat(denunciaDe(ip, dono).getStatusCode().value()).isEqualTo(429);
	}

	/** Qualquer escrita em /estabelecimentos conta para o limite, mesmo a que responde 404. */
	private ResponseEntity<String> denunciaDe(String ip, String token) {
		return http.post().uri("/api/v1/estabelecimentos/{id}/denuncia", UUID.randomUUID()).header("CF-Connecting-IP", ip)
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("motivo", "FALSO")).retrieve().toEntity(String.class);
	}

	@Test
	void respostasTemCabecalhosDeSeguranca() {
		ResponseEntity<String> api = http.get().uri("/actuator/health").retrieve().toEntity(String.class);
		assertThat(api.getHeaders().getFirst("Content-Security-Policy")).isEqualTo("default-src 'none'; frame-ancestors 'none'");
		assertThat(api.getHeaders().getFirst("Permissions-Policy")).contains("camera=()");
		assertThat(api.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
		assertThat(api.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
	}

	@Test
	void corsSoLiberaOSiteDoAgora() {
		ResponseEntity<String> liberado = http.options().uri("/api/v1/eu/reservas")
				.header("Origin", "https://agora.valtrea.com.br").header("Access-Control-Request-Method", "GET")
				.retrieve().toEntity(String.class);
		assertThat(liberado.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo("https://agora.valtrea.com.br");

		ResponseEntity<String> outro = http.options().uri("/api/v1/eu/reservas")
				.header("Origin", "https://site-qualquer.com").header("Access-Control-Request-Method", "GET")
				.retrieve().toEntity(String.class);
		assertThat(outro.getHeaders().getFirst("Access-Control-Allow-Origin")).isNull();
	}

}
