# Pack Assets Script for Firebase Library Upgrade
# Creates libs.zip and dexs.zip from processed library files
#
# Usage: .\scripts\pack-assets.ps1

param(
    [string]$JarsDir = ".\firebase-libs-upgrade\jars",
    [string]$DexsDir = ".\firebase-libs-upgrade\dexs",
    [string]$AssetsDir = ".\app\src\main\assets\libs"
)

$ErrorActionPreference = "Stop"

# Current libs.zip structure: each library is a folder named like "firebase-auth-19.0.0"
# containing classes.jar and optionally res/, AndroidManifest.xml, etc.
# Current dexs.zip structure: each library is a folder containing classes.dex

$libsZip = Join-Path $AssetsDir "libs.zip"
$dexsZip = Join-Path $AssetsDir "dexs.zip"

# Libraries to REMOVE from existing zips
$removeLibs = @(
    "firebase-auth-19.0.0",
    "firebase-auth-interop-18.0.0",
    "firebase-common-19.3.0",
    "firebase-components-16.0.0",
    "firebase-database-19.3.1",
    "firebase-database-collection-17.0.1",
    "firebase-iid-19.0.0",
    "firebase-iid-interop-17.0.0",
    "firebase-measurement-connector-18.0.0",
    "firebase-messaging-19.0.0",
    "firebase-storage-19.0.0",
    "play-services-base-18.0.0",
    "play-services-basement-18.0.0",
    "play-services-tasks-18.0.1",
    "play-services-auth-19.0.0",
    "play-services-auth-api-phone-17.0.0",
    "play-services-auth-base-17.0.0",
    "play-services-stats-17.0.0"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pack Assets Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Backup existing zips
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
if (Test-Path $libsZip) {
    Copy-Item $libsZip "$libsZip.backup-$timestamp"
    Write-Host "Backed up libs.zip" -ForegroundColor Green
}
if (Test-Path $dexsZip) {
    Copy-Item $dexsZip "$dexsZip.backup-$timestamp"
    Write-Host "Backed up dexs.zip" -ForegroundColor Green
}

# Step 2: Extract existing zips to temp directories
$tempLibs = Join-Path $env:TEMP "firebase-upgrade-libs"
$tempDexs = Join-Path $env:TEMP "firebase-upgrade-dexs"

if (Test-Path $tempLibs) { Remove-Item $tempLibs -Recurse -Force }
if (Test-Path $tempDexs) { Remove-Item $tempDexs -Recurse -Force }

Write-Host "Extracting existing libs.zip..." -ForegroundColor Yellow
Expand-Archive -Path $libsZip -DestinationPath $tempLibs -Force
Write-Host "Extracting existing dexs.zip..." -ForegroundColor Yellow
Expand-Archive -Path $dexsZip -DestinationPath $tempDexs -Force

# Step 3: Remove old libraries
Write-Host ""
Write-Host "Removing old library versions..." -ForegroundColor Yellow
foreach ($lib in $removeLibs) {
    $libPath = Join-Path $tempLibs $lib
    $dexPath = Join-Path $tempDexs $lib
    if (Test-Path $libPath) {
        Remove-Item $libPath -Recurse -Force
        Write-Host "  Removed libs/$lib" -ForegroundColor Gray
    }
    if (Test-Path $dexPath) {
        Remove-Item $dexPath -Recurse -Force
        Write-Host "  Removed dexs/$lib" -ForegroundColor Gray
    }
}

# Step 4: Add new/upgraded libraries
Write-Host ""
Write-Host "Adding new/upgraded libraries..." -ForegroundColor Yellow

$jarFiles = Get-ChildItem $JarsDir -Filter "*.jar" -File
foreach ($jar in $jarFiles) {
    $libName = $jar.BaseName
    $targetLibDir = Join-Path $tempLibs $libName

    # Create library directory and copy classes.jar
    New-Item -ItemType Directory -Force -Path $targetLibDir | Out-Null
    Copy-Item $jar.FullName (Join-Path $targetLibDir "classes.jar") -Force

    # Copy res/ and AndroidManifest.xml if they exist
    $libResDir = Join-Path $JarsDir $libName
    if (Test-Path $libResDir) {
        $resSubDir = Join-Path $libResDir "res"
        $manifestFile = Join-Path $libResDir "AndroidManifest.xml"
        if (Test-Path $resSubDir) {
            Copy-Item $resSubDir (Join-Path $targetLibDir "res") -Recurse -Force
        }
        if (Test-Path $manifestFile) {
            Copy-Item $manifestFile (Join-Path $targetLibDir "AndroidManifest.xml") -Force
        }
    }

    Write-Host "  Added libs/$libName" -ForegroundColor Green
}

$dexDirs = Get-ChildItem $DexsDir -Directory
foreach ($dex in $dexDirs) {
    $libName = $dex.Name
    $targetDexDir = Join-Path $tempDexs $libName

    New-Item -ItemType Directory -Force -Path $targetDexDir | Out-Null
    Copy-Item (Join-Path $dex.FullName "classes.dex") (Join-Path $targetDexDir "classes.dex") -Force

    Write-Host "  Added dexs/$libName" -ForegroundColor Green
}

# Step 5: Repack zips
Write-Host ""
Write-Host "Repacking libs.zip..." -ForegroundColor Yellow
Remove-Item $libsZip -Force
Compress-Archive -Path "$tempLibs\*" -DestinationPath $libsZip -CompressionLevel Optimal

Write-Host "Repacking dexs.zip..." -ForegroundColor Yellow
Remove-Item $dexsZip -Force
Compress-Archive -Path "$tempDexs\*" -DestinationPath $dexsZip -CompressionLevel Optimal

# Cleanup
Remove-Item $tempLibs -Recurse -Force
Remove-Item $tempDexs -Recurse -Force

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Done!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "libs.zip: $('{0:N2}' -f ((Get-Item $libsZip).Length / 1MB)) MB"
Write-Host "dexs.zip: $('{0:N2}' -f ((Get-Item $dexsZip).Length / 1MB)) MB"
Write-Host ""
Write-Host "Backups saved with suffix: .backup-$timestamp" -ForegroundColor Gray
Write-Host ""
Write-Host "Run 'gradlew assembleDebug' to verify the build." -ForegroundColor Yellow
