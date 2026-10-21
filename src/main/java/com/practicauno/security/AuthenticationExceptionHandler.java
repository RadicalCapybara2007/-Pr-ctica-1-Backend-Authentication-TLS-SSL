package com.practicauno.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentials() {
		logger.warn("Login rejected because the credentials are invalid.");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "No autenticado",
						"Usuario o contraseña inválidos."));
	}
}
