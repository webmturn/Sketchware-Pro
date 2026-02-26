# Firebase Library Download & Processing Script
# Downloads all required Firebase/Play Services libraries for Sketchware-Pro upgrade
# Target: Firebase BOM 33.7.0
#
# Usage: .\scripts\download-firebase-libs.ps1
# Prerequisites: Java (for d8 dexer)

param(
    [string]$OutputDir = ".\firebase-libs-upgrade",
    [string]$D8Path = "" # Path to d8.jar or d8.bat, auto-detected if empty
)

$ErrorActionPreference = "Stop"

# ============================================================
# Library definitions: [group, artifact, version, type]
# type: "aar" or "jar"
# ============================================================

$libraries = @(
    # ═══ Firebase libraries (upgraded) ═══
    @("com.google.firebase", "firebase-common", "21.0.0", "aar"),
    @("com.google.firebase", "firebase-components", "18.0.0", "aar"),
    @("com.google.firebase", "firebase-auth", "23.1.0", "aar"),
    @("com.google.firebase", "firebase-auth-interop", "20.0.0", "aar"),
    @("com.google.firebase", "firebase-database", "21.0.0", "aar"),
    @("com.google.firebase", "firebase-database-collection", "18.0.1", "aar"),
    @("com.google.firebase", "firebase-storage", "21.0.1", "aar"),
    @("com.google.firebase", "firebase-messaging", "24.1.0", "aar"),
    @("com.google.firebase", "firebase-iid-interop", "17.1.0", "aar"),
    @("com.google.firebase", "firebase-measurement-connector", "19.0.0", "aar"),

    # ═══ Firebase libraries (new) ═══
    @("com.google.firebase", "firebase-annotations", "16.2.0", "jar"),
    @("com.google.firebase", "firebase-appcheck", "17.1.0", "aar"),
    @("com.google.firebase", "firebase-appcheck-interop", "17.1.0", "aar"),
    @("com.google.firebase", "firebase-common-ktx", "21.0.0", "aar"),
    @("com.google.firebase", "firebase-datatransport", "18.2.0", "aar"),
    @("com.google.firebase", "firebase-encoders", "17.0.0", "jar"),
    @("com.google.firebase", "firebase-encoders-json", "18.0.0", "aar"),
    @("com.google.firebase", "firebase-encoders-proto", "16.0.0", "aar"),
    @("com.google.firebase", "firebase-installations", "17.2.0", "aar"),
    @("com.google.firebase", "firebase-installations-interop", "17.1.0", "aar"),

    # ═══ Play Services (upgraded) ═══
    @("com.google.android.gms", "play-services-base", "18.1.0", "aar"),
    @("com.google.android.gms", "play-services-basement", "18.3.0", "aar"),
    @("com.google.android.gms", "play-services-tasks", "18.1.0", "aar"),
    @("com.google.android.gms", "play-services-auth", "20.7.0", "aar"),
    @("com.google.android.gms", "play-services-auth-api-phone", "17.4.0", "aar"),
    @("com.google.android.gms", "play-services-auth-base", "18.0.4", "aar"),
    @("com.google.android.gms", "play-services-stats", "17.0.2", "aar"),

    # ═══ Play Services (new) ═══
    @("com.google.android.gms", "play-services-cloud-messaging", "17.2.0", "aar"),
    @("com.google.android.gms", "play-services-fido", "20.1.0", "aar"),

    # ═══ Transport (new) ═══
    @("com.google.android.datatransport", "transport-api", "3.1.0", "aar"),
    @("com.google.android.datatransport", "transport-runtime", "3.1.8", "aar"),
    @("com.google.android.datatransport", "transport-backend-cct", "3.1.8", "aar"),

    # ═══ Auth extra dependencies (new) ═══
    @("com.google.android.play", "integrity", "1.3.0", "aar"),
    @("com.google.android.play", "core-common", "2.0.3", "aar"),
    @("com.google.android.recaptcha", "recaptcha", "18.5.1", "aar")
)

# ============================================================
# Maven repository URLs
# ============================================================

function Get-MavenUrl {
    param([string]$Group, [string]$Artifact, [string]$Version, [string]$Type)
    $groupPath = $Group -replace '\.', '/'
    return "https://maven.google.com/$groupPath/$Artifact/$Version/$Artifact-$Version.$Type"
}

function Get-LibraryDirName {
    param([string]$Artifact, [string]$Version)
    return "$Artifact-$Version"
}

# ============================================================
# Main script
# ============================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Firebase Library Download Script" -ForegroundColor Cyan
Write-Host " Target: BOM 33.7.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Create output directories
$downloadDir = Join-Path $OutputDir "downloads"
$extractDir = Join-Path $OutputDir "extracted"
$jarsDir = Join-Path $OutputDir "jars"
$dexsDir = Join-Path $OutputDir "dexs"

New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null
New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
New-Item -ItemType Directory -Force -Path $jarsDir | Out-Null
New-Item -ItemType Directory -Force -Path $dexsDir | Out-Null

$total = $libraries.Count
$current = 0
$failed = @()

foreach ($lib in $libraries) {
    $group = $lib[0]
    $artifact = $lib[1]
    $version = $lib[2]
    $type = $lib[3]
    $current++

    $libName = Get-LibraryDirName $artifact $version
    $url = Get-MavenUrl $group $artifact $version $type
    $downloadFile = Join-Path $downloadDir "$libName.$type"

    Write-Host "[$current/$total] $libName" -ForegroundColor Yellow -NoNewline

    # Step 1: Download
    if (-not (Test-Path $downloadFile)) {
        try {
            Write-Host " downloading..." -NoNewline
            Invoke-WebRequest -Uri $url -OutFile $downloadFile -UseBasicParsing
            Write-Host " OK" -ForegroundColor Green -NoNewline
        } catch {
            Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
            $failed += $libName
            continue
        }
    } else {
        Write-Host " (cached)" -NoNewline
    }

    # Step 2: Extract classes.jar (for AAR) or copy (for JAR)
    $jarFile = Join-Path $jarsDir "$libName.jar"
    $libExtractDir = Join-Path $extractDir $libName

    if ($type -eq "aar") {
        if (-not (Test-Path $jarFile)) {
            New-Item -ItemType Directory -Force -Path $libExtractDir | Out-Null
            try {
                Expand-Archive -Path $downloadFile -DestinationPath $libExtractDir -Force
                $classesJar = Join-Path $libExtractDir "classes.jar"
                if (Test-Path $classesJar) {
                    Copy-Item $classesJar $jarFile
                    Write-Host " extracted" -ForegroundColor Green -NoNewline
                } else {
                    Write-Host " NO classes.jar!" -ForegroundColor Red
                    $failed += "$libName (no classes.jar)"
                    continue
                }
            } catch {
                Write-Host " extract failed: $($_.Exception.Message)" -ForegroundColor Red
                $failed += "$libName (extract)"
                continue
            }
        } else {
            Write-Host " (jar cached)" -NoNewline
        }
    } else {
        # JAR type - just copy
        if (-not (Test-Path $jarFile)) {
            Copy-Item $downloadFile $jarFile
            Write-Host " copied" -ForegroundColor Green -NoNewline
        }
    }

    # Step 3: Copy res/ and AndroidManifest.xml if present (for AAR)
    if ($type -eq "aar") {
        $resDir = Join-Path $libExtractDir "res"
        $manifestFile = Join-Path $libExtractDir "AndroidManifest.xml"
        $libJarsDir = Join-Path $jarsDir $libName

        if (Test-Path $resDir) {
            $resItems = Get-ChildItem $resDir -Recurse -File
            if ($resItems.Count -gt 0) {
                New-Item -ItemType Directory -Force -Path $libJarsDir | Out-Null
                Copy-Item $resDir (Join-Path $libJarsDir "res") -Recurse -Force
                Write-Host " +res" -NoNewline
            }
        }
        if (Test-Path $manifestFile) {
            if (-not (Test-Path $libJarsDir)) {
                New-Item -ItemType Directory -Force -Path $libJarsDir | Out-Null
            }
            Copy-Item $manifestFile (Join-Path $libJarsDir "AndroidManifest.xml") -Force
        }
    }

    Write-Host ""
}

# ============================================================
# Summary
# ============================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Download Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total libraries: $total"
Write-Host "Successful: $($total - $failed.Count)" -ForegroundColor Green
if ($failed.Count -gt 0) {
    Write-Host "Failed: $($failed.Count)" -ForegroundColor Red
    foreach ($f in $failed) {
        Write-Host "  - $f" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Output directories:" -ForegroundColor Yellow
Write-Host "  Downloads: $downloadDir"
Write-Host "  Extracted: $extractDir"
Write-Host "  JARs:      $jarsDir"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Generate DEX files for each JAR using d8"
Write-Host "  2. Replace files in app/src/main/assets/libs/libs.zip"
Write-Host "  3. Replace files in app/src/main/assets/libs/dexs.zip"
Write-Host ""
Write-Host "To generate DEX files, run:" -ForegroundColor Yellow
Write-Host "  .\scripts\generate-dex.ps1 -JarsDir '$jarsDir' -DexsDir '$dexsDir'"
