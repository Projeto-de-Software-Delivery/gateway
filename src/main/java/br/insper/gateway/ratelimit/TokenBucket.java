package br.insper.gateway.ratelimit;

/**
 * Balde de tokens de um cliente. Enche continuamente até a capacidade e cada
 * requisição consome um token.
 */
class TokenBucket {

	private final int capacidade;
	private final double tokensPorNano;
	private double tokens;
	private long ultimaReposicao;

	TokenBucket(int capacidade, long janelaNanos, long agoraNanos) {
		this.capacidade = capacidade;
		this.tokensPorNano = (double) capacidade / janelaNanos;
		this.tokens = capacidade;
		this.ultimaReposicao = agoraNanos;
	}

	/**
	 * Tenta consumir um token.
	 *
	 * @param agoraNanos Instante atual em nanossegundos.
	 * @return Resultado com se a requisição foi permitida, tokens restantes e espera sugerida.
	 */
	synchronized Consumo consumir(long agoraNanos) {
		repor(agoraNanos);
		if (tokens >= 1) {
			tokens -= 1;
			return new Consumo(true, (int) tokens, 0);
		}
		long esperaNanos = (long) Math.ceil((1 - tokens) / tokensPorNano);
		return new Consumo(false, 0, esperaNanos);
	}

	/**
	 * Indica se o balde está cheio, ou seja, se o cliente ficou inativo tempo
	 * suficiente para o balde poder ser descartado sem mudar o comportamento.
	 */
	synchronized boolean cheio(long agoraNanos) {
		repor(agoraNanos);
		return tokens >= capacidade;
	}

	private void repor(long agoraNanos) {
		long decorrido = agoraNanos - ultimaReposicao;
		if (decorrido > 0) {
			tokens = Math.min(capacidade, tokens + decorrido * tokensPorNano);
			ultimaReposicao = agoraNanos;
		}
	}

	record Consumo(boolean permitido, int restantes, long esperaNanos) {
	}
}
