package com.practicauno.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ObjectMapper objectMapper,
			JwtDecoder jwtDecoder) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/login").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint((request, response, exception) -> {
							logger.warn("Unauthorized request to {}", request.getRequestURI());
							response.setStatus(HttpStatus.UNAUTHORIZED.value());
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							objectMapper.writeValue(response.getOutputStream(),
									new ApiError(HttpStatus.UNAUTHORIZED.value(), "No autenticado",
											"Se requiere un token Bearer válido."));
						}))
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler((request, response, exception) -> {
							logger.warn("Forbidden request to {}", request.getRequestURI());
							response.setStatus(HttpStatus.FORBIDDEN.value());
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							objectMapper.writeValue(response.getOutputStream(),
									new ApiError(HttpStatus.FORBIDDEN.value(), "Acceso denegado",
											"No tienes permisos para acceder a este recurso."));
						}))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable());
		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService(
			PasswordEncoder passwordEncoder,
			@Value("${app.security.username}") String username,
			@Value("${app.security.password}") String password) {
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles("USER")
				.build());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes.");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("authorities");
		authoritiesConverter.setAuthorityPrefix("");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}
}
