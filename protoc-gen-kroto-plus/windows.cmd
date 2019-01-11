@if "%DEBUG%" == "" @echo off

set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/javaw.exe

"%JAVA_EXE%" -jar C:\Projects\kroto-plus\protoc-gen-kroto-plus\build\libs\protoc-gen-kroto-plus-0.2.2-RC1-jvm8.jar