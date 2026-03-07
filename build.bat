@echo off
cd /D %~dp0
set PATH=%WIX%\bin;%PATH%
if defined LAUNCH4J set PATH=%LAUNCH4J%;%PATH%

if not defined JAVA_HOME (set JAVA_HOME=C:\Program Files\Java\jdk-21)

echo Downloading dependencies...
call ant resolve

echo:

echo Compiling...
call ant fatjar

echo:

echo Building MSI installer...
call ant msi

if not defined WORKSPACE pause
