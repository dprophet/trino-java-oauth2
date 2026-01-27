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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientCredentialsConfigTest {

    private static final String CLIENT_ID = "test_client_id";
    private static final String CLIENT_SECRET = "test_client_secret";
    private static final String SCOPE = "read write";

    @Test
    void testClientCredentialsInstantiationWithOidc() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        ClientCredentialsConfig config = ClientCredentialsConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .scope(SCOPE)
                .urlConfig(urlConfig)
                .build();

        assertEquals(CLIENT_ID, config.getClientId());
        assertEquals(CLIENT_SECRET, config.getClientSecret());
        assertEquals(SCOPE, config.getScope());
        assertEquals(urlConfig, config.getUrlConfig());
    }

    @Test
    void testClientCredentialsInstantiationWithManualUrls() {
        ManualUrlsConfig urlConfig = ManualUrlsConfig.builder()
                .tokenEndpoint("https://auth.example.com/oauth/token")
                .build();

        ClientCredentialsConfig config = ClientCredentialsConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .urlConfig(urlConfig)
                .build();

        assertEquals(CLIENT_ID, config.getClientId());
        assertEquals(CLIENT_SECRET, config.getClientSecret());
        assertEquals(urlConfig, config.getUrlConfig());
    }

    @Test
    void testClientCredentialsWithAudience() {
        List<String> audience = Arrays.asList("api1", "api2");

        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        ClientCredentialsConfig config = ClientCredentialsConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .audience(audience)
                .urlConfig(urlConfig)
                .build();

        assertEquals(audience, config.getAudience());
    }

    @Test
    void testClientCredentialsRequiresClientId() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            ClientCredentialsConfig.builder()
                    .clientSecret(CLIENT_SECRET)
                    .urlConfig(urlConfig)
                    .build();
        });
    }

    @Test
    void testClientCredentialsRequiresClientSecret() {
        OidcConfig urlConfig = new OidcConfig(
                "https://auth.example.com/.well-known/openid-configuration"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            ClientCredentialsConfig.builder()
                    .clientId(CLIENT_ID)
                    .urlConfig(urlConfig)
                    .build();
        });
    }

    @Test
    void testClientCredentialsRequiresUrlConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClientCredentialsConfig.builder()
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .build();
        });
    }
}
