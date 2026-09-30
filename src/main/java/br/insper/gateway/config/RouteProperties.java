package br.insper.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.routes")
public record RouteProperties(String clienteServiceUrl, String lojaServiceUrl) {
}
