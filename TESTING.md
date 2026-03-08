# Running Tests Locally

Tests can be executed via Ant, and are configured to run even if failures occur.

## Prerequisites
- JDK 21+ available as `java`
- Apache Ant
- Apache Ivy

## Command

```bash
ant -lib /usr/share/java/ivy.jar clean resolve test
```

## Notes
- The `test` target compiles test sources and executes all `*Test`, `*Tests`, and `*TestSuite` classes.
- Failures are expected while development is in progress; the target is configured to continue execution.
- Reports are written to:
  - `dist/test-reports`

## Debian package install test

Install local `.deb` packages with `apt`, not `dpkg`, so dependencies are resolved automatically:

```bash
sudo apt install ./dist/openfilebot_<version>_<arch>.deb
```

Notes:
- `apt install ./package.deb` installs missing dependencies from configured repositories.
- `dpkg -i package.deb` does **not** resolve dependencies automatically.
