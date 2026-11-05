package br.com.valtrea.agora.estabelecimento;

import java.time.Instant;
import java.util.Locale;
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
 * Salão, barbearia ou outro serviço. Os dados dele (nome, endereço, telefone) são do negócio e públicos depois da
 * aprovação. Volta para análise se for recusado e editado, ou se receber denúncias demais.
 */
@Entity
@Table(name = "estabelecimentos")
public class Estabelecimento {

	public enum Status {
		EM_ANALISE, APROVADO, RECUSADO
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dono_id")
	private Usuario dono;

	@Column(nullable = false, length = 80)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Tipo tipo;

	@Column(length = 500)
	private String descricao;

	@Column(nullable = false, length = 160)
	private String endereco;

	@Column(nullable = false, length = 80)
	private String bairro;

	@Column(nullable = false, length = 80)
	private String cidade;

	@Column(nullable = false, length = 2)
	private String uf;

	@Column(nullable = false, length = 20)
	private String telefone;

	/** Cidade e bairro normalizados ("rio de janeiro|tijuca"), para a busca sem acento. */
	@Column(nullable = false, length = 200)
	private String busca;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "motivo_recusa", length = 300)
	private String motivoRecusa;

	@Column(name = "revisado_em")
	private Instant revisadoEm;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Estabelecimento() {
	}

	public Estabelecimento(Usuario dono) {
		this.dono = dono;
		this.status = Status.EM_ANALISE;
		this.criadoEm = Instant.now();
	}

	public void preencher(String nome, Tipo tipo, String descricao, String endereco, String bairro, String cidade,
			String uf, String telefone) {
		this.nome = Texto.arrumar(nome);
		this.tipo = tipo;
		this.descricao = Texto.vazioParaNulo(descricao);
		this.endereco = Texto.arrumar(endereco);
		this.bairro = Texto.arrumar(bairro);
		this.cidade = Texto.arrumar(cidade);
		this.uf = uf.strip().toUpperCase(Locale.ROOT);
		this.telefone = telefone.replaceAll("[^0-9+()\\- ]", "").strip();
		this.busca = Texto.normalizar(this.cidade) + "|" + Texto.normalizar(this.bairro);
		this.atualizadoEm = Instant.now();
		if (status == Status.RECUSADO) {
			// Corrigiu o que foi pedido: volta para a fila de análise.
			status = Status.EM_ANALISE;
			motivoRecusa = null;
		}
	}

	public void aprovar() {
		status = Status.APROVADO;
		motivoRecusa = null;
		revisadoEm = Instant.now();
	}

	public void recusar(String motivo) {
		status = Status.RECUSADO;
		motivoRecusa = motivo;
		revisadoEm = Instant.now();
	}

	/** Denúncias demais: some do público até a administração olhar de novo. */
	public void suspenderParaAnalise() {
		status = Status.EM_ANALISE;
	}

	public boolean ehDo(UUID usuarioId) {
		return dono.getId().equals(usuarioId);
	}

	public boolean isPublico() {
		return status == Status.APROVADO;
	}

	public UUID getId() {
		return id;
	}

	public Usuario getDono() {
		return dono;
	}

	public String getNome() {
		return nome;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public String getDescricao() {
		return descricao;
	}

	public String getEndereco() {
		return endereco;
	}

	public String getBairro() {
		return bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public String getUf() {
		return uf;
	}

	public String getTelefone() {
		return telefone;
	}

	public Status getStatus() {
		return status;
	}

	public String getMotivoRecusa() {
		return motivoRecusa;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
