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
import io.trino.oauth2.models.AuthorizationCodeConfig;
import io.trino.oauth2.models.OidcConfig;
import org.junit.jupiter.api.Test;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E error tests for Authorization Code flow with Hydra.
 * Corresponds to test_e2e_hydra_bad_authcode_tests.py
 */
public class HydraAuthorizationCodeErrorE2ETest extends HydraE2ETestBase {

    @Test
    void testAuthorizationCodeFlowBadScope() throws Exception {
        purgeTokens(AC_SCOPES_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(AC_SCOPES_CLIENT_ID)
                        .scope(BAD_SCOPE)  // Intentionally bad
                        .redirectUri(AC_SCOPES_REDIRECT_URI)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        // In Python, this raises ValueError; in Java we use IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("error: invalid_scope") ||
                        exception.getMessage().contains("invalid_scope"),
                "Expected invalid_scope error, got: " + exception.getMessage());
        System.out.println("Hydra Authorization Code bad scope test completed successfully.");
    }

    @Test
    void testAuthorizationCodeFlowBadClientId() throws Exception {
        purgeTokens(AC_SCOPES_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(BAD_CLIENT_ID)  // Intentionally bad
                        .scope(AC_SCOPES_SCOPE)
                        .redirectUri(AC_SCOPES_REDIRECT_URI)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, oauthClient::token);
        assertTrue(exception.getMessage().contains("error: invalid_client") ||
                        exception.getMessage().contains("invalid_client"),
                "Expected invalid_client error, got: " + exception.getMessage());
        System.out.println("Hydra Authorization Code bad client ID test completed successfully.");
    }
}
