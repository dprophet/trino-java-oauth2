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

package io.trino.oauth2.configs;

public enum OAuthFlow {
    CLIENT_CREDENTIALS("client_credentials"),
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code"),
    AUTH_CODE_PKCE("authorization_code");

    private final String grantType;

    OAuthFlow(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantType() {
        return grantType;
    }
}
