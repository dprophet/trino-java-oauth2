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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automates the Ory Hydra authentication flows by acting as the Administrator.
 * Java port of tests/hydra_helper.py
 */
public class HydraAutoConsent {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String adminUrl;
    private final String userEmail;
    private final OkHttpClient client;

    // Simple in-memory cookie jar to preserve cookies across requests
    private static class SimpleCookieJar implements CookieJar {
        private final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<>();
        }
    }

    public HydraAutoConsent(String adminUrl, String userEmail) {
        this.adminUrl = adminUrl;
        this.userEmail = userEmail;
        // Use a non-redirecting client with cookie jar for most requests
        SimpleCookieJar cookieJar = new SimpleCookieJar();
        this.client = new OkHttpClient.Builder()
                .followRedirects(false)
                .cookieJar(cookieJar)
                .build();
    }

    // Create a client that follows redirects (for final completion steps)
    // Shares the same cookie jar as the main client
    private OkHttpClient getRedirectingClient() {
        CookieJar cookieJar = client.cookieJar();
        return new OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(cookieJar)
                .build();
    }

    public HydraAutoConsent(String adminUrl) {
        this(adminUrl, "foo@bar.com");
    }

    /**
     * Normalizes URLs by replacing Docker internal hostnames with the public URL.
     * Uses HYDRA_PUBLIC_URL environment variable when running tests on the host machine.
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        // Get the public URL from environment (e.g., http://localhost:4444)
        String publicUrl = System.getenv("HYDRA_PUBLIC_URL");
        if (publicUrl == null) {
            publicUrl = "http://localhost:4444"; // Default fallback
        }

        // Replace http://hydra:4444 with the public URL
        return url.replace("http://hydra:4444", publicUrl)
                  .replace("http://hydra:4445", publicUrl.replace(":4444", ":4445"));
    }

    private String getQueryParam(String url, String paramName) {
        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null) {
                throw new IllegalArgumentException("Missing '" + paramName + "' in URL: " + url);
            }

            // URI.getQuery() already returns decoded values, so we don't need to decode again
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);  // Limit split to 2 parts in case value contains '='
                if (pair.length == 2 && pair[0].equals(paramName)) {
                    return pair[1];
                }
            }
            throw new IllegalArgumentException("Missing '" + paramName + "' in URL: " + url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse query param: " + paramName, e);
        }
    }

    private JsonNode getConsentRequestDetails(String consentChallenge) throws IOException {
        HttpUrl url = HttpUrl.parse(adminUrl + "/admin/oauth2/auth/requests/consent")
                .newBuilder()
                .addQueryParameter("consent_challenge", consentChallenge)
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get consent request: " + response.code());
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    private String acceptDeviceRequest(String deviceChallenge, String userCode) throws IOException {
        HttpUrl url = HttpUrl.parse(adminUrl + "/admin/oauth2/auth/requests/device/accept")
                .newBuilder()
                .addQueryParameter("device_challenge", deviceChallenge)
                .build();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("user_code", userCode);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder().url(url).put(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to accept device request: " + response.code());
            }
            JsonNode result = objectMapper.readTree(response.body().string());
            return normalizeUrl(result.get("redirect_to").asText());
        }
    }

    private String acceptLoginRequest(String loginChallenge) throws IOException {
        HttpUrl url = HttpUrl.parse(adminUrl + "/admin/oauth2/auth/requests/login/accept")
                .newBuilder()
                .addQueryParameter("login_challenge", loginChallenge)
                .build();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("subject", userEmail);
        json.put("remember", true);
        json.put("remember_for", 3600);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder().url(url).put(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to accept login: " + response.code());
            }
            JsonNode result = objectMapper.readTree(response.body().string());
            return normalizeUrl(result.get("redirect_to").asText());
        }
    }

    private String acceptConsentRequest(String consentChallenge) throws IOException {
        JsonNode details = getConsentRequestDetails(consentChallenge);

        JsonNode requestedScope = details.get("requested_scope");
        JsonNode requestedAudience = details.get("requested_access_token_audience");

        List<String> scopes;
        if (requestedScope != null && requestedScope.isArray() && requestedScope.size() > 0) {
            scopes = new ArrayList<>();
            for (JsonNode scope : requestedScope) {
                scopes.add(scope.asText());
            }
        } else {
            scopes = Arrays.asList("openid", "offline");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode scopeArray = objectMapper.createArrayNode();
        scopes.forEach(scopeArray::add);
        payload.set("grant_scope", scopeArray);
        payload.put("remember", true);
        payload.put("remember_for", 3600);

        ObjectNode session = objectMapper.createObjectNode();
        ObjectNode idToken = objectMapper.createObjectNode();
        idToken.put("email", userEmail);
        idToken.put("name", "Automated Tester");
        session.set("id_token", idToken);
        payload.set("session", session);

        if (requestedAudience != null && requestedAudience.isArray() && requestedAudience.size() > 0) {
            payload.set("grant_access_token_audience", requestedAudience);
        }

        HttpUrl url = HttpUrl.parse(adminUrl + "/admin/oauth2/auth/requests/consent/accept")
                .newBuilder()
                .addQueryParameter("consent_challenge", consentChallenge)
                .build();

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder().url(url).put(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to accept consent: " + response.code());
            }
            JsonNode result = objectMapper.readTree(response.body().string());
            return normalizeUrl(result.get("redirect_to").asText());
        }
    }

    private String performLoginConsentDance(String startUrl) throws IOException {
        Request request = new Request.Builder().url(startUrl).get().build();

        try (Response response = client.newCall(request).execute()) {
            String loginUiUrl;

            if ((response.code() == 302 || response.code() == 303) && response.header("Location") != null) {
                String location = normalizeUrl(response.header("Location"));

                // Check for errors in redirect
                if (location.contains("error=")) {
                    String error = getQueryParam(location, "error");
                    if ("invalid_state".equals(error) || "invalid_scope".equals(error)) {
                        throw new IllegalArgumentException("OAuth error: " + error);
                    }
                    if ("invalid_client".equals(error)) {
                        throw new RuntimeException("OAuth error: " + error);
                    }
                    throw new RuntimeException("Unknown OAuth error: " + error);
                }
                loginUiUrl = location;
            } else {
                loginUiUrl = startUrl;
            }

            String loginChallenge = getQueryParam(loginUiUrl, "login_challenge");
            String consentRedirectUrl = acceptLoginRequest(loginChallenge);

            Request consentRequest = new Request.Builder().url(consentRedirectUrl).get().build();
            try (Response consentResponse = client.newCall(consentRequest).execute()) {
                if (consentResponse.header("Location") == null) {
                    throw new IOException("Expected redirect to Consent UI, got: " + consentResponse.code());
                }

                String consentUiUrl = normalizeUrl(consentResponse.header("Location"));
                String consentChallenge = getQueryParam(consentUiUrl, "consent_challenge");

                return acceptConsentRequest(consentChallenge);
            }
        }
    }

    /**
     * Completes the Device Code flow.
     * Consumer callback for DeviceCodeConfig.automation_callback
     */
    public void completeDeviceFlow(String verificationUriComplete) {
        try {
            // Normalize the input URL to use localhost if running on host
            String normalizedUri = normalizeUrl(verificationUriComplete);
            String userCode = getQueryParam(normalizedUri, "user_code");

            Request request = new Request.Builder().url(normalizedUri).get().build();

            try (Response response = client.newCall(request).execute()) {
                if (response.header("Location") == null) {
                    throw new IOException("Expected redirect to Device UI");
                }

                String deviceUiUrlFull = normalizeUrl(response.header("Location"));
                String deviceChallenge = getQueryParam(deviceUiUrlFull, "device_challenge");

                String verifierRedirectUrl = acceptDeviceRequest(deviceChallenge, userCode);

                // Perform Login and Consent dance
                String urlWithVerifier = performLoginConsentDance(verifierRedirectUrl);

                // Finalize - the Python code uses allow_redirects=True, but we can't follow
                // redirects to Docker internal hostnames. Just make the request without following.
                Request finalRequest = new Request.Builder().url(urlWithVerifier).get().build();
                client.newCall(finalRequest).execute().close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to complete device flow", e);
        }
    }

    /**
     * Completes the Authorization Code flow and returns the full callback URL.
     * Function callback for AuthorizationCodeConfig.automation_callback
     */
    public String completeAuthFlow(String authorizationUrl) {
        try {
            // Normalize the input URL to use localhost if running on host
            String normalizedAuthUrl = normalizeUrl(authorizationUrl);
            String urlWithVerifier = performLoginConsentDance(normalizedAuthUrl);

            Request request = new Request.Builder().url(urlWithVerifier).get().build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 303) {
                    throw new IOException("Expected 303 redirect, got: " + response.code());
                }

                String location = response.header("Location");
                if (location == null) {
                    throw new IOException("Hydra did not return Location header");
                }

                return normalizeUrl(location);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to complete auth flow", e);
        }
    }
}
