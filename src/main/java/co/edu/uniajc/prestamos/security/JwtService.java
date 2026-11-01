package co.edu.uniajc.prestamos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final byte[] secret;
    private final long lifetimeSeconds;
    private final Clock clock;
    private final ObjectMapper mapper;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-hours}") long hours,
                      Clock clock, ObjectMapper mapper) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("APP_JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetimeSeconds = Duration.ofHours(hours).toSeconds();
        this.clock = clock;
        this.mapper = mapper;
    }

    public String issue(Long userId) {
        try {
            String header = encodeJson(new Header("JWT", "HS256"));
            String payload = encodeJson(new Payload(userId.toString(), Instant.now(clock).plusSeconds(lifetimeSeconds).getEpochSecond()));
            String signed = header + "." + payload;
            return signed + "." + encoder.encodeToString(sign(signed));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear token", ex);
        }
    }

    public Long verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;
            Header header = mapper.readValue(decoder.decode(parts[0]), Header.class);
            if (!"JWT".equals(header.typ()) || !"HS256".equals(header.alg())) return null;
            String signed = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(sign(signed), decoder.decode(parts[2]))) return null;
            Payload payload = mapper.readValue(decoder.decode(parts[1]), Payload.class);
            if (payload.exp() <= Instant.now(clock).getEpochSecond()) return null;
            return Long.valueOf(payload.sub());
        } catch (Exception ex) {
            return null;
        }
    }

    private String encodeJson(Object value) throws Exception {
        return encoder.encodeToString(mapper.writeValueAsBytes(value));
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private record Header(String typ, String alg) {}
    private record Payload(String sub, long exp) {}
}
