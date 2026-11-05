package br.com.valtrea.agora.estabelecimento;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Uma por pessoa em cada estabelecimento. A administração vê o motivo e quantas, nunca quem denunciou. */
@Entity
@Table(name = "denuncias")
public class Denuncia {

	@Embeddable
	public static class Chave implements Serializable {

		@Column(name = "estabelecimento_id")
		private UUID estabelecimentoId;

		@Column(name = "usuario_id")
		private UUID usuarioId;

		protected Chave() {
		}

		public Chave(UUID estabelecimentoId, UUID usuarioId) {
			this.estabelecimentoId = estabelecimentoId;
			this.usuarioId = usuarioId;
		}

		public UUID getEstabelecimentoId() {
			return estabelecimentoId;
		}

		public UUID getUsuarioId() {
			return usuarioId;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Chave c && Objects.equals(estabelecimentoId, c.estabelecimentoId)
					&& Objects.equals(usuarioId, c.usuarioId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(estabelecimentoId, usuarioId);
		}

	}

	@EmbeddedId
	private Chave id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MotivoDenuncia motivo;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Denuncia() {
	}

	public Denuncia(UUID estabelecimentoId, UUID usuarioId, MotivoDenuncia motivo) {
		this.id = new Chave(estabelecimentoId, usuarioId);
		this.motivo = motivo;
		this.criadoEm = Instant.now();
	}

	public Chave getId() {
		return id;
	}

	public MotivoDenuncia getMotivo() {
		return motivo;
	}

}
