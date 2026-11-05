package br.com.valtrea.agora.estabelecimento;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class Texto {

	public static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
			"MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

	private Texto() {
	}

	/** Sem acentos, em minúsculas e só letras e números: "São  João" vira "sao joao". */
	public static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
	}

	public static String arrumar(String texto) {
		return texto == null ? null : texto.strip().replaceAll("\\s+", " ");
	}

	public static String vazioParaNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.strip();
	}

}
