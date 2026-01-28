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
        .scope("read write")
        .urlConfig(new OidcConfig("https://auth.example.com/.well-known/openid-configuration"))
        .build()
);

// Fetch a token
String token = oauthClient.token();
System.out.println("Access Token: " + token);
```

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

