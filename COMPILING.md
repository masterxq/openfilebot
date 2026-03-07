# Compiling on Windows

## Requirements

* JDK 21 (LTS) or newer
* [Apache Ant](https://ant.apache.org/bindownload.cgi)
* [Apache Ivy](https://ant.apache.org/ivy/download.cgi)
* [Launch4j](https://github.com/mirror/launch4j/releases)
* [WiX Toolset](https://github.com/wixtoolset/wix3/releases/latest)

## Compiling

Run the default Windows build:

`build.bat`

Or run the explicit Ant targets manually:

`ant -lib ivy.jar clean resolve fatjar launch4j-all msi`

Notes:

* `launch4j-all` rebuilds all Windows launcher `.exe` files from the current Launch4j XML configs.
* `msi` now expects freshly built `openfilebot*.exe` launcher binaries.
