package br.insper.gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exige um access token JWT do Auth0 nas rotas roteadas pelo gateway
 * ({@code /clientes/**}, {@code /lojas/**}, {@code /produtos/**},
 * {@code /pedidos/**}, {@code /entregadores/**}). Demais endpoints (actuator,
 * swagger) ficam liberados. pedidos-lojas e entregador não validam o token
 * de novo: o gateway é o único ponto de entrada do front-end.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

	private final Auth0Properties auth0Properties;
	private final List<String> allowedOrigins;

	public SecurityConfig(Auth0Properties auth0Properties, @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
		this.auth0Properties = auth0Properties;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigins.toArray(new String[0]))
				.allowedMethods("*")
				.allowedHeaders("*");
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
						.requestMatchers("/clientes/**", "/lojas/**", "/produtos/**", "/pedidos/**",
								"/entregadores/**").authenticated()
						.anyRequest().permitAll()
				)
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(jwtDecoder()))
				);

		return http.build();
	}

	/**
	 * Decoder manual (em vez de {@code spring.security.oauth2.resourceserver.jwt.issuer-uri})
	 * porque o auto-configurado busca o /.well-known/openid-configuration do
	 * Auth0 na subida da aplicação — o que derruba o boot (e os testes) sem
	 * rede ou com AUTH0_DOMAIN ainda não configurado. O JWK set aqui só é
	 * buscado na hora de validar um token de verdade.
	 */
	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(auth0Properties.jwkSetUri()).build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(auth0Properties.issuerUri());
		OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(auth0Properties.audience());
		jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

		return jwtDecoder;
	}

	private record AudienceValidator(String audience) implements OAuth2TokenValidator<Jwt> {

		@Override
		public OAuth2TokenValidatorResult validate(Jwt jwt) {
			if (jwt.getAudience().contains(audience)) {
				return OAuth2TokenValidatorResult.success();
			}
			OAuth2Error erro = new OAuth2Error("invalid_token", "O token não contém o audience exigido", null);
			return OAuth2TokenValidatorResult.failure(erro);
		}
	}
}
