#!/bin/bash
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
    echo "Test classes not found. Compiling..."
    mvn test-compile -s artifactory_settings.xml
fi

# Run the configuration tool
java -cp "target/test-classes:target/classes:$(mvn dependency:build-classpath -s artifactory_settings.xml -q -Dmdep.outputFile=/dev/stdout)" \
    io.trino.oauth2.e2e.ConfigureHydra
