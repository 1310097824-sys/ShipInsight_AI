package com.gsmv.auth.dto;

public record CaptchaResponse(
        String captchaId,
        String imageBase64,
        long expiresInSeconds
) {
}
