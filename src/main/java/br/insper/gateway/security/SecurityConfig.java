package br.insper.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * O gateway é um resource server OAuth2: toda rota roteada exige um JWT
 * (HS256, assinado com {@code JWT_SECRET}, o mesmo segredo dos outros
 * serviços) com as claims {@code sub} e {@code role}. O papel vira a
 * authority {@code ROLE_<papel>} e decide o que cada um pode acessar.
 */
@Configuration
public class SecurityConfig {

	public static final String CLIENTE = "cliente";
	public static final String LOJA = "loja";
	public static final String ENTREGADOR = "entregador";

	private static final Set<String> PAPEIS = Set.of(CLIENTE, LOJA, ENTREGADOR);

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**", "/error", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
						.permitAll()
						.requestMatchers("/clientes/**").hasRole(CLIENTE)
						// Todo mundo logado vê as lojas; só a própria loja altera.
						.requestMatchers(HttpMethod.GET, "/lojas/**").hasAnyRole(CLIENTE, LOJA, ENTREGADOR)
						.requestMatchers("/lojas/**").hasRole(LOJA)
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(@Value("${gateway.jwt.secret}") String secret) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefault(),
				new JwtClaimValidator<Object>(JwtClaimNames.SUB, sub -> sub != null),
				new JwtClaimValidator<Object>("role", PAPEIS::contains)));
		return decoder;
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
		papeis.setAuthoritiesClaimName("role");
		papeis.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(papeis);
		return converter;
	}
}
