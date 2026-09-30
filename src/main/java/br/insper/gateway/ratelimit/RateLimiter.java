package br.insper.gateway.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.insper.gateway.config.RateLimitProperties;
import br.insper.gateway.ratelimit.TokenBucket.Consumo;

/**
 * Rate limiter em memória com um token bucket por cliente.
 */
@Component
public class RateLimiter {

	private final RateLimitProperties properties;
	private final LongSupplier relogioNanos;
	private final Map<String, TokenBucket> baldes = new ConcurrentHashMap<>();

	@Autowired
	public RateLimiter(RateLimitProperties properties) {
		this(properties, System::nanoTime);
	}

	RateLimiter(RateLimitProperties properties, LongSupplier relogioNanos) {
		this.properties = properties;
		this.relogioNanos = relogioNanos;
	}

	/**
	 * Registra uma requisição do cliente e diz se ela pode seguir.
	 *
	 * @param cliente Identificador do cliente (hoje, o IP de origem).
	 * @return Resultado do consumo do balde do cliente.
	 */
	public Consumo consumir(String cliente) {
		long agora = relogioNanos.getAsLong();
		Consumo[] resultado = new Consumo[1];
		baldes.compute(cliente, (c, balde) -> {
			if (balde == null) {
				balde = new TokenBucket(properties.capacidade(), properties.janela().toNanos(), agora);
			}
			resultado[0] = balde.consumir(agora);
			return balde;
		});
		return resultado[0];
	}

	public int capacidade() {
		return properties.capacidade();
	}

	/**
	 * Descarta os baldes cheios para a memória não crescer com clientes que já
	 * pararam de chamar o gateway. Um balde cheio é igual a um novo, então
	 * removê-lo não muda o limite de ninguém.
	 */
	@Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	public void removerInativos() {
		long agora = relogioNanos.getAsLong();
		for (String cliente : baldes.keySet()) {
			baldes.computeIfPresent(cliente, (c, balde) -> balde.cheio(agora) ? null : balde);
		}
	}

	int clientesRastreados() {
		return baldes.size();
	}
}
