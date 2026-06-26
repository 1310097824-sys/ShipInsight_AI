package com.gsmv.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gsmv.auth.dto.CaptchaResponse;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CaptchaServiceTests {

    @Test
    void generateReturnsTokenAndImageDataUrl() {
        CaptchaService service = fixedService("ABCD2", Duration.ofMinutes(5));

        CaptchaResponse response = service.generate();

        assertTrue(response.captchaId().length() > 20);
        assertTrue(response.imageBase64().startsWith("data:image/png;base64,"));
        assertTrue(response.imageBase64().length() > 200);
        assertEquals(300, response.expiresInSeconds());
    }

    @Test
    void verifyIsCaseInsensitiveAndOneTimeOnly() {
        CaptchaService service = fixedService("ABCD2", Duration.ofMinutes(5));
        CaptchaResponse response = service.generate();

        assertTrue(service.verify(response.captchaId(), "abcd2"));
        assertFalse(service.verify(response.captchaId(), "ABCD2"));
    }

    @Test
    void verifyRejectsWrongOrExpiredCaptcha() {
        CaptchaService wrongService = fixedService("ABCD2", Duration.ofMinutes(5));
        CaptchaResponse wrongResponse = wrongService.generate();
        assertFalse(wrongService.verify(wrongResponse.captchaId(), "AAAAA"));
        assertFalse(wrongService.verify(wrongResponse.captchaId(), "ABCD2"));

        CaptchaService expiredService = fixedService("ABCD2", Duration.ofMillis(-1));
        CaptchaResponse expiredResponse = expiredService.generate();
        assertFalse(expiredService.verify(expiredResponse.captchaId(), "ABCD2"));
    }

    private CaptchaService fixedService(String code, Duration ttl) {
        return new CaptchaService(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC),
                ttl,
                () -> code
        );
    }
}
