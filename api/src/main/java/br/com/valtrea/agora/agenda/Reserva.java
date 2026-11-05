package br.com.valtrea.agora.agenda;

import java.time.Instant;
import java.util.UUID;

import br.com.valtrea.agora.usuario.Usuario;
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

/**
 * Reserva de um horário. O estabelecimento vê só o primeiro nome do cliente e, se ele quiser, um telefone e uma
 * observação. Tudo é apagado 90 dias depois do horário.
 */
@Entity
@Table(name = "reservas")
public class Reserva {

	public enum Status {
		CONFIRMADA, CANCELADA_CLIENTE, CANCELADA_ESTABELECIMENTO, ATENDIDA, NAO_COMPARECEU
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "horario_id")
	private Horario horario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id")
	private Usuario cliente;

	@Column(length = 20)
	private String telefone;

	@Column(length = 200)
	private String observacao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Status status;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Reserva() {
	}

	public Reserva(Horario horario, Usuario cliente, String telefone, String observacao) {
		this.horario = horario;
		this.cliente = cliente;
		this.telefone = telefone;
		this.observacao = observacao;
		this.status = Status.CONFIRMADA;
		this.criadoEm = Instant.now();
		this.atualizadoEm = criadoEm;
	}

	public void mudar(Status novo) {
		this.status = novo;
		this.atualizadoEm = Instant.now();
	}

	public boolean ehDo(UUID usuarioId) {
		return cliente.getId().equals(usuarioId);
	}

	public UUID getId() {
		return id;
	}

	public Horario getHorario() {
		return horario;
	}

	public Usuario getCliente() {
		return cliente;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getObservacao() {
		return observacao;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
