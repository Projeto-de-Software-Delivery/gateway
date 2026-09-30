package br.insper.gateway.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class RateLimitFilterTest {

	@RestController
	static class RotasFalsas {

		@RequestMapping({ "/clientes/1", "/actuator/health" })
		String ok() {
			return "ok";
		}
	}

	// Limite de 2 requisições por minuto.
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RotasFalsas())
			.addFilters(new RateLimitFilter(2, Duration.ofMinutes(1)))
			.build();

	@Test
	void bloqueiaQuandoPassaDoLimite() throws Exception {
		mockMvc.perform(get("/clientes/1")).andExpect(status().isOk());
		mockMvc.perform(post("/clientes/1")).andExpect(status().isOk());
		mockMvc.perform(get("/clientes/1")).andExpect(status().isTooManyRequests());
	}

	@Test
	void cadaIpTemSeuProprioLimite() throws Exception {
		mockMvc.perform(get("/clientes/1")).andExpect(status().isOk());
		mockMvc.perform(get("/clientes/1")).andExpect(status().isOk());

		mockMvc.perform(get("/clientes/1").with(r -> { r.setRemoteAddr("10.0.0.2"); return r; }))
				.andExpect(status().isOk());
	}

	@Test
	void naoLimitaRotasForaDoGateway() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		}
	}
}
