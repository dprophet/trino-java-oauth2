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

# Configure Maven settings based on what's in the prepared settings file
RUN mkdir -p /root/.m2 && \
    if [ -s /app/.docker-maven-settings.xml ]; then \
        echo "=== VPN ON: Configuring Maven to use Bloomberg Artifactory ==="; \
        cp /app/.docker-maven-settings.xml /root/.m2/settings.xml; \
        sed -i '/<\/settings>/i \  <mirrors>\n    <mirror>\n      <id>artifactory-central</id>\n      <mirrorOf>central</mirrorOf>\n      <name>Artifactory mirror of Maven Central</name>\n      <url>http://artprod.dev.bloomberg.com/artifactory/repo1-cache</url>\n    </mirror>\n    <mirror>\n      <id>maven-default-http-blocker</id>\n      <mirrorOf>external:dummy:*</mirrorOf>\n      <name>Pseudo repository to mirror external repositories initially using HTTP.</name>\n      <url>http://0.0.0.0/</url>\n      <blocked>false</blocked>\n    </mirror>\n  </mirrors>' /root/.m2/settings.xml; \
        cp /root/.m2/settings.xml /app/settings.xml; \
    else \
        echo "=== VPN OFF: Using Maven Central directly ==="; \
    fi

# Copy source code
COPY src /app/src

# Copy test configuration
COPY tests /app/tests

# Copy configure script
COPY configure-hydra.sh /app/

# Compile the project and download all dependencies
# This will download everything needed including test dependencies
RUN mvn clean compile test-compile -B

# Download test plugin dependencies explicitly (surefire-junit-platform, etc.)
# This ensures everything is available when tests run
RUN mvn test -DskipTests -B || true

# Generate a classpath file for runtime use (includes all dependencies from local repo)
RUN mvn dependency:build-classpath -Dmdep.outputFile=/app/classpath.txt -q || echo ""

# Default command runs tests
CMD ["mvn", "test"]
