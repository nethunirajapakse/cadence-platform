package com.cadence.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilTest {

    @Test
    void tokensAreUniqueAndUrlSafe() {
        String a = TokenUtil.newToken();
        String b = TokenUtil.newToken();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void hashIsStableHexAndNotTheToken() {
        String token = TokenUtil.newToken();

        assertThat(TokenUtil.hash(token)).isEqualTo(TokenUtil.hash(token));
        assertThat(TokenUtil.hash(token)).matches("[0-9a-f]{64}").isNotEqualTo(token);
        assertThat(TokenUtil.hash("a")).isNotEqualTo(TokenUtil.hash("b"));
    }

    @Test
    void emailsAreNormalizedToTrimmedLowerCase() {
        assertThat(TokenUtil.normalizeEmail("  Jane.Doe@Example.COM ")).isEqualTo("jane.doe@example.com");
        assertThat(TokenUtil.normalizeEmail(null)).isNull();
    }
}
