# Fix zip path separators (backslash -> forward slash) and add missing res/ directories
# This script processes libs.zip and dexs.zip to be compatible with Android/Linux
# Uses a single zip-to-zip copy pass to avoid unreliable ZipArchive Update mode

param(
    [string]$AssetsDir = ".\app\src\main\assets\libs"
)

$ErrorActionPreference = "Stop"
Add-Type -Assembly System.IO.Compression
Add-Type -Assembly System.IO.Compression.FileSystem

# Libraries that need res/ directories extracted from their AARs
$librariesNeedingRes = @(
    @{ LibName = "play-services-base-18.1.0"; GroupId = "com.google.android.gms"; ArtifactId = "play-services-base"; Version = "18.1.0" }
)

# Step 1: Download AAR resources for libraries that need them
Write-Host "=== Downloading AAR resources ==="
$aarResources = @{}  # LibName -> extracted AAR directory path

foreach ($lib in $librariesNeedingRes) {
    $tempDir = "$env:TEMP\aar-$($lib.ArtifactId)"
    if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
    New-Item -ItemType Directory $tempDir | Out-Null
    
    $groupPath = $lib.GroupId.Replace('.', '/')
    $url = "https://dl.google.com/dl/android/maven2/$groupPath/$($lib.ArtifactId)/$($lib.Version)/$($lib.ArtifactId)-$($lib.Version).aar"
    Write-Host "Downloading $($lib.ArtifactId)-$($lib.Version).aar..."
    Invoke-WebRequest -Uri $url -OutFile "$tempDir\lib.aar"
    Copy-Item "$tempDir\lib.aar" "$tempDir\lib.zip"
    Expand-Archive "$tempDir\lib.zip" "$tempDir\aar" -Force
    $aarResources[$lib.LibName] = "$tempDir\aar"
    Write-Host "  Extracted to $tempDir\aar"
}

# Step 2: Fix libs.zip - copy entries with forward slashes and inject missing resources
Write-Host ""
Write-Host "=== Rebuilding libs.zip ==="
$libsZip = Join-Path $AssetsDir "libs.zip"
$libsTmp = "$libsZip.tmp"
if (Test-Path $libsTmp) { Remove-Item $libsTmp -Force }

$srcZip = [IO.Compression.ZipFile]::OpenRead($libsZip)
$dstZip = [IO.Compression.ZipFile]::Open($libsTmp, [IO.Compression.ZipArchiveMode]::Create)

# Track which libraries we've seen to know when to inject extra entries
$injectedLibs = @{}
$copiedCount = 0

foreach ($entry in $srcZip.Entries) {
    $fixedName = $entry.FullName.Replace('\', '/')
    
    $newEntry = $dstZip.CreateEntry($fixedName, [IO.Compression.CompressionLevel]::Optimal)
    $newEntry.LastWriteTime = $entry.LastWriteTime
    if ($entry.Length -gt 0) {
        $srcStream = $entry.Open()
        $dstStream = $newEntry.Open()
        $srcStream.CopyTo($dstStream)
        $dstStream.Dispose()
        $srcStream.Dispose()
    }
    $copiedCount++
    
    # Check if this entry belongs to a library that needs res/ injection
    foreach ($lib in $librariesNeedingRes) {
        $prefix = $lib.LibName + '/'
        if ($fixedName.StartsWith($prefix) -and -not $injectedLibs.ContainsKey($lib.LibName)) {
            # Check if this library already has res/ entries in the source zip
            $hasRes = ($srcZip.Entries | Where-Object { 
                $n = $_.FullName.Replace('\', '/')
                $n -eq "$($lib.LibName)/res/" -or $n -like "$($lib.LibName)/res/*"
            }).Count -gt 0
            
            if (-not $hasRes -and $aarResources.ContainsKey($lib.LibName)) {
                $aarDir = $aarResources[$lib.LibName]
                $injected = 0
                
                # Inject res/ directory and files
                $resDir = Join-Path $aarDir 'res'
                if (Test-Path $resDir) {
                    $resBase = (Get-Item $resDir).FullName
                    if (-not $resBase.EndsWith('\')) { $resBase += '\' }
                    Get-ChildItem $resDir -Recurse | ForEach-Object {
                        $rel = $_.FullName.Substring($resBase.Length).Replace('\', '/')
                        $eName = "$($lib.LibName)/res/$rel"
                        if ($_.PSIsContainer) {
                            $null = $dstZip.CreateEntry("$eName/")
                        } else {
                            $e = $dstZip.CreateEntry($eName, [IO.Compression.CompressionLevel]::Optimal)
                            $s = $e.Open()
                            $f = [IO.File]::OpenRead($_.FullName)
                            $f.CopyTo($s)
                            $f.Dispose()
                            $s.Dispose()
                        }
                        $injected++
                    }
                }
                
                # Inject R.txt and proguard.txt
                foreach ($extra in @('R.txt', 'proguard.txt')) {
                    $fp = Join-Path $aarDir $extra
                    if (Test-Path $fp) {
                        $existsInSrc = ($srcZip.Entries | Where-Object { $_.FullName.Replace('\','/') -eq "$($lib.LibName)/$extra" }).Count -gt 0
                        if (-not $existsInSrc) {
                            $e = $dstZip.CreateEntry("$($lib.LibName)/$extra", [IO.Compression.CompressionLevel]::Optimal)
                            $s = $e.Open()
                            $f = [IO.File]::OpenRead($fp)
                            $f.CopyTo($s)
                            $f.Dispose()
                            $s.Dispose()
                            $injected++
                        }
                    }
                }
                
                Write-Host "  Injected $injected entries for $($lib.LibName)"
            }
            $injectedLibs[$lib.LibName] = $true
        }
    }
}

$srcZip.Dispose()
$dstZip.Dispose()

Remove-Item $libsZip -Force
Move-Item $libsTmp $libsZip -Force
Write-Host "libs.zip: copied $copiedCount entries + injected resources"

# Step 3: Fix dexs.zip (just fix backslash paths)
Write-Host ""
Write-Host "=== Rebuilding dexs.zip ==="
$dexsZip = Join-Path $AssetsDir "dexs.zip"
$dexsTmp = "$dexsZip.tmp"
if (Test-Path $dexsTmp) { Remove-Item $dexsTmp -Force }

$srcZip = [IO.Compression.ZipFile]::OpenRead($dexsZip)
$dstZip = [IO.Compression.ZipFile]::Open($dexsTmp, [IO.Compression.ZipArchiveMode]::Create)
$dexCount = 0
foreach ($entry in $srcZip.Entries) {
    $fixedName = $entry.FullName.Replace('\', '/')
    $newEntry = $dstZip.CreateEntry($fixedName, [IO.Compression.CompressionLevel]::Optimal)
    $newEntry.LastWriteTime = $entry.LastWriteTime
    if ($entry.Length -gt 0) {
        $srcStream = $entry.Open()
        $dstStream = $newEntry.Open()
        $srcStream.CopyTo($dstStream)
        $dstStream.Dispose()
        $srcStream.Dispose()
    }
    $dexCount++
}
$srcZip.Dispose()
$dstZip.Dispose()
Remove-Item $dexsZip -Force
Move-Item $dexsTmp $dexsZip -Force
Write-Host "dexs.zip: fixed $dexCount entries"

# Step 4: Cleanup AAR temp dirs
foreach ($dir in $aarResources.Values) {
    $parent = Split-Path $dir -Parent
    if (Test-Path $parent) { Remove-Item $parent -Recurse -Force }
}

# Step 5: Verify
Write-Host ""
Write-Host "=== Verification ==="
$zip = [IO.Compression.ZipFile]::OpenRead($libsZip)
Write-Host "libs.zip total entries: $($zip.Entries.Count)"
Write-Host "First 3 entries:"
$zip.Entries | Select-Object -First 3 | ForEach-Object { Write-Host "  $($_.FullName)" }

foreach ($lib in $librariesNeedingRes) {
    $resEntries = @($zip.Entries | Where-Object { $_.FullName -like "$($lib.LibName)/res*" })
    Write-Host "$($lib.LibName)/res entries: $($resEntries.Count)"
    if ($resEntries.Count -gt 0) {
        $resEntries | Select-Object -First 3 | ForEach-Object { Write-Host "  $($_.FullName)" }
    }
}
$zip.Dispose()

$zip = [IO.Compression.ZipFile]::OpenRead($dexsZip)
Write-Host "dexs.zip total entries: $($zip.Entries.Count)"
Write-Host "First 3 dexs entries:"
$zip.Entries | Select-Object -First 3 | ForEach-Object { Write-Host "  $($_.FullName)" }
$zip.Dispose()

Write-Host ""
Write-Host "libs.zip: $('{0:N2}' -f ((Get-Item $libsZip).Length / 1MB)) MB"
Write-Host "dexs.zip: $('{0:N2}' -f ((Get-Item $dexsZip).Length / 1MB)) MB"
Write-Host ""
Write-Host "Done!"
