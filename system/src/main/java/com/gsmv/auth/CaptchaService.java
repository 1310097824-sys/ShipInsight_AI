package com.gsmv.auth;

import com.gsmv.auth.dto.CaptchaResponse;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 5;
    private static final int WIDTH = 154;
    private static final int HEIGHT = 46;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String DATA_URL_PREFIX = "data:image/png;base64,";

    private final SecureRandom random;
    private final Clock clock;
    private final Duration ttl;
    private final Supplier<String> codeSupplier;
    private final ConcurrentHashMap<String, CaptchaEntry> entries = new ConcurrentHashMap<>();

    public CaptchaService() {
        this(new SecureRandom(), Clock.systemUTC(), TTL, null);
    }

    CaptchaService(SecureRandom random, Clock clock, Duration ttl, Supplier<String> codeSupplier) {
        this.random = random;
        this.clock = clock;
        this.ttl = ttl;
        this.codeSupplier = codeSupplier == null ? this::randomCode : codeSupplier;
    }

    public CaptchaResponse generate() {
        purgeExpired();
        String captchaId = UUID.randomUUID().toString();
        String code = codeSupplier.get().toUpperCase(Locale.ROOT);
        long expiresAt = clock.millis() + ttl.toMillis();
        entries.put(captchaId, new CaptchaEntry(code, expiresAt));
        return new CaptchaResponse(captchaId, DATA_URL_PREFIX + renderBase64Png(code), ttl.toSeconds());
    }

    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null || captchaCode.isBlank()) {
            return false;
        }
        CaptchaEntry entry = entries.remove(captchaId);
        if (entry == null || entry.expiresAt() < clock.millis()) {
            return false;
        }
        return entry.code().equalsIgnoreCase(captchaCode.trim());
    }

    int size() {
        return entries.size();
    }

    private void purgeExpired() {
        long now = clock.millis();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }

    private String renderBase64Png(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintBackground(graphics);
            paintNoise(graphics);
            paintCode(graphics, code);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("验证码图片生成失败", ex);
        }
    }

    private void paintBackground(Graphics2D graphics) {
        graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(7, 20, 46), WIDTH, HEIGHT, new Color(30, 20, 75)));
        graphics.fillRoundRect(0, 0, WIDTH, HEIGHT, 18, 18);
        graphics.setColor(new Color(0, 229, 255, 36));
        for (int x = 8; x < WIDTH; x += 18) {
            graphics.drawLine(x, 0, x - 16, HEIGHT);
        }
    }

    private void paintNoise(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 9; i++) {
            graphics.setColor(new Color(random.nextInt(90), 180 + random.nextInt(60), 210 + random.nextInt(45), 80));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            graphics.drawLine(x1, y1, x2, y2);
        }
        for (int i = 0; i < 34; i++) {
            graphics.setColor(new Color(255, 255, 255, 35 + random.nextInt(80)));
            graphics.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }
    }

    private void paintCode(Graphics2D graphics, String code) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 28);
        int charWidth = WIDTH / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            AffineTransform original = graphics.getTransform();
            int x = 16 + i * charWidth;
            int y = 31 + random.nextInt(6);
            double rotation = Math.toRadians(-12 + random.nextInt(25));
            graphics.rotate(rotation, x + 9, y - 10);
            graphics.setFont(font.deriveFont(26f + random.nextInt(5)));
            graphics.setColor(new Color(230, 252, 255));
            graphics.drawString(String.valueOf(code.charAt(i)), x, y);
            graphics.setTransform(original);
        }
    }

    private record CaptchaEntry(String code, long expiresAt) {
    }
}
