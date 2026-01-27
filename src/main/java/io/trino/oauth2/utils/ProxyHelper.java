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

import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

/**
 * Utility class for configuring HTTP proxies.
 */
public final class ProxyHelper {
    private ProxyHelper() {
        // Utility class
    }

    /**
     * Configures an OkHttpClient.Builder with the given proxy URL.
     *
     * @param builder the OkHttpClient builder to configure
     * @param proxyUrl the proxy URL (e.g., "http://proxy.example.com:8080")
     * @return the configured builder
     */
    public static OkHttpClient.Builder configureProxy(OkHttpClient.Builder builder, String proxyUrl) {
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return builder;
        }

        try {
            URI proxyUri = URI.create(proxyUrl);
            String host = proxyUri.getHost();
            int port = proxyUri.getPort();

            if (host == null || port == -1) {
                throw new IllegalArgumentException("Invalid proxy URL: " + proxyUrl);
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            return builder.proxy(proxy);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to configure proxy: " + proxyUrl, e);
        }
    }
}
