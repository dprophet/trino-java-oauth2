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

package io.trino.oauth2.e2e;

import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.ClientCredentialsConfig;
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.OidcConfig;
import org.junit.jupiter.api.Test;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E error tests for Client Credentials and Device Code flows with Hydra.
 * Corresponds to test_e2e_hydra_bad_tests.py
 */
public class HydraClientCredentialsErrorE2ETest extends HydraE2ETestBase {

    @Test
    void testClientCredentialsFlowBadClientId() {
        // A bad client ID is one that doesn't exist. No need to purge.
        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(BAD_CLIENT_ID)  // Intentionally bad
                        .clientSecret(CC_CLIENT_SECRET)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("'error': 'invalid_client'") ||
                        exception.getMessage().contains("invalid_client"),
                "Expected invalid_client error, got: " + exception.getMessage());
        System.out.println("Hydra Client Credentials bad client ID test completed successfully.");
    }

    @Test
    void testClientCredentialsFlowBadSecret() throws Exception {
        purgeTokens(CC_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_CLIENT_ID)
                        .clientSecret(BAD_CLIENT_SECRET)  // Intentionally bad
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("'error': 'invalid_client'") ||
                        exception.getMessage().contains("invalid_client"),
                "Expected invalid_client error, got: " + exception.getMessage());
        System.out.println("Hydra Client Credentials bad secret test completed successfully.");
    }

    @Test
    void testClientCredentialsFlowBadScope() throws Exception {
        purgeTokens(CC_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_CLIENT_ID)
                        .clientSecret(CC_CLIENT_SECRET)
                        .scope(BAD_SCOPE)  // Intentionally bad
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("'error': 'invalid_scope'") ||
                        exception.getMessage().contains("invalid_scope"),
                "Expected invalid_scope error, got: " + exception.getMessage());
        System.out.println("Hydra Client Credentials bad scope test completed successfully.");
    }

    @Test
    void testDeviceCodeFlowBadScope() throws Exception {
        purgeTokens(DC_NO_SECRET_CLIENT_ID, "DeviceCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(DC_NO_SECRET_CLIENT_ID)
                        .scope(BAD_SCOPE)  // Intentionally bad
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .pollForToken(true)
                        .automationCallback(hydraAutomator::completeDeviceFlow)
                        .build()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("The requested scope is invalid") ||
                        exception.getMessage().contains("invalid_scope"),
                "Expected invalid scope error, got: " + exception.getMessage());
        System.out.println("Hydra Device Code bad scope test completed successfully.");
    }
}
