package br.com.valtrea.agora.retencao;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rotina de retenção (LGPD, princípio da necessidade e arts. 15 e 16): apaga o que não precisa mais ser guardado.
 * <ul>
 * <li>Cadastros que nunca confirmaram o e-mail e não têm estabelecimento, 30 dias depois de criados.</li>
 * <li>Horários (e as reservas deles, com telefone e observação) 90 dias depois de passarem.</li>
 * <li>Estabelecimentos recusados, 30 dias depois da recusa.</li>
 * <li>Códigos de e-mail e sessões vencidos há mais de um dia.</li>
 * </ul>
 */
@Service
public class RetencaoService {

	public static final Duration CADASTRO_SEM_CONFIRMACAO = Duration.ofDays(30);
	public static final Duration HORARIOS_PASSADOS = Duration.ofDays(90);
	public static final Duration RECUSADOS = Duration.ofDays(30);
	private static final Duration FOLGA = Duration.ofDays(1);

	private static final Logger log = LoggerFactory.getLogger(RetencaoService.class);

	public record Resultado(int cadastrosNaoConfirmados, int horarios, int recusados, int codigos, int sessoes) {

		int total() {
			return cadastrosNaoConfirmados + horarios + recusados + codigos + sessoes;
		}

	}

	private final JdbcTemplate jdbc;

	public RetencaoService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional
	public Resultado executar(Instant agora) {
		int cadastros = jdbc.update("""
				DELETE FROM usuarios u
				 WHERE u.email_confirmado_em IS NULL
				   AND u.criado_em < ?
				   AND NOT EXISTS (SELECT 1 FROM estabelecimentos e WHERE e.dono_id = u.id)
				""", Timestamp.from(agora.minus(CADASTRO_SEM_CONFIRMACAO)));
		int horarios = jdbc.update("DELETE FROM horarios WHERE inicio < ?", Timestamp.from(agora.minus(HORARIOS_PASSADOS)));
		int recusados = jdbc.update("DELETE FROM estabelecimentos WHERE status = 'RECUSADO' AND revisado_em < ?",
				Timestamp.from(agora.minus(RECUSADOS)));
		Timestamp vencidos = Timestamp.from(agora.minus(FOLGA));
		int codigos = jdbc.update("DELETE FROM codigos_email WHERE expira_em < ?", vencidos);
		int sessoes = jdbc.update("DELETE FROM sessoes WHERE expira_em < ?", vencidos);
		return new Resultado(cadastros, horarios, recusados, codigos, sessoes);
	}

	/** Roda um minuto depois de a API subir e depois a cada 12 horas (os testes desligam o início automático). */
	@Scheduled(initialDelayString = "${valtrea.retencao.atraso-inicial-ms:60000}",
			fixedDelayString = "${valtrea.retencao.intervalo-ms:43200000}")
	public void agendada() {
		try {
			Resultado r = executar(Instant.now());
			if (r.total() > 0) {
				log.info("[retenção] cadastros não confirmados: {}; horários antigos: {}; recusados: {}; "
						+ "códigos e sessões vencidos: {}", r.cadastrosNaoConfirmados(), r.horarios(), r.recusados(),
						r.codigos() + r.sessoes());
			}
		} catch (RuntimeException e) {
			log.error("[retenção] falhou", e);
		}
	}

}
