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

package io.trino.oauth2.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.trino.oauth2.models.AuthorizationCodeConfig;
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.OAuth2Config;
import io.trino.oauth2.utils.OAuthTokenStore;
import io.trino.oauth2.utils.ProxyHelper;
import okhttp3.*;

import java.io.IOException;

/**
 * Utility class for refreshing OAuth2 tokens.
 */
public final class RefreshToken {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private RefreshToken() {
        // Utility class
    }

    public static String refresh(OAuth2Config config, String refreshUrl, String proxyUrl) throws IOException {
        String clientId = config.getClientId();
        String clientSecret = null;

        if (config instanceof DeviceCodeConfig) {
            clientSecret = ((DeviceCodeConfig) config).getClientSecret();
        } else if (config instanceof AuthorizationCodeConfig) {
            clientSecret = ((AuthorizationCodeConfig) config).getClientSecret();
        }

        String refreshToken = OAuthTokenStore.getRefreshToken(clientId, config.getClass().getSimpleName());
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty refresh token");
        }

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        ProxyHelper.configureProxy(clientBuilder, proxyUrl);
        OkHttpClient httpClient = clientBuilder.build();

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId);

        if (clientSecret != null) {
            formBuilder.add("client_secret", clientSecret);
        }

        Request request = new Request.Builder()
                .url(refreshUrl)
                .post(formBuilder.build())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to refresh token: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode tokenData = objectMapper.readTree(responseBody);

            String newAccessToken = tokenData.get("access_token").asText();
            String newRefreshToken = tokenData.has("refresh_token") ?
                    tokenData.get("refresh_token").asText() : refreshToken;

            OAuthTokenStore.setAccessAndRefreshTokens(
                    clientId,
                    config.getClass().getSimpleName(),
                    newAccessToken,
                    newRefreshToken
            );

            String accessToken = OAuthTokenStore.getActiveAccessToken(
                    clientId,
                    config.getClass().getSimpleName()
            );

            if (accessToken == null) {
                throw new RuntimeException("Failed to retrieve access token");
            }

            return accessToken;
        }
    }
}
