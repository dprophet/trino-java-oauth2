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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthTokenStoreTest {

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String MODE = "ClientCredentialsConfig";

    // Sample JWT token (expired, for testing only)
    private static final String EXPIRED_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9." +
            "4Adcj0ut0_ytQJXBiHPJkz9J1M7vHdOjQsKJYPJiw7I";

    // Sample JWT token with far future expiration
    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk5OTk5OTk5OTl9." +
            "Ks7KcdjrlUWcOseM3hKK9fLYqo1FQl9Dl2S5f-caNyQ";

    @AfterEach
    void cleanup() {
        // Clean up after each test
        OAuthTokenStore.purgeTokens(TEST_CLIENT_ID, null);
    }

    @Test
    void testSetAndGetAccessToken() {
        OAuthTokenStore.setAccessToken(TEST_CLIENT_ID, MODE, VALID_JWT);
        String retrieved = OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE);
        assertEquals(VALID_JWT, retrieved);
    }

    @Test
    void testGetActiveAccessTokenReturnsNullForExpiredToken() {
        OAuthTokenStore.setAccessToken(TEST_CLIENT_ID, MODE, EXPIRED_JWT);
        String retrieved = OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE);
        assertNull(retrieved);
    }

    @Test
    void testGetActiveAccessTokenReturnsNullWhenNoToken() {
        String retrieved = OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE);
        assertNull(retrieved);
    }

    @Test
    void testSetAccessAndRefreshTokens() {
        String refreshToken = "refresh_token_value";
        OAuthTokenStore.setAccessAndRefreshTokens(TEST_CLIENT_ID, MODE, VALID_JWT, refreshToken);

        String accessToken = OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE);
        String retrievedRefreshToken = OAuthTokenStore.getRefreshToken(TEST_CLIENT_ID, MODE);

        assertEquals(VALID_JWT, accessToken);
        assertEquals(refreshToken, retrievedRefreshToken);
    }

    @Test
    void testPurgeTokens() {
        String refreshToken = "refresh_token_value";
        OAuthTokenStore.setAccessAndRefreshTokens(TEST_CLIENT_ID, MODE, VALID_JWT, refreshToken);

        // Verify tokens are stored
        assertNotNull(OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE));
        assertNotNull(OAuthTokenStore.getRefreshToken(TEST_CLIENT_ID, MODE));

        // Purge tokens
        OAuthTokenStore.purgeTokens(TEST_CLIENT_ID, MODE);

        // Verify tokens are removed
        assertNull(OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, MODE));
        assertNull(OAuthTokenStore.getRefreshToken(TEST_CLIENT_ID, MODE));
    }

    @Test
    void testPurgeAllTokens() {
        // Set tokens for multiple modes
        OAuthTokenStore.setAccessToken(TEST_CLIENT_ID, "ClientCredentialsConfig", VALID_JWT);
        OAuthTokenStore.setAccessToken(TEST_CLIENT_ID, "DeviceCodeConfig", VALID_JWT);

        // Purge all tokens for the client
        OAuthTokenStore.purgeTokens(TEST_CLIENT_ID, null);

        // Verify all tokens are removed
        assertNull(OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, "ClientCredentialsConfig"));
        assertNull(OAuthTokenStore.getActiveAccessToken(TEST_CLIENT_ID, "DeviceCodeConfig"));
    }

    @Test
    void testInvalidModeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            OAuthTokenStore.setAccessToken(TEST_CLIENT_ID, "InvalidMode", VALID_JWT);
        });
    }
}
