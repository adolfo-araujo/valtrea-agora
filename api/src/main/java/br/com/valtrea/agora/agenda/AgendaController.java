package br.com.valtrea.agora.agenda;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valtrea.agora.agenda.AgendaDtos.HorarioDono;
import br.com.valtrea.agora.agenda.AgendaDtos.HorariosRequest;
import br.com.valtrea.agora.agenda.AgendaDtos.ReservaCliente;
import br.com.valtrea.agora.agenda.AgendaDtos.ReservaRequest;
import br.com.valtrea.agora.agenda.AgendaDtos.StatusReservaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Agenda e reservas")
public class AgendaController {

	private final AgendaService servico;

	public AgendaController(AgendaService servico) {
		this.servico = servico;
	}

	@GetMapping("/estabelecimentos/{id}/agenda")
	@Operation(summary = "Agenda do dono: horários da semana passada em diante, com as reservas")
	public List<HorarioDono> agenda(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return servico.agenda(id(jwt), id);
	}

	@PostMapping("/estabelecimentos/{id}/horarios")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Publica até 20 horários vagos de uma vez")
	public List<HorarioDono> publicar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid HorariosRequest pedido) {
		return servico.publicar(id(jwt), id, pedido);
	}

	@DeleteMapping("/horarios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Apaga um horário sem reserva")
	public void apagar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		servico.apagarHorario(id(jwt), id);
	}

	@PostMapping("/horarios/{id}/reserva")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Reserva o horário (e-mail confirmado; até 3 reservas futuras)")
	public ReservaCliente reservar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid ReservaRequest pedido) {
		return servico.reservar(id(jwt), id, pedido);
	}

	@GetMapping("/eu/reservas")
	@Operation(summary = "As suas reservas, das mais novas para as mais antigas")
	public List<ReservaCliente> minhas(@AuthenticationPrincipal Jwt jwt) {
		return servico.minhas(id(jwt));
	}

	@PostMapping("/reservas/{id}/cancelar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cancela (cliente ou estabelecimento); o outro lado recebe um e-mail")
	public void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		servico.cancelar(id(jwt), id);
	}

	@PostMapping("/reservas/{id}/status")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Depois do horário, o estabelecimento marca se a pessoa veio")
	public void marcar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@RequestBody @Valid StatusReservaRequest pedido) {
		servico.marcar(id(jwt), id, pedido.status());
	}

	private static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
