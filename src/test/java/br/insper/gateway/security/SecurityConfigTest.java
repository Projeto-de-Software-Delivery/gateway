package br.insper.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes";

	// Faz o papel dos serviços de cliente e loja: responde 200 e guarda o
	// Authorization recebido.
	private static final HttpServer servico = iniciaServicoFalso();
	private static volatile String ultimoAuthorization;

	@DynamicPropertySource
	static void propriedades(DynamicPropertyRegistry registry) {
		String url = "http://localhost:" + servico.getAddress().getPort();
		registry.add("gateway.routes.cliente-service-url", () -> url);
		registry.add("gateway.routes.loja-service-url", () -> url);
		registry.add("gateway.jwt.secret", () -> SEGREDO);
	}

	@AfterAll
	static void paraServicoFalso() {
		servico.stop(0);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void semTokenResponde401() throws Exception {
		mockMvc.perform(get("/clientes/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/lojas/1")).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenInvalidoResponde401() throws Exception {
		mockMvc.perform(comToken(get("/clientes/1"), "nao-e-um-jwt")).andExpect(status().isUnauthorized());
		mockMvc.perform(comToken(get("/clientes/1"), token("cliente", "42", "outro-segredo-com-pelo-menos-32-bytes!")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(comToken(get("/clientes/1"), token("admin", "42", SEGREDO)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void clientesSoParaCliente() throws Exception {
		mockMvc.perform(comToken(get("/clientes/1"), token("cliente", "42", SEGREDO))).andExpect(status().isOk());
		mockMvc.perform(comToken(get("/clientes/1"), token("loja", "7", SEGREDO))).andExpect(status().isForbidden());
		mockMvc.perform(comToken(get("/clientes/1"), token("entregador", "13", SEGREDO)))
				.andExpect(status().isForbidden());
	}

	@Test
	void lojasTodosVeemMasSoLojaAltera() throws Exception {
		for (String papel : new String[] { "cliente", "loja", "entregador" }) {
			mockMvc.perform(comToken(get("/lojas/1"), token(papel, "1", SEGREDO))).andExpect(status().isOk());
		}
		mockMvc.perform(comToken(post("/lojas/1"), token("loja", "7", SEGREDO))).andExpect(status().isOk());
		mockMvc.perform(comToken(post("/lojas/1"), token("cliente", "42", SEGREDO)))
				.andExpect(status().isForbidden());
	}

	@Test
	void repassaOTokenParaOServico() throws Exception {
		String jwt = token("cliente", "42", SEGREDO);
		mockMvc.perform(comToken(get("/clientes/1"), jwt)).andExpect(status().isOk());

		assertEquals("Bearer " + jwt, ultimoAuthorization);
	}

	@Test
	void healthEDocumentacaoNaoExigemToken() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	private static MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder request, String jwt) {
		return request.header("Authorization", "Bearer " + jwt);
	}

	private static String token(String papel, String sub, String segredo) throws Exception {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(sub)
				.claim("role", papel)
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(segredo.getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}

	private static HttpServer iniciaServicoFalso() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", troca -> {
				ultimoAuthorization = troca.getRequestHeaders().getFirst("Authorization");
				byte[] corpo = "ok".getBytes(StandardCharsets.UTF_8);
				troca.sendResponseHeaders(200, corpo.length);
				troca.getResponseBody().write(corpo);
				troca.close();
			});
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
