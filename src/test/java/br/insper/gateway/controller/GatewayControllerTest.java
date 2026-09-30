package br.insper.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import br.insper.gateway.config.RouteProperties;
import com.sun.net.httpserver.HttpServer;

class GatewayControllerTest {

	private HttpServer downstream;
	private GatewayController controller;

	@BeforeEach
	void subirServicoFalso() throws IOException {
		downstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		downstream.createContext("/clientes/1", exchange -> {
			byte[] corpo = "{\"error\":\"Not Found\"}".getBytes();
			exchange.sendResponseHeaders(404, corpo.length);
			exchange.getResponseBody().write(corpo);
			exchange.close();
		});
		downstream.createContext("/clientes/2", exchange -> {
			byte[] corpo = "{\"id\":2}".getBytes();
			exchange.sendResponseHeaders(200, corpo.length);
			exchange.getResponseBody().write(corpo);
			exchange.close();
		});
		downstream.start();
		int porta = downstream.getAddress().getPort();

		RouteProperties routeProperties = new RouteProperties("http://localhost:" + porta, "http://localhost:" + porta);
		ApplicationEventPublisher eventPublisher = evento -> {
		};
		controller = new GatewayController(routeProperties, eventPublisher);
	}

	@AfterEach
	void derrubarServicoFalso() {
		downstream.stop(0);
	}

	@Test
	void repassaStatus404DoServicoDeDestinoEmVezDeVirar500() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/1");
		request.setRequestURI("/clientes/1");

		ResponseEntity<String> resposta = controller.rotearParaCliente(HttpMethod.GET, request, null);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(resposta.getBody()).contains("Not Found");
	}

	@Test
	void repassaStatus200DoServicoDeDestino() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/2");
		request.setRequestURI("/clientes/2");

		ResponseEntity<String> resposta = controller.rotearParaCliente(HttpMethod.GET, request, null);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).contains("\"id\":2");
	}
}
