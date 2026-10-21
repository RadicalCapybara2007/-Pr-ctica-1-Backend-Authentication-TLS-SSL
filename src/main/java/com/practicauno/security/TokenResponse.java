package com.practicauno.security;

public record TokenResponse(String tokenType, String accessToken, long expiresIn) {
}
