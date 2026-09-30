package br.insper.gateway.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do rate limiting do gateway.
 *
 * @param habilitado Liga ou desliga o rate limiting.
 * @param capacidade Máximo de requisições que um cliente pode fazer em rajada.
 * @param janela     Tempo para o balde de um cliente se encher por completo de novo.
 */
@ConfigurationProperties(prefix = "gateway.rate-limit")
public record RateLimitProperties(
		@DefaultValue("true") boolean habilitado,
		@DefaultValue("100") int capacidade,
		@DefaultValue("1m") Duration janela) {

	public RateLimitProperties {
		if (capacidade <= 0) {
			throw new IllegalArgumentException("gateway.rate-limit.capacidade deve ser positiva");
		}
		if (janela == null || janela.isZero() || janela.isNegative()) {
			throw new IllegalArgumentException("gateway.rate-limit.janela deve ser positiva");
		}
	}
}
