package br.insper.gateway.controller;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import br.insper.gateway.config.RouteProperties;
import br.insper.gateway.event.RequisicaoRoteadaEvent;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador para gerenciar o roteamento de requisições.
 */
@RestController
public class GatewayController {

	private final RestClient restClient;
	private final RouteProperties routeProperties;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Construtor da classe GatewayController.
	 *
	 * @param routeProperties Propriedades das rotas.
	 * @param eventPublisher  Publicador de eventos.
	 */
	public GatewayController(RouteProperties routeProperties, ApplicationEventPublisher eventPublisher) {
		this.restClient = RestClient.create();
		this.routeProperties = routeProperties;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Roteia uma requisição para o serviço de clientes.
	 *
	 * @param method  Método HTTP da requisição.
	 * @param request Requisição HTTP.
	 * @param body    Corpo da requisição.
	 * @return ResponseEntity com a resposta da requisição.
	 */
	@RequestMapping("/clientes/**")
	public ResponseEntity<String> rotearParaCliente(HttpMethod method, HttpServletRequest request,
			@RequestBody(required = false) String body) {
		return rotear(routeProperties.clienteServiceUrl(), "cliente-service", method, request, body);
	}

	/**
	 * Roteia uma requisição para o serviço de lojas.
	 *
	 * @param method  Método HTTP da requisição.
	 * @param request Requisição HTTP.
	 * @param body    Corpo da requisição.
	 * @return ResponseEntity com a resposta da requisição.
	 */
	@RequestMapping("/lojas/**")
	public ResponseEntity<String> rotearParaLoja(HttpMethod method, HttpServletRequest request,
			@RequestBody(required = false) String body) {
		return rotear(routeProperties.lojaServiceUrl(), "loja-service", method, request, body);
	}

	/**
	 * Encaminha a requisição para o serviço de destino e publica o evento de roteamento.
	 *
	 * @param baseUrl        URL base do serviço de destino.
	 * @param servicoDestino Nome do serviço de destino, usado no evento de roteamento.
	 * @param method         Método HTTP da requisição.
	 * @param request        Requisição HTTP.
	 * @param body           Corpo da requisição.
	 * @return ResponseEntity com a resposta da requisição.
	 */
	private ResponseEntity<String> rotear(String baseUrl, String servicoDestino, HttpMethod method,
			HttpServletRequest request, String body) {
		String path = request.getRequestURI();
		eventPublisher.publishEvent(new RequisicaoRoteadaEvent(this, servicoDestino, path));

		return restClient.method(method)
				.uri(baseUrl + path)
				.body(body == null ? "" : body)
				.retrieve()
				// repassa o status do servico de destino (4xx/5xx inclusive) em vez de
				// deixar o RestClient converter em excecao e virar 500 aqui no gateway
				.onStatus(status -> true, (req, res) -> {
				})
				.toEntity(String.class);
	}
}
