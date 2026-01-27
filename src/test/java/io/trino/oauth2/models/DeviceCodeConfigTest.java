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

class DeviceCodeConfigTest {

    private static final String CLIENT_ID = "fake_client_id";
    private static final String CLIENT_SECRET = "fake_client_secret";

    @Test
    void testDeviceCodeInstantiationManual() {
        // Test that the DeviceCodeConfig class can be instantiated correctly with ManualUrlsConfig
        ManualUrlsConfig urlConfig = ManualUrlsConfig.builder()
                .tokenEndpoint("https://sso.example.com/token")
                .deviceAuthorizationEndpoint("https://sso.example.com/device")
                .build();

        DeviceCodeConfig deviceCode = DeviceCodeConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .urlConfig(urlConfig)
                .build();

        assertEquals(CLIENT_ID, deviceCode.getClientId());
        assertEquals(CLIENT_SECRET, deviceCode.getClientSecret());
        assertEquals(urlConfig, deviceCode.getUrlConfig());
    }

    @Test
    void testDeviceCodeInstantiationOidc() {
        // Test that the DeviceCodeConfig class can be instantiated correctly with OidcConfig
        OidcConfig urlConfig = new OidcConfig(
                "https://sso.example.com/.well-known/openid-configuration"
        );

        DeviceCodeConfig deviceCode = DeviceCodeConfig.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .urlConfig(urlConfig)
                .build();

        assertEquals(CLIENT_ID, deviceCode.getClientId());
        assertEquals(CLIENT_SECRET, deviceCode.getClientSecret());
        assertEquals(urlConfig, deviceCode.getUrlConfig());
    }
}
