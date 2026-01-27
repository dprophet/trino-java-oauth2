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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configures Hydra with test OAuth2 clients.
 * Java port of tests/configure_hydra.py
 *
 * Usage: java -cp target/test-classes:target/classes io.trino.oauth2.e2e.ConfigureHydra
 */
public class ConfigureHydra {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String HYDRA_ADMIN_URL = System.getenv().getOrDefault("HYDRA_ADMIN_URL", "http://localhost:4445");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        System.out.println("Configuring Hydra OAuth2 test clients...");

        if (!waitForHydra(30, 2)) {
            System.err.println("ERROR: Hydra is not ready. Exiting.");
            System.exit(1);
        }

        try {
            createClientCredentialsClient();
            createClientCredentialsWithAudience();
            createDeviceCodeClientWithSecrets();
            createDeviceCodeClientNoSecretsWithScopes();
            createDeviceCodeClientNoSecretsWithScopesAudience();
            createAuthorizationCodeClient();
            createAuthorizationCodeClientWithScopes();

            System.out.println("\n✅ All Hydra test clients configured successfully!");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to configure Hydra clients: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean waitForHydra(int maxRetries, int delaySec) {
        System.out.println("Waiting for Hydra at " + HYDRA_ADMIN_URL + "/health/ready to be ready...");

        for (int i = 0; i < maxRetries; i++) {
            try {
                Request request = new Request.Builder()
                        .url(HYDRA_ADMIN_URL + "/health/ready")
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("Hydra is ready at " + HYDRA_ADMIN_URL + "!");
                        return true;
                    }
                }
            } catch (IOException e) {
                // Expected when Hydra is not ready yet
            }

            System.out.println("Attempt " + (i + 1) + "/" + maxRetries + ": Hydra not ready yet, waiting " + delaySec + "s...");
            try {
                Thread.sleep(delaySec * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        System.err.println("ERROR: Hydra did not become ready after " + (maxRetries * delaySec) + " seconds");
        return false;
    }

    private static void createOAuthClient(ObjectNode clientData) throws IOException {
        String clientId = clientData.get("client_id").asText();
        String url = HYDRA_ADMIN_URL + "/clients";

        // Check if client exists and delete it
        Request listRequest = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response listResponse = client.newCall(listRequest).execute()) {
            if (!listResponse.isSuccessful()) {
                throw new IOException("Failed to list clients: " + listResponse.code());
            }

            JsonNode clients = objectMapper.readTree(listResponse.body().string());
            if (clients.isArray()) {
                for (JsonNode existingClient : clients) {
                    if (clientId.equals(existingClient.get("client_id").asText())) {
                        System.out.println("Client '" + clientId + "' already exists. Deleting it.");
                        String deleteUrl = url + "/" + clientId;
                        Request deleteRequest = new Request.Builder()
                                .url(deleteUrl)
                                .delete()
                                .build();

                        try (Response deleteResponse = ConfigureHydra.client.newCall(deleteRequest).execute()) {
                            if (!deleteResponse.isSuccessful()) {
                                throw new IOException("Failed to delete client '" + clientId + "': " + deleteResponse.code());
                            }
                        }
                        break;
                    }
                }
            }
        }

        // Create client
        RequestBody body = RequestBody.create(clientData.toString(), JSON);
        Request createRequest = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response createResponse = client.newCall(createRequest).execute()) {
            if (!createResponse.isSuccessful()) {
                String errorBody = createResponse.body() != null ? createResponse.body().string() : "";
                throw new IOException("Failed to create client '" + clientId + "': " + createResponse.code() + " - " + errorBody);
            }
            System.out.println("Client '" + clientId + "' created successfully.");
        }
    }

    private static ObjectNode createClientData(
            String clientId,
            String clientSecret,
            List<String> grantTypes,
            List<String> responseTypes,
            String tokenEndpointAuthMethod) {
        ObjectNode client = objectMapper.createObjectNode();
        client.put("client_id", clientId);
        if (clientSecret != null) {
            client.put("client_secret", clientSecret);
        }

        ArrayNode grantTypesArray = objectMapper.createArrayNode();
        grantTypes.forEach(grantTypesArray::add);
        client.set("grant_types", grantTypesArray);

        ArrayNode responseTypesArray = objectMapper.createArrayNode();
        responseTypes.forEach(responseTypesArray::add);
        client.set("response_types", responseTypesArray);

        client.put("token_endpoint_auth_method", tokenEndpointAuthMethod);

        return client;
    }

    private static void createClientCredentialsClient() throws IOException {
        ObjectNode client = createClientData(
                "client-credentials-client",
                "client-credentials-secret",
                Arrays.asList("client_credentials"),
                Arrays.asList("token"),
                "client_secret_post"
        );
        client.put("scope", "read write product1");
        createOAuthClient(client);
    }

    private static void createClientCredentialsWithAudience() throws IOException {
        ObjectNode client = createClientData(
                "client-credentials-audience",
                "client-credentials-audience-secret",
                Arrays.asList("client_credentials"),
                Arrays.asList("token"),
                "client_secret_post"
        );

        ArrayNode audience = objectMapper.createArrayNode();
        audience.add("aud1");
        audience.add("aud2");
        client.set("audience", audience);

        createOAuthClient(client);
    }

    private static void createDeviceCodeClientWithSecrets() throws IOException {
        ObjectNode client = createClientData(
                "device-code-client",
                "device-code-secret",
                Arrays.asList("urn:ietf:params:oauth:grant-type:device_code", "refresh_token"),
                Arrays.asList("token"),
                "client_secret_post"
        );
        client.put("scope", "read write offline");
        createOAuthClient(client);
    }

    private static void createDeviceCodeClientNoSecretsWithScopes() throws IOException {
        ObjectNode client = createClientData(
                "device-code-client-no-secrets-w-scopes",
                null,
                Arrays.asList("urn:ietf:params:oauth:grant-type:device_code", "refresh_token"),
                Arrays.asList("token"),
                "none"
        );
        client.put("scope", "product1 offline");
        createOAuthClient(client);
    }

    private static void createDeviceCodeClientNoSecretsWithScopesAudience() throws IOException {
        ObjectNode client = createClientData(
                "device-code-client-no-secrets-w-scopes-audience",
                null,
                Arrays.asList("urn:ietf:params:oauth:grant-type:device_code", "refresh_token"),
                Arrays.asList("token"),
                "none"
        );
        client.put("scope", "offline product3 product4");

        ArrayNode audience = objectMapper.createArrayNode();
        audience.add("aud3");
        client.set("audience", audience);

        createOAuthClient(client);
    }

    private static void createAuthorizationCodeClient() throws IOException {
        ObjectNode client = createClientData(
                "auth-code-client-no-scopes",
                "auth-code-secret-no-scopes",
                Arrays.asList("authorization_code", "refresh_token"),
                Arrays.asList("code"),
                "client_secret_post"
        );

        ArrayNode redirectUris = objectMapper.createArrayNode();
        redirectUris.add("http://localhost:61234/auth/token.callback");
        client.set("redirect_uris", redirectUris);

        createOAuthClient(client);
    }

    private static void createAuthorizationCodeClientWithScopes() throws IOException {
        ObjectNode client = createClientData(
                "auth-code-client-with-scopes",
                null,
                Arrays.asList("authorization_code", "refresh_token"),
                Arrays.asList("code"),
                "none"
        );
        client.put("scope", "openid offline");

        ArrayNode redirectUris = objectMapper.createArrayNode();
        redirectUris.add("http://localhost:61234/auth/token.callback");
        client.set("redirect_uris", redirectUris);

        createOAuthClient(client);
    }
}
