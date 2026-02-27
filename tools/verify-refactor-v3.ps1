# Verify BlockSpecRegistry refactor v3: exhaustive VALUE comparison for ALL 4 tables
# Also checks for structural issues in the new file

$ErrorActionPreference = "Stop"

$oldLines = git show "HEAD~1:app/src/main/java/pro/sketchware/core/BlockSpecRegistry.java"
$newContent = Get-Content "C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\src\main\java\pro\sketchware\core\BlockSpecRegistry.java" -Raw

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  BlockSpecRegistry Refactor Verifier v3" -ForegroundColor Cyan
Write-Host "  (Exhaustive VALUE + structural check)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

$totalIssues = 0

# ============================================================
# PART 1: Simulate old file's switch logic for each method
# ============================================================

# Generic function: trace blockName through hashCode switch -> matchIndex -> second switch
function Trace-OldSwitch {
    param($lines, $startLine, $endLine)
    
    # Pass 1: Build name -> matchIndex from first switch
    $nameToIndex = @{}
    $currentName = $null
    for ($i = $startLine; $i -lt $endLine; $i++) {
        if ($lines[$i] -match 'blockName\.equals\("([^"]+)"\)') {
            $currentName = $Matches[1]
        }
        if ($currentName -and $lines[$i] -match 'matchIndex\s*=\s*(\d+);') {
            $idx = $Matches[1]
            $nameToIndex[$currentName] = $idx
            $currentName = $null
        }
    }
    return $nameToIndex
}

# ============================================================
# CHECK 1: BLOCK_PARAMS - trace every entry's matchIndex and params
# ============================================================
Write-Host "`n=== CHECK 1: BLOCK_PARAMS values ===" -ForegroundColor Yellow

$bpStart = 10
$bpEnd = 3112
$bpNameToIdx = Trace-OldSwitch $oldLines $bpStart $bpEnd

# Build matchIndex -> params from second switch (sequential case numbers with params.add)
$bpIdxToParams = @{}
$curIdx = $null
$curParams = @()
for ($i = $bpStart; $i -lt $bpEnd; $i++) {
    if ($oldLines[$i] -match '^\s*case (\d+):$') {
        # Save previous if had params
        if ($null -ne $curIdx) {
            if ($curParams.Count -gt 0) {
                $bpIdxToParams[$curIdx] = ($curParams -join "|")
            } elseif (-not $bpIdxToParams.ContainsKey($curIdx)) {
                $bpIdxToParams[$curIdx] = ""
            }
        }
        $curIdx = $Matches[1]
        # Don't reset params if this is a fall-through (next line is also case)
        if (-not ($oldLines[$i+1] -match '^\s*case \d+:')) {
            $curParams = @()
        }
    }
    if ($oldLines[$i] -match 'params\.add\("([^"]+)"\)') {
        $curParams += $Matches[1]
    }
    if ($oldLines[$i] -match 'result\s*=\s*params') {
        if ($null -ne $curIdx) {
            $bpIdxToParams[$curIdx] = ($curParams -join "|")
        }
    }
}

# Extract new BLOCK_PARAMS values
$newBP = @{}
$bpPuts = [regex]::Matches($newContent, 'BLOCK_PARAMS\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\)')
foreach ($m in $bpPuts) {
    $key = $m.Groups[1].Value
    $vals = $m.Groups[2].Value
    $items = [regex]::Matches($vals, '"([^"]+)"')
    $arr = @()
    foreach ($item in $items) { $arr += $item.Groups[1].Value }
    $newBP[$key] = ($arr -join "|")
}

# Compare
$bpIssues = 0
foreach ($name in $bpNameToIdx.Keys) {
    $idx = $bpNameToIdx[$name]
    $oldVal = ""
    if ($bpIdxToParams.ContainsKey($idx)) { $oldVal = $bpIdxToParams[$idx] }
    $newVal = ""
    if ($newBP.ContainsKey($name)) { $newVal = $newBP[$name] }
    
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx)" -ForegroundColor Red
        Write-Host "    OLD: [$oldVal]" -ForegroundColor DarkYellow
        Write-Host "    NEW: [$newVal]" -ForegroundColor DarkYellow
        $bpIssues++
    }
}
if ($bpIssues -eq 0) { Write-Host "  All $($bpNameToIdx.Count) entries verified" -ForegroundColor Green }
$totalIssues += $bpIssues

# ============================================================
# CHECK 2: BLOCK_SPECS - trace every entry's matchIndex and spec
# ============================================================
Write-Host "`n=== CHECK 2: BLOCK_SPECS values ===" -ForegroundColor Yellow

$bsStart = 3968
$bsEnd = $oldLines.Count
$bsNameToIdx = Trace-OldSwitch $oldLines $bsStart $bsEnd

# Build matchIndex -> specKey from second switch
$bsIdxToSpec = @{}
$curIdx2 = $null
for ($i = $bsStart; $i -lt $bsEnd; $i++) {
    if ($oldLines[$i] -match '^\s*case (\d+):$') {
        $curIdx2 = $Matches[1]
    }
    if ($oldLines[$i] -match 'blockName\s*=\s*"([^"]+)";') {
        if ($null -ne $curIdx2) {
            $bsIdxToSpec[$curIdx2] = $Matches[1]
        }
    }
}

# Extract new BLOCK_SPECS values
$newBS = @{}
$bsPuts = [regex]::Matches($newContent, 'BLOCK_SPECS\.put\("([^"]+)",\s*"([^"]+)"\)')
foreach ($m in $bsPuts) { $newBS[$m.Groups[1].Value] = $m.Groups[2].Value }

# Compare
$bsIssues = 0
foreach ($name in $bsNameToIdx.Keys) {
    $idx = $bsNameToIdx[$name]
    $oldVal = $name  # default: blockName stays unchanged
    if ($bsIdxToSpec.ContainsKey($idx)) { $oldVal = $bsIdxToSpec[$idx] }
    $newVal = "false"  # default
    if ($newBS.ContainsKey($name)) { $newVal = $newBS[$name] }
    
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx)" -ForegroundColor Red
        Write-Host "    OLD: [$oldVal]" -ForegroundColor DarkYellow
        Write-Host "    NEW: [$newVal]" -ForegroundColor DarkYellow
        $bsIssues++
    }
}
if ($bsIssues -eq 0) { Write-Host "  All $($bsNameToIdx.Count) entries verified" -ForegroundColor Green }
$totalIssues += $bsIssues

# ============================================================
# CHECK 3: EVENT_MENUS - trace every entry
# ============================================================
Write-Host "`n=== CHECK 3: EVENT_MENUS values ===" -ForegroundColor Yellow

$emStart = 3112
$emEnd = 3541
$emNameToIdx = Trace-OldSwitch $oldLines $emStart $emEnd

# Build matchIndex -> menu items
$emIdxToItems = @{}
$curIdx3 = $null
$curItems = @()
for ($i = $emStart; $i -lt $emEnd; $i++) {
    if ($oldLines[$i] -match '^\s*case (\d+):$') {
        if ($null -ne $curIdx3 -and $curItems.Count -gt 0) {
            $emIdxToItems[$curIdx3] = ($curItems -join "|")
        }
        $curIdx3 = $Matches[1]
        if (-not ($oldLines[$i+1] -match '^\s*case \d+:')) {
            $curItems = @()
        }
    }
    if ($oldLines[$i] -match 'menuItems\.add\("([^"]+)"\)') {
        $curItems += $Matches[1]
    }
    if ($oldLines[$i] -match '^\s*break;') {
        if ($null -ne $curIdx3 -and $curItems.Count -gt 0) {
            $emIdxToItems[$curIdx3] = ($curItems -join "|")
        }
    }
}

# Extract new EVENT_MENUS
$newEM = @{}
$emPuts = [regex]::Matches($newContent, 'EVENT_MENUS\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\)')
foreach ($m in $emPuts) {
    $key = $m.Groups[1].Value
    $vals = $m.Groups[2].Value
    $items = [regex]::Matches($vals, '"([^"]+)"')
    $arr = @()
    foreach ($item in $items) { $arr += $item.Groups[1].Value }
    $newEM[$key] = ($arr -join "|")
}

# Compare
$emIssues = 0
foreach ($name in $emNameToIdx.Keys) {
    $idx = $emNameToIdx[$name]
    $oldVal = ""
    if ($emIdxToItems.ContainsKey($idx)) { $oldVal = $emIdxToItems[$idx] }
    $newVal = ""
    if ($newEM.ContainsKey($name)) { $newVal = $newEM[$name] }
    
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx)" -ForegroundColor Red
        Write-Host "    OLD: [$oldVal]" -ForegroundColor DarkYellow
        Write-Host "    NEW: [$newVal]" -ForegroundColor DarkYellow
        $emIssues++
    }
}
if ($emIssues -eq 0) { Write-Host "  All $($emNameToIdx.Count) entries verified" -ForegroundColor Green }
$totalIssues += $emIssues

# ============================================================
# CHECK 4: EVENT_SPECS - trace every entry
# ============================================================
Write-Host "`n=== CHECK 4: EVENT_SPECS values ===" -ForegroundColor Yellow

$esStart = 3548  # computeC
$esEnd = 3968
$esNameToIdx = Trace-OldSwitch $oldLines $esStart $esEnd

# Build matchIndex -> spec from return statements
$esIdxToSpec = @{}
for ($i = $esStart; $i -lt $esEnd; $i++) {
    if ($oldLines[$i] -match '^\s*case (\d+):$') {
        $curIdx4 = $Matches[1]
    }
    if ($oldLines[$i] -match '^\s*return\s*"([^"]+)";') {
        if ($null -ne $curIdx4) {
            $esIdxToSpec[$curIdx4] = $Matches[1]
        }
    }
}

# Extract new EVENT_SPECS
$newES = @{}
$esPuts = [regex]::Matches($newContent, 'EVENT_SPECS\.put\("([^"]+)",\s*"([^"]+)"\)')
foreach ($m in $esPuts) { $newES[$m.Groups[1].Value] = $m.Groups[2].Value }

# Compare
$esIssues = 0
foreach ($name in $esNameToIdx.Keys) {
    $idx = $esNameToIdx[$name]
    $oldVal = "initialize"  # default
    if ($esIdxToSpec.ContainsKey($idx)) { $oldVal = $esIdxToSpec[$idx] }
    $newVal = "initialize"  # default from getOrDefault
    if ($newES.ContainsKey($name)) { $newVal = $newES[$name] }
    
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx)" -ForegroundColor Red
        Write-Host "    OLD: [$oldVal]" -ForegroundColor DarkYellow
        Write-Host "    NEW: [$newVal]" -ForegroundColor DarkYellow
        $esIssues++
    }
}
if ($esIssues -eq 0) { Write-Host "  All $($esNameToIdx.Count) entries verified" -ForegroundColor Green }
$totalIssues += $esIssues

# ============================================================
# PART 2: Structural checks on new file
# ============================================================
Write-Host "`n=== STRUCTURAL CHECKS ===" -ForegroundColor Yellow

# Check 5: No duplicate keys in any map
$structIssues = 0
function Check-Duplicates($content, $mapName) {
    $keys = @{}
    $dupes = @()
    $pattern = [regex]::Escape($mapName) + '\.put\("([^"]+)"'
    $allPuts = [regex]::Matches($content, $pattern)
    foreach ($m in $allPuts) {
        $key = $m.Groups[1].Value
        if ($keys.ContainsKey($key)) {
            $dupes += $key
        }
        $keys[$key] = $true
    }
    return $dupes
}

foreach ($mapName in @("BLOCK_PARAMS", "BLOCK_SPECS", "EVENT_MENUS", "EVENT_SPECS")) {
    $dupes = Check-Duplicates $newContent $mapName
    if ($dupes.Count -gt 0) {
        Write-Host "  DUPLICATE keys in $mapName`:" -ForegroundColor Red
        foreach ($d in $dupes) { Write-Host "    - $d" -ForegroundColor Red }
        $structIssues += $dupes.Count
    } else {
        Write-Host "  $mapName`: no duplicates" -ForegroundColor Green
    }
}

# Check 6: Ensure BLOCK_PARAMS.put is only in initBlockParams, etc.
$newLines = $newContent -split "`n"
$wrongPlacement = 0
for ($i = 0; $i -lt $newLines.Count; $i++) {
    $line = $newLines[$i].Trim()
    if ($line -match '^BLOCK_PARAMS\.put' -or $line -match '^BLOCK_SPECS\.put' -or 
        $line -match '^EVENT_MENUS\.put' -or $line -match '^EVENT_SPECS\.put') {
        # Check it's not a comment
        if (-not ($line.StartsWith("//"))) {
            # This shouldn't happen at class level
            Write-Host "  WRONG PLACEMENT at line $($i+1): $line" -ForegroundColor Red
            $wrongPlacement++
        }
    }
}
if ($wrongPlacement -eq 0) {
    Write-Host "  No misplaced .put() calls" -ForegroundColor Green
}
$structIssues += $wrongPlacement

# Check 7: Method signatures match original
$methodChecks = @(
    @{Pattern='public static ArrayList<String> getBlockParams\(String blockName\)'; Name='getBlockParams'},
    @{Pattern='public static ArrayList<String> getBlockMenuItems\(String blockName\)'; Name='getBlockMenuItems'},
    @{Pattern='public static String getEventSpec\(String blockName\)'; Name='getEventSpec'},
    @{Pattern='public static String getBlockSpec\(String blockName\)'; Name='getBlockSpec'}
)
foreach ($mc in $methodChecks) {
    if ($newContent -match $mc.Pattern) {
        Write-Host "  $($mc.Name) signature: OK" -ForegroundColor Green
    } else {
        Write-Host "  $($mc.Name) signature: MISSING or CHANGED" -ForegroundColor Red
        $structIssues++
    }
}

# Check 8: Default return values
if ($newContent -match 'getOrDefault\(blockName,\s*"initialize"\)') {
    Write-Host "  getEventSpec default 'initialize': OK" -ForegroundColor Green
} else {
    Write-Host "  getEventSpec default: WRONG" -ForegroundColor Red
    $structIssues++
}
if ($newContent -match 'getOrDefault\(blockName,\s*"false"\)') {
    Write-Host "  getBlockSpec default 'false': OK" -ForegroundColor Green
} else {
    Write-Host "  getBlockSpec default: WRONG" -ForegroundColor Red
    $structIssues++
}

$totalIssues += $structIssues

# ============================================================
# FINAL SUMMARY
# ============================================================
Write-Host "`n============================================" -ForegroundColor Cyan
if ($totalIssues -eq 0) {
    Write-Host "  ALL CHECKS PASSED - Zero issues!" -ForegroundColor Green
} else {
    Write-Host "  $totalIssues ISSUES FOUND" -ForegroundColor Red
}
Write-Host "============================================" -ForegroundColor Cyan
