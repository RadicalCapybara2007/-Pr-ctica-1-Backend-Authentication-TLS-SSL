package com.practicauno.security;

public record ApiError(int status, String error, String message) {
}
