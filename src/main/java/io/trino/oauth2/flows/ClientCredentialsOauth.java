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
import io.trino.oauth2.configs.Constants;
import io.trino.oauth2.configs.OAuthFlow;
import io.trino.oauth2.models.ClientCredentialsConfig;
import io.trino.oauth2.models.ManualUrlsConfig;
import io.trino.oauth2.models.OidcConfig;
import io.trino.oauth2.utils.OAuthTokenStore;
import io.trino.oauth2.utils.ProxyHelper;
import io.trino.oauth2.utils.UrlHelpers;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class contains functions related to server mode/client credentials flow.
 */
public class ClientCredentialsOauth {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");

    private final ClientCredentialsConfig config;
    private final String proxyUrl;
    private final OkHttpClient httpClient;

    public ClientCredentialsOauth(ClientCredentialsConfig config, String proxyUrl) {
        this.config = config;
        this.proxyUrl = proxyUrl;

        if (!(config.getUrlConfig() instanceof OidcConfig) &&
                !(config.getUrlConfig() instanceof ManualUrlsConfig)) {
            throw new RuntimeException(
                    "url_config class '" + config.getUrlConfig().getClass().getSimpleName() +
                            "' is not allowed."
            );
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Constants.REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.REQUEST_TIMEOUT, TimeUnit.SECONDS);

        ProxyHelper.configureProxy(builder, proxyUrl);
        this.httpClient = builder.build();
    }

    public String generateOrRefreshToken() throws IOException {
        return fetchAndStoreAccessToken();
    }

    private String getTokenEndpoint() throws IOException {
        if (config.getUrlConfig() instanceof OidcConfig) {
            OidcConfig oidcConfig = (OidcConfig) config.getUrlConfig();
            return UrlHelpers.getTokenEndpointFromOidc(oidcConfig.getOidcDiscoveryUrl());
        } else if (config.getUrlConfig() instanceof ManualUrlsConfig) {
            ManualUrlsConfig manualConfig = (ManualUrlsConfig) config.getUrlConfig();
            return manualConfig.getTokenEndpoint();
        }
        throw new IllegalStateException("Invalid URL config type");
    }

    private String fetchAndStoreAccessToken() throws IOException {
        String accessToken = OAuthTokenStore.getActiveAccessToken(
                config.getClientId(),
                config.getClass().getSimpleName()
        );
        if (accessToken != null) {
            return accessToken;
        }

        String serverModeUrl = getTokenEndpoint();
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", config.getClientId())
                .add("client_secret", config.getClientSecret())
                .add("grant_type", OAuthFlow.CLIENT_CREDENTIALS.getGrantType());

        if (config.getScope() != null) {
            formBuilder.add("scope", config.getScope());
        }

        if (config.getAudience() != null) {
            for (String aud : config.getAudience()) {
                formBuilder.add("audience", aud);
            }
        }

        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url(serverModeUrl)
                .post(formBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            if (responseJson.has("error")) {
                throw new RuntimeException(
                        "Failed to generate server mode access token: " + responseJson.toString()
                );
            }

            accessToken = responseJson.get("access_token").asText();
            OAuthTokenStore.setAccessToken(
                    config.getClientId(),
                    config.getClass().getSimpleName(),
                    accessToken
            );

            return accessToken;
        }
    }
}
