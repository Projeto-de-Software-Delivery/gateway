package br.insper.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import br.insper.gateway.config.RateLimitProperties;
import br.insper.gateway.ratelimit.TokenBucket.Consumo;

class RateLimiterTest {

	private final AtomicLong agora = new AtomicLong(0);
	private final RateLimiter rateLimiter = new RateLimiter(
			new RateLimitProperties(true, 3, Duration.ofSeconds(3)), agora::get);

	@Test
	void permiteAteACapacidadeEBloqueiaDepois() {
		assertThat(rateLimiter.consumir("a").restantes()).isEqualTo(2);
		assertThat(rateLimiter.consumir("a").restantes()).isEqualTo(1);
		assertThat(rateLimiter.consumir("a").restantes()).isZero();

		Consumo bloqueado = rateLimiter.consumir("a");
		assertThat(bloqueado.permitido()).isFalse();
		assertThat(bloqueado.esperaNanos()).isEqualTo(Duration.ofSeconds(1).toNanos());
	}

	@Test
	void clientesTemBaldesIndependentes() {
		for (int i = 0; i < 3; i++) {
			rateLimiter.consumir("a");
		}

		assertThat(rateLimiter.consumir("a").permitido()).isFalse();
		assertThat(rateLimiter.consumir("b").permitido()).isTrue();
	}

	@Test
	void repoeTokensComOTempo() {
		for (int i = 0; i < 3; i++) {
			rateLimiter.consumir("a");
		}
		assertThat(rateLimiter.consumir("a").permitido()).isFalse();

		agora.addAndGet(Duration.ofSeconds(1).toNanos());

		assertThat(rateLimiter.consumir("a").permitido()).isTrue();
		assertThat(rateLimiter.consumir("a").permitido()).isFalse();
	}

	@Test
	void naoAcumulaAlemDaCapacidade() {
		rateLimiter.consumir("a");
		agora.addAndGet(Duration.ofHours(1).toNanos());

		assertThat(rateLimiter.consumir("a").restantes()).isEqualTo(2);
	}

	@Test
	void removeSoOsBaldesCheios() {
		rateLimiter.consumir("a");
		agora.addAndGet(Duration.ofSeconds(1).toNanos());
		rateLimiter.consumir("b");

		rateLimiter.removerInativos();

		assertThat(rateLimiter.clientesRastreados()).isEqualTo(1);
		assertThat(rateLimiter.consumir("b").restantes()).isEqualTo(1);
	}
}
