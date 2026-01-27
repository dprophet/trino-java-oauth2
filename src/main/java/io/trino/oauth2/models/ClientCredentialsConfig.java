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

/**
 * Configuration for the OAuth 2.0 Client Credentials Grant flow.
 *
 * This flow is used for machine-to-machine (M2M) authentication where
 * no user interaction is present. The application authenticates as itself.
 */
public class ClientCredentialsConfig implements OAuth2Config {
    /**
     * The public identifier for the application (client).
     */
    private final String clientId;

    /**
     * The secret credential for the application. Since Client Credentials flow
     * implies a confidential client, this is required.
     */
    private final String clientSecret;

    /**
     * Configuration for finding the necessary OAuth endpoints.
     */
    private final UrlConfig urlConfig;

    /**
     * A space-delimited string defining the permissions the application is requesting.
     */
    private final String scope;

    /**
     * A list of audience identifiers (resource indicators)
     * for which the token is intended. Used in OAuth 2.0 to specify target
     * APIs or services that will accept the token.
     */
    private final List<String> audience;

    private ClientCredentialsConfig(Builder builder) {
        if (builder.clientId == null || builder.clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (builder.clientSecret == null || builder.clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("clientSecret is required");
        }
        if (builder.urlConfig == null) {
            throw new IllegalArgumentException("urlConfig is required");
        }

        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.urlConfig = builder.urlConfig;
        this.scope = builder.scope;
        this.audience = builder.audience;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public UrlConfig getUrlConfig() {
        return urlConfig;
    }

    public String getScope() {
        return scope;
    }

    public List<String> getAudience() {
        return audience;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientId;
        private String clientSecret;
        private UrlConfig urlConfig;
        private String scope;
        private List<String> audience;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder urlConfig(UrlConfig urlConfig) {
            this.urlConfig = urlConfig;
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

        public ClientCredentialsConfig build() {
            return new ClientCredentialsConfig(this);
        }
    }
}
