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
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.ManualUrlsConfig;
import io.trino.oauth2.models.OidcConfig;
import io.trino.oauth2.utils.TestHelpers;
import org.junit.jupiter.api.Test;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * E2E tests for Device Code flow with Hydra.
 * Corresponds to device code tests in test_e2e_hydra_good_tests.py
 */
public class HydraDeviceCodeE2ETest extends HydraE2ETestBase {

    @Test
    void testDeviceCodeFlowManual() throws Exception {
        purgeTokens(DC_CLIENT_ID, "DeviceCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(DC_CLIENT_ID)
                        .clientSecret(DC_CLIENT_SECRET)
                        .scope(DC_SCOPE)
                        .urlConfig(ManualUrlsConfig.builder()
                                .tokenEndpoint(TOKEN_ENDPOINT)
                                .deviceAuthorizationEndpoint(DEVICE_AUTH_ENDPOINT)
                                .build())
                        .pollForToken(true)
                        .automationCallback(hydraAutomator::completeDeviceFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Device flow manual completed successfully.");
    }

    @Test
    void testDeviceCodeFlowOidc() throws Exception {
        purgeTokens(DC_CLIENT_ID, "DeviceCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(DC_CLIENT_ID)
                        .clientSecret(DC_CLIENT_SECRET)
                        .scope(DC_SCOPE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .pollForToken(true)
                        .automationCallback(hydraAutomator::completeDeviceFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra Device flow OIDC completed successfully.");
    }

    @Test
    void testDeviceCodeFlowNoSecrets() throws Exception {
        purgeTokens(DC_NO_SECRET_CLIENT_ID, "DeviceCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(DC_NO_SECRET_CLIENT_ID)
                        .scope(DC_NO_SECRET_SCOPE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .pollForToken(true)
                        .automationCallback(hydraAutomator::completeDeviceFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        System.out.println("Hydra device flow no secrets completed successfully.");
    }

    @Test
    void testDeviceCodeFlowNoSecretsScopesAudience() throws Exception {
        purgeTokens(DC_NO_SECRET_AUD_CLIENT_ID, "DeviceCodeConfig");

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(DC_NO_SECRET_AUD_CLIENT_ID)
                        .scope(DC_NO_SECRET_AUD_SCOPE)
                        .audience(DC_NO_SECRET_AUD_AUDIENCE)
                        .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                        .pollForToken(true)
                        .automationCallback(hydraAutomator::completeDeviceFlow)
                        .build()
        );

        String token = oauthClient.token();
        assertNotNull(token);
        TestHelpers.assertIsJwt(token);
        TestHelpers.assertJwtAudiences(token, String.join(" ", DC_NO_SECRET_AUD_AUDIENCE));
        System.out.println("Hydra device flow with audience completed successfully.");
    }
}
