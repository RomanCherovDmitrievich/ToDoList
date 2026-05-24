param()

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSCommandPath
Set-Location $ProjectRoot

$SrcDir = Join-Path $ProjectRoot "src"
$BinDir = Join-Path $ProjectRoot "bin"
$TestsDir = Join-Path $ProjectRoot "tests"
$ClassesDir = Join-Path $TestsDir "classes"
$ReportsDir = Join-Path $TestsDir "reports"
$LibDir = Join-Path $ProjectRoot "lib"
$PostgresJar = Join-Path $LibDir "postgresql-42.7.4.jar"
$MysqlJar = Join-Path $LibDir "mysql-connector-j-9.3.0.jar"
$MailApiJar = Join-Path $LibDir "jakarta.mail-api-2.1.5.jar"
$AngusMailJar = Join-Path $LibDir "angus-mail-2.0.5.jar"
$ActivationApiJar = Join-Path $LibDir "jakarta.activation-api-2.1.4.jar"
$AngusActivationJar = Join-Path $LibDir "angus-activation-2.0.3.jar"

$JUnitVersion = "1.9.2"
$JUnitDir = Join-Path $TestsDir "lib"
$JUnitJar = Join-Path $JUnitDir "junit-platform-console-standalone-$JUnitVersion.jar"

New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null
New-Item -ItemType Directory -Force -Path $ReportsDir | Out-Null
New-Item -ItemType Directory -Force -Path $JUnitDir | Out-Null

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
    Write-Host "[tests] JavaFX SDK not found. Set JAVAFX_HOME or place javafx-sdk-<version> in project root." -ForegroundColor Red
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
    Write-Host "[tests] Required jars missing in lib/. Run scripts\setup.ps1 first." -ForegroundColor Red
    exit 1
}

$AppClassPathEntries = $RequiredJars | ForEach-Object { (Resolve-Path $_).Path }
if (Test-Path $PostgresJar) {
    $AppClassPathEntries += (Resolve-Path $PostgresJar).Path
}
if (Test-Path $MysqlJar) {
    $AppClassPathEntries += (Resolve-Path $MysqlJar).Path
}
$AppClassPath = [string]::Join(";", $AppClassPathEntries)

if (-not (Test-Path $JUnitJar)) {
    Write-Host "[tests] Downloading JUnit console..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUnitVersion/junit-platform-console-standalone-$JUnitVersion.jar" -OutFile $JUnitJar
}

& (Join-Path $ProjectRoot "scripts\run.ps1") -BuildOnly
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$TestSources = Get-ChildItem -Path $TestsDir -Recurse -Filter "*Test.java" | Sort-Object FullName
if ($TestSources.Count -eq 0) {
    Write-Host "[tests] No test sources found in tests/." -ForegroundColor Red
    exit 1
}

$TestSourcesList = Join-Path $ReportsDir "test-sources.list"
$TestSources.FullName | Set-Content -Path $TestSourcesList -Encoding UTF8
$TestSourcesArg = "@$TestSourcesList"

Write-Host "[tests] Compiling tests..."
& javac `
    --module-path $JavaFxLib `
    --add-modules javafx.controls,javafx.fxml,javafx.media `
    -cp "$AppClassPath;$ClassesDir;$JUnitJar" `
    -d $ClassesDir `
    $TestSourcesArg
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ResultsFile = Join-Path $ReportsDir "test_results_$Timestamp.txt"
$XmlReportDir = Join-Path $ReportsDir "junit-xml-$Timestamp"
$HtmlReportFile = Join-Path $ReportsDir "test_report_$Timestamp.html"
$RuntimeDataDir = Join-Path $ReportsDir "runtime-data-$Timestamp"
New-Item -ItemType Directory -Force -Path $RuntimeDataDir | Out-Null
New-Item -ItemType Directory -Force -Path $XmlReportDir | Out-Null

Write-Host "[tests] Running JUnit..."
$JavaOutput = & java `
    --module-path $JavaFxLib `
    --add-modules javafx.controls,javafx.fxml,javafx.media `
    --enable-native-access=javafx.graphics `
    -Djava.awt.headless=true `
    -Dtodolist.disableAudio=true `
    -Dtodolist.email.mode=disabled `
    -Dtodolist.dataDir="$RuntimeDataDir" `
    -cp "$AppClassPath;$ClassesDir;$JUnitJar" `
    org.junit.platform.console.ConsoleLauncher `
    --scan-class-path `
    --class-path "$AppClassPath;$ClassesDir" `
    --details=tree `
    --disable-banner `
    --reports-dir "$XmlReportDir" 2>&1
$ExitCode = $LASTEXITCODE
$JavaOutput | Tee-Object -FilePath $ResultsFile | Out-Host

& java -cp $BinDir util.TestReportGenerator $XmlReportDir $HtmlReportFile $ResultsFile

Write-Host "[tests] Results: $ResultsFile"
Write-Host "[tests] HTML report: $HtmlReportFile"
exit $ExitCode
