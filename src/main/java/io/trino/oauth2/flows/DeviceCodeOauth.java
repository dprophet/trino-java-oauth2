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
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.ManualUrlsConfig;
import io.trino.oauth2.models.OidcConfig;
import io.trino.oauth2.utils.OAuthTokenStore;
import io.trino.oauth2.utils.ProxyHelper;
import io.trino.oauth2.utils.UrlHelpers;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * This class contains functions related to device code flow (aka user mode).
 */
public class DeviceCodeOauth {
    private static final Logger logger = LoggerFactory.getLogger(DeviceCodeOauth.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeviceCodeConfig config;
    private final String proxyUrl;
    private final OkHttpClient httpClient;

    public DeviceCodeOauth(DeviceCodeConfig config, String proxyUrl) {
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

    private String getDeviceAuthorizationEndpoint() throws IOException {
        if (config.getUrlConfig() instanceof OidcConfig) {
            OidcConfig oidcConfig = (OidcConfig) config.getUrlConfig();
            return UrlHelpers.getDeviceAuthorizationEndpointFromOidc(oidcConfig.getOidcDiscoveryUrl());
        } else if (config.getUrlConfig() instanceof ManualUrlsConfig) {
            ManualUrlsConfig manualConfig = (ManualUrlsConfig) config.getUrlConfig();
            if (manualConfig.getDeviceAuthorizationEndpoint() == null) {
                throw new RuntimeException(
                        "device_authorization_endpoint must be provided in ManualUrlsConfig."
                );
            }
            return manualConfig.getDeviceAuthorizationEndpoint();
        }
        throw new IllegalStateException("Invalid URL config type");
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

    public String generateOrRefreshToken() throws IOException {
        try {
            return RefreshToken.refresh(
                    config,
                    getTokenEndpoint(),
                    proxyUrl
            );
        } catch (Exception e) {
            logger.debug(
                    "Failed to update the access token. " +
                            "A new access token must be obtained before API calls can succeed. " +
                            "Error: {}", e.getMessage()
            );
            OAuthTokenStore.purgeTokens(config.getClientId(), config.getClass().getSimpleName());
            return fetchAndStoreAccessToken();
        }
    }

    private String fetchAndStoreAccessToken() throws IOException {
        String accessToken = OAuthTokenStore.getActiveAccessToken(
                config.getClientId(),
                config.getClass().getSimpleName()
        );
        if (accessToken != null) {
            logger.debug("Skipping auth. Using token in local storage.");
            return accessToken;
        }

        if (config.isPollForToken()) {
            startDeviceCodeAuthPoll();
        } else {
            startDeviceCodeAuthNoPoll();
        }

        accessToken = OAuthTokenStore.getActiveAccessToken(
                config.getClientId(),
                config.getClass().getSimpleName()
        );
        if (accessToken == null) {
            throw new RuntimeException("Failed to retrieve access token");
        }

        return accessToken;
    }

    private void startDeviceCodeAuthNoPoll() throws IOException {
        DeviceFlowResponse deviceFlow = startDeviceFlow();

        if (config.getAutomationCallback() != null) {
            config.getAutomationCallback().accept(deviceFlow.verificationUriComplete);
        } else {
            openLoginWindow(deviceFlow);

            String promptMsg = "\nConfirm authorization code shown in UI matches: " +
                    deviceFlow.userCode + ". If so, authenticate and press ENTER once done...\n";

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print(promptMsg);
                reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read user input", e);
            }
        }

        boolean tokenStored = fetchAndStoreDeviceFlowToken(deviceFlow.deviceCode);
        if (!tokenStored) {
            throw new RuntimeException("No token data was received via device flow.");
        }
    }

    private void startDeviceCodeAuthPoll() throws IOException {
        DeviceFlowResponse deviceFlow = startDeviceFlow();

        if (config.getAutomationCallback() != null) {
            config.getAutomationCallback().accept(deviceFlow.verificationUriComplete);
        } else {
            openLoginWindow(deviceFlow);

            String msg = "If browser did not open, copy this link into your browser " +
                    "and follow instructions. " + deviceFlow.verificationUriComplete + "\n\n" +
                    "Confirm authorization code shown in UI matches: " +
                    deviceFlow.userCode + ". If not, do not authenticate and abort.";
            System.out.println(msg);
        }

        pollForDeviceFlowToken(deviceFlow);
    }

    private DeviceFlowResponse startDeviceFlow() throws IOException {
        String deviceFlowAuthUrl = getDeviceAuthorizationEndpoint();

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("client_id", config.getClientId());

        if (config.getClientSecret() != null) {
            formBuilder.add("client_secret", config.getClientSecret());
        }

        if (config.getScope() != null) {
            formBuilder.add("scope", config.getScope());
        }

        if (config.getAudience() != null) {
            for (String aud : config.getAudience()) {
                formBuilder.add("audience", aud);
            }
        }

        Request request = new Request.Builder()
                .url(deviceFlowAuthUrl)
                .post(formBuilder.build())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                JsonNode errorJson = objectMapper.readTree(errorBody);
                String error = errorJson.get("error_description").asText();
                throw new RuntimeException(error);
            }

            String responseBody = response.body().string();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            return new DeviceFlowResponse(
                    responseJson.get("verification_uri_complete").asText(),
                    responseJson.get("user_code").asText(),
                    responseJson.get("device_code").asText(),
                    responseJson.get("interval").asInt(),
                    responseJson.get("verification_uri").asText(),
                    responseJson.get("expires_in").asInt()
            );
        }
    }

    private void openLoginWindow(DeviceFlowResponse deviceFlow) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(deviceFlow.verificationUriComplete));
            }
        } catch (Exception e) {
            logger.error("Error opening browser", e);
        }
    }

    private void pollForDeviceFlowToken(DeviceFlowResponse deviceFlow) {
        int maxAttempts = deviceFlow.expiresIn / deviceFlow.interval;
        for (int i = 0; i < maxAttempts; i++) {
            if (i % deviceFlow.interval == 0) {
                logger.info("Polling for token...");
            }

            try {
                Thread.sleep(deviceFlow.interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Polling interrupted", e);
            }

            boolean tokenStored = fetchAndStoreDeviceFlowToken(deviceFlow.deviceCode);
            if (tokenStored) {
                logger.info("Authentication has completed, the token has been retrieved.");
                return;
            }
        }

        logger.warn("\nDevice code has expired, polling has stopped.\n");
    }

    private boolean fetchAndStoreDeviceFlowToken(String deviceCode) {
        try {
            String getTokenUrl = getTokenEndpoint();

            FormBody.Builder formBuilder = new FormBody.Builder()
                    .add("client_id", config.getClientId())
                    .add("device_code", deviceCode)
                    .add("grant_type", OAuthFlow.DEVICE_CODE.getGrantType());

            if (config.getClientSecret() != null) {
                formBuilder.add("client_secret", config.getClientSecret());
            }

            Request request = new Request.Builder()
                    .url(getTokenUrl)
                    .post(formBuilder.build())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);

                if (responseJson.has("access_token")) {
                    String accessToken = responseJson.get("access_token").asText();
                    String refreshToken = responseJson.has("refresh_token") ?
                            responseJson.get("refresh_token").asText() : null;

                    OAuthTokenStore.setAccessAndRefreshTokens(
                            config.getClientId(),
                            config.getClass().getSimpleName(),
                            accessToken,
                            refreshToken
                    );
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Request to OAuth for access token failed with exception", e);
        }

        return false;
    }

    private static class DeviceFlowResponse {
        final String verificationUriComplete;
        final String userCode;
        final String deviceCode;
        final int interval;
        final String verificationUri;
        final int expiresIn;

        DeviceFlowResponse(String verificationUriComplete, String userCode, String deviceCode,
                           int interval, String verificationUri, int expiresIn) {
            this.verificationUriComplete = verificationUriComplete;
            this.userCode = userCode;
            this.deviceCode = deviceCode;
            this.interval = interval;
            this.verificationUri = verificationUri;
            this.expiresIn = expiresIn;
        }
    }
}
