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

import io.trino.oauth2.utils.OAuthTokenStore;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static io.trino.oauth2.e2e.HydraTestConfig.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base class for Hydra E2E integration tests.
 */
@Tag("e2e")
@Tag("hydra")
public abstract class HydraE2ETestBase {

    protected HydraAutoConsent hydraAutomator;

    @BeforeEach
    void checkHydraRunning() {
        boolean hydraRunning = isHydraRunning();
        assumeTrue(hydraRunning, "Hydra is not running at " + HYDRA_ADMIN_URL);

        hydraAutomator = new HydraAutoConsent(HYDRA_ADMIN_URL);
    }

    private boolean isHydraRunning() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(HYDRA_ADMIN_URL + "/health/ready")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    protected void purgeTokens(String clientId, String mode) {
        OAuthTokenStore.purgeTokens(clientId, mode);
    }
}
