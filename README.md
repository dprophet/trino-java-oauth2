# Trino OAuth2 Java Library

The library supports OAuth 2.0 authentication flows including [Client Credentials](https://datatracker.ietf.org/doc/html/rfc6749#section-1.3.4), [Device Authorization Grant](https://datatracker.ietf.org/doc/html/rfc8628), and Authorization Code flows. This package is designed to make interaction with OAuth 2.0 flows as simple as possible.

## Features

- **Client Credentials Flow**: For machine-to-machine communication.
- **Device Code Flow**: For devices with limited input capabilities.
- **Authorization Code Flow**: For standard user authentication.
- **Secure Token Storage**: Integration with Java Preferences API for token persistence.
- **OIDC Discovery**: Automatic configuration using OpenID Connect discovery URLs.

## Installation

### Maven

```xml
<dependency>
    <groupId>io.trino</groupId>
    <artifactId>trino-oauth2</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.trino:trino-oauth2:1.0.0'
```

## Quick Start

Check out `Example.java` in the repository for complete, runnable examples of all supported flows.

### Basic Usage

#### Client Credentials Flow

Best for machine-to-machine communication where no user interaction is required.

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.ClientCredentialsConfig;
import io.trino.oauth2.models.OidcConfig;

OAuth2Client oauthClient = new OAuth2Client(
    ClientCredentialsConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .scope("read write")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .build()
);

String token = oauthClient.token();
System.out.println("Access Token: " + token);
```

#### Device Code Flow

Best for devices with limited input capabilities (BI tools, smart TVs, IoT devices, CLI tools).

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.DeviceCodeConfig;
import io.trino.oauth2.models.OidcConfig;

OAuth2Client oauthClient = new OAuth2Client(
    DeviceCodeConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")  // Optional for public clients
        .scope("openid offline")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .pollForToken(true)  // Automatically poll for token
        .build()
);

// This will display a user code and verification URL
String token = oauthClient.token();
System.out.println("Access Token: " + token);
```

#### Authorization Code Flow

Best for standard web applications and native apps with user authentication.

```java
import io.trino.oauth2.OAuth2Client;
import io.trino.oauth2.models.AuthorizationCodeConfig;
import io.trino.oauth2.models.OidcConfig;

OAuth2Client oauthClient = new OAuth2Client(
    AuthorizationCodeConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")  // Optional for public clients with PKCE
        .redirectUri("http://localhost:8080/callback")
        .scope("openid profile email")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .usePkce(true)  // Recommended for security
        .build()
);

// This will open a browser for user authentication
String token = oauthClient.token();
System.out.println("Access Token: " + token);
```

### Additional Configuration Options

All three flows support additional configuration options:

#### Audience Parameter

Specify the intended audience(s) for the access token. This is useful when your OAuth provider requires or supports the `audience` parameter (RFC 8707).

```java
OAuth2Client oauthClient = new OAuth2Client(
    ClientCredentialsConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .audience(new String[]{"https://api.example.com", "https://api2.example.com"})
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .build()
);
```

The `audience` parameter works with all flows:
- `ClientCredentialsConfig.builder().audience(...)`
- `DeviceCodeConfig.builder().audience(...)`
- `AuthorizationCodeConfig.builder().audience(...)`

#### Manual URL Configuration

If your OAuth provider doesn't support OIDC discovery, you can manually specify the endpoints using `ManualUrlsConfig`:

```java
import io.trino.oauth2.models.ManualUrlsConfig;

// Client Credentials - only needs token endpoint
OAuth2Client oauthClient = new OAuth2Client(
    ClientCredentialsConfig.builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .urlConfig(ManualUrlsConfig.builder()
            .tokenEndpoint("https://auth.example.com/oauth2/token")
            .build())
        .build()
);

// Device Code - needs token and device authorization endpoints
OAuth2Client deviceClient = new OAuth2Client(
    DeviceCodeConfig.builder()
        .clientId("your-client-id")
        .urlConfig(ManualUrlsConfig.builder()
            .tokenEndpoint("https://auth.example.com/oauth2/token")
            .deviceAuthorizationEndpoint("https://auth.example.com/oauth2/device/code")
            .build())
        .pollForToken(true)
        .build()
);

// Authorization Code - needs token and authorization endpoints
OAuth2Client authClient = new OAuth2Client(
    AuthorizationCodeConfig.builder()
        .clientId("your-client-id")
        .redirectUri("http://localhost:8080/callback")
        .urlConfig(ManualUrlsConfig.builder()
            .tokenEndpoint("https://auth.example.com/oauth2/token")
            .authorizationEndpoint("https://auth.example.com/oauth2/auth")
            .build())
        .build()
);
```

**When to use ManualUrlsConfig vs OidcConfig:**
- Use `OidcConfig` when your provider supports OIDC discovery (most modern OAuth providers)
- Use `ManualUrlsConfig` when:
  - Your provider doesn't support OIDC discovery
  - You need to use custom or non-standard endpoints
  - You want to avoid the additional network request for discovery

## Configuration

The `OAuth2Client` can be configured with different flow configurations:

- `ClientCredentialsConfig`
- `DeviceCodeConfig`
- `AuthorizationCodeConfig`

It also supports manual URL configuration via `ManualUrlsConfig` if OIDC discovery is not available.

### Secure Token Storage

The library supports secure token storage using Java's Preferences API for token persistence across sessions.

Token storage is enabled by default and uses the system's secure storage mechanism. Tokens are automatically cached and reused until they expire.

## Development

### Setup

```bash
# Clone the repository
git clone https://github.com/trinodb/trino-java-oauth2.git
cd trino-java-oauth2

# Build and install the project
make install
```

### Running Tests

We use JUnit for testing. The end-to-end tests run against a Dockerized Hydra instance.

```bash
# Run all tests
make test

# Run only unit tests
make test-unit

# Run E2E tests (requires Hydra)
make test-e2e

# Start Hydra for E2E testing
make start-hydra

# Full E2E test flow (restart Hydra, configure, and run tests)
make e2e
```

