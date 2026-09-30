package br.insper.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI gatewayOpenApi() {
		return new OpenAPI().info(new Info()
				.title("API Gateway")
				.description("Roteia /clientes/** e /lojas/** para os serviços correspondentes. "
						+ "As rotas reais expostas são as documentadas nos serviços de destino.")
				.version("v1"));
	}
}
