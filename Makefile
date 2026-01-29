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

# Maven settings file detection based on proxy environment
# If HTTP_PROXY is set, artifactory_settings.xml MUST exist (corporate network)
# Otherwise, use settings.xml if it exists, or Maven defaults
ifdef HTTP_PROXY
    ifeq ("$(wildcard artifactory_settings.xml)","")
        $(error HTTP_PROXY is set but artifactory_settings.xml not found. When using a proxy, you must have artifactory_settings.xml configured for your corporate network.)
    endif
    # Use temp settings file with HTTP blocker override if it exists, otherwise use original
    MVN_SETTINGS := -s $(if $(wildcard .local-maven-settings.xml),.local-maven-settings.xml,artifactory_settings.xml)
else
    MVN_SETTINGS :=
endif

# Docker build args - pass proxy settings if they exist
# Proxy environment variables (HTTP_PROXY, HTTPS_PROXY, NO_PROXY) are automatically
# passed through to all commands. Scripts will honor them if set.
ifdef HTTP_PROXY
    DOCKER_BUILD_ARGS := --build-arg HTTP_PROXY=$(HTTP_PROXY) \
                         --build-arg HTTPS_PROXY=$(HTTP_PROXY) \
                         --build-arg NO_PROXY=$(NO_PROXY)
else
    DOCKER_BUILD_ARGS :=
endif

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
	@echo "Pulling Docker images (proxy honored if set)..."
	docker pull oryd/hydra:v2.2.0
	docker pull oryd/hydra-login-consent-node:v2.2.0

.PHONY: start-hydra
start-hydra:
	@if [ -f .env.local ]; then \
		echo "Loading environment from .env.local before starting Hydra..."; \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		docker-compose -f tests/docker-compose.yml up -d hydra consent; \
	else \
		docker-compose -f tests/docker-compose.yml up -d hydra consent; \
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
	@# Create temporary settings file with HTTP blocker override if using proxy
	@if [ -n "$$HTTP_PROXY" ] && [ -f artifactory_settings.xml ]; then \
		cp artifactory_settings.xml .local-maven-settings.xml; \
		sed -i.bak '/<\/proxies>/a\  <mirrors>\n    <mirror>\n      <id>maven-default-http-blocker</id>\n      <mirrorOf>external:dummy:*</mirrorOf>\n      <name>Disable HTTP blocking</name>\n      <url>http://0.0.0.0/</url>\n      <blocked>false</blocked>\n    </mirror>\n  </mirrors>' .local-maven-settings.xml && rm -f .local-maven-settings.xml.bak; \
	fi
	@if [ -f .env.local ]; then \
		echo "Loading environment from .env.local..."; \
		export $$(cat .env.local | grep -v '^#' | xargs) && \
		./configure-hydra.sh; \
	else \
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
	@rm -f .local-maven-settings.xml

#
# Docker Daemon Proxy Management
#

.PHONY: docker-proxy-enable
docker-proxy-enable:
	@echo "Enabling Docker daemon proxy (requires sudo)..."
	sudo ./configure-docker-proxy.sh enable

.PHONY: docker-proxy-disable
docker-proxy-disable:
	@echo "Disabling Docker daemon proxy (requires sudo)..."
	sudo ./configure-docker-proxy.sh disable

.PHONY: docker-proxy-status
docker-proxy-status:
	@./configure-docker-proxy.sh status

#
# Docker-based Testing
#

.PHONY: build-test
build-test:
	@echo "Preparing Docker build context..."
	@# Proxy detection: If HTTP_PROXY is set, artifactory_settings.xml MUST exist
	@if [ -n "$$HTTP_PROXY" ]; then \
		if [ ! -f artifactory_settings.xml ]; then \
			echo "ERROR: HTTP_PROXY is set but artifactory_settings.xml not found!"; \
			echo "When using a proxy, you must have artifactory_settings.xml configured for your corporate network."; \
			exit 1; \
		fi; \
		echo "Proxy detected: Using artifactory_settings.xml for Docker build"; \
		cp artifactory_settings.xml .docker-maven-settings.xml; \
		echo "Adding HTTP blocker override to temporary .docker-maven-settings.xml..."; \
		sed -i.bak '/<\/proxies>/a\  <mirrors>\n    <mirror>\n      <id>maven-default-http-blocker</id>\n      <mirrorOf>external:dummy:*</mirrorOf>\n      <name>Disable HTTP blocking</name>\n      <url>http://0.0.0.0/</url>\n      <blocked>false</blocked>\n    </mirror>\n  </mirrors>' .docker-maven-settings.xml && rm -f .docker-maven-settings.xml.bak; \
	else \
		echo "No proxy: Using Maven Central for Docker build"; \
		echo '<?xml version="1.0" encoding="UTF-8"?><settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"></settings>' > .docker-maven-settings.xml; \
	fi
	@echo "Building test Docker container..."
	docker compose -f tests/docker-compose.yml build \
		--build-arg HTTP_PROXY=$(HTTP_PROXY) \
		--build-arg HTTPS_PROXY=$(HTTPS_PROXY) \
		--build-arg NO_PROXY=$(NO_PROXY) \
		test
	@rm -f .docker-maven-settings.xml

.PHONY: test-docker
test-docker:
	@echo "Running tests in Docker containers..."
	@echo "Note: Containers will inherit proxy settings from environment"
	docker compose -f tests/docker-compose.yml up --abort-on-container-exit --exit-code-from test

.PHONY: test-docker-rebuild
test-docker-rebuild: build-test test-docker

.PHONY: clean-docker
clean-docker:
	@echo "Cleaning up Docker containers and images..."
	docker compose -f tests/docker-compose.yml down -v --rmi local

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
	@echo "Docker Daemon Proxy Management:"
	@echo "  make docker-proxy-enable  - Configure Docker daemon to use proxy (requires sudo)"
	@echo "  make docker-proxy-disable - Remove Docker daemon proxy config (requires sudo)"
	@echo "  make docker-proxy-status  - Show current Docker daemon proxy status"
	@echo ""
	@echo "  NOTE: Your network requires the proxy to access docker.io"
	@echo "  Run 'make docker-proxy-enable' once before Docker operations"
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
	@echo "Docker-based Testing:"
	@echo "  make build-test     - Build test Docker container"
	@echo "  make test-docker    - Run all tests in Docker (no .env.local needed)"
	@echo "  make test-docker-rebuild - Rebuild and run tests in Docker"
	@echo "  make clean-docker   - Clean up Docker containers and images"
	@echo ""
	@echo "Development:"
	@echo "  make verify         - Run Maven verify phase"
	@echo "  make test-device-curl           - Test device code endpoint with curl"
	@echo "  make test-client-credentials-curl - Test client credentials with curl"
	@echo ""

# Phony target to treat flags as targets (for docker logs -f)
-f:
	@:
