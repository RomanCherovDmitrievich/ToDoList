@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
cd /d "%PROJECT_ROOT%" || exit /b 1

set "LIB_DIR=%PROJECT_ROOT%\lib"
set "DATA_DIR=%PROJECT_ROOT%\data"
set "AUDIO_DIR=%PROJECT_ROOT%\audio"

echo [setup] Project: %PROJECT_ROOT%

call :resolve_java_home
if errorlevel 1 exit /b 1
echo [setup] Java: %JAVA_EXE%

call :resolve_javafx_home
if errorlevel 1 exit /b 1
echo [setup] JavaFX SDK: %JAVAFX_HOME%\lib

if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%AUDIO_DIR%" mkdir "%AUDIO_DIR%"

call :check_required_jar "%LIB_DIR%\gson-2.10.1.jar"
call :check_required_jar "%LIB_DIR%\sqlite-jdbc-3.45.1.0.jar"
call :check_required_jar "%LIB_DIR%\slf4j-api-2.0.12.jar"
call :check_required_jar "%LIB_DIR%\slf4j-nop-2.0.12.jar"
call :check_required_jar "%LIB_DIR%\jakarta.mail-api-2.1.5.jar"
call :check_required_jar "%LIB_DIR%\angus-mail-2.0.5.jar"
call :check_required_jar "%LIB_DIR%\jakarta.activation-api-2.1.4.jar"
call :check_required_jar "%LIB_DIR%\angus-activation-2.0.3.jar"
if errorlevel 1 exit /b 1

echo [setup] Done.
echo [setup] Next: run.bat
exit /b 0

:check_required_jar
if not exist "%~1" (
    echo [setup] Missing jar: %~nx1
    echo [setup] Put all required jars into lib\ before running the project.
    exit /b 1
)
exit /b 0

:resolve_java_home
set "JAVA_EXE="
set "JAVAC_EXE="

if defined JAVA_HOME (
    call :validate_java_home "%JAVA_HOME%"
    if not errorlevel 1 exit /b 0
    echo [setup] Ignoring invalid JAVA_HOME: %JAVA_HOME%
    set "JAVA_HOME="
)

for /f "delims=" %%F in ('where javac 2^>nul') do (
    call :derive_from_javac "%%~fF"
    if not errorlevel 1 exit /b 0
)

call :search_java_home
if defined JAVA_HOME (
    call :validate_java_home "%JAVA_HOME%"
    if not errorlevel 1 exit /b 0
)

echo [setup] JDK not found. Install full JDK 21+ and make sure javac.exe exists.
exit /b 1

:derive_from_javac
if not exist "%~1" exit /b 1
for %%I in ("%~1") do set "JAVA_BIN=%%~dpI"
for %%I in ("%JAVA_BIN%..") do set "CANDIDATE_JAVA_HOME=%%~fI"
call :validate_java_home "%CANDIDATE_JAVA_HOME%"
exit /b %ERRORLEVEL%

:validate_java_home
if "%~1"=="" exit /b 1
if exist "%~1\bin\javac.exe" if exist "%~1\bin\java.exe" (
    for %%I in ("%~1") do set "JAVA_HOME=%%~fI"
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
    exit /b 0
)
exit /b 1

:search_java_home
for %%R in (
    "%ProgramFiles%\Java"
    "%ProgramFiles%\Eclipse Adoptium"
    "%ProgramFiles%\Microsoft"
    "%ProgramFiles%\BellSoft"
    "%ProgramFiles%\Amazon Corretto"
    "%ProgramFiles%\Zulu"
) do (
    if not defined JAVA_HOME call :search_java_root "%%~R"
)
if defined ProgramFiles(x86) if not defined JAVA_HOME call :search_java_root "%ProgramFiles(x86)%\Java"
if defined ProgramW6432 if not defined JAVA_HOME call :search_java_root "%ProgramW6432%\Java"
exit /b 0

:search_java_root
if not exist "%~1" exit /b 0
for /f "delims=" %%D in ('dir /b /ad /o-n "%~1\\jdk*" 2^>nul') do (
    if exist "%~1\%%D\bin\javac.exe" if not defined JAVA_HOME set "JAVA_HOME=%~1\%%D"
)
for /f "delims=" %%D in ('dir /b /ad /o-n "%~1\\*jdk*" 2^>nul') do (
    if exist "%~1\%%D\bin\javac.exe" if not defined JAVA_HOME set "JAVA_HOME=%~1\%%D"
)
exit /b 0

:resolve_javafx_home
if defined JAVAFX_HOME if exist "%JAVAFX_HOME%\lib" exit /b 0

if exist "%PROJECT_ROOT%\.javafx\lib" (
    set "JAVAFX_HOME=%PROJECT_ROOT%\.javafx"
    exit /b 0
)

set "JAVAFX_CANDIDATE="
for /f "delims=" %%D in ('dir /b /ad /o-n "%PROJECT_ROOT%\\javafx-sdk-*" 2^>nul') do (
    if not defined JAVAFX_CANDIDATE set "JAVAFX_CANDIDATE=%PROJECT_ROOT%\%%D"
)
if defined JAVAFX_CANDIDATE if exist "%JAVAFX_CANDIDATE%\lib" (
    set "JAVAFX_HOME=%JAVAFX_CANDIDATE%"
    exit /b 0
)

set "JAVAFX_CANDIDATE="
for /f "delims=" %%D in ('dir /b /ad /o-n "%PROJECT_ROOT%\\..\\javafx-sdk-*" 2^>nul') do (
    if not defined JAVAFX_CANDIDATE set "JAVAFX_CANDIDATE=%PROJECT_ROOT%\..\%%D"
)
if defined JAVAFX_CANDIDATE if exist "%JAVAFX_CANDIDATE%\lib" (
    for %%I in ("%JAVAFX_CANDIDATE%") do set "JAVAFX_HOME=%%~fI"
    exit /b 0
)

echo [setup] JavaFX SDK not found.
echo [setup] Put javafx-sdk-25.0.2 next to setup.bat or set JAVAFX_HOME.
exit /b 1
