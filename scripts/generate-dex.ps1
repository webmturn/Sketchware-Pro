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

# Collect existing bundled JARs for classpath resolution
$existingLibsDir = Join-Path (Split-Path $JarsDir) "existing-libs"
$existingJars = @()
if (Test-Path $existingLibsDir) {
    $existingJars = Get-ChildItem $existingLibsDir -Recurse -Filter "classes.jar" -File
    Write-Host "Found $($existingJars.Count) existing bundled JARs for classpath" -ForegroundColor Green
}

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
        # Build args via temp file to avoid Windows command line length limit
        $argFile = Join-Path $env:TEMP "d8-args-$libName.txt"
        $argLines = @("--min-api", "21")
        if ($AndroidJar) {
            $argLines += "--lib"
            $argLines += $AndroidJar
        }
        foreach ($otherJar in $jarFiles) {
            if ($otherJar.FullName -ne $jar.FullName) {
                $argLines += "--classpath"
                $argLines += $otherJar.FullName
            }
        }
        foreach ($existingJar in $existingJars) {
            $argLines += "--classpath"
            $argLines += $existingJar.FullName
        }
        $argLines += "--output"
        $argLines += $dexDir
        $argLines += $jar.FullName
        # Write without BOM — d8 can't parse BOM-prefixed argfiles
        [System.IO.File]::WriteAllLines($argFile, $argLines)

        & $d8 "@$argFile" 2>&1 | Out-Null
        Remove-Item $argFile -Force -ErrorAction SilentlyContinue
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
