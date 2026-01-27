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

package io.trino.oauth2.models;

import java.util.List;
import java.util.function.Function;

/**
 * Configuration for the OAuth 2.0 Authorization Code Grant flow.
 *
 * This class holds all necessary parameters to initiate and complete
 * the standard authorization code flow, including client identifiers,
 * endpoints, and security settings.
 */
public class AuthorizationCodeConfig implements OAuth2Config {
    private final String clientId;
    private final UrlConfig urlConfig;
    private final String redirectUri;
    private final String clientSecret;
    private final String scope;
    private final List<String> audience;
    private final String state;
    private final boolean usePkce;
    private final Function<String, String> automationCallback;

    private AuthorizationCodeConfig(Builder builder) {
        if (builder.clientId == null || builder.clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (builder.urlConfig == null) {
            throw new IllegalArgumentException("urlConfig is required");
        }
        if (builder.redirectUri == null || builder.redirectUri.trim().isEmpty()) {
            throw new IllegalArgumentException("redirectUri is required");
        }

        this.clientId = builder.clientId;
        this.urlConfig = builder.urlConfig;
        this.redirectUri = builder.redirectUri;
        this.clientSecret = builder.clientSecret;
        this.scope = builder.scope;
        this.audience = builder.audience;
        this.state = builder.state;
        this.usePkce = builder.usePkce;
        this.automationCallback = builder.automationCallback;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public UrlConfig getUrlConfig() {
        return urlConfig;
    }

    /**
     * The URI where the Authorization Server will redirect the user-agent
     * after granting access. This must exactly match one of the redirect URIs
     * registered with the OAuth provider.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * The secret known only to the application and the authorization server.
     * Required for confidential clients (web apps) but often omitted for
     * public clients (native/mobile apps) or when using PKCE without a secret.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * A space-delimited string defining the permissions the application is
     * requesting. Examples: "openid profile email offline_access".
     */
    public String getScope() {
        return scope;
    }

    /**
     * A list of audience identifiers (resource indicators)
     * for which the token is intended.
     */
    public List<String> getAudience() {
        return audience;
    }

    /**
     * An opaque value used to maintain state between the request and the callback.
     *
     * Security: It is critical for preventing Cross-Site Request Forgery (CSRF).
     * If null, the library will automatically generate a secure random string.
     */
    public String getState() {
        return state;
    }

    /**
     * Enables Proof Key for Code Exchange (PKCE).
     *
     * Default: true.
     * PKCE is highly recommended for all clients (public and confidential) to
     * prevent authorization code injection attacks.
     */
    public boolean isUsePkce() {
        return usePkce;
    }

    /**
     * A callback function used for automated testing or headless environments.
     *
     * Input: The constructed authorization URL (String).
     * Output: The full redirect URL (String) containing the code and state.
     * Behavior: If provided, the library calls this function instead of
     * opening a browser. The function is expected to programmatically visit
     * the auth URL, handle login/consent, and return the final URL the
     * browser was redirected to.
     */
    public Function<String, String> getAutomationCallback() {
        return automationCallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientId;
        private UrlConfig urlConfig;
        private String redirectUri;
        private String clientSecret;
        private String scope;
        private List<String> audience;
        private String state;
        private boolean usePkce = true;
        private Function<String, String> automationCallback;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder urlConfig(UrlConfig urlConfig) {
            this.urlConfig = urlConfig;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder audience(List<String> audience) {
            this.audience = audience;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder usePkce(boolean usePkce) {
            this.usePkce = usePkce;
            return this;
        }

        public Builder automationCallback(Function<String, String> automationCallback) {
            this.automationCallback = automationCallback;
            return this;
        }

        public AuthorizationCodeConfig build() {
            return new AuthorizationCodeConfig(this);
        }
    }
}
