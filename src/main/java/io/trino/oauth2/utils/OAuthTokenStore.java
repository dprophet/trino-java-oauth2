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

package io.trino.oauth2.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.trino.oauth2.configs.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.prefs.Preferences;

/**
 * Functions for storing OAuth tokens in local cache.
 * Uses Java Preferences API for cross-platform persistent storage.
 */
public final class OAuthTokenStore {
    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenStore.class);
    private static final String SERVICE_NAME = "trino-java-client";
    private static final String ACCESS_TOKEN_SUFFIX = "access_token";
    private static final String REFRESH_TOKEN_SUFFIX = "refresh_token";

    private OAuthTokenStore() {
        // Utility class
    }

    private static void validateMode(String modeName) {
        if (!"ClientCredentialsConfig".equals(modeName) &&
                !"DeviceCodeConfig".equals(modeName) &&
                !"AuthorizationCodeConfig".equals(modeName)) {
            throw new IllegalArgumentException(
                    "Invalid mode '" + modeName + "'. Allowed modes are: " +
                            "ClientCredentialsConfig, DeviceCodeConfig, AuthorizationCodeConfig"
            );
        }
    }

    private static String getKeyringUsername(String clientId, String mode, String tokenType) {
        validateMode(mode);
        return clientId + ":" + mode + ":" + tokenType;
    }

    private static boolean hasActiveAccessToken(String accessToken, int validMinDurationThreshold) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }

        try {
            DecodedJWT jwt = JWT.decode(accessToken);
            Instant expiresAt = jwt.getExpiresAt().toInstant();
            long secondsUntilExpiration = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
            return secondsUntilExpiration >= validMinDurationThreshold;
        } catch (Exception e) {
            logger.debug("Failed to decode JWT token", e);
            return false;
        }
    }

    public static String getActiveAccessToken(
            String clientId,
            String mode,
            int validMinDurationThreshold
    ) {
        String username = getKeyringUsername(clientId, mode, ACCESS_TOKEN_SUFFIX);
        Preferences prefs = Preferences.userRoot().node(SERVICE_NAME);
        String accessToken = prefs.get(username, null);

        return hasActiveAccessToken(accessToken, validMinDurationThreshold) ? accessToken : null;
    }

    public static String getActiveAccessToken(String clientId, String mode) {
        return getActiveAccessToken(clientId, mode, Constants.VALID_MIN_DURATION_THRESHOLD);
    }

    public static void setAccessToken(String clientId, String mode, String accessToken) {
        String username = getKeyringUsername(clientId, mode, ACCESS_TOKEN_SUFFIX);
        Preferences prefs = Preferences.userRoot().node(SERVICE_NAME);
        prefs.put(username, accessToken);
    }

    public static String getRefreshToken(String clientId, String mode) {
        String username = getKeyringUsername(clientId, mode, REFRESH_TOKEN_SUFFIX);
        Preferences prefs = Preferences.userRoot().node(SERVICE_NAME);
        return prefs.get(username, null);
    }

    public static void setAccessAndRefreshTokens(
            String clientId,
            String mode,
            String accessToken,
            String refreshToken
    ) {
        setAccessToken(clientId, mode, accessToken);
        String refreshUsername = getKeyringUsername(clientId, mode, REFRESH_TOKEN_SUFFIX);
        Preferences prefs = Preferences.userRoot().node(SERVICE_NAME);
        prefs.put(refreshUsername, refreshToken);
    }

    /**
     * Purges stored access and refresh tokens from the keyring.
     *
     * @param clientId The client ID for which to purge tokens.
     * @param mode     Specifies which token(s) to purge. If null, purges tokens for all modes.
     */
    public static void purgeTokens(String clientId, String mode) {
        String[] modesToPurge;
        if (mode != null) {
            modesToPurge = new String[]{mode};
        } else {
            modesToPurge = new String[]{"ClientCredentialsConfig", "DeviceCodeConfig", "AuthorizationCodeConfig"};
        }

        Preferences prefs = Preferences.userRoot().node(SERVICE_NAME);
        for (String modeName : modesToPurge) {
            String accessUsername = getKeyringUsername(clientId, modeName, ACCESS_TOKEN_SUFFIX);
            String refreshUsername = getKeyringUsername(clientId, modeName, REFRESH_TOKEN_SUFFIX);

            prefs.remove(accessUsername);
            prefs.remove(refreshUsername);
        }
    }
}
