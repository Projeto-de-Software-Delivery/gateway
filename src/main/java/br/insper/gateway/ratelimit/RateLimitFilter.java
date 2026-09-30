package br.insper.gateway.ratelimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.insper.gateway.config.RateLimitProperties;
import br.insper.gateway.ratelimit.TokenBucket.Consumo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica o rate limiting às rotas roteadas pelo gateway, respondendo 429 quando
 * o cliente estoura o limite.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;

	public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return !properties.habilitado() || !(path.startsWith("/clientes/") || path.startsWith("/lojas/"));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		// Enquanto não houver autenticação (KAN-17), o cliente é identificado pelo IP de origem.
		String cliente = request.getRemoteAddr();
		Consumo consumo = rateLimiter.consumir(cliente);

		response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimiter.capacidade()));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(consumo.restantes()));

		if (consumo.permitido()) {
			chain.doFilter(request, response);
			return;
		}

		long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(consumo.esperaNanos() + 999_999_999));
		log.warn("Rate limit excedido para {} em {} {}", cliente, request.getMethod(), request.getRequestURI());
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfter));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"erro\":\"Limite de requisições excedido. Tente novamente em "
				+ retryAfter + " segundo(s).\"}");
	}
}
