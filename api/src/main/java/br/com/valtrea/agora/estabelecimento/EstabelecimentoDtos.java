package br.com.valtrea.agora.estabelecimento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class EstabelecimentoDtos {

	private EstabelecimentoDtos() {
	}

	public record EstabelecimentoRequest(
			@NotBlank @Size(min = 2, max = 80) String nome,
			@NotNull Tipo tipo,
			@Size(max = 500) String descricao,
			@NotBlank @Size(min = 5, max = 160) String endereco,
			@NotBlank @Size(min = 2, max = 80) String bairro,
			@NotBlank @Size(min = 2, max = 80) String cidade,
			@NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "use a sigla do estado, como SP") String uf,
			@NotBlank @Pattern(regexp = "[0-9+()\\- ]{8,20}", message = "digite o telefone com DDD") String telefone) {
	}

	public record DenunciaRequest(@NotNull MotivoDenuncia motivo) {
	}

	/** Para o dono: inclui a situação na análise. */
	public record EstabelecimentoResponse(UUID id, String nome, Tipo tipo, String descricao, String endereco,
			String bairro, String cidade, String uf, String telefone, Estabelecimento.Status status, String motivoRecusa,
			Instant criadoEm) {
	}

	/** O que o público vê (dados do negócio). Nada sobre o dono. */
	public record EstabelecimentoPublico(UUID id, String nome, Tipo tipo, String descricao, String endereco,
			String bairro, String cidade, String uf, String telefone, long horariosLivres) {
	}

	public record EstabelecimentoResumo(UUID id, String nome, Tipo tipo, String bairro, String cidade, String uf) {
	}

	public record HorarioLivre(UUID id, Instant inicio, int duracaoMin, String servico, Integer precoCentavos) {
	}

	public record EstabelecimentoDetalhe(EstabelecimentoPublico estabelecimento, List<HorarioLivre> horarios,
			boolean denunciei, boolean meu) {
	}

	public static EstabelecimentoResponse resposta(Estabelecimento e) {
		return new EstabelecimentoResponse(e.getId(), e.getNome(), e.getTipo(), e.getDescricao(), e.getEndereco(),
				e.getBairro(), e.getCidade(), e.getUf(), e.getTelefone(), e.getStatus(), e.getMotivoRecusa(),
				e.getCriadoEm());
	}

	public static EstabelecimentoPublico publico(Estabelecimento e, long livres) {
		return new EstabelecimentoPublico(e.getId(), e.getNome(), e.getTipo(), e.getDescricao(), e.getEndereco(),
				e.getBairro(), e.getCidade(), e.getUf(), e.getTelefone(), livres);
	}

	public static EstabelecimentoResumo resumo(Estabelecimento e) {
		return new EstabelecimentoResumo(e.getId(), e.getNome(), e.getTipo(), e.getBairro(), e.getCidade(), e.getUf());
	}

}
