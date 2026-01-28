# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Makefile for Trino OAuth2 Java Library

# Maven settings file
MVN_SETTINGS := -s artifactory_settings.xml

# Default target
.PHONY: default
default: build

# use bash for shell commands
SHELL := /bin/bash

# Filter out known targets from the command line goals to get extra flags
EXTRA_ARGS := $(filter-out logs,$(MAKECMDGOALS))

#
# Build Targets
#

.PHONY: clean
clean:
	mvn $(MVN_SETTINGS) clean

.PHONY: compile
compile:
	mvn $(MVN_SETTINGS) compile

.PHONY: build
build:
	mvn $(MVN_SETTINGS) clean package

.PHONY: install
install:
	mvn $(MVN_SETTINGS) clean install

#
# Test Targets
#

.PHONY: test
test:
	@if [ -f .env.local ]; then \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		mvn $(MVN_SETTINGS) test; \
	else \
		mvn $(MVN_SETTINGS) test; \
	fi

.PHONY: test-unit
test-unit:
	mvn $(MVN_SETTINGS) test -Dtest=OAuth2ClientTest,DeviceCodeConfigTest,ClientCredentialsConfigTest,AuthorizationCodeConfigTest,OAuthTokenStoreTest

.PHONY: test-e2e
test-e2e:
	@if [ -f .env.local ]; then \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		mvn $(MVN_SETTINGS) test -Dgroups=e2e; \
	else \
		mvn $(MVN_SETTINGS) test -Dgroups=e2e; \
	fi

.PHONY: test-one
test-one:
	@if [ -z "$(TEST)" ]; then \
		echo "Usage: make test-one TEST=TestClassName#testMethodName"; \
		echo ""; \
		echo "Examples:"; \
		echo "  make test-one TEST=HydraClientCredentialsE2ETest#testClientCredentialsFlowManual"; \
		echo "  make test-one TEST=HydraDeviceCodeE2ETest#testDeviceCodeFlowManual"; \
		echo "  make test-one TEST=OAuth2ClientTest#testOAuth2ClientInstantiation"; \
		exit 1; \
	fi
	@if [ -f .env.local ]; then \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		mvn $(MVN_SETTINGS) test -Dtest=$(TEST); \
	else \
		mvn $(MVN_SETTINGS) test -Dtest=$(TEST); \
	fi

.PHONY: test-compile
test-compile:
	mvn $(MVN_SETTINGS) test-compile

#
# Hydra Docker Targets
#

.PHONY: pull
pull:
	docker pull oryd/hydra:v2.2.0
	docker pull oryd/hydra-login-consent-node:v2.2.0

.PHONY: start-hydra
start-hydra:
	@if [ -f .env.local ]; then \
		echo "Loading environment from .env.local before starting Hydra..."; \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		docker-compose -f tests/docker-compose.yml up -d; \
	else \
		docker-compose -f tests/docker-compose.yml up -d; \
	fi

.PHONY: stop-hydra
stop-hydra:
	docker-compose -f tests/docker-compose.yml down

.PHONY: restart-hydra
restart-hydra:
	$(MAKE) stop-hydra
	$(MAKE) start-hydra
	@echo "Waiting for Hydra to be ready..."
	@sleep 5
	$(MAKE) configure-hydra

.PHONY: configure-hydra
configure-hydra:
	@echo "Configuring Hydra with test OAuth2 clients..."
	@if [ ! -d "target/test-classes" ]; then \
		echo "Compiling test classes first..."; \
		mvn $(MVN_SETTINGS) test-compile; \
	fi
	@if [ -f .env.local ]; then \
		echo "Loading environment from .env.local..."; \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		unset HTTP_PROXY && \
		unset http_proxy && \
		./configure-hydra.sh; \
	else \
		unset HTTP_PROXY && \
		unset http_proxy && \
		./configure-hydra.sh; \
	fi

.PHONY: hydra-logs
hydra-logs:
	docker logs $(EXTRA_ARGS) hydra

#
# Full E2E Test Flow
#

.PHONY: e2e
e2e: restart-hydra
	@echo "Running E2E tests..."
	$(MAKE) test-e2e

#
# Development Targets
#

.PHONY: test-device-curl
test-device-curl:
	curl --noproxy '*' -s -S -X POST \
		-d "client_id=device-code-client&client_secret=device-code-secret&scope=offline" \
		http://localhost:4444/oauth2/device/auth

.PHONY: test-client-credentials-curl
test-client-credentials-curl:
	curl --noproxy '*' -s -S -X POST \
		-d "grant_type=client_credentials&client_id=client-credentials-client&client_secret=client-credentials-secret" \
		http://localhost:4444/oauth2/token

.PHONY: verify
verify:
	mvn $(MVN_SETTINGS) verify

.PHONY: format
format:
	@echo "Code formatting not configured. Consider adding google-java-format or spotless."

.PHONY: checkstyle
checkstyle:
	@echo "Checkstyle not configured. Add maven-checkstyle-plugin to pom.xml to enable."

#
# Help
#

.PHONY: help
help:
	@echo "Trino OAuth2 Java Library - Make Targets"
	@echo ""
	@echo "IMPORTANT: E2E tests require .env.local file with localhost URLs"
	@echo "  Run: cp .env.local.template .env.local"
	@echo ""
	@echo "Build Targets:"
	@echo "  make build          - Clean and build the project (default)"
	@echo "  make compile        - Compile source code"
	@echo "  make clean          - Clean build artifacts"
	@echo "  make install        - Install to local Maven repository"
	@echo ""
	@echo "Test Targets:"
	@echo "  make test           - Run all tests (unit + E2E if Hydra running)"
	@echo "  make test-unit      - Run only unit tests"
	@echo "  make test-e2e       - Run only E2E tests (requires Hydra)"
	@echo "  make test-one TEST=ClassName#method - Run a single test method"
	@echo "  make test-compile   - Compile test classes"
	@echo ""
	@echo "Hydra Targets:"
	@echo "  make start-hydra    - Start Hydra in Docker"
	@echo "  make stop-hydra     - Stop Hydra"
	@echo "  make restart-hydra  - Restart Hydra and configure test clients"
	@echo "  make configure-hydra - Configure Hydra test OAuth2 clients"
	@echo "  make hydra-logs     - Show Hydra logs (use: make hydra-logs -f)"
	@echo ""
	@echo "E2E Test Flow:"
	@echo "  make e2e            - Restart Hydra + configure + run E2E tests"
	@echo ""
	@echo "Development:"
	@echo "  make verify         - Run Maven verify phase"
	@echo "  make test-device-curl           - Test device code endpoint with curl"
	@echo "  make test-client-credentials-curl - Test client credentials with curl"
	@echo ""

# Phony target to treat flags as targets (for docker logs -f)
-f:
	@:
