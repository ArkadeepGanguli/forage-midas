@echo off
REM Run TaskTwoTests and capture full output to mvn_test_output.log
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME=%JAVA_HOME%
cd /d "%~dp0"
.\mvnw.cmd -Dtest=TaskTwoTests test > mvn_test_output.log 2>&1
echo Done. Output written to mvn_test_output.log

