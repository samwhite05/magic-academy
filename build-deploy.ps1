#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building Magic Academy Plugins" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$env:JAVA_HOME = Join-Path $scriptDir ".tools\jdk21\jdk-21.0.10+7"
$gradleBat = Join-Path $scriptDir ".tools\gradle-8.11.1\bin\gradle.bat"

if (-not (Test-Path $env:JAVA_HOME)) {
    Write-Host "ERROR: JDK 21 not found at $env:JAVA_HOME" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $gradleBat)) {
    Write-Host "ERROR: Gradle not found at $gradleBat" -ForegroundColor Red
    exit 1
}

Write-Host "Building plugins with Gradle..." -ForegroundColor Yellow
& $gradleBat -p . shadowJar

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deploying to dev server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$pluginsDir = Join-Path $scriptDir "server\plugins"

if (-not (Test-Path $pluginsDir)) {
    Write-Host "ERROR: Plugins directory not found: $pluginsDir" -ForegroundColor Red
    exit 1
}

Write-Host "Copying JARs to server/plugins..." -ForegroundColor Yellow

# Clean old magic JARs from plugins folder first
$oldJars = Get-ChildItem -Path $pluginsDir -Filter "magic-*.jar" -ErrorAction SilentlyContinue
foreach ($jar in $oldJars) {
    Remove-Item $jar.FullName -Force
    Write-Host "  Removed old: $($jar.Name)" -ForegroundColor Gray
}

# Copy all shaded JARs (magic-*)
$moduleDirs = @(
    (Join-Path $scriptDir "magic-core\build\libs"),
    (Join-Path $scriptDir "magic-items\build\libs"),
    (Join-Path $scriptDir "magic-npcs\build\libs"),
    (Join-Path $scriptDir "magic-spells\build\libs"),
    (Join-Path $scriptDir "magic-dungeons\build\libs"),
    (Join-Path $scriptDir "magic-hideouts\build\libs"),
    (Join-Path $scriptDir "magic-academy\build\libs"),
    (Join-Path $scriptDir "magic-world\build\libs")
)

foreach ($dir in $moduleDirs) {
    if (Test-Path $dir) {
        $jars = Get-ChildItem -Path $dir -Filter "*-all.jar" -ErrorAction SilentlyContinue
        foreach ($jar in $jars) {
            Copy-Item $jar.FullName -Destination $pluginsDir -Force
            Write-Host "  Copied: $($jar.Name)" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "Plugins deployed:" -ForegroundColor Cyan
Get-ChildItem $pluginsDir -Filter "magic-*.jar" | ForEach-Object { Write-Host "  $($_.Name)" -ForegroundColor Gray }

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Build and deploy complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Run 'server\run.bat' to start the dev server" -ForegroundColor Yellow
