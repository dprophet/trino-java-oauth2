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
import io.trino.oauth2.models.ManualUrlsConfig;
import io.trino.oauth2.models.OidcConfig;
import io.trino.oauth2.utils.TestHelpers;
import org.junit.jupiter.api.Test;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * E2E tests for Authorization Code flow with Hydra.
 * Corresponds to auth code tests in test_e2e_hydra_good_authcode_tests.py
 */
public class HydraAuthorizationCodeE2ETest extends HydraE2ETestBase {

    @Test
    void testAuthorizationCodeFlowManual() throws Exception {
        purgeTokens(AC_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(AC_CLIENT_ID)
                        .clientSecret(AC_CLIENT_SECRET)
                        .redirectUri(AC_REDIRECT_URI)
                        .urlConfig(ManualUrlsConfig.builder()
                                .tokenEndpoint(TOKEN_ENDPOINT)
                                .authorizationEndpoint(HYDRA_PUBLIC_URL + "/oauth2/auth")
                                .build())
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Authorization Code flow manual completed successfully.");
    }

    @Test
    void testAuthorizationCodeFlowOidc() throws Exception {
        purgeTokens(AC_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(AC_CLIENT_ID)
                        .clientSecret(AC_CLIENT_SECRET)
                        .redirectUri(AC_REDIRECT_URI)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Authorization Code flow OIDC completed successfully.");
    }

    @Test
    void testAuthorizationCodeFlowWithScopes() throws Exception {
        purgeTokens(AC_SCOPES_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(AC_SCOPES_CLIENT_ID)
                        .redirectUri(AC_SCOPES_REDIRECT_URI)
                        .scope(AC_SCOPES_SCOPE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Authorization Code flow with scopes completed successfully.");
    }

    @Test
    void testAuthorizationCodeFlowWithPkce() throws Exception {
        purgeTokens(AC_SCOPES_CLIENT_ID, "AuthorizationCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                AuthorizationCodeConfig.builder()
                        .clientId(AC_SCOPES_CLIENT_ID)
                        .redirectUri(AC_SCOPES_REDIRECT_URI)
                        .scope(AC_SCOPES_SCOPE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .usePkce(true)
                        .automationCallback(hydraAutomator::completeAuthFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Authorization Code flow with PKCE completed successfully.");
    }
}
