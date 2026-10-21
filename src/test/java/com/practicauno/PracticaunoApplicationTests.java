package com.practicauno;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"server.ssl.enabled=false",
		"server.ssl.key-store-password=test-keystore-password",
		"app.security.username=test-user",
		"app.security.password=test-password",
		"app.jwt.secret=0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class PracticaunoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void protectedEndpointRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/secure"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Se requiere un token Bearer válido."));
	}

	@Test
	void protectedEndpointRejectsInvalidToken() throws Exception {
		mockMvc.perform(get("/api/secure").header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginTokenGrantsAccessToProtectedEndpoint() throws Exception {
		String token = loginAndGetToken();

		mockMvc.perform(get("/api/secure").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("test-user"));
	}

	@Test
	void authenticatedUserWithoutAdminRoleGetsForbidden() throws Exception {
		String token = loginAndGetToken();

		mockMvc.perform(get("/api/admin").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("No tienes permisos para acceder a este recurso."));
	}

	@Test
	void invalidCredentialsReturnUnauthorized() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"test-user","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuario o contraseña inválidos."));
	}

	private String loginAndGetToken() throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"test-user","password":"test-password"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asString();
	}
}
