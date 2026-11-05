package br.com.valtrea.agora.admin;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valtrea.agora.config.ValtreaProperties;
import br.com.valtrea.agora.email.Email;
import br.com.valtrea.agora.erro.NegocioException;
import br.com.valtrea.agora.estabelecimento.Denuncia;
import br.com.valtrea.agora.estabelecimento.DenunciaRepository;
import br.com.valtrea.agora.estabelecimento.Estabelecimento;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoRepository;
import br.com.valtrea.agora.estabelecimento.MotivoDenuncia;
import br.com.valtrea.agora.estabelecimento.Tipo;
import br.com.valtrea.agora.usuario.Usuario;
import br.com.valtrea.agora.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fila de análise: estabelecimentos novos, corrigidos depois de recusa ou denunciados. A administração vê os dados do
 * negócio, o primeiro nome do dono e quantas denúncias de cada motivo, nunca quem denunciou nem o e-mail do dono.
 */
@RestController
@RequestMapping("/api/v1/admin/estabelecimentos")
@Tag(name = "Administração")
public class AnaliseController {

	public record ParaAnalisar(UUID id, String nome, Tipo tipo, String descricao, String endereco, String bairro,
			String cidade, String uf, String telefone, String dono, Instant criadoEm, Instant atualizadoEm,
			Map<MotivoDenuncia, Long> denuncias) {
	}

	public record RecusaRequest(@NotBlank @Size(min = 5, max = 300) String motivo) {
	}

	private final EstabelecimentoRepository estabelecimentos;
	private final DenunciaRepository denuncias;
	private final UsuarioRepository usuarios;
	private final Administradores administradores;
	private final ApplicationEventPublisher eventos;
	private final ValtreaProperties.Site site;

	public AnaliseController(EstabelecimentoRepository estabelecimentos, DenunciaRepository denuncias,
			UsuarioRepository usuarios, Administradores administradores, ApplicationEventPublisher eventos,
			ValtreaProperties propriedades) {
		this.estabelecimentos = estabelecimentos;
		this.denuncias = denuncias;
		this.usuarios = usuarios;
		this.administradores = administradores;
		this.eventos = eventos;
		this.site = propriedades.site();
	}

	@GetMapping
	@Transactional(readOnly = true)
	@Operation(summary = "Estabelecimentos esperando análise, dos mais antigos para os mais novos")
	public List<ParaAnalisar> fila(@AuthenticationPrincipal Jwt jwt) {
		administradores.exigir(usuarios, jwt);
		return estabelecimentos.findByStatusOrderByAtualizadoEmAsc(Estabelecimento.Status.EM_ANALISE).stream()
				.map(this::paraAnalisar).toList();
	}

	@PostMapping("/{id}/aprovar")
	@Transactional
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Aprova: aparece na busca (avisa o dono por e-mail)")
	public void aprovar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		administradores.exigir(usuarios, jwt);
		Estabelecimento e = emAnalise(id);
		e.aprovar();
		denuncias.apagarDo(id);
		Usuario dono = e.getDono();
		eventos.publishEvent(Email.estabelecimentoAprovado(dono.getEmail(), dono.getNome(), e.getNome(),
				site.link("/negocio")));
	}

	@PostMapping("/{id}/recusar")
	@Transactional
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Recusa com um motivo (avisa o dono; se ele corrigir, volta para a fila)")
	public void recusar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid RecusaRequest pedido) {
		administradores.exigir(usuarios, jwt);
		Estabelecimento e = emAnalise(id);
		e.recusar(pedido.motivo().strip());
		denuncias.apagarDo(id);
		Usuario dono = e.getDono();
		eventos.publishEvent(Email.estabelecimentoRecusado(dono.getEmail(), dono.getNome(), e.getNome(),
				pedido.motivo().strip(), site.link("/negocio")));
	}

	private Estabelecimento emAnalise(UUID id) {
		return estabelecimentos.findById(id).filter(e -> e.getStatus() == Estabelecimento.Status.EM_ANALISE)
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Nada para analisar com este id."));
	}

	private ParaAnalisar paraAnalisar(Estabelecimento e) {
		Map<MotivoDenuncia, Long> motivos = new EnumMap<>(MotivoDenuncia.class);
		for (Denuncia d : denuncias.findByIdEstabelecimentoId(e.getId())) {
			motivos.merge(d.getMotivo(), 1L, Long::sum);
		}
		return new ParaAnalisar(e.getId(), e.getNome(), e.getTipo(), e.getDescricao(), e.getEndereco(), e.getBairro(),
				e.getCidade(), e.getUf(), e.getTelefone(), e.getDono().getPrimeiroNome(), e.getCriadoEm(),
				e.getAtualizadoEm(), motivos);
	}

}
