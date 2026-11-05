package br.com.valtrea.agora.agenda;

import java.time.Instant;
import java.util.UUID;

import br.com.valtrea.agora.estabelecimento.Estabelecimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Um horário vago. O preço é só informativo: o pagamento é combinado e feito no local. */
@Entity
@Table(name = "horarios")
public class Horario {

	public enum Status {
		LIVRE, RESERVADO,
		/** O estabelecimento cancelou a reserva: o horário não volta a ficar livre. */
		CANCELADO
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estabelecimento_id")
	private Estabelecimento estabelecimento;

	@Column(nullable = false)
	private Instant inicio;

	@Column(name = "duracao_min", nullable = false)
	private int duracaoMin;

	@Column(nullable = false, length = 80)
	private String servico;

	@Column(name = "preco_centavos")
	private Integer precoCentavos;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Horario() {
	}

	public Horario(Estabelecimento estabelecimento, Instant inicio, int duracaoMin, String servico,
			Integer precoCentavos) {
		this.estabelecimento = estabelecimento;
		this.inicio = inicio;
		this.duracaoMin = duracaoMin;
		this.servico = servico;
		this.precoCentavos = precoCentavos;
		this.status = Status.LIVRE;
		this.criadoEm = Instant.now();
	}

	public void reservar() {
		status = Status.RESERVADO;
	}

	public void liberar() {
		status = Status.LIVRE;
	}

	public void cancelar() {
		status = Status.CANCELADO;
	}

	public boolean jaComecou(Instant agora) {
		return !inicio.isAfter(agora);
	}

	public UUID getId() {
		return id;
	}

	public Estabelecimento getEstabelecimento() {
		return estabelecimento;
	}

	public Instant getInicio() {
		return inicio;
	}

	public int getDuracaoMin() {
		return duracaoMin;
	}

	public String getServico() {
		return servico;
	}

	public Integer getPrecoCentavos() {
		return precoCentavos;
	}

	public Status getStatus() {
		return status;
	}

}
