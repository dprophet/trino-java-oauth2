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

package io.trino.oauth2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for OIDC discovery and endpoint resolution.
 * Uses caching to avoid multiple network calls for the same OIDC discovery URL.
 */
public final class UrlHelpers {
    private static final Map<String, JsonNode> oidcDocumentCache = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient httpClient = new OkHttpClient();

    private UrlHelpers() {
        // Utility class
    }

    private static JsonNode getOidcDocument(String oidcDiscoveryUrl) throws IOException {
        return oidcDocumentCache.computeIfAbsent(oidcDiscoveryUrl, url -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    String responseBody = response.body().string();
                    return objectMapper.readTree(responseBody);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch OIDC discovery document from " + url, e);
            }
        });
    }

    public static String getTokenEndpointFromOidc(String oidcDiscoveryUrl) throws IOException {
        JsonNode doc = getOidcDocument(oidcDiscoveryUrl);
        JsonNode tokenEndpoint = doc.get("token_endpoint");
        if (tokenEndpoint == null) {
            throw new IllegalStateException("token_endpoint not found in OIDC discovery document");
        }
        return tokenEndpoint.asText();
    }

    public static String getDeviceAuthorizationEndpointFromOidc(String oidcDiscoveryUrl) throws IOException {
        JsonNode doc = getOidcDocument(oidcDiscoveryUrl);
        JsonNode deviceAuthEndpoint = doc.get("device_authorization_endpoint");
        if (deviceAuthEndpoint == null) {
            throw new IllegalStateException("device_authorization_endpoint not found in OIDC discovery document");
        }
        return deviceAuthEndpoint.asText();
    }

    public static String getAuthorizationEndpointFromOidc(String oidcDiscoveryUrl) throws IOException {
        JsonNode doc = getOidcDocument(oidcDiscoveryUrl);
        JsonNode authEndpoint = doc.get("authorization_endpoint");
        if (authEndpoint == null) {
            throw new IllegalStateException("authorization_endpoint not found in OIDC discovery document");
        }
        return authEndpoint.asText();
    }

    public static String getJwksFromOidc(String oidcDiscoveryUrl) throws IOException {
        JsonNode doc = getOidcDocument(oidcDiscoveryUrl);
        JsonNode jwksUri = doc.get("jwks_uri");
        if (jwksUri == null) {
            throw new IllegalStateException("jwks_uri not found in OIDC discovery document");
        }
        return jwksUri.asText();
    }
}
