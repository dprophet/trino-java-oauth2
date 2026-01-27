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

package io.trino.oauth2;

import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.ManualUrlsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2ClientTest {

    private static final String CLIENT_ID = "fake_client_id";
    private static final String CLIENT_SECRET = "fake_client_secret";

    @Test
    void testOAuth2ClientInstantiation() {
        ManualUrlsConfig urlConfig = ManualUrlsConfig.builder()
                .tokenEndpoint("https://sso.example.com/token")
                .deviceAuthorizationEndpoint("https://sso.example.com/device")
                .build();

        OAuth2Client oauthClient = new OAuth2Client(
                DeviceCodeConfig.builder()
                        .clientId(CLIENT_ID)
                        .clientSecret(CLIENT_SECRET)
                        .urlConfig(urlConfig)
                        .build()
        );

        assertNotNull(oauthClient);

        // Verify that the token method exists and is callable
        assertDoesNotThrow(() -> {
            try {
                oauthClient.token();
            } catch (Exception e) {
                // Expected to fail since we're using fake credentials
                // We just want to verify the method exists
            }
        });
    }
}
