package br.insper.gateway.acesso;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Registra cada requisição às rotas do gateway: uma linha de log de acesso e
 * uma medição no timer {@code gateway.requisicoes} (por serviço, método e
 * status). Roda antes do rate limiting para que os 429 também apareçam.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcessoFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(AcessoFilter.class);

	private final MeterRegistry registry;

	public AcessoFilter(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return servico(request.getRequestURI()) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long inicio = System.nanoTime();
		int status = 500;
		try {
			chain.doFilter(request, response);
			status = response.getStatus();
		} finally {
			long duracao = System.nanoTime() - inicio;
			String servico = servico(request.getRequestURI());

			Timer.builder("gateway.requisicoes")
					.description("Requisições recebidas pelo gateway")
					.tag("servico", servico)
					.tag("metodo", request.getMethod())
					.tag("status", String.valueOf(status))
					.register(registry)
					.record(duracao, TimeUnit.NANOSECONDS);

			log.info("{} {} {} -> {} {} ({} ms)", request.getRemoteAddr(), request.getMethod(),
					request.getRequestURI(), servico, status, TimeUnit.NANOSECONDS.toMillis(duracao));
		}
	}

	private static String servico(String path) {
		if (path.startsWith("/clientes/")) {
			return "cliente-service";
		}
		if (path.startsWith("/lojas/")) {
			return "loja-service";
		}
		return null;
	}
}
