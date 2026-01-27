// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.trino.oauth2.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test helper utilities for JWT validation and assertion.
 */
public final class TestHelpers {

    private TestHelpers() {
        // Utility class
    }

    /**
     * Asserts that the token is a well-formed JWT by decoding it without signature verification.
     */
    public static void assertIsJwt(String token) {
        try {
            JWT.decode(token);
        } catch (Exception e) {
            fail("Token is not a valid JWT: " + e.getMessage());
        }
    }

    /**
     * Asserts that the JWT contains all expected audiences in its 'aud' claim.
     * Signature is NOT verified.
     *
     * @param token               the JWT token
     * @param expectedAudiences   space-delimited string (e.g. "aud1 aud2")
     */
    public static void assertJwtAudiences(String token, String expectedAudiences) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            List<String> audClaim = jwt.getAudience();
            if (audClaim == null) {
                audClaim = List.of();
            }

            List<String> expected = Arrays.asList(expectedAudiences.split("\\s+"));
            for (String aud : expected) {
                if (!audClaim.contains(aud)) {
                    fail("JWT is missing expected audience: " + aud + ". Actual: " + audClaim);
                }
            }
        } catch (Exception e) {
            fail("JWT audience validation failed: " + e.getMessage());
        }
    }

    /**
     * Asserts that the JWT contains all expected scopes in its 'scp' or 'scope' claim.
     * Signature is NOT verified.
     *
     * @param token            the JWT token
     * @param expectedScopes   space-delimited string (e.g. "scope1 scope2")
     */
    public static void assertJwtScopes(String token, String expectedScopes) {
        try {
            DecodedJWT jwt = JWT.decode(token);

            // Try 'scp' claim first (common in OAuth tokens)
            List<String> scopeClaim = jwt.getClaim("scp").asList(String.class);
            if (scopeClaim == null || scopeClaim.isEmpty()) {
                // Try 'scope' claim as fallback
                String scopeString = jwt.getClaim("scope").asString();
                if (scopeString != null) {
                    scopeClaim = Arrays.asList(scopeString.split("\\s+"));
                } else {
                    scopeClaim = List.of();
                }
            }

            List<String> expected = Arrays.asList(expectedScopes.split("\\s+"));
            for (String scope : expected) {
                if (!scopeClaim.contains(scope)) {
                    fail("JWT is missing expected scope: " + scope + ". Actual: " + scopeClaim);
                }
            }
        } catch (Exception e) {
            fail("JWT scope validation failed: " + e.getMessage());
        }
    }

    /**
     * Extracts and returns the 'exp' (expiration) claim from a JWT.
     * Returns null if the claim is not present.
     */
    public static Long getJwtExpiration(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt() != null ? jwt.getExpiresAt().getTime() / 1000 : null;
        } catch (Exception e) {
            fail("Failed to extract JWT expiration: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts and returns the 'sub' (subject) claim from a JWT.
     */
    public static String getJwtSubject(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            fail("Failed to extract JWT subject: " + e.getMessage());
            return null;
        }
    }
}
