package br.com.valtrea.agora.estabelecimento;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valtrea.agora.agenda.HorarioRepository;
import br.com.valtrea.agora.config.ValtreaProperties;
import br.com.valtrea.agora.email.Email;
import br.com.valtrea.agora.erro.NegocioException;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoDetalhe;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoPublico;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoRequest;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.HorarioLivre;
import br.com.valtrea.agora.usuario.Usuario;
import br.com.valtrea.agora.usuario.UsuarioRepository;

/**
 * Cadastro dos estabelecimentos e o que o público vê deles. Só aprovados aparecem; para quem não é o dono, um
 * estabelecimento em análise ou recusado responde 404, como se não existisse.
 */
@Service
public class EstabelecimentoService {

	static final int MAX_POR_DONO = 3;
	/** Com 3 denúncias de pessoas diferentes, sai da busca até a administração revisar. */
	static final int DENUNCIAS_PARA_ANALISE = 3;
	public static final Duration JANELA_BUSCA = Duration.ofDays(7);
	public static final Duration JANELA_DETALHE = Duration.ofDays(14);

	private final EstabelecimentoRepository estabelecimentos;
	private final DenunciaRepository denuncias;
	private final HorarioRepository horarios;
	private final UsuarioRepository usuarios;
	private final ApplicationEventPublisher eventos;
	private final ValtreaProperties.Site site;

	public EstabelecimentoService(EstabelecimentoRepository estabelecimentos, DenunciaRepository denuncias,
			HorarioRepository horarios, UsuarioRepository usuarios, ApplicationEventPublisher eventos,
			ValtreaProperties propriedades) {
		this.estabelecimentos = estabelecimentos;
		this.denuncias = denuncias;
		this.horarios = horarios;
		this.usuarios = usuarios;
		this.eventos = eventos;
		this.site = propriedades.site();
	}

	// ---------- público ----------

	@Transactional(readOnly = true)
	public List<EstabelecimentoPublico> buscar(String uf, Tipo tipo, String busca) {
		List<Estabelecimento> lista = estabelecimentos.buscar(uf(uf), tipo, Texto.normalizar(busca), Limit.of(50));
		if (lista.isEmpty()) {
			return List.of();
		}
		Instant agora = Instant.now();
		Map<UUID, Long> livres = new HashMap<>();
		for (Object[] linha : horarios.contarLivres(lista.stream().map(Estabelecimento::getId).toList(), agora,
				agora.plus(JANELA_BUSCA))) {
			livres.put((UUID) linha[0], (Long) linha[1]);
		}
		return lista.stream().map(e -> EstabelecimentoDtos.publico(e, livres.getOrDefault(e.getId(), 0L))).toList();
	}

	@Transactional(readOnly = true)
	public EstabelecimentoDetalhe detalhe(UUID id, UUID quemPede) {
		Estabelecimento e = estabelecimentos.findById(id)
				.filter(x -> x.isPublico() || (quemPede != null && x.ehDo(quemPede)))
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado."));
		Instant agora = Instant.now();
		List<HorarioLivre> livres = horarios.livresDo(id, agora, agora.plus(JANELA_DETALHE)).stream()
				.map(h -> new HorarioLivre(h.getId(), h.getInicio(), h.getDuracaoMin(), h.getServico(),
						h.getPrecoCentavos()))
				.toList();
		boolean denunciei = quemPede != null && denuncias.existsById(new Denuncia.Chave(id, quemPede));
		return new EstabelecimentoDetalhe(EstabelecimentoDtos.publico(e, livres.size()), livres, denunciei,
				quemPede != null && e.ehDo(quemPede));
	}

	// ---------- dono ----------

	@Transactional(readOnly = true)
	public List<EstabelecimentoResponse> meus(UUID donoId) {
		return estabelecimentos.findByDonoIdOrderByCriadoEmAsc(donoId).stream().map(EstabelecimentoDtos::resposta)
				.toList();
	}

	@Transactional
	public EstabelecimentoResponse criar(UUID donoId, EstabelecimentoRequest pedido) {
		Usuario dono = confirmado(donoId, "cadastrar um estabelecimento");
		if (estabelecimentos.countByDonoId(donoId) >= MAX_POR_DONO) {
			throw new NegocioException(HttpStatus.CONFLICT,
					"Cada conta pode ter até " + MAX_POR_DONO + " estabelecimentos.");
		}
		Estabelecimento e = new Estabelecimento(dono);
		preencher(e, pedido);
		return EstabelecimentoDtos.resposta(estabelecimentos.save(e));
	}

	@Transactional
	public EstabelecimentoResponse editar(UUID donoId, UUID id, EstabelecimentoRequest pedido) {
		Estabelecimento e = doDono(donoId, id);
		preencher(e, pedido);
		return EstabelecimentoDtos.resposta(e);
	}

	/** Apaga o estabelecimento com os horários e as reservas. */
	@Transactional
	public void excluir(UUID donoId, UUID id) {
		estabelecimentos.delete(doDono(donoId, id));
	}

	public Estabelecimento doDono(UUID donoId, UUID id) {
		return estabelecimentos.findById(id).filter(e -> e.ehDo(donoId))
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado."));
	}

	// ---------- denúncia ----------

	@Transactional
	public void denunciar(UUID usuarioId, UUID id, MotivoDenuncia motivo) {
		confirmado(usuarioId, "denunciar");
		Estabelecimento e = estabelecimentos.findById(id).filter(Estabelecimento::isPublico)
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado."));
		if (e.ehDo(usuarioId)) {
			throw new NegocioException(HttpStatus.CONFLICT, "Você não pode denunciar o seu próprio estabelecimento.");
		}
		if (denuncias.existsById(new Denuncia.Chave(id, usuarioId))) {
			return;
		}
		denuncias.saveAndFlush(new Denuncia(id, usuarioId, motivo));
		if (denuncias.countByIdEstabelecimentoId(id) >= DENUNCIAS_PARA_ANALISE) {
			e.suspenderParaAnalise();
			Usuario dono = e.getDono();
			eventos.publishEvent(Email.estabelecimentoEmAnalise(dono.getEmail(), dono.getNome(), e.getNome(),
					site.link("/negocio")));
		}
	}

	// ---------- auxiliares ----------

	private void preencher(Estabelecimento e, EstabelecimentoRequest p) {
		String uf = p.uf().strip().toUpperCase(Locale.ROOT);
		if (!Texto.UFS.contains(uf)) {
			throw new NegocioException(HttpStatus.BAD_REQUEST, "Estado inválido. Use a sigla, como SP ou RJ.");
		}
		e.preencher(p.nome(), p.tipo(), p.descricao(), p.endereco(), p.bairro(), p.cidade(), uf, p.telefone());
	}

	private Usuario confirmado(UUID usuarioId, String acao) {
		Usuario u = usuarios.findById(usuarioId)
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
		if (!u.isEmailConfirmado()) {
			throw new NegocioException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de " + acao + ".");
		}
		return u;
	}

	static String uf(String uf) {
		return uf == null || uf.isBlank() ? null : uf.strip().toUpperCase(Locale.ROOT);
	}

}
