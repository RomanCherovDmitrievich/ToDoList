param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$LibDir = Join-Path $ProjectRoot "lib"
New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

Write-Host "[setup] Project: $ProjectRoot"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[setup] Java not found in PATH. Install JDK 21+ first." -ForegroundColor Red
    exit 1
}

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host "[setup] javac not found in PATH. Install full JDK, not JRE." -ForegroundColor Red
    exit 1
}

Write-Host "[setup] Java: $((java -version) 2>&1 | Select-Object -First 1)"

function Download-IfMissing {
    param(
        [string]$FilePath,
        [string]$Url
    )

    if (Test-Path $FilePath) {
        Write-Host "[setup] OK: $([System.IO.Path]::GetFileName($FilePath))"
        return
    }

    Write-Host "[setup] Downloading $([System.IO.Path]::GetFileName($FilePath))"
    try {
        Invoke-WebRequest -Uri $Url -OutFile $FilePath
    } catch {
        Write-Host "[setup] Failed to download $([System.IO.Path]::GetFileName($FilePath))." -ForegroundColor Yellow
        Write-Host "[setup] Download manually: $Url"
    }
}

Download-IfMissing (Join-Path $LibDir "gson-2.10.1.jar") "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
Download-IfMissing (Join-Path $LibDir "sqlite-jdbc-3.45.1.0.jar") "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"
Download-IfMissing (Join-Path $LibDir "slf4j-api-2.0.12.jar") "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar"
Download-IfMissing (Join-Path $LibDir "slf4j-nop-2.0.12.jar") "https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.12/slf4j-nop-2.0.12.jar"
Download-IfMissing (Join-Path $LibDir "postgresql-42.7.4.jar") "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar"
Download-IfMissing (Join-Path $LibDir "jakarta.mail-api-2.1.5.jar") "https://repo1.maven.org/maven2/jakarta/mail/jakarta.mail-api/2.1.5/jakarta.mail-api-2.1.5.jar"
Download-IfMissing (Join-Path $LibDir "angus-mail-2.0.5.jar") "https://repo1.maven.org/maven2/org/eclipse/angus/angus-mail/2.0.5/angus-mail-2.0.5.jar"
Download-IfMissing (Join-Path $LibDir "jakarta.activation-api-2.1.4.jar") "https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.4/jakarta.activation-api-2.1.4.jar"
Download-IfMissing (Join-Path $LibDir "angus-activation-2.0.3.jar") "https://repo1.maven.org/maven2/org/eclipse/angus/angus-activation/2.0.3/angus-activation-2.0.3.jar"

if ($env:JAVAFX_HOME -and (Test-Path (Join-Path $env:JAVAFX_HOME "lib"))) {
    Write-Host "[setup] JavaFX SDK: $env:JAVAFX_HOME\lib"
} else {
    $Candidate = Get-ChildItem -Path $ProjectRoot -Directory -Filter "javafx-sdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name |
        Select-Object -Last 1
    if ($Candidate -and (Test-Path (Join-Path $Candidate.FullName "lib"))) {
        Write-Host "[setup] JavaFX SDK: $($Candidate.FullName)\lib"
    } else {
        Write-Host "[setup] JavaFX SDK not found." -ForegroundColor Yellow
        Write-Host "[setup] Set JAVAFX_HOME=C:\path\to\javafx-sdk or unpack javafx-sdk-<version> into project root."
    }
}

New-Item -ItemType Directory -Force -Path (Join-Path $ProjectRoot "data") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ProjectRoot "audio") | Out-Null

Write-Host "[setup] Done."
Write-Host "[setup] Next: powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1"
