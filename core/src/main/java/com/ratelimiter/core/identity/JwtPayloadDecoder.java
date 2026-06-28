package com.ratelimiter.core.identity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JwtPayloadDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtPayloadDecoder() {}

    /**
     * Extracts {@code sub} from a Bearer JWT payload without verifying the signature.
     * Suitable for rate-limit identity only — NOT for authentication.
     */
    public static Optional<String> readSubject(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode sub = node.get("sub");
            return sub != null && sub.isTextual()
                    ? Optional.of(sub.asText())
                    : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}