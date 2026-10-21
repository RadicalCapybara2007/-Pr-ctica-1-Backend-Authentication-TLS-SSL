package com.practicauno.security;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SecurityTestController {

	@GetMapping("/secure")
	public Map<String, String> secure(Authentication authentication) {
		return Map.of("message", "Acceso autorizado", "username", authentication.getName());
	}

	@GetMapping("/admin")
	public Map<String, String> admin() {
		return Map.of("message", "Acceso de administrador autorizado");
	}
}
