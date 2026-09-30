package br.insper.gateway.ratelimit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.insper.gateway.config.RateLimitProperties;

class RateLimitFilterTest {

	@RestController
	static class RotasFalsas {

		@GetMapping({ "/clientes/1", "/lojas/1", "/actuator/health" })
		String ok() {
			return "ok";
		}
	}

	private MockMvc mockMvc(boolean habilitado, int capacidade) {
		RateLimitProperties properties = new RateLimitProperties(habilitado, capacidade, Duration.ofMinutes(1));
		RateLimiter rateLimiter = new RateLimiter(properties, () -> 0L);
		return MockMvcBuilders.standaloneSetup(new RotasFalsas())
				.addFilters(new RateLimitFilter(rateLimiter, properties))
				.build();
	}

	@Test
	void respondeTooManyRequestsAoEstourarOLimite() throws Exception {
		MockMvc mockMvc = mockMvc(true, 2);

		mockMvc.perform(get("/clientes/1"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "2"))
				.andExpect(header().string("X-RateLimit-Remaining", "1"));
		mockMvc.perform(get("/lojas/1"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Remaining", "0"));
		mockMvc.perform(get("/clientes/1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "30"))
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(content().encoding("UTF-8"))
				.andExpect(content().string(containsString("Limite de requisições excedido")));
	}

	@Test
	void separaClientesPeloIp() throws Exception {
		MockMvc mockMvc = mockMvc(true, 1);

		mockMvc.perform(get("/clientes/1").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
				.andExpect(status().isOk());
		mockMvc.perform(get("/clientes/1").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
				.andExpect(status().isTooManyRequests());
		mockMvc.perform(get("/clientes/1").with(r -> { r.setRemoteAddr("10.0.0.2"); return r; }))
				.andExpect(status().isOk());
	}

	@Test
	void naoLimitaRotasForaDoRoteamento() throws Exception {
		MockMvc mockMvc = mockMvc(true, 1);

		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/actuator/health"))
					.andExpect(status().isOk())
					.andExpect(header().doesNotExist("X-RateLimit-Limit"));
		}
	}

	@Test
	void naoLimitaQuandoDesabilitado() throws Exception {
		MockMvc mockMvc = mockMvc(false, 1);

		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/clientes/1")).andExpect(status().isOk());
		}
	}
}
