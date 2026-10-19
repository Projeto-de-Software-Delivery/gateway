package br.insper.gateway.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void requisicaoSemTokenParaRotaRoteadaRetorna401() throws Exception {
		mockMvc.perform(get("/clientes/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void requisicaoSemTokenParaRotaRoteadaDeLojaRetorna401() throws Exception {
		mockMvc.perform(get("/lojas/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void actuatorHealthFicaLiberadoSemToken() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void jwtDecoderEExposto() {
		org.assertj.core.api.Assertions.assertThat(jwtDecoder).isNotNull();
	}
}
