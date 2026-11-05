package br.com.valtrea.agora.agenda;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.valtrea.agora.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AgendaDtos {

	private AgendaDtos() {
	}

	/** Vários horários de uma vez, com o mesmo serviço (ex.: 14h, 15h e 16h de hoje). */
	public record HorariosRequest(
			@NotBlank @Size(min = 2, max = 80) String servico,
			@Min(10) @Max(480) int duracaoMin,
			@Min(0) @Max(1_000_000) Integer precoCentavos,
			@NotEmpty @Size(max = 20, message = "até 20 horários de cada vez") List<@NotNull Instant> inicios) {
	}

	public record ReservaRequest(
			@Pattern(regexp = "[0-9+()\\- ]{8,20}", message = "digite o telefone com DDD") String telefone,
			@Size(max = 200) String observacao) {
	}

	public record StatusReservaRequest(@NotNull Reserva.Status status) {
	}

	/** O que o estabelecimento vê de quem reservou: primeiro nome e o que a pessoa escolheu informar. */
	public record ReservaParaDono(UUID id, String cliente, String telefone, String observacao, Reserva.Status status) {
	}

	public record HorarioDono(UUID id, Instant inicio, int duracaoMin, String servico, Integer precoCentavos,
			Horario.Status status, ReservaParaDono reserva) {
	}

	/** Horário livre na busca, com o estabelecimento. */
	public record HorarioPublico(UUID id, Instant inicio, int duracaoMin, String servico, Integer precoCentavos,
			EstabelecimentoResumo estabelecimento) {
	}

	public record EstabelecimentoDaReserva(UUID id, String nome, String endereco, String bairro, String cidade,
			String uf, String telefone) {
	}

	public record ReservaCliente(UUID id, Reserva.Status status, Instant criadoEm, Instant inicio, int duracaoMin,
			String servico, Integer precoCentavos, String telefone, String observacao,
			EstabelecimentoDaReserva estabelecimento) {
	}

}
