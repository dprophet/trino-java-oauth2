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
 * Configuration for OpenID Connect (OIDC) discovery.
 *
 * This configuration method allows the client to automatically discover
 * endpoints (like token, authorization, and device endpoints) by querying
 * a standard metadata URL.
 */
public class OidcConfig implements UrlConfig {
    /**
     * The URL of the OIDC discovery document (usually ends in
     * /.well-known/openid-configuration).
     *
     * Example: https://auth.example.com/.well-known/openid-configuration
     */
    private final String oidcDiscoveryUrl;

    public OidcConfig(String oidcDiscoveryUrl) {
        if (oidcDiscoveryUrl == null || oidcDiscoveryUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("oidcDiscoveryUrl cannot be null or empty");
        }
        this.oidcDiscoveryUrl = oidcDiscoveryUrl;
    }

    public String getOidcDiscoveryUrl() {
        return oidcDiscoveryUrl;
    }
}
