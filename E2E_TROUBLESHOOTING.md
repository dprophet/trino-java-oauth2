# E2E Test Troubleshooting Guide

## Quick Start

To run E2E tests from your host machine (outside Docker):

```bash
# 1. Create .env.local file (one-time setup)
cp .env.local.template .env.local

# 2. Start Hydra and configure it
make restart-hydra

# 3. Run a single E2E test for troubleshooting
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual

# 4. Run all E2E tests
make test-e2e
```

## Common Issues

### Issue 1: `UnknownHostException: hydra`

**Symptom:**
```
java.net.UnknownHostException: hydra: No address associated with hostname
```

**Cause:** The `.env.local` file is missing or not being loaded.

**Solution:**
```bash
# Create .env.local from template
cp .env.local.template .env.local

# Verify it contains:
cat .env.local
# Should show:
#   export HYDRA_ADMIN_URL=http://localhost:4445
#   export HYDRA_PUBLIC_URL=http://localhost:4444

# Run test again
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual
```

### Issue 2: Hydra Not Running

**Symptom:**
```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 18
```
All E2E tests are skipped.

**Cause:** Hydra Docker container is not running.

**Solution:**
```bash
# Check if Hydra is running
docker ps | grep hydra

# If not running, start it
make restart-hydra
```

### Issue 3: Hydra Not Configured

**Symptom:**
```
Failed to create client: 401 Unauthorized
```

**Cause:** Hydra test clients haven't been created.

**Solution:**
```bash
# Configure Hydra with test OAuth2 clients
make configure-hydra

# Verify clients were created (should show 7 clients)
curl -s http://localhost:4445/clients | python3 -m json.tool | grep client_id
```

### Issue 4: Tests Fail After First Run

**Symptom:** First test passes, subsequent tests fail with token errors.

**Cause:** Token caching causing conflicts between tests.

**Solution:**
```bash
# Purge cached tokens
rm -rf ~/.java/.userPrefs/io/trino/oauth2

# Or restart Hydra to reset everything
make restart-hydra
```

## Running Individual Tests

### All Available E2E Tests

**Client Credentials Tests:**
```bash
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowOidc
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowWithScope
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowWithAudience
```

**Device Code Tests:**
```bash
make test-one TEST=HydraDeviceCodeE2ETest#testDeviceCodeFlowManual
make test-one TEST=HydraDeviceCodeE2ETest#testDeviceCodeFlowOidc
make test-one TEST=HydraDeviceCodeE2ETest#testDeviceCodeFlowNoSecrets
make test-one TEST=HydraDeviceCodeE2ETest#testDeviceCodeFlowNoSecretsScopesAudience
```

**Authorization Code Tests:**
```bash
make test-one TEST=HydraAuthorizationCodeE2ETest#testAuthorizationCodeFlowManual
make test-one TEST=HydraAuthorizationCodeE2ETest#testAuthorizationCodeFlowOidc
make test-one TEST=HydraAuthorizationCodeE2ETest#testAuthorizationCodeFlowWithScopes
make test-one TEST=HydraAuthorizationCodeE2ETest#testAuthorizationCodeFlowWithPkce
```

**Error Tests:**
```bash
make test-one TEST=HydraClientCredentialsErrorE2ETest#testClientCredentialsFlowBadClientId
make test-one TEST=HydraClientCredentialsErrorE2ETest#testClientCredentialsFlowBadSecret
make test-one TEST=HydraClientCredentialsErrorE2ETest#testClientCredentialsFlowBadScope
make test-one TEST=HydraClientCredentialsErrorE2ETest#testDeviceCodeFlowBadScope
make test-one TEST=HydraAuthorizationCodeErrorE2ETest#testAuthorizationCodeFlowBadScope
make test-one TEST=HydraAuthorizationCodeErrorE2ETest#testAuthorizationCodeFlowBadClientId
```

## Debugging Tips

### View Hydra Logs

```bash
# View recent logs
make hydra-logs

# Follow logs in real-time
make hydra-logs -f

# View last 100 lines
docker logs --tail 100 hydra
```

### Test Hydra Directly with curl

```bash
# Test client credentials flow
make test-client-credentials-curl

# Test device code flow
make test-device-curl
```

### Check Hydra Health

```bash
# Public endpoint
curl http://localhost:4444/health/ready

# Admin endpoint
curl http://localhost:4445/health/ready
```

### List Configured Clients

```bash
curl -s http://localhost:4445/clients | python3 -m json.tool
```

## Environment Variables

The `.env.local` file should contain:

```bash
export HYDRA_ADMIN_URL=http://localhost:4445
export HYDRA_PUBLIC_URL=http://localhost:4444
```

These variables tell the Java tests to use `localhost` instead of the Docker internal hostname `hydra`.

## Running Tests Inside Docker (Alternative)

If you want to run tests inside Docker (where `hydra` hostname works):

```bash
# Build and run tests in Docker container
make container-test  # (if this target exists in your setup)

# Or manually:
docker-compose -f tests/docker-compose.yml run --rm test
```

## Clean Slate

To start completely fresh:

```bash
# 1. Stop Hydra
make stop-hydra

# 2. Remove cached tokens
rm -rf ~/.java/.userPrefs/io/trino/oauth2

# 3. Start Hydra fresh
make restart-hydra

# 4. Run a single test
make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual
```
