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

package io.trino.oauth2.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationCodeConfigTest {

    private static final String CLIENT_ID = "test_client_id";
    private static final String CLIENT_SECRET = "test_client_secret";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String SCOPE = "openid profile email";

    @Test
    void testAuthorizationCodeInstantiation() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        AuthorizationCodeConfig config = AuthorizationCodeConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .scope(SCOPE)
                .urlConfig(urlConfig)
                .build();

        assertEquals(CLIENT_ID, config.getClientId());
        assertEquals(CLIENT_SECRET, config.getClientSecret());
        assertEquals(REDIRECT_URI, config.getRedirectUri());
        assertEquals(SCOPE, config.getScope());
        assertTrue(config.isUsePkce()); // Default is true
    }

    @Test
    void testAuthorizationCodeWithPkceDisabled() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        AuthorizationCodeConfig config = AuthorizationCodeConfig.builder()
                .clientId(CLIENT_ID)
                .redirectUri(REDIRECT_URI)
                .urlConfig(urlConfig)
                .usePkce(false)
                .build();

        assertFalse(config.isUsePkce());
    }

    @Test
    void testAuthorizationCodeWithCustomState() {
        String customState = "my-custom-state";
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        AuthorizationCodeConfig config = AuthorizationCodeConfig.builder()
                .clientId(CLIENT_ID)
                .redirectUri(REDIRECT_URI)
                .urlConfig(urlConfig)
                .state(customState)
                .build();

        assertEquals(customState, config.getState());
    }

    @Test
    void testAuthorizationCodeRequiresClientId() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            AuthorizationCodeConfig.builder()
                    .redirectUri(REDIRECT_URI)
                    .urlConfig(urlConfig)
                    .build();
        });
    }

    @Test
    void testAuthorizationCodeRequiresRedirectUri() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            AuthorizationCodeConfig.builder()
                    .clientId(CLIENT_ID)
                    .urlConfig(urlConfig)
                    .build();
        });
    }
}
