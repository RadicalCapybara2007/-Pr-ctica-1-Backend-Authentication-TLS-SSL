package com.practicauno.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final long expirationSeconds;

	public JwtService(
			JwtEncoder jwtEncoder,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
		this.jwtEncoder = jwtEncoder;
		this.expirationSeconds = expirationSeconds;
	}

	public TokenResponse createToken(Authentication authentication) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(expirationSeconds, ChronoUnit.SECONDS);
		List<String> authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("practicauno")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(authentication.getName())
				.claim("authorities", authorities)
				.build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		return new TokenResponse("Bearer", token, expirationSeconds);
	}
}
