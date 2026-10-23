# NagaClan source distribution

This archive contains the NagaClan 1.0.8 Java source, resources, regression tests, Maven project and release documentation. It continues the existing NagaClan project. Version 1.0.8 removes the NagaCore dependency and uses its own color converter and reload response.

## Build with Maven

Install Java 21 and Maven 3.9+. Extract the entire archive, preserving this layout:

```text
NagaClan/
  pom.xml
  src/main/
  src/test/
```

Run from the extracted archive root:

```shell
mvn -f NagaClan/pom.xml clean verify
```

The Maven output is `NagaClan/target/nagaclan-1.0.8.jar`. Its plugin identity/version is the same as the delivered `NagaClan-1.0.8.jar`.

Internet access is required the first time Maven resolves Paper API, PlaceholderAPI and JUnit. NagaCore is neither a compile-time nor a runtime dependency. This archive does not include runtime clan data or credentials.

The working development workspace additionally uses `build.ps1` and Eclipse ECJ 3.39.0 to work around a local Windows javac ZipFS issue. That workspace-only script and its caches are not required by this portable Maven source distribution.

## Structure

- `model`: clan, role, access-policy and invitation data.
- `service`: clan mutations, private chat state and compensated economy operations.
- `storage`: YAML persistence and atomic replacement.
- `gui`: inventory routing, pages and single-use input sessions.
- `gui/NativeInputDialog`: isolated modern Paper dialog adapter.
- `listener`: private chat routing and friendly-fire handling.
- `text`: language snapshots and optional small-cap styling.
- `command`, `limit`, `economy`, `placeholder`, `config`: commands and integrations.

Language files use quoted fixed-text keys and named keys. Keep keys unchanged and edit values. They are UTF-8 YAML; preserve quotes around values containing color codes, colons or other YAML syntax.

Distribution rights and marketplace choice remain decisions for the project owner. No third-party marketplace upload was performed.
