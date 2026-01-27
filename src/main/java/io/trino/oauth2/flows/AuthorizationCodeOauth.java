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
import io.trino.oauth2.models.AuthorizationCodeConfig;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains functions related to authorization code flow.
 */
public class AuthorizationCodeOauth {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationCodeOauth.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom secureRandom = new SecureRandom();

    private final AuthorizationCodeConfig config;
    private final String proxyUrl;
    private final OkHttpClient httpClient;
    private final String state;
    private String codeVerifier;

    public AuthorizationCodeOauth(AuthorizationCodeConfig config, String proxyUrl) {
        this.config = config;
        this.proxyUrl = proxyUrl;
        this.state = config.getState() != null ? config.getState() : generateSecureToken(16);

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

    private static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getAuthorizationEndpoint() throws IOException {
        if (config.getUrlConfig() instanceof OidcConfig) {
            OidcConfig oidcConfig = (OidcConfig) config.getUrlConfig();
            return UrlHelpers.getAuthorizationEndpointFromOidc(oidcConfig.getOidcDiscoveryUrl());
        } else if (config.getUrlConfig() instanceof ManualUrlsConfig) {
            ManualUrlsConfig manualConfig = (ManualUrlsConfig) config.getUrlConfig();
            if (manualConfig.getAuthorizationEndpoint() == null) {
                throw new RuntimeException(
                        "authorization_endpoint must be provided in ManualUrlsConfig."
                );
            }
            return manualConfig.getAuthorizationEndpoint();
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

        startAuthorizationFlow();

        accessToken = OAuthTokenStore.getActiveAccessToken(
                config.getClientId(),
                config.getClass().getSimpleName()
        );
        if (accessToken == null) {
            throw new RuntimeException("Failed to retrieve access token");
        }

        return accessToken;
    }

    private PkceChallenge generatePkceChallenge() {
        String verifier = generateSecureToken(64);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return new PkceChallenge(verifier, challenge);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void startAuthorizationFlow() throws IOException {
        String authEndpoint = getAuthorizationEndpoint();

        StringBuilder params = new StringBuilder();
        params.append("client_id=").append(URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8));
        params.append("&response_type=code");
        params.append("&redirect_uri=").append(URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8));
        params.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));

        if (config.getScope() != null) {
            params.append("&scope=").append(URLEncoder.encode(config.getScope(), StandardCharsets.UTF_8));
        }

        if (config.getAudience() != null) {
            for (String aud : config.getAudience()) {
                params.append("&audience=").append(URLEncoder.encode(aud, StandardCharsets.UTF_8));
            }
        }

        if (config.isUsePkce()) {
            PkceChallenge pkce = generatePkceChallenge();
            this.codeVerifier = pkce.verifier;
            params.append("&code_challenge=").append(URLEncoder.encode(pkce.challenge, StandardCharsets.UTF_8));
            params.append("&code_challenge_method=S256");
        }

        String authorizationUrl = authEndpoint + "?" + params;

        String authCode;
        if (config.getAutomationCallback() != null) {
            String finalRedirectUrl = config.getAutomationCallback().apply(authorizationUrl);
            authCode = extractCodeFromInput(finalRedirectUrl);
        } else {
            openLoginWindow(authorizationUrl);

            String promptMsg = "\nStandard Authorization Code Flow:\n" +
                    "1. The browser should have opened to the login page.\n" +
                    "2. Please log in and authorize the application.\n" +
                    "3. You will be redirected to a URL containing a 'code' parameter.\n" +
                    "4. Copy the value of the 'code' parameter (or the full URL) " +
                    "and paste it below.\n\n" +
                    "Enter Authorization Code: ";

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print(promptMsg);
                String userInput = reader.readLine();
                authCode = extractCodeFromInput(userInput);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read user input", e);
            }
        }

        boolean tokenStored = exchangeCodeForToken(authCode);
        if (!tokenStored) {
            throw new RuntimeException("No token data was received via authorization flow.");
        }
    }

    private String extractCodeFromInput(String userInput) {
        if (userInput.contains("code=")) {
            Pattern pattern = Pattern.compile("[?&]code=([^&]+)");
            Matcher matcher = pattern.matcher(userInput);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return userInput.trim();
    }

    private void openLoginWindow(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            logger.error("Error opening browser", e);
        }
    }

    private boolean exchangeCodeForToken(String code) throws IOException {
        String getTokenUrl = getTokenEndpoint();

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("grant_type", OAuthFlow.AUTH_CODE_PKCE.getGrantType())
                .add("client_id", config.getClientId())
                .add("code", code)
                .add("redirect_uri", config.getRedirectUri());

        if (config.getClientSecret() != null) {
            formBuilder.add("client_secret", config.getClientSecret());
        }

        if (codeVerifier != null) {
            formBuilder.add("code_verifier", codeVerifier);
        }

        Request request = new Request.Builder()
                .url(getTokenUrl)
                .post(formBuilder.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                JsonNode errorJson = objectMapper.readTree(errorBody);
                String error = errorJson.has("error_description") ?
                        errorJson.get("error_description").asText() : errorBody;
                throw new RuntimeException(error);
            }

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

            return false;
        }
    }

    private static class PkceChallenge {
        final String verifier;
        final String challenge;

        PkceChallenge(String verifier, String challenge) {
            this.verifier = verifier;
            this.challenge = challenge;
        }
    }
}
