param(
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$SrcDir = Join-Path $ProjectRoot "src"
$BinDir = Join-Path $ProjectRoot "bin"
$LibDir = Join-Path $ProjectRoot "lib"
$PostgresJar = Join-Path $LibDir "postgresql-42.7.4.jar"
$MysqlJar = Join-Path $LibDir "mysql-connector-j-9.3.0.jar"
$MailApiJar = Join-Path $LibDir "jakarta.mail-api-2.1.5.jar"
$AngusMailJar = Join-Path $LibDir "angus-mail-2.0.5.jar"
$ActivationApiJar = Join-Path $LibDir "jakarta.activation-api-2.1.4.jar"
$AngusActivationJar = Join-Path $LibDir "angus-activation-2.0.3.jar"

function Resolve-JavaFxLib {
    if ($env:JAVAFX_HOME) {
        $candidate = Join-Path $env:JAVAFX_HOME "lib"
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    $linked = Join-Path $ProjectRoot ".javafx\lib"
    if (Test-Path $linked) {
        return (Resolve-Path $linked).Path
    }

    $candidate = Get-ChildItem -Path $ProjectRoot -Directory -Filter "javafx-sdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name |
        Select-Object -Last 1
    if ($candidate) {
        $libPath = Join-Path $candidate.FullName "lib"
        if (Test-Path $libPath) {
            return $libPath
        }
    }

    return $null
}

$JavaFxLib = Resolve-JavaFxLib
if (-not $JavaFxLib) {
    Write-Host "[run] JavaFX SDK not found." -ForegroundColor Red
    Write-Host "[run] Set JAVAFX_HOME or place javafx-sdk-<version> in project root."
    exit 1
}

$RequiredJars = @(
    (Join-Path $LibDir "gson-2.10.1.jar"),
    (Join-Path $LibDir "sqlite-jdbc-3.45.1.0.jar"),
    (Join-Path $LibDir "slf4j-api-2.0.12.jar"),
    (Join-Path $LibDir "slf4j-nop-2.0.12.jar"),
    $MailApiJar,
    $AngusMailJar,
    $ActivationApiJar,
    $AngusActivationJar
)

$MissingJars = $RequiredJars | Where-Object { -not (Test-Path $_) }
if ($MissingJars.Count -gt 0) {
    Write-Host "[run] Required jars missing in lib/." -ForegroundColor Red
    Write-Host "[run] Run scripts\setup.ps1 first."
    exit 1
}

$ClassPathEntries = @()
$ClassPathEntries += $RequiredJars | ForEach-Object { (Resolve-Path $_).Path }
if (Test-Path $PostgresJar) {
    $ClassPathEntries += (Resolve-Path $PostgresJar).Path
}
if (Test-Path $MysqlJar) {
    $ClassPathEntries += (Resolve-Path $MysqlJar).Path
}
$ClassPath = [string]::Join(";", $ClassPathEntries)

New-Item -ItemType Directory -Force -Path $BinDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ProjectRoot "data") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ProjectRoot "audio") | Out-Null

Write-Host "[run] Compiling sources..."
$Sources = Get-ChildItem -Path $SrcDir -Recurse -Filter "*.java" | Sort-Object FullName
if ($Sources.Count -eq 0) {
    Write-Host "[run] No Java sources found in src/." -ForegroundColor Red
    exit 1
}

$SourcesList = Join-Path $ProjectRoot ".sources.list"
$Sources.FullName | Set-Content -Path $SourcesList -Encoding UTF8
$SourcesArg = "@$SourcesList"

& javac `
    --module-path $JavaFxLib `
    --add-modules javafx.controls,javafx.fxml,javafx.media `
    -cp $ClassPath `
    -d $BinDir `
    $SourcesArg
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path (Join-Path $BinDir "view") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $BinDir "resources") | Out-Null
Copy-Item -Path (Join-Path $SrcDir "view\*.fxml") -Destination (Join-Path $BinDir "view") -Force
Copy-Item -Path (Join-Path $SrcDir "resources\*") -Destination (Join-Path $BinDir "resources") -Recurse -Force

if ($BuildOnly) {
    Remove-Item -Path $SourcesList -ErrorAction SilentlyContinue
    Write-Host "[run] Build complete (build-only mode)."
    exit 0
}

Write-Host "[run] Starting app..."
$RunClassPath = "{0};{1}" -f (Resolve-Path $BinDir).Path, $ClassPath
& java `
    --module-path $JavaFxLib `
    --add-modules javafx.controls,javafx.fxml,javafx.media `
    --enable-native-access=javafx.graphics `
    -cp $RunClassPath `
    app.MainApp
$ExitCode = $LASTEXITCODE

Remove-Item -Path $SourcesList -ErrorAction SilentlyContinue
exit $ExitCode
