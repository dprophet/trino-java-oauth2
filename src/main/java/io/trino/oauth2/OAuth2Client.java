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

import io.trino.oauth2.configs.Constants;
import io.trino.oauth2.flows.AuthorizationCodeOauth;
import io.trino.oauth2.flows.ClientCredentialsOauth;
import io.trino.oauth2.flows.DeviceCodeOauth;
import io.trino.oauth2.models.AuthorizationCodeConfig;
import io.trino.oauth2.models.ClientCredentialsConfig;
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.OAuth2Config;
import io.trino.oauth2.utils.OAuthTokenStore;

import java.io.IOException;

/**
 * The OAuth2Client is the main entry point for the OAuth2 library. It is
 * used to generate or refresh an access token.
 * <p>
 * The Client offers three configuration options:
 * 1. Client Credentials Flow: For machine-to-machine communication.
 * 2. Device Code Flow: For devices with limited input capabilities.
 * 3. Authorization Code Flow: For standard user authentication.
 */
public class OAuth2Client {
    private final OAuth2Config config;
    private final String proxyUrl;
    private final int validMinDurationThreshold;
    private final Object oauthFlowClient;

    /**
     * Creates a new OAuth2Client with the specified configuration.
     *
     * @param config                      The OAuth2 configuration (ClientCredentialsConfig, DeviceCodeConfig, or AuthorizationCodeConfig)
     * @param validMinDurationThreshold   The minimum duration (in seconds) that a token must be valid for before it's considered expired
     * @param proxyUrl                    Optional HTTP proxy URL
     */
    public OAuth2Client(
            OAuth2Config config,
            int validMinDurationThreshold,
            String proxyUrl
    ) {
        this.config = config;
        this.proxyUrl = proxyUrl;
        this.validMinDurationThreshold = validMinDurationThreshold;
        this.oauthFlowClient = initiateOAuthFlowClient();
    }

    /**
     * Creates a new OAuth2Client with default settings.
     *
     * @param config The OAuth2 configuration
     */
    public OAuth2Client(OAuth2Config config) {
        this(config, Constants.VALID_MIN_DURATION_THRESHOLD, null);
    }

    /**
     * Creates a new OAuth2Client with custom validity threshold.
     *
     * @param config                      The OAuth2 configuration
     * @param validMinDurationThreshold   The minimum duration (in seconds) that a token must be valid for
     */
    public OAuth2Client(OAuth2Config config, int validMinDurationThreshold) {
        this(config, validMinDurationThreshold, null);
    }

    private Object initiateOAuthFlowClient() {
        if (config instanceof ClientCredentialsConfig) {
            return new ClientCredentialsOauth((ClientCredentialsConfig) config, proxyUrl);
        } else if (config instanceof DeviceCodeConfig) {
            return new DeviceCodeOauth((DeviceCodeConfig) config, proxyUrl);
        } else if (config instanceof AuthorizationCodeConfig) {
            return new AuthorizationCodeOauth((AuthorizationCodeConfig) config, proxyUrl);
        }
        throw new IllegalArgumentException("Invalid OAuth mode: " + config.getClass().getSimpleName());
    }

    /**
     * Gets an access token, either from cache or by generating a new one.
     *
     * @return The access token
     * @throws IOException If there's an error obtaining the token
     */
    public String token() throws IOException {
        String accessToken = OAuthTokenStore.getActiveAccessToken(
                config.getClientId(),
                config.getClass().getSimpleName(),
                validMinDurationThreshold
        );

        if (accessToken != null) {
            return accessToken;
        }

        if (oauthFlowClient instanceof ClientCredentialsOauth) {
            return ((ClientCredentialsOauth) oauthFlowClient).generateOrRefreshToken();
        } else if (oauthFlowClient instanceof DeviceCodeOauth) {
            return ((DeviceCodeOauth) oauthFlowClient).generateOrRefreshToken();
        } else if (oauthFlowClient instanceof AuthorizationCodeOauth) {
            return ((AuthorizationCodeOauth) oauthFlowClient).generateOrRefreshToken();
        }

        throw new IllegalStateException("Unknown OAuth flow client type");
    }
}
