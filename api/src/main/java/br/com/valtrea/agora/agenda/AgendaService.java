package br.com.valtrea.agora.agenda;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valtrea.agora.agenda.AgendaDtos.EstabelecimentoDaReserva;
import br.com.valtrea.agora.agenda.AgendaDtos.HorarioDono;
import br.com.valtrea.agora.agenda.AgendaDtos.HorarioPublico;
import br.com.valtrea.agora.agenda.AgendaDtos.HorariosRequest;
import br.com.valtrea.agora.agenda.AgendaDtos.ReservaCliente;
import br.com.valtrea.agora.agenda.AgendaDtos.ReservaParaDono;
import br.com.valtrea.agora.agenda.AgendaDtos.ReservaRequest;
import br.com.valtrea.agora.config.ValtreaProperties;
import br.com.valtrea.agora.email.Email;
import br.com.valtrea.agora.erro.NegocioException;
import br.com.valtrea.agora.estabelecimento.Estabelecimento;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoService;
import br.com.valtrea.agora.estabelecimento.Texto;
import br.com.valtrea.agora.estabelecimento.Tipo;
import br.com.valtrea.agora.usuario.Usuario;
import br.com.valtrea.agora.usuario.UsuarioRepository;

/**
 * Horários vagos e reservas. Sem pagamento: a reserva só guarda o horário. O id de quem pede vem sempre do token.
 */
@Service
public class AgendaService {

	/** Ninguém segura horários de todo mundo: até 3 reservas futuras confirmadas por pessoa. */
	static final int MAX_RESERVAS_FUTURAS = 3;
	static final int MAX_LIVRES_POR_ESTABELECIMENTO = 300;
	static final Duration ANTECEDENCIA_MINIMA = Duration.ofMinutes(10);
	static final Duration HORIZONTE = Duration.ofDays(60);
	private static final Duration AGENDA_PASSADA = Duration.ofDays(7);
	private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("EEE, dd/MM 'às' HH:mm",
			Locale.forLanguageTag("pt-BR"));
	private static final Set<String> FUSO_MANAUS = Set.of("AM", "RR", "RO", "MT", "MS");

	private final HorarioRepository horarios;
	private final ReservaRepository reservas;
	private final UsuarioRepository usuarios;
	private final EstabelecimentoService estabelecimentos;
	private final ApplicationEventPublisher eventos;
	private final ValtreaProperties.Site site;

	public AgendaService(HorarioRepository horarios, ReservaRepository reservas, UsuarioRepository usuarios,
			EstabelecimentoService estabelecimentos, ApplicationEventPublisher eventos,
			ValtreaProperties propriedades) {
		this.horarios = horarios;
		this.reservas = reservas;
		this.usuarios = usuarios;
		this.estabelecimentos = estabelecimentos;
		this.eventos = eventos;
		this.site = propriedades.site();
	}

	// ---------- público ----------

	@Transactional(readOnly = true)
	public List<HorarioPublico> livres(String uf, Tipo tipo, String busca) {
		Instant agora = Instant.now();
		String estado = uf == null || uf.isBlank() ? null : uf.strip().toUpperCase(Locale.ROOT);
		return horarios.livres(agora, agora.plus(EstabelecimentoService.JANELA_BUSCA), estado, tipo,
				Texto.normalizar(busca), Limit.of(200)).stream()
				.map(h -> new HorarioPublico(h.getId(), h.getInicio(), h.getDuracaoMin(), h.getServico(),
						h.getPrecoCentavos(), EstabelecimentoDtos.resumo(h.getEstabelecimento())))
				.toList();
	}

	// ---------- dono: horários ----------

	@Transactional
	public List<HorarioDono> publicar(UUID donoId, UUID estabelecimentoId, HorariosRequest pedido) {
		Estabelecimento e = estabelecimentos.doDono(donoId, estabelecimentoId);
		Instant agora = Instant.now();
		for (Instant inicio : pedido.inicios()) {
			if (inicio.isBefore(agora.plus(ANTECEDENCIA_MINIMA)) || inicio.isAfter(agora.plus(HORIZONTE))) {
				throw new NegocioException(HttpStatus.BAD_REQUEST,
						"Os horários precisam ser de daqui a 10 minutos até 60 dias.");
			}
		}
		long livres = horarios.countByEstabelecimentoIdAndStatusAndInicioAfter(estabelecimentoId, Horario.Status.LIVRE,
				agora);
		if (livres + pedido.inicios().size() > MAX_LIVRES_POR_ESTABELECIMENTO) {
			throw new NegocioException(HttpStatus.CONFLICT,
					"Limite de " + MAX_LIVRES_POR_ESTABELECIMENTO + " horários vagos ao mesmo tempo.");
		}
		String servico = Texto.arrumar(pedido.servico());
		pedido.inicios().stream().distinct().forEach(inicio -> horarios
				.save(new Horario(e, inicio, pedido.duracaoMin(), servico, pedido.precoCentavos())));
		return agenda(donoId, estabelecimentoId);
	}

	@Transactional(readOnly = true)
	public List<HorarioDono> agenda(UUID donoId, UUID estabelecimentoId) {
		estabelecimentos.doDono(donoId, estabelecimentoId);
		List<Horario> lista = horarios.findByEstabelecimentoIdAndInicioAfterOrderByInicioAsc(estabelecimentoId,
				Instant.now().minus(AGENDA_PASSADA));
		Map<UUID, Reserva> porHorario = lista.isEmpty() ? Map.of()
				: reservas.dosHorarios(lista.stream().map(Horario::getId).toList()).stream()
						.collect(Collectors.toMap(r -> r.getHorario().getId(), Function.identity(), (a, b) -> a));
		return lista.stream().map(h -> {
			Reserva r = porHorario.get(h.getId());
			ReservaParaDono reserva = r == null ? null
					: new ReservaParaDono(r.getId(), r.getCliente().getPrimeiroNome(), r.getTelefone(),
							r.getObservacao(), r.getStatus());
			return new HorarioDono(h.getId(), h.getInicio(), h.getDuracaoMin(), h.getServico(), h.getPrecoCentavos(),
					h.getStatus(), reserva);
		}).toList();
	}

	/** Só horário livre: o reservado precisa ser cancelado antes (e o cliente é avisado). */
	@Transactional
	public void apagarHorario(UUID donoId, UUID horarioId) {
		Horario h = horarios.findById(horarioId).filter(x -> x.getEstabelecimento().ehDo(donoId))
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Horário não encontrado."));
		if (h.getStatus() == Horario.Status.RESERVADO) {
			throw new NegocioException(HttpStatus.CONFLICT, "Este horário tem reserva. Cancele a reserva antes.");
		}
		horarios.delete(h);
	}

	// ---------- cliente: reservar ----------

	@Transactional
	public ReservaCliente reservar(UUID clienteId, UUID horarioId, ReservaRequest pedido) {
		Usuario cliente = usuarios.findById(clienteId)
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
		if (!cliente.isEmailConfirmado()) {
			throw new NegocioException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de reservar.");
		}
		Instant agora = Instant.now();
		Horario h = horarios.travar(horarioId)
				.filter(x -> x.getEstabelecimento().isPublico())
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Horário não encontrado."));
		if (h.getEstabelecimento().ehDo(clienteId)) {
			throw new NegocioException(HttpStatus.CONFLICT, "Este horário é do seu próprio estabelecimento.");
		}
		if (h.getStatus() != Horario.Status.LIVRE || h.jaComecou(agora)) {
			throw new NegocioException(HttpStatus.CONFLICT, "Que pena: alguém acabou de reservar este horário.");
		}
		if (reservas.confirmadasFuturas(clienteId, agora) >= MAX_RESERVAS_FUTURAS) {
			throw new NegocioException(HttpStatus.CONFLICT, "Você já tem " + MAX_RESERVAS_FUTURAS
					+ " reservas marcadas. Cancele uma ou espere passar para reservar outra.");
		}
		h.reservar();
		Reserva r = reservas.saveAndFlush(new Reserva(h, cliente, Texto.vazioParaNulo(pedido.telefone()),
				Texto.vazioParaNulo(pedido.observacao())));
		Estabelecimento e = h.getEstabelecimento();
		Usuario dono = e.getDono();
		eventos.publishEvent(Email.novaReserva(dono.getEmail(), dono.getNome(), e.getNome(), h.getServico(),
				quando(h), cliente.getPrimeiroNome(), site.link("/negocio")));
		return resposta(r);
	}

	@Transactional(readOnly = true)
	public List<ReservaCliente> minhas(UUID clienteId) {
		return reservas.doCliente(clienteId).stream().map(AgendaService::resposta).toList();
	}

	/** O cliente cancela antes do horário (ele volta a ficar livre) ou o estabelecimento cancela (não volta). */
	@Transactional
	public void cancelar(UUID usuarioId, UUID reservaId) {
		Reserva r = reservas.findById(reservaId)
				.filter(x -> x.ehDo(usuarioId) || x.getHorario().getEstabelecimento().ehDo(usuarioId))
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
		if (r.getStatus() != Reserva.Status.CONFIRMADA) {
			throw new NegocioException(HttpStatus.CONFLICT, "Esta reserva já foi encerrada.");
		}
		Horario h = r.getHorario();
		Estabelecimento e = h.getEstabelecimento();
		boolean peloCliente = r.ehDo(usuarioId);
		if (peloCliente) {
			if (h.jaComecou(Instant.now())) {
				throw new NegocioException(HttpStatus.CONFLICT, "O horário já passou: não dá mais para cancelar.");
			}
			r.mudar(Reserva.Status.CANCELADA_CLIENTE);
			h.liberar();
			Usuario dono = e.getDono();
			eventos.publishEvent(Email.reservaCancelada(dono.getEmail(), dono.getNome(), e.getNome(), h.getServico(),
					quando(h), r.getCliente().getPrimeiroNome(), site.link("/negocio")));
		} else {
			r.mudar(Reserva.Status.CANCELADA_ESTABELECIMENTO);
			h.cancelar();
			Usuario cliente = r.getCliente();
			eventos.publishEvent(Email.reservaCancelada(cliente.getEmail(), cliente.getNome(), e.getNome(),
					h.getServico(), quando(h), e.getNome(), site.link("/reservas")));
		}
	}

	/** Depois do horário, o estabelecimento marca se a pessoa veio. */
	@Transactional
	public void marcar(UUID donoId, UUID reservaId, Reserva.Status status) {
		if (status != Reserva.Status.ATENDIDA && status != Reserva.Status.NAO_COMPARECEU) {
			throw new NegocioException(HttpStatus.BAD_REQUEST, "Marque como atendida ou como não compareceu.");
		}
		Reserva r = reservas.findById(reservaId).filter(x -> x.getHorario().getEstabelecimento().ehDo(donoId))
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
		if (r.getStatus() != Reserva.Status.CONFIRMADA && r.getStatus() != Reserva.Status.ATENDIDA
				&& r.getStatus() != Reserva.Status.NAO_COMPARECEU) {
			throw new NegocioException(HttpStatus.CONFLICT, "Esta reserva foi cancelada.");
		}
		if (!r.getHorario().jaComecou(Instant.now())) {
			throw new NegocioException(HttpStatus.CONFLICT, "Só dá para marcar depois do horário.");
		}
		r.mudar(status);
	}

	// ---------- auxiliares ----------

	private static ReservaCliente resposta(Reserva r) {
		Horario h = r.getHorario();
		Estabelecimento e = h.getEstabelecimento();
		return new ReservaCliente(r.getId(), r.getStatus(), r.getCriadoEm(), h.getInicio(), h.getDuracaoMin(),
				h.getServico(), h.getPrecoCentavos(), r.getTelefone(), r.getObservacao(), new EstabelecimentoDaReserva(e.getId(), e.getNome(),
						e.getEndereco(), e.getBairro(), e.getCidade(), e.getUf(), e.getTelefone()));
	}

	/** Data e hora no fuso do estabelecimento (o Brasil tem quatro). */
	static String quando(Horario h) {
		String uf = h.getEstabelecimento().getUf();
		ZoneId fuso = "AC".equals(uf) ? ZoneId.of("America/Rio_Branco")
				: FUSO_MANAUS.contains(uf) ? ZoneId.of("America/Manaus") : ZoneId.of("America/Sao_Paulo");
		return FORMATO.format(h.getInicio().atZone(fuso));
	}

}
