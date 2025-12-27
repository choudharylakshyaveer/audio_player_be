package org.audio.player.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class StreamTokenService {

    private static final Duration BUFFER_FOR_SHORT_LIVED_TOKEN = Duration.ofSeconds(30);
    private static final String SECRET = "stream-secret-key";
    public static final String HMAC_SHA_256 = "HmacSHA256";

    public String create(Long trackId, int trackLengthInSec) {
        long exp = Instant.now().plus(BUFFER_FOR_SHORT_LIVED_TOKEN).plus(Duration.ofSeconds(trackLengthInSec)).getEpochSecond();
        String payload = trackId + ":" + exp;

        String sig = hmac(payload);
        return Base64.getUrlEncoder()
                .encodeToString((payload + ":" + sig).getBytes());
    }

    public boolean isValid(String token, Long trackId) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token));
            String[] parts = decoded.split(":");

            long tokenTrackId = Long.parseLong(parts[0]);
            long exp = Long.parseLong(parts[1]);
            String sig = parts[2];

            if (tokenTrackId != trackId) return false;
            if (Instant.now().getEpochSecond() > exp) return false;

            return hmac(parts[0] + ":" + parts[1]).equals(sig);

        } catch (Exception e) {
            return false;
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKey =
                    new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);

            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // URL-safe Base64 (important for query params)
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawHmac);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC", e);
        }
    }
}
