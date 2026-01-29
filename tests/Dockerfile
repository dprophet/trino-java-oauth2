# Use Maven with Java 11
FROM maven:3.9-eclipse-temurin-11

# Accept proxy build arguments
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
ARG USE_VPN=auto

# Set proxy environment variables for the build if provided
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    NO_PROXY=${NO_PROXY} \
    MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -Dhttps.protocols=TLSv1.2"

# Set working directory
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml /app/

# Copy .docker-maven-settings.xml which is prepared by the Makefile
# VPN ON: Contains artifactory_settings.xml content
# VPN OFF: Contains empty/minimal settings or doesn't exist
COPY .docker-maven-settings.xml /app/.docker-maven-settings.xml

# Configure Maven settings based on proxy environment
# If HTTP_PROXY is set, .docker-maven-settings.xml MUST exist (corporate network)
# Otherwise, use Maven Central directly
RUN mkdir -p /root/.m2 && \
    if [ -n "$HTTP_PROXY" ]; then \
        if [ ! -s /app/.docker-maven-settings.xml ]; then \
            echo "ERROR: HTTP_PROXY is set but .docker-maven-settings.xml is missing or empty!"; \
            echo "When using a proxy, you must provide artifactory_settings.xml for your corporate network."; \
            exit 1; \
        fi; \
        echo "=== Proxy detected: Configuring Maven to use Artifactory/JFrog/Nexus ==="; \
        cp /app/.docker-maven-settings.xml /app/settings.xml; \
    else \
        echo "=== No proxy: Using Maven Central directly ==="; \
        rm -f /app/settings.xml; \
    fi

# Copy source code
COPY src /app/src

# Copy test configuration
COPY tests /app/tests

# Copy configure script
COPY configure-hydra.sh /app/

# Compile the project and download all dependencies
# This will download everything needed including test dependencies
RUN if [ -f /app/settings.xml ]; then \
        echo "Using settings.xml for Maven"; \
        mvn -s /app/settings.xml clean compile test-compile -B; \
    else \
        echo "Using Maven Central (no settings file)"; \
        mvn clean compile test-compile -B; \
    fi

# Download test plugin dependencies explicitly (surefire-junit-platform, etc.)
# This ensures everything is available when tests run
RUN if [ -f /app/settings.xml ]; then \
        mvn -s /app/settings.xml test -DskipTests -B || true; \
    else \
        mvn test -DskipTests -B || true; \
    fi

# Generate a classpath file for runtime use by finding all jars in Maven repo
# This is critical for configure-hydra.sh to work properly
RUN echo "Generating classpath for ConfigureHydra..." && \
    find /root/.m2/repository -name "*.jar" -type f | tr '\n' ':' > /app/classpath.txt && \
    JAR_COUNT=$(grep -o '\.jar:' /app/classpath.txt | wc -l) && \
    echo "Classpath file generated with $JAR_COUNT JAR dependencies" && \
    echo "classpath.txt=" && cat /app/classpath.txt && echo "" \
    ls -lh /app/classpath.txt

# Default command runs tests
# Use shell form to support conditional settings file
CMD if [ -f /app/settings.xml ]; then mvn -s /app/settings.xml test; else mvn test; fi
