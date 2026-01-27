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
import io.trino.oauth2.models.ManualUrlsConfig;
import io.trino.oauth2.models.OidcConfig;
import io.trino.oauth2.utils.TestHelpers;
import org.junit.jupiter.api.Test;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * E2E tests for Client Credentials flow with Hydra.
 * Corresponds to client credentials tests in test_e2e_hydra_good_tests.py
 */
public class HydraClientCredentialsE2ETest extends HydraE2ETestBase {

    @Test
    void testClientCredentialsFlowManual() throws Exception {
        purgeTokens(CC_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_CLIENT_ID)
                        .clientSecret(CC_CLIENT_SECRET)
                        .urlConfig(ManualUrlsConfig.builder()
                                .tokenEndpoint(TOKEN_ENDPOINT)
                                .build())
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Client Credentials manual completed successfully.");
    }

    @Test
    void testClientCredentialsFlowOidc() throws Exception {
        purgeTokens(CC_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_CLIENT_ID)
                        .clientSecret(CC_CLIENT_SECRET)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Client Credentials OIDC completed successfully.");
    }

    @Test
    void testClientCredentialsFlowWithScope() throws Exception {
        purgeTokens(CC_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_CLIENT_ID)
                        .clientSecret(CC_CLIENT_SECRET)
                        .scope(CC_SCOPE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        TestHelpers.assertJwtScopes(token, CC_SCOPE);
        System.out.println("Hydra Client Credentials flow with scope completed successfully.");
    }

    @Test
    void testClientCredentialsFlowWithAudience() throws Exception {
        purgeTokens(CC_AUD_CLIENT_ID, "ClientCredentialsConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                ClientCredentialsConfig.builder()
                        .clientId(CC_AUD_CLIENT_ID)
                        .clientSecret(CC_AUD_CLIENT_SECRET)
                        .audience(CC_AUD_AUDIENCE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        TestHelpers.assertJwtAudiences(token, String.join(" ", CC_AUD_AUDIENCE));
        System.out.println("Hydra Client Credentials flow with audience completed successfully.");
    }
}
