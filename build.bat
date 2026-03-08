@echo off
cd /D %~dp0

if not defined JAVA_HOME (set JAVA_HOME=C:\Program Files\Java\jdk-21)

echo Downloading dependencies...
call ant resolve

echo:

echo Compiling...
call ant fatjar

if not defined WORKSPACE pause
