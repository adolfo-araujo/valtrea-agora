package br.com.valtrea.agora.agenda;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.valtrea.agora.agenda.AgendaDtos.HorarioPublico;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoDetalhe;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoPublico;
import br.com.valtrea.agora.estabelecimento.EstabelecimentoService;
import br.com.valtrea.agora.estabelecimento.Tipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Busca sem conta: horários vagos e estabelecimentos aprovados. Nada aqui identifica alguém. */
@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Público")
public class PublicoController {

	private final AgendaService agenda;
	private final EstabelecimentoService estabelecimentos;

	public PublicoController(AgendaService agenda, EstabelecimentoService estabelecimentos) {
		this.agenda = agenda;
		this.estabelecimentos = estabelecimentos;
	}

	@GetMapping("/horarios")
	@Operation(summary = "Horários livres dos próximos 7 dias, do mais próximo para o mais distante")
	public List<HorarioPublico> horarios(@RequestParam(required = false) String uf,
			@RequestParam(required = false) Tipo tipo, @RequestParam(required = false) String busca) {
		return agenda.livres(uf, tipo, busca);
	}

	@GetMapping("/estabelecimentos")
	@Operation(summary = "Estabelecimentos aprovados, com quantos horários livres têm nos próximos 7 dias")
	public List<EstabelecimentoPublico> estabelecimentos(@RequestParam(required = false) String uf,
			@RequestParam(required = false) Tipo tipo, @RequestParam(required = false) String busca) {
		return estabelecimentos.buscar(uf, tipo, busca);
	}

	@GetMapping("/estabelecimentos/{id}")
	@Operation(summary = "Um estabelecimento com os horários livres dos próximos 14 dias")
	public EstabelecimentoDetalhe estabelecimento(@PathVariable UUID id) {
		return estabelecimentos.detalhe(id, null);
	}

}
