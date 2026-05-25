@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "BUILD_ONLY=false"
if /I "%~1"=="--build-only" set "BUILD_ONLY=true"

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
cd /d "%PROJECT_ROOT%" || exit /b 1

set "SRC_DIR=%PROJECT_ROOT%\src"
set "BIN_DIR=%PROJECT_ROOT%\bin"
set "LIB_DIR=%PROJECT_ROOT%\lib"
set "DATA_DIR=%PROJECT_ROOT%\data"
set "AUDIO_DIR=%PROJECT_ROOT%\audio"
set "SOURCES_LIST=%PROJECT_ROOT%\.sources.list"

set "POSTGRES_JAR=%LIB_DIR%\postgresql-42.7.4.jar"
set "MYSQL_JAR=%LIB_DIR%\mysql-connector-j-9.3.0.jar"
set "MAIL_API_JAR=%LIB_DIR%\jakarta.mail-api-2.1.5.jar"
set "ANGUS_MAIL_JAR=%LIB_DIR%\angus-mail-2.0.5.jar"
set "ACTIVATION_API_JAR=%LIB_DIR%\jakarta.activation-api-2.1.4.jar"
set "ANGUS_ACTIVATION_JAR=%LIB_DIR%\angus-activation-2.0.3.jar"

call :resolve_java_home
if errorlevel 1 exit /b 1

call :resolve_javafx_home
if errorlevel 1 exit /b 1
set "JAVAFX_LIB=%JAVAFX_HOME%\lib"

call :check_required_jar "%LIB_DIR%\gson-2.10.1.jar"
call :check_required_jar "%LIB_DIR%\sqlite-jdbc-3.45.1.0.jar"
call :check_required_jar "%LIB_DIR%\slf4j-api-2.0.12.jar"
call :check_required_jar "%LIB_DIR%\slf4j-nop-2.0.12.jar"
call :check_required_jar "%MAIL_API_JAR%"
call :check_required_jar "%ANGUS_MAIL_JAR%"
call :check_required_jar "%ACTIVATION_API_JAR%"
call :check_required_jar "%ANGUS_ACTIVATION_JAR%"
if errorlevel 1 exit /b 1

set "CLASSPATH=%LIB_DIR%\gson-2.10.1.jar;%LIB_DIR%\sqlite-jdbc-3.45.1.0.jar;%LIB_DIR%\slf4j-api-2.0.12.jar;%LIB_DIR%\slf4j-nop-2.0.12.jar;%MAIL_API_JAR%;%ANGUS_MAIL_JAR%;%ACTIVATION_API_JAR%;%ANGUS_ACTIVATION_JAR%"
if exist "%POSTGRES_JAR%" set "CLASSPATH=%CLASSPATH%;%POSTGRES_JAR%"
if exist "%MYSQL_JAR%" set "CLASSPATH=%CLASSPATH%;%MYSQL_JAR%"

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"
if not exist "%AUDIO_DIR%" mkdir "%AUDIO_DIR%"

echo [run] Using Java: %JAVA_EXE%
echo [run] Using JavaFX: %JAVAFX_LIB%
echo [run] Compiling sources...

> "%SOURCES_LIST%" (
    for /r "%SRC_DIR%" %%F in (*.java) do echo "%%~fF"
)

findstr /r /c:"." "%SOURCES_LIST%" >nul
if errorlevel 1 (
    echo [run] No Java sources found in src\.
    call :cleanup
    exit /b 1
)

"%JAVAC_EXE%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media ^
  -cp "%CLASSPATH%" ^
  -d "%BIN_DIR%" ^
  @"%SOURCES_LIST%"
if errorlevel 1 (
    call :cleanup
    exit /b 1
)

if not exist "%BIN_DIR%\view" mkdir "%BIN_DIR%\view"
if not exist "%BIN_DIR%\resources" mkdir "%BIN_DIR%\resources"

copy /Y "%SRC_DIR%\view\*.fxml" "%BIN_DIR%\view\" >nul
if errorlevel 1 (
    echo [run] Failed to copy FXML files.
    call :cleanup
    exit /b 1
)

xcopy "%SRC_DIR%\resources" "%BIN_DIR%\resources\" /E /I /Y >nul
if errorlevel 1 if not errorlevel 2 (
    echo [run] Failed to copy resources.
    call :cleanup
    exit /b 1
)

if /I "%BUILD_ONLY%"=="true" (
    call :cleanup
    echo [run] Build complete (build-only mode).
    exit /b 0
)

echo [run] Starting app...
"%JAVA_EXE%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media ^
  --enable-native-access=javafx.graphics ^
  -cp "%BIN_DIR%;%CLASSPATH%" ^
  app.MainApp
set "EXIT_CODE=%ERRORLEVEL%"

call :cleanup
exit /b %EXIT_CODE%

:cleanup
if exist "%SOURCES_LIST%" del /q "%SOURCES_LIST%" >nul 2>nul
exit /b 0

:check_required_jar
if not exist "%~1" (
    echo [run] Required jar missing: %~nx1
    echo [run] Run setup.bat first.
    exit /b 1
)
exit /b 0

:resolve_java_home
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
        exit /b 0
    )
)

for /f "delims=" %%F in ('where javac 2^>nul') do (
    set "JAVAC_EXE=%%~fF"
    goto :derive_java_home
)

call :search_java_home
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
    exit /b 0
)

echo [run] JDK not found. Install full JDK 21+ and make sure javac.exe exists.
exit /b 1

:derive_java_home
for %%I in ("%JAVAC_EXE%") do set "JAVA_BIN=%%~dpI"
for %%I in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fI"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVAC_EXE%" if exist "%JAVA_EXE%" exit /b 0
echo [run] Invalid JAVA_HOME detected.
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

echo [run] JavaFX SDK not found.
echo [run] Put javafx-sdk-25.0.2 next to run.bat or set JAVAFX_HOME.
exit /b 1
