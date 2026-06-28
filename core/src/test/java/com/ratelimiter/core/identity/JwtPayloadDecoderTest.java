package com.ratelimiter.core.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class JwtPayloadDecoderTest {

    @Test
    void readsSubClaim() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"user-42\"}".getBytes(StandardCharsets.UTF_8));
        String token = "header." + payload + ".sig";

        assertThat(JwtPayloadDecoder.readSubject("Bearer " + token))
                .contains("user-42");
    }
}
