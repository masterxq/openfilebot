# Compiling on Windows

## Requirements

* JDK 21 (LTS) or newer
* [Apache Ant](https://ant.apache.org/bindownload.cgi)
* [Apache Ivy](https://ant.apache.org/ivy/download.cgi)
* [WiX Toolset](https://github.com/wixtoolset/wix3/releases/latest)

## Compiling

Run the default Windows build:

`build.bat`

Or build manually (no Microsoft Store required):

1. `ant -lib ivy.jar clean resolve fatjar`
2. Build MSI with `jpackage --type msi` from the generated fatjar

Notes:

* The release workflow uses `jpackage` for Windows MSI builds.
* The generated MSI can be installed and run by standard Windows users without Microsoft Store.
