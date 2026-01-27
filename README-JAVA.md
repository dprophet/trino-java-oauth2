# Trino OAuth2 Java Library

The library supports OAuth 2.0 authentication flows including [Client Credentials](https://datatracker.ietf.org/doc/html/rfc6749#section-1.3.4), [Device Authorization Grant](https://datatracker.ietf.org/doc/html/rfc8628), and Authorization Code flows. This package is designed to make interaction with OAuth 2.0 flows as simple as possible.

## Features

- **Client Credentials Flow**: For machine-to-machine communication.
- **Device Code Flow**: For devices with limited input capabilities.
- **Authorization Code Flow**: For standard user authentication.
- **Secure Token Storage**: Integration with Java Preferences API for persistent token storage.
- **OIDC Discovery**: Automatic configuration using OpenID Connect discovery URLs.

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Installation

### Maven

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.trino</groupId>
    <artifactId>trino-oauth2</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Building from Source

```bash
# Using the included artifactory settings
mvn -s artifactory_settings.xml clean install

# Or with standard Maven
mvn clean install
```

## Quick Start

Check out `io.trino.oauth2.Example` in the repository for complete, runnable examples of all supported flows.

### Basic Usage (Client Credentials)

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.ClientCredentialsConfig;
import io.trino.oauth2.models.OidcConfig;

// Configure the client
OAuth2Client oauthClient = new OAuth2Client(
    ClientCredentialsConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .build()
);

// Fetch a token
String token = oauthClient.token();
System.out.println("Access Token: " + token);
```

### Device Code Flow

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.OidcConfig;

OAuth2Client oauthClient = new OAuth2Client(
    DeviceCodeConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .scope("read write")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .pollForToken(true)
        .build()
);

String token = oauthClient.token();
```

### Authorization Code Flow

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.AuthorizationCodeConfig;
import io.trino.oauth2.models.OidcConfig;

OAuth2Client oauthClient = new OAuth2Client(
    AuthorizationCodeConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .redirectUri("http://localhost:8080/callback")
        .scope("openid profile email")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .build()
);

String token = oauthClient.token();
```

## Configuration

The `OAuth2Client` can be configured with different flow configurations:

- `ClientCredentialsConfig` - For service-to-service authentication
- `DeviceCodeConfig` - For devices with limited input capabilities
- `AuthorizationCodeConfig` - For web/mobile applications with user authentication

It also supports manual URL configuration via `ManualUrlsConfig` if OIDC discovery is not available.

### Manual URL Configuration

```java
import io.trino.oauth2.models.ManualUrlsConfig;

ManualUrlsConfig urlConfig = ManualUrlsConfig.builder()
    .tokenEndpoint("https://auth.example.com/oauth/token")
    .authorizationEndpoint("https://auth.example.com/oauth/authorize")
    .deviceAuthorizationEndpoint("https://auth.example.com/oauth/device/code")
    .build();
```

### Secure Token Storage

The library uses Java's Preferences API for secure, persistent token storage. Tokens are automatically cached and reused until they expire.

To clear stored tokens:

```java
import io.trino.oauth2.utils.OAuthTokenStore;

// Purge all tokens for a client
OAuthTokenStore.purgeTokens("your-client-id", null);

// Purge tokens for a specific flow
OAuthTokenStore.purgeTokens("your-client-id", "ClientCredentialsConfig");
```

## Development

### Building

**Using Makefile (recommended):**
```bash
# Clean and build (default target)
make build

# Or just compile
make compile

# Clean build artifacts
make clean

# Install to local Maven repository
make install

# Display all available make targets
make help
```

**Using Maven directly:**
```bash
# Using the included artifactory settings (for internal builds)
mvn -s artifactory_settings.xml clean compile

# Or with standard Maven
mvn clean compile
```

### Running Tests

**Using Makefile (recommended):**
```bash
# Run only unit tests
make test-unit

# Run all tests (unit + E2E if Hydra running)
make test

# Run a single test (for troubleshooting)
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual

# Full E2E test flow (starts Hydra, configures it, runs E2E tests)
make e2e
```

**Note:** E2E tests require a `.env.local` file to run from the host. Copy the template:
```bash
cp .env.local.template .env.local
```

**E2E Integration Tests (step by step):**
```bash
# 1. Start Hydra and configure test clients
make restart-hydra

# 2. Run E2E tests
make test-e2e

# View Hydra logs
make hydra-logs

# Or follow Hydra logs in real-time
make hydra-logs -f
```

**Using Maven directly:**
```bash
# Unit tests only
mvn -s artifactory_settings.xml test -Dtest=OAuth2ClientTest,DeviceCodeConfigTest,ClientCredentialsConfigTest,AuthorizationCodeConfigTest,OAuthTokenStoreTest

# All tests
mvn -s artifactory_settings.xml test

# E2E tests only
mvn -s artifactory_settings.xml test -Dgroups=e2e
```

The `make restart-hydra` target automatically runs `configure-hydra.sh`, which is the Java equivalent of `python tests/configure_hydra.py`. It creates all necessary OAuth2 test clients in Hydra via the Admin API.

### Running the Example

```bash
# Build the project
mvn -s artifactory_settings.xml clean package

# Run client credentials example
java -cp target/trino-oauth2-1.0.0.jar io.trino.oauth2.Example client_credentials

# Run device code example
java -cp target/trino-oauth2-1.0.0.jar io.trino.oauth2.Example device_code

# Run authorization code example
java -cp target/trino-oauth2-1.0.0.jar io.trino.oauth2.Example auth_code
```

## Architecture

The library is organized into the following packages:

- `io.trino.oauth2` - Main OAuth2Client class
- `io.trino.oauth2.models` - Configuration models (Config classes)
- `io.trino.oauth2.configs` - Constants and enums
- `io.trino.oauth2.flows` - OAuth flow implementations
- `io.trino.oauth2.utils` - Utility classes (token storage, URL helpers, proxy configuration)

## Dependencies

- OkHttp 4.12.0 - HTTP client
- Jackson 2.16.1 - JSON processing
- java-jwt 4.4.0 - JWT decoding and validation
- SLF4J 2.0.9 - Logging facade

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

## Migration from Python

This library is a Java port of the `trino.oauth2` Python library. The API has been adapted to follow Java conventions while maintaining functional parity:

- Python dataclasses → Java builder pattern
- Python properties → Java getters
- Python functions → Java methods
- keyring library → Java Preferences API

Key differences:
- Token storage uses Java Preferences API instead of Python's keyring
- Builder pattern for configuration instead of keyword arguments
- Checked exceptions (IOException) instead of Python exceptions
