package com.ratelimiter.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

class ClientIdentityResolverTest {

    private final ClientIdentityResolver resolver = new ClientIdentityResolver();

    @Test
    void resolvesXUserId() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-User-Id")).thenReturn("alice");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        ClientIdentity id = resolver.resolve(req);

        assertThat(id.userId()).isEqualTo("alice");
        assertThat(id.ipAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void resolvesFirstForwardedForHop() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(resolver.resolve(req).ipAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    void resolvesApiKey() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-API-Key")).thenReturn("sk-live-abc");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(resolver.resolve(req).apiKey()).isEqualTo("sk-live-abc");
    }

    @Test
    void anonymousWhenNoUserHeaders() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(resolver.resolve(req).userId()).isEqualTo(ClientIdentity.ANONYMOUS_USER);
    }
}
