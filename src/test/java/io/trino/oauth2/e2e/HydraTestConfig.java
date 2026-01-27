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

import java.util.Arrays;
import java.util.List;

/**
 * Configuration constants for Hydra E2E tests.
 * Corresponds to tests/configure_hydra.py
 */
public final class HydraTestConfig {

    private HydraTestConfig() {
        // Utility class
    }

    public static final String HYDRA_ADMIN_URL = System.getenv().getOrDefault("HYDRA_ADMIN_URL", "http://localhost:4445");
    public static final String HYDRA_PUBLIC_URL = System.getenv().getOrDefault("HYDRA_PUBLIC_URL", "http://localhost:4444");
    public static final String TOKEN_ENDPOINT = HYDRA_PUBLIC_URL + "/oauth2/token";
    public static final String DEVICE_AUTH_ENDPOINT = HYDRA_PUBLIC_URL + "/oauth2/device/auth";
    public static final String HYDRA_OIDC_ENDPOINT = HYDRA_PUBLIC_URL + "/.well-known/openid-configuration";
    public static final String HYDRA_PUBLIC_KEY = HYDRA_PUBLIC_URL + "/.well-known/jwks.json";

    // Client Credentials Client
    public static final String CC_CLIENT_ID = "client-credentials-client";
    public static final String CC_CLIENT_SECRET = "client-credentials-secret";
    public static final String CC_SCOPE = "read write product1";

    // Client Credentials with Audience
    public static final String CC_AUD_CLIENT_ID = "client-credentials-audience";
    public static final String CC_AUD_CLIENT_SECRET = "client-credentials-audience-secret";
    public static final List<String> CC_AUD_AUDIENCE = Arrays.asList("aud1", "aud2");

    // Device Code Client with Secrets
    public static final String DC_CLIENT_ID = "device-code-client";
    public static final String DC_CLIENT_SECRET = "device-code-secret";
    public static final String DC_SCOPE = "read write offline";

    // Device Code Client No Secrets with Scopes
    public static final String DC_NO_SECRET_CLIENT_ID = "device-code-client-no-secrets-w-scopes";
    public static final String DC_NO_SECRET_SCOPE = "product1 offline";

    // Device Code Client No Secrets with Scopes and Audience
    public static final String DC_NO_SECRET_AUD_CLIENT_ID = "device-code-client-no-secrets-w-scopes-audience";
    public static final String DC_NO_SECRET_AUD_SCOPE = "offline product3 product4";
    public static final List<String> DC_NO_SECRET_AUD_AUDIENCE = Arrays.asList("aud3");

    // Authorization Code Client
    public static final String AC_CLIENT_ID = "auth-code-client-no-scopes";
    public static final String AC_CLIENT_SECRET = "auth-code-secret-no-scopes";
    public static final String AC_REDIRECT_URI = "http://localhost:61234/auth/token.callback";

    // Authorization Code Client with Scopes
    public static final String AC_SCOPES_CLIENT_ID = "auth-code-client-with-scopes";
    public static final String AC_SCOPES_SCOPE = "openid offline";
    public static final String AC_SCOPES_REDIRECT_URI = "http://localhost:61234/auth/token.callback";

    // Bad Client Config
    public static final String BAD_CLIENT_ID = "bad-client-id";
    public static final String BAD_CLIENT_SECRET = "bad-client-secret";
    public static final String BAD_SCOPE = "product-bad";
}
