package br.com.valtrea.agora.estabelecimento;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.DenunciaRequest;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoDetalhe;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoRequest;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Estabelecimentos")
public class EstabelecimentoController {

	private final EstabelecimentoService servico;

	public EstabelecimentoController(EstabelecimentoService servico) {
		this.servico = servico;
	}

	@GetMapping("/eu/estabelecimentos")
	@Operation(summary = "Os seus estabelecimentos, com a situação da análise")
	public List<EstabelecimentoResponse> meus(@AuthenticationPrincipal Jwt jwt) {
		return servico.meus(id(jwt));
	}

	@PostMapping("/estabelecimentos")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cadastra um estabelecimento (fica em análise até a aprovação)")
	public EstabelecimentoResponse criar(@AuthenticationPrincipal Jwt jwt,
			@RequestBody @Valid EstabelecimentoRequest pedido) {
		return servico.criar(id(jwt), pedido);
	}

	@GetMapping("/estabelecimentos/{id}")
	@Operation(summary = "Um estabelecimento com os horários livres, dizendo se você denunciou")
	public EstabelecimentoDetalhe detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return servico.detalhe(id, id(jwt));
	}

	@PutMapping("/estabelecimentos/{id}")
	@Operation(summary = "Edita o cadastro (se tinha sido recusado, volta para a análise)")
	public EstabelecimentoResponse editar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid EstabelecimentoRequest pedido) {
		return servico.editar(id(jwt), id, pedido);
	}

	@DeleteMapping("/estabelecimentos/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Apaga o estabelecimento, os horários e as reservas")
	public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		servico.excluir(id(jwt), id);
	}

	@PostMapping("/estabelecimentos/{id}/denuncia")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Denuncia o estabelecimento. Com 3 denúncias, sai da busca até a revisão")
	public void denunciar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid DenunciaRequest pedido) {
		servico.denunciar(id(jwt), id, pedido.motivo());
	}

	private static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
