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

import io.trino.oauth2.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Example demonstrating how to use the Trino OAuth2 library.
 *
 * This example shows three OAuth2 flows:
 * 1. Client Credentials Flow - for machine-to-machine communication
 * 2. Device Code Flow - for devices with limited input capabilities
 * 3. Authorization Code Flow - for standard user authentication
 */
public class Example {
    private static final Logger logger = LoggerFactory.getLogger(Example.class);

    // Hydra Configuration (matching test setup)
    private static final String HYDRA_PUBLIC_URL = "http://localhost:4444";
    private static final String HYDRA_OIDC_ENDPOINT = HYDRA_PUBLIC_URL + "/.well-known/openid-configuration";

    // Client Definitions
    private static final String CLIENT_CREDENTIALS_CLIENT_ID = "client-credentials-client";
    private static final String CLIENT_CREDENTIALS_CLIENT_SECRET = "client-credentials-secret";
    private static final String CLIENT_CREDENTIALS_SCOPE = "read write product1";

    private static final String DEVICE_CODE_CLIENT_ID = "device-code-client";
    private static final String DEVICE_CODE_CLIENT_SECRET = "device-code-secret";
    private static final String DEVICE_CODE_SCOPE = "read write offline";

    private static final String AUTH_CODE_CLIENT_ID = "my-auth-code-client";
    private static final String AUTH_CODE_CLIENT_SECRET = "my-secret";
    private static final String AUTH_CODE_REDIRECT_URI = "http://localhost:5555/callback";
    private static final String AUTH_CODE_SCOPE = "openid offline";

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "client_credentials";

        System.out.println("Usage: java -jar trino-oauth2.jar [client_credentials|device_code|auth_code]");
        System.out.println("Defaulting to '" + mode + "' for this run.\n");

        try {
            switch (mode) {
                case "client_credentials":
                    exampleClientCredentials();
                    break;
                case "device_code":
                    exampleDeviceCode();
                    break;
                case "auth_code":
                    exampleAuthorizationCodeFlow();
                    break;
                default:
                    System.out.println("Unknown mode: " + mode);
            }
        } catch (Exception e) {
            logger.error("Error running example", e);
        }
    }

    private static void exampleClientCredentials() {
        System.out.println("\n--- Client Credentials Flow ---");

        try {
            OAuth2Client oauthClient = new OAuth2Client(
                    ClientCredentialsConfig.builder()
                            .clientId(CLIENT_CREDENTIALS_CLIENT_ID)
                            .clientSecret(CLIENT_CREDENTIALS_CLIENT_SECRET)
                            .scope(CLIENT_CREDENTIALS_SCOPE)
                            .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                            .build()
            );

            String token = oauthClient.token();
            System.out.println("Successfully obtained access token: " + token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            System.out.println("Failed to obtain token: " + e.getMessage());
            logger.error("Error in client credentials flow", e);
        }
    }

    private static void exampleDeviceCode() {
        System.out.println("\n--- Device Code Flow ---");
        System.out.println("This flow requires user interaction. A URL will be printed.");

        try {
            OAuth2Client oauthClient = new OAuth2Client(
                    DeviceCodeConfig.builder()
                            .clientId(DEVICE_CODE_CLIENT_ID)
                            .clientSecret(DEVICE_CODE_CLIENT_SECRET)
                            .scope(DEVICE_CODE_SCOPE)
                            .urlConfig(new OidcConfig(HYDRA_OIDC_ENDPOINT))
                            .pollForToken(true)
                            .build()
            );

            // Without an automation callback, this will print the verification URL
            // and wait for the user to complete the login in the browser.
            String token = oauthClient.token();
            System.out.println("Successfully obtained access token: " + token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            System.out.println("Failed to obtain token: " + e.getMessage());
            logger.error("Error in device code flow", e);
        }
    }

    private static void exampleAuthorizationCodeFlow() {
        System.out.println("\n--- Authorization Code Flow ---");

        try {
            // Manual input callback for demonstration
            OAuth2Client client = new OAuth2Client(
                    AuthorizationCodeConfig.builder()
                            .clientId(AUTH_CODE_CLIENT_ID)
                            .clientSecret(AUTH_CODE_CLIENT_SECRET)
                            .scope(AUTH_CODE_SCOPE)
                            .redirectUri(AUTH_CODE_REDIRECT_URI)
                            .urlConfig(new OidcConfig("https://hydra.example.com/.well-known/openid-configuration"))
                            .automationCallback(authUrl -> {
                                System.out.println("Please visit this URL to authenticate: " + authUrl);
                                System.out.print("Paste the full redirect URL here: ");
                                try {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                                    return reader.readLine();
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to read redirect URL", e);
                                }
                            })
                            .build()
            );

            String token = client.token();
            System.out.println("Successfully obtained access token: " + token.substring(0, Math.min(10, token.length())) + "...");
        } catch (Exception e) {
            System.out.println("Failed to obtain token: " + e.getMessage());
            logger.error("Error in authorization code flow", e);
        }
    }
}
