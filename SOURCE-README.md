# NagaClan source distribution

This archive contains the NagaClan 1.0.6 Java source, resources, regression tests, Maven project and release documentation. It is based on the existing NagaClan project, not a new replacement implementation.

## Build with Maven

Install Java 21 and Maven 3.9+. Extract the entire archive, preserving this layout:

```text
NagaClan/
  pom.xml
  src/main/
  src/test/
outputs/
  NagaCore-1.1.0.jar
```

Run from the extracted archive root:

```shell
mvn -f NagaClan/pom.xml clean verify
```

The Maven output is `NagaClan/target/nagaclan-1.0.6.jar`. Its plugin identity/version is the same as the delivered `NagaClan-1.0.6.jar`.

Internet access is required the first time Maven resolves Paper API, PlaceholderAPI and JUnit. The supplied NagaCore binary is a compile-time dependency and a separate runtime plugin; it is not embedded in the NagaClan JAR. This archive does not include NagaCore source, runtime clan data, credentials or license records.

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

Publishing source does not make a client-side Java license check tamper-proof. Distribution rights and marketplace choice remain decisions for the project owner. No third-party marketplace upload was performed.
