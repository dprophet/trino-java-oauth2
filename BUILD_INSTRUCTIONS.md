# Build Instructions

This project includes `artifactory_settings.xml` for building with internal Bloomberg Artifactory.

## Quick Build

```bash
# Clean compile
mvn -s artifactory_settings.xml clean compile

# Package as JAR
mvn -s artifactory_settings.xml clean package

# Install to local Maven repository
mvn -s artifactory_settings.xml clean install
```

## Without Artifactory Settings

If you have Maven configured with access to Maven Central or another repository:

```bash
mvn clean compile
mvn clean package
mvn clean install
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
mvn -s artifactory_settings.xml clean package

# Run client credentials example
java -cp target/trino-oauth2-1.0.0.jar io.trino.oauth2.Example client_credentials
```

## Clean Build

```bash
mvn -s artifactory_settings.xml clean
```
