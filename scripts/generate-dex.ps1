# DEX Generation Script for Firebase Library Upgrade
# Generates .dex files from .jar files using d8
#
# Usage: .\scripts\generate-dex.ps1 [-JarsDir path] [-DexsDir path]
# Prerequisites: Android SDK with d8 (build-tools)

param(
    [string]$JarsDir = ".\firebase-libs-upgrade\jars",
    [string]$DexsDir = ".\firebase-libs-upgrade\dexs",
    [string]$AndroidJar = "" # Path to android.jar, auto-detected if empty
)

$ErrorActionPreference = "Stop"

# Auto-detect Android SDK
if (-not $AndroidJar) {
    $sdkRoot = $env:ANDROID_HOME
    if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
    if (-not $sdkRoot) { $sdkRoot = "$env:LOCALAPPDATA\Android\Sdk" }
    
    if (Test-Path $sdkRoot) {
        $platforms = Get-ChildItem (Join-Path $sdkRoot "platforms") -Directory | Sort-Object Name -Descending | Select-Object -First 1
        if ($platforms) {
            $AndroidJar = Join-Path $platforms.FullName "android.jar"
        }
    }
    
    if (-not (Test-Path $AndroidJar)) {
        Write-Host "WARNING: android.jar not found. DEX generation may fail for some libraries." -ForegroundColor Yellow
        Write-Host "Set ANDROID_HOME or pass -AndroidJar parameter." -ForegroundColor Yellow
        $AndroidJar = $null
    } else {
        Write-Host "Using android.jar: $AndroidJar" -ForegroundColor Green
    }
}

# Auto-detect d8
$d8 = $null
$sdkRoot2 = $env:ANDROID_HOME
if (-not $sdkRoot2) { $sdkRoot2 = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot2) { $sdkRoot2 = "$env:LOCALAPPDATA\Android\Sdk" }

if (Test-Path $sdkRoot2) {
    $buildTools = Get-ChildItem (Join-Path $sdkRoot2 "build-tools") -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if ($buildTools) {
        $d8Bat = Join-Path $buildTools.FullName "d8.bat"
        if (Test-Path $d8Bat) {
            $d8 = $d8Bat
        }
    }
}

if (-not $d8) {
    Write-Host "ERROR: d8 not found. Install Android SDK build-tools." -ForegroundColor Red
    exit 1
}

Write-Host "Using d8: $d8" -ForegroundColor Green
Write-Host ""

New-Item -ItemType Directory -Force -Path $DexsDir | Out-Null

$jarFiles = Get-ChildItem $JarsDir -Filter "*.jar" -File
$total = $jarFiles.Count
$current = 0
$failed = @()

foreach ($jar in $jarFiles) {
    $current++
    $libName = $jar.BaseName
    $dexDir = Join-Path $DexsDir $libName
    $dexFile = Join-Path $dexDir "classes.dex"

    Write-Host "[$current/$total] $libName" -ForegroundColor Yellow -NoNewline

    if (Test-Path $dexFile) {
        Write-Host " (cached)" -ForegroundColor Gray
        continue
    }

    New-Item -ItemType Directory -Force -Path $dexDir | Out-Null

    try {
        $d8Args = @("--output", $dexDir, $jar.FullName)
        if ($AndroidJar) {
            $d8Args = @("--lib", $AndroidJar) + $d8Args
        }
        $d8Args = @("--min-api", "21") + $d8Args

        $result = & $d8 @d8Args 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "d8 exited with code $LASTEXITCODE"
        }
        Write-Host " OK" -ForegroundColor Green
    } catch {
        Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $failed += $libName
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " DEX Generation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total: $total"
Write-Host "Successful: $($total - $failed.Count)" -ForegroundColor Green
if ($failed.Count -gt 0) {
    Write-Host "Failed: $($failed.Count)" -ForegroundColor Red
    foreach ($f in $failed) { Write-Host "  - $f" -ForegroundColor Red }
}

Write-Host ""
Write-Host "Next step: Run pack-assets.ps1 to create libs.zip and dexs.zip" -ForegroundColor Yellow
