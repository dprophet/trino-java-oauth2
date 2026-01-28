# Build Instructions

This project can be built with standard Maven using Maven Central repositories.

## Quick Build

```bash
# Clean compile
mvn clean compile

# Package as JAR
mvn clean package

# Install to local Maven repository
mvn clean install
```

## Using Custom Maven Settings

If you have a custom Maven settings file (e.g., for a corporate Artifactory):

```bash
mvn -s settings.xml clean compile
mvn -s settings.xml clean package
mvn -s settings.xml clean install
```

## Verify Build

After a successful build, you should see:

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  ~5s
```

And the compiled classes in:
```
target/classes/io/trino/oauth2/
```

## Run Example

```bash
# Build first
mvn clean package

# Run client credentials example
java -cp target/trino-oauth2-1.0.0.jar io.trino.oauth2.Example client_credentials
```

## Clean Build

```bash
mvn clean
```
