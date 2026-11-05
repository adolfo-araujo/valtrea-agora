package br.com.valtrea.agora.estabelecimento;

public enum MotivoDenuncia {
	/** O lugar não existe ou não é o que diz. */
	FALSO,
	/** Não respeita as reservas (marca e não atende, cobra diferente). */
	NAO_RESPEITA,
	/** Pede pagamento antecipado, Pix ou dados. */
	GOLPE,
	/** Nome, descrição ou serviço ofensivo ou impróprio. */
	IMPROPRIO,
	OUTRO
}
