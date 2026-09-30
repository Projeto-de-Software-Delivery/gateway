package br.insper.gateway.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita quantas requisições cada IP pode fazer por janela de tempo nas rotas
 * do gateway. Passou do limite, responde 429 até a janela seguinte começar.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final int limite;
	private final long janelaMillis;

	// Quantas requisições cada IP já fez na janela atual.
	private final Map<String, Integer> contagemPorIp = new ConcurrentHashMap<>();
	private long inicioDaJanela = System.currentTimeMillis();

	public RateLimitFilter(@Value("${gateway.rate-limit.limite:100}") int limite,
			@Value("${gateway.rate-limit.janela:1m}") Duration janela) {
		this.limite = limite;
		this.janelaMillis = janela.toMillis();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !path.startsWith("/clientes/") && !path.startsWith("/lojas/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (excedeuLimite(request.getRemoteAddr())) {
			response.sendError(429, "Limite de requisições excedido");
			return;
		}
		chain.doFilter(request, response);
	}

	private synchronized boolean excedeuLimite(String ip) {
		long agora = System.currentTimeMillis();
		if (agora - inicioDaJanela >= janelaMillis) {
			contagemPorIp.clear();
			inicioDaJanela = agora;
		}
		int requisicoes = contagemPorIp.merge(ip, 1, Integer::sum);
		return requisicoes > limite;
	}
}
