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

/**
 * Manual configuration for OAuth 2.0 endpoints.
 *
 * Use this configuration when OIDC discovery is not available or when
 * specific endpoints need to be overridden.
 */
public class ManualUrlsConfig implements UrlConfig {
    /**
     * The URL of the token endpoint where access tokens are requested.
     * Required for all OAuth flows.
     */
    private final String tokenEndpoint;

    /**
     * The URL of the device authorization endpoint.
     * Required only for the Device Code flow.
     */
    private final String deviceAuthorizationEndpoint;

    /**
     * The URL of the authorization endpoint where the user is directed to log in.
     * Required only for the Authorization Code flow.
     */
    private final String authorizationEndpoint;

    private ManualUrlsConfig(Builder builder) {
        if (builder.tokenEndpoint == null || builder.tokenEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("tokenEndpoint is required");
        }
        this.tokenEndpoint = builder.tokenEndpoint;
        this.deviceAuthorizationEndpoint = builder.deviceAuthorizationEndpoint;
        this.authorizationEndpoint = builder.authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getDeviceAuthorizationEndpoint() {
        return deviceAuthorizationEndpoint;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tokenEndpoint;
        private String deviceAuthorizationEndpoint;
        private String authorizationEndpoint;

        public Builder tokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
            return this;
        }

        public Builder deviceAuthorizationEndpoint(String deviceAuthorizationEndpoint) {
            this.deviceAuthorizationEndpoint = deviceAuthorizationEndpoint;
            return this;
        }

        public Builder authorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
            return this;
        }

        public ManualUrlsConfig build() {
            return new ManualUrlsConfig(this);
        }
    }
}
