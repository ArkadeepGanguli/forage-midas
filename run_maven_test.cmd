@echo off
REM Temporary script to run TaskTwoTests with JAVA_HOME set to Java 21
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME=%JAVA_HOME%
java -version
javac -version
cd /d "%~dp0"
.\mvnw.cmd -Dtest=TaskTwoTests test

