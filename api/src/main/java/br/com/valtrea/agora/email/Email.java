package br.com.valtrea.agora.email;

/** E-mail pronto para envio. Publicado como evento e enviado só depois que a transação do banco é gravada. */
public record Email(String para, String assunto, String texto) {

	public static Email confirmarEmail(String para, String nome, String codigo, long minutos) {
		return new Email(para, "Seu código Valtrea: " + codigo, """
				Olá, %s!

				Use este código no Valtrea Agora para confirmar seu e-mail:

				    %s

				Ele vale por %d minutos. Se você não criou uma conta na Valtrea, ignore este e-mail.

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), codigo, minutos));
	}

	public static Email redefinirSenha(String para, String nome, String codigo, long minutos) {
		return new Email(para, "Código para redefinir sua senha Valtrea", """
				Olá, %s!

				Recebemos um pedido para redefinir a senha da sua conta no Valtrea Agora. Use este código:

				    %s

				Ele vale por %d minutos. Se não foi você, ignore este e-mail: sua senha continua a mesma.

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), codigo, minutos));
	}

	public static Email contaBloqueada(String para, String nome, long minutos) {
		return new Email(para, "Tentativas de acesso à sua conta Valtrea", """
				Olá, %s!

				Houve várias tentativas de entrar na sua conta com a senha errada, então bloqueamos o login por %d minutos.

				Se foi você, é só esperar e tentar de novo, ou usar "Esqueci minha senha" no app.
				Se não foi você, recomendamos redefinir sua senha.

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), minutos));
	}

	public static Email senhaAlterada(String para, String nome) {
		return new Email(para, "Sua senha Valtrea foi alterada", """
				Olá, %s!

				A senha da sua conta acabou de ser alterada e todos os aparelhos foram desconectados.

				Se não foi você, fale com a gente imediatamente: contato@valtrea.com.br

				— Equipe Valtrea
				""".formatted(primeiroNome(nome)));
	}

	public static Email contaExcluida(String para, String nome) {
		return new Email(para, "Sua conta Valtrea Agora foi excluída", """
				Olá, %s!

				Sua conta no Valtrea Agora foi excluída, como você pediu. Apagamos seus dados, as suas reservas e,
				se você tinha, os seus estabelecimentos com os horários e as reservas deles.

				Se não foi você, fale com a gente imediatamente: contato@valtrea.com.br

				— Equipe Valtrea
				""".formatted(primeiroNome(nome)));
	}

	/** Para o estabelecimento: só o primeiro nome do cliente; o resto ele vê no app. */
	public static Email novaReserva(String para, String nome, String estabelecimento, String servico, String quando,
			String cliente, String urlAgenda) {
		return new Email(para, "Nova reserva: " + servico + " " + quando, """
				Olá, %s!

				%s reservou "%s" em %s, %s.

				Veja os detalhes na agenda: %s

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), cliente, servico, estabelecimento, quando, urlAgenda));
	}

	public static Email reservaCancelada(String para, String nome, String estabelecimento, String servico,
			String quando, String quemCancelou, String url) {
		return new Email(para, "Reserva cancelada: " + servico + " " + quando, """
				Olá, %s!

				A reserva de "%s" em %s, %s, foi cancelada por %s.

				Mais detalhes: %s

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), servico, estabelecimento, quando, quemCancelou, url));
	}

	public static Email estabelecimentoAprovado(String para, String nome, String estabelecimento, String url) {
		return new Email(para, estabelecimento + " já aparece no Valtrea Agora", """
				Olá, %s!

				Aprovamos "%s". Agora os horários vagos que você publicar aparecem para quem procura na sua região.

				Publique os primeiros horários: %s

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), estabelecimento, url));
	}

	public static Email estabelecimentoRecusado(String para, String nome, String estabelecimento, String motivo,
			String url) {
		return new Email(para, "Precisamos de um ajuste em " + estabelecimento, """
				Olá, %s!

				Ainda não aprovamos "%s" no Valtrea Agora. Motivo: %s

				Corrija o cadastro e ele volta para a análise: %s

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), estabelecimento, motivo, url));
	}

	public static Email estabelecimentoEmAnalise(String para, String nome, String estabelecimento, String url) {
		return new Email(para, estabelecimento + " está em revisão", """
				Olá, %s!

				"%s" recebeu denúncias de clientes e saiu da busca até a nossa equipe revisar. As reservas já feitas
				continuam valendo. Acompanhe: %s

				Se você acha que é um engano, responda para contato@valtrea.com.br.

				— Equipe Valtrea
				""".formatted(primeiroNome(nome), estabelecimento, url));
	}

	private static String primeiroNome(String nome) {
		return nome.strip().split("\\s+")[0];
	}

}
