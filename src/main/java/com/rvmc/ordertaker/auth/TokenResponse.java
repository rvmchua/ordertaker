package com.rvmc.ordertaker.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    public static TokenResponse bearer(String accessToken) {
        return new TokenResponse(accessToken, "Bearer", 900);
    }
}
