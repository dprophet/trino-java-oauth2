#!/bin/bash -xv
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

# Configure Hydra with test OAuth2 clients
# This is the Java equivalent of: python tests/configure_hydra.py

set -e

# Ensure compiled
if [ ! -d "target/test-classes" ]; then
    echo "Error: Test classes not found. Run mvn test-compile first."
    exit 1
fi

# Determine which settings file to use based on proxy environment
# If HTTP_PROXY is set, use artifactory settings (corporate network)
# Otherwise, use Maven Central directly
if [ -n "$HTTP_PROXY" ] || [ -n "$http_proxy" ]; then
    if [ -f ".local-maven-settings.xml" ]; then
        MVN_SETTINGS="-s .local-maven-settings.xml"
        echo "Proxy detected: Using .local-maven-settings.xml (temporary with HTTP blocker override)"
    elif [ -f "artifactory_settings.xml" ]; then
        MVN_SETTINGS="-s artifactory_settings.xml"
        echo "Proxy detected: Using artifactory_settings.xml"
    elif [ -f "settings.xml" ]; then
        MVN_SETTINGS="-s settings.xml"
        echo "Proxy detected: Using settings.xml"
    else
        MVN_SETTINGS=""
        echo "Warning: Proxy set but no settings file found"
    fi
else
    # No proxy - use Maven Central directly
    if [ -f "settings.xml" ]; then
        MVN_SETTINGS="-s settings.xml"
        echo "No proxy: Using settings.xml"
    else
        MVN_SETTINGS=""
        echo "No proxy: Using Maven Central directly"
    fi
fi

# Build classpath - try multiple methods in order of preference
if [ -f "classpath.txt" ] && [ -s "classpath.txt" ]; then
    # Method 1: Use pre-generated classpath.txt (from Docker build)
    DEP_CLASSPATH=$(cat classpath.txt)
    CLASSPATH="target/test-classes:target/classes:$DEP_CLASSPATH"
elif command -v mvn >/dev/null 2>&1; then
    # Method 2: Use Maven to build classpath (most reliable)
    echo "Building classpath using Maven..."
    DEP_CLASSPATH=$(mvn $MVN_SETTINGS -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout 2>/dev/null || echo "")
    if [ -z "$DEP_CLASSPATH" ]; then
        echo "Warning: Maven classpath generation failed, trying alternative method..."
        # Method 3: Find all jars in local Maven repository
        MAVEN_REPO="${HOME}/.m2/repository"
        if [ -d "$MAVEN_REPO" ]; then
            DEP_CLASSPATH=$(find "$MAVEN_REPO" -name "*.jar" -type f 2>/dev/null | tr '\n' ':')
        fi
    fi
    CLASSPATH="target/test-classes:target/classes:$DEP_CLASSPATH"
else
    # Method 4: Final fallback - find jars in Maven repository
    echo "Building classpath from local Maven repository..."
    MAVEN_REPO="${HOME}/.m2/repository"
    if [ ! -d "$MAVEN_REPO" ]; then
        # Try Docker path as last resort
        MAVEN_REPO="/root/.m2/repository"
    fi
    DEP_CLASSPATH=$(find "$MAVEN_REPO" -name "*.jar" -type f 2>/dev/null | tr '\n' ':')
    CLASSPATH="target/test-classes:target/classes:$DEP_CLASSPATH"
fi

# Run the configuration tool with all dependencies on classpath
java -cp "$CLASSPATH" io.trino.oauth2.e2e.ConfigureHydra
