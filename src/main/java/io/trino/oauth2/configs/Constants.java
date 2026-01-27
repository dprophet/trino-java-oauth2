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

public final class Constants {
    private Constants() {
        // Utility class
    }

    /**
     * The access token minimum expiration duration (seconds)
     */
    public static final int VALID_MIN_DURATION_THRESHOLD = 30;

    /**
     * Request timeout limit (seconds)
     */
    public static final int REQUEST_TIMEOUT = 60;

    /**
     * LOCALHOST_REDIRECT_URI must not be changed as credential configs depend on it
     */
    public static final String LOCALHOST_REDIRECT_URI = "http://localhost:61234/auth/token.callback";
}
