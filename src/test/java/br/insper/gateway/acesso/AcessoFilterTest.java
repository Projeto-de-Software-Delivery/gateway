package br.insper.gateway.acesso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.insper.gateway.ratelimit.RateLimitFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AcessoFilterTest {

	@RestController
	static class RotasFalsas {

		@RequestMapping({ "/clientes/1", "/lojas/1", "/actuator/health" })
		String ok() {
			return "ok";
		}
	}

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	// Mesma ordem da aplicação: acesso antes do rate limiting (limite de 1).
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RotasFalsas())
			.addFilters(new AcessoFilter(registry), new RateLimitFilter(1, Duration.ofMinutes(1)))
			.build();

	@Test
	void medePorServicoMetodoEStatus() throws Exception {
		mockMvc.perform(get("/clientes/1"));
		mockMvc.perform(post("/lojas/1"));

		assertEquals(1, contagem("cliente-service", "GET", "200"));
		assertEquals(0, contagem("loja-service", "POST", "200"));
		assertEquals(1, contagem("loja-service", "POST", "429"));
	}

	@Test
	void ignoraRotasForaDoGateway() throws Exception {
		mockMvc.perform(get("/actuator/health"));

		assertNull(registry.find("gateway.requisicoes").timer());
	}

	private long contagem(String servico, String metodo, String status) {
		var timer = registry.find("gateway.requisicoes")
				.tags("servico", servico, "metodo", metodo, "status", status)
				.timer();
		return timer == null ? 0 : timer.count();
	}
}
