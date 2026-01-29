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
import java.util.function.Consumer;

/**
 * Configuration for the OAuth 2.0 Device Code Grant flow.
 *
 * This flow is designed for internet-connected devices that either lack a
 * browser or have limited input capability (e.g., smart TVs, CLI tools).
 */
public class DeviceCodeConfig implements OAuth2Config {
    private final String clientId;
    private final UrlConfig urlConfig;
    private final String clientSecret;
    private final String scope;
    private final List<String> audience;
    private final boolean pollForToken;
    private final Consumer<String> automationCallback;

    private DeviceCodeConfig(Builder builder) {
        if (builder.clientId == null || builder.clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (builder.urlConfig == null) {
            throw new IllegalArgumentException("urlConfig is required");
        }

        this.clientId = builder.clientId;
        this.urlConfig = builder.urlConfig;
        this.clientSecret = builder.clientSecret;
        this.scope = builder.scope;
        this.audience = builder.audience;
        this.pollForToken = builder.pollForToken;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public List<String> getAudience() {
        return audience;
    }

    /**
     * If true, the client will automatically poll the token endpoint
     * while waiting for the user to complete the login in a separate browser.
     *
     * If false, the client may wait for a user signal (like pressing Enter)
     * before attempting to fetch the token.
     */
    public boolean isPollForToken() {
        return pollForToken;
    }

    /**
     * A callback function used for automated testing.
     *
     * Input: The verification_uri_complete (URL) that the user needs to visit.
     * Output: None.
     * Behavior: In Device Code flow, the automation is expected to visit the URL
     * and approve the request. Unlike Auth Code flow, no return value (callback URL)
     * is needed because the device client polls the server directly for the token.
     */
    public Consumer<String> getAutomationCallback() {
        return automationCallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientId;
        private UrlConfig urlConfig;
        private String clientSecret;
        private String scope;
        private List<String> audience;
        private boolean pollForToken = true;
        private Consumer<String> automationCallback;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder urlConfig(UrlConfig urlConfig) {
            this.urlConfig = urlConfig;
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

        public Builder pollForToken(boolean pollForToken) {
            this.pollForToken = pollForToken;
            return this;
        }

        /**
         * Sets an automation callback for testing purposes only.
         * This method is restricted to test environments and will throw an exception
         * if called outside of test mode.
         *
         * @param automationCallback the callback to invoke during automated testing
         * @return this builder
         * @throws IllegalStateException if not running in test mode
         */
        public Builder automationCallback(Consumer<String> automationCallback) {
            if (!Boolean.getBoolean("io.trino.oauth2.test.automation")) {
                throw new IllegalStateException(
                    "automationCallback is only available in test mode. " +
                    "This method is intended for automated testing only. " +
                    "Set system property: -Dio.trino.oauth2.test.automation=true"
                );
            }
            this.automationCallback = automationCallback;
            return this;
        }

        public DeviceCodeConfig build() {
            return new DeviceCodeConfig(this);
        }
    }
}
