package com.ratelimiter.core.identity;

import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIdentityResolver {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    public ClientIdentity resolve(HttpServletRequest request) {
        String userId = resolveUserId(request);
        String apiKey = blankToNull(request.getHeader(HEADER_API_KEY));
        String ipAddress = resolveClientIp(request);
        return new ClientIdentity(userId, ipAddress, apiKey);
    }

    private String resolveUserId(HttpServletRequest request) {
        return JwtPayloadDecoder.readSubject(request.getHeader(HEADER_AUTHORIZATION))
                .or(() -> Optional.ofNullable(blankToNull(request.getHeader(HEADER_USER_ID))))
                .orElse(ClientIdentity.ANONYMOUS_USER);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the original client per RFC 7239 de-facto convention
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}