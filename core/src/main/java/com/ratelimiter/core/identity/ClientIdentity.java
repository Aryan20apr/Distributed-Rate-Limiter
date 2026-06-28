package com.ratelimiter.core.identity;

public record ClientIdentity(
    String userId,
    String ipAddress,
    String apiKey
) {
public static final String ANONYMOUS_USER = "anonymous";

public static ClientIdentity anonymous(String ipAddress) {
    return new ClientIdentity(ANONYMOUS_USER, ipAddress, null);
}
}