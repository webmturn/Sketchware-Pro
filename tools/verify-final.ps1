# FINAL verification: correct fall-through handling
# Strategy: group consecutive case labels, assign shared body to ALL

$ErrorActionPreference = "Stop"
$oldLines = git show "HEAD~1:app/src/main/java/pro/sketchware/core/BlockSpecRegistry.java"
$newContent = Get-Content "C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\src\main\java\pro\sketchware\core\BlockSpecRegistry.java" -Raw

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  FINAL BlockSpecRegistry Verification" -ForegroundColor Cyan
Write-Host "  (Correct fall-through + full value compare)" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

$totalIssues = 0

# ============================================================
# Build name->matchIndex from first switch of each method
# ============================================================
function Get-NameToIndex($lines, $startLine, $endLine) {
    $map = @{}
    $curName = $null
    for ($i = $startLine; $i -lt $endLine -and $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'blockName\.equals\("([^"]+)"\)') {
            $curName = $Matches[1]
        }
        if ($curName -and $lines[$i] -match 'matchIndex\s*=\s*(\d+);') {
            $map[$curName] = [int]$Matches[1]
            $curName = $null
        }
    }
    return $map
}

# ============================================================
# Parse second switch with CORRECT fall-through grouping
# Returns: hashtable of matchIndex -> value (string)
# ============================================================
function Get-IndexToValue-Params($lines, $startLine, $endLine) {
    # Find all case blocks in the second switch (sequential numbers)
    # Group consecutive case labels, extract body
    $result = @{}
    $i = $startLine
    while ($i -lt $endLine) {
        # Look for case N:
        if ($lines[$i] -match '^\s*case (\d+):$') {
            # Collect all consecutive case labels (fall-through group)
            $groupIndices = @()
            while ($i -lt $endLine -and $lines[$i] -match '^\s*case (\d+):$') {
                $groupIndices += [int]$Matches[1]
                $i++
            }
            # Now collect body lines until break/result=params/next case group
            $bodyParams = @()
            while ($i -lt $endLine) {
                if ($lines[$i] -match 'params\.add\("([^"]+)"\)') {
                    $bodyParams += $Matches[1]
                }
                if ($lines[$i] -match '^\s*break;' -or $lines[$i] -match 'result\s*=\s*params' -or $lines[$i] -match '^\s*case \d+:') {
                    break
                }
                $i++
            }
            # Assign to ALL indices in group
            $val = ($bodyParams -join "|")
            foreach ($idx in $groupIndices) {
                $result[$idx] = $val
            }
        } else {
            $i++
        }
    }
    return $result
}

function Get-IndexToValue-Specs($lines, $startLine, $endLine) {
    $result = @{}
    $i = $startLine
    while ($i -lt $endLine) {
        if ($lines[$i] -match '^\s*case (\d+):$') {
            $groupIndices = @()
            while ($i -lt $endLine -and $lines[$i] -match '^\s*case (\d+):$') {
                $groupIndices += [int]$Matches[1]
                $i++
            }
            $specVal = $null
            while ($i -lt $endLine) {
                if ($lines[$i] -match 'blockName\s*=\s*"([^"]+)";') {
                    $specVal = $Matches[1]
                    break
                }
                if ($lines[$i] -match '^\s*break;' -or $lines[$i] -match '^\s*case \d+:') {
                    break
                }
                $i++
            }
            foreach ($idx in $groupIndices) {
                if ($specVal) { $result[$idx] = $specVal }
                else { $result[$idx] = $null }
            }
        } else {
            $i++
        }
    }
    return $result
}

function Get-IndexToValue-MenuItems($lines, $startLine, $endLine) {
    $result = @{}
    $i = $startLine
    while ($i -lt $endLine) {
        if ($lines[$i] -match '^\s*case (\d+):$') {
            $groupIndices = @()
            while ($i -lt $endLine -and $lines[$i] -match '^\s*case (\d+):$') {
                $groupIndices += [int]$Matches[1]
                $i++
            }
            $bodyItems = @()
            while ($i -lt $endLine) {
                if ($lines[$i] -match 'menuItems\.add\("([^"]+)"\)') {
                    $bodyItems += $Matches[1]
                }
                if ($lines[$i] -match '^\s*break;' -or $lines[$i] -match '^\s*case \d+:') {
                    break
                }
                $i++
            }
            $val = ($bodyItems -join "|")
            foreach ($idx in $groupIndices) {
                $result[$idx] = $val
            }
        } else {
            $i++
        }
    }
    return $result
}

function Get-IndexToValue-EventSpecs($lines, $startLine, $endLine) {
    $result = @{}
    $i = $startLine
    while ($i -lt $endLine) {
        if ($lines[$i] -match '^\s*case (\d+):$') {
            $groupIndices = @()
            while ($i -lt $endLine -and $lines[$i] -match '^\s*case (\d+):$') {
                $groupIndices += [int]$Matches[1]
                $i++
            }
            $retVal = $null
            while ($i -lt $endLine) {
                if ($lines[$i] -match '^\s*return\s*"([^"]+)";') {
                    $retVal = $Matches[1]
                    break
                }
                if ($lines[$i] -match '^\s*break;' -or $lines[$i] -match '^\s*case \d+:') {
                    break
                }
                $i++
            }
            foreach ($idx in $groupIndices) {
                if ($retVal) { $result[$idx] = $retVal }
                else { $result[$idx] = $null }
            }
        } else {
            $i++
        }
    }
    return $result
}

# ============================================================
# Extract new file values
# ============================================================
function Get-NewParamsMap($content) {
    $result = @{}
    $puts = [regex]::Matches($content, 'BLOCK_PARAMS\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\)')
    foreach ($m in $puts) {
        $key = $m.Groups[1].Value
        $items = [regex]::Matches($m.Groups[2].Value, '"([^"]+)"')
        $arr = @(); foreach ($item in $items) { $arr += $item.Groups[1].Value }
        $result[$key] = ($arr -join "|")
    }
    return $result
}

function Get-NewStringMap($content, $mapName) {
    $result = @{}
    $puts = [regex]::Matches($content, [regex]::Escape($mapName) + '\.put\("([^"]+)",\s*"([^"]+)"\)')
    foreach ($m in $puts) { $result[$m.Groups[1].Value] = $m.Groups[2].Value }
    return $result
}

function Get-NewMenuMap($content) {
    $result = @{}
    $puts = [regex]::Matches($content, 'EVENT_MENUS\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\)')
    foreach ($m in $puts) {
        $key = $m.Groups[1].Value
        $items = [regex]::Matches($m.Groups[2].Value, '"([^"]+)"')
        $arr = @(); foreach ($item in $items) { $arr += $item.Groups[1].Value }
        $result[$key] = ($arr -join "|")
    }
    return $result
}

# ============================================================
# Method boundaries
# ============================================================
$boundaries = @{}
for ($i = 0; $i -lt $oldLines.Count; $i++) {
    if ($oldLines[$i] -match 'public static .+ getBlockParams\(') { $boundaries['bp'] = $i }
    if ($oldLines[$i] -match 'public static .+ getBlockMenuItems\(') { $boundaries['bm'] = $i }
    if ($oldLines[$i] -match 'public static String getEventSpec\(') { $boundaries['es'] = $i }
    if ($oldLines[$i] -match 'private static String computeC\(') { $boundaries['cc'] = $i }
    if ($oldLines[$i] -match 'public static String getBlockSpec\(') { $boundaries['bs'] = $i }
}

# ============================================================
# CHECK 1: BLOCK_PARAMS
# ============================================================
Write-Host "`n--- BLOCK_PARAMS ---" -ForegroundColor Yellow
$bpNames = Get-NameToIndex $oldLines $boundaries['bp'] $boundaries['bm']
$bpValues = Get-IndexToValue-Params $oldLines $boundaries['bp'] $boundaries['bm']
$newBP = Get-NewParamsMap $newContent

$bpIssues = 0
foreach ($name in $bpNames.Keys) {
    $idx = $bpNames[$name]
    $oldVal = ""
    if ($bpValues.ContainsKey($idx)) { $oldVal = $bpValues[$idx] }
    $newVal = ""
    if ($newBP.ContainsKey($name)) { $newVal = $newBP[$name] }
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx) OLD=[$oldVal] NEW=[$newVal]" -ForegroundColor Red
        $bpIssues++
    }
}
# Check reverse: new has entries old doesn't
foreach ($name in $newBP.Keys) {
    if (-not $bpNames.ContainsKey($name)) {
        Write-Host "  EXTRA in new: $name -> [$($newBP[$name])]" -ForegroundColor Magenta
        $bpIssues++
    }
}
if ($bpIssues -eq 0) { Write-Host "  PASS: $($bpNames.Count) old, $($newBP.Count) new - all values match" -ForegroundColor Green }
$totalIssues += $bpIssues

# ============================================================
# CHECK 2: BLOCK_SPECS
# ============================================================
Write-Host "`n--- BLOCK_SPECS ---" -ForegroundColor Yellow
$bsNames = Get-NameToIndex $oldLines $boundaries['bs'] $oldLines.Count
$bsValues = Get-IndexToValue-Specs $oldLines $boundaries['bs'] $oldLines.Count
$newBS = Get-NewStringMap $newContent "BLOCK_SPECS"

$bsIssues = 0
foreach ($name in $bsNames.Keys) {
    $idx = $bsNames[$name]
    # Default: if no spec found, blockName stays as-is (returned as-is)
    $oldVal = $name
    if ($bsValues.ContainsKey($idx) -and $null -ne $bsValues[$idx]) { $oldVal = $bsValues[$idx] }
    $newVal = "false"  # getOrDefault default
    if ($newBS.ContainsKey($name)) { $newVal = $newBS[$name] }
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx) OLD=[$oldVal] NEW=[$newVal]" -ForegroundColor Red
        $bsIssues++
    }
}
foreach ($name in $newBS.Keys) {
    if (-not $bsNames.ContainsKey($name)) {
        Write-Host "  EXTRA in new: $name -> $($newBS[$name])" -ForegroundColor Magenta
        $bsIssues++
    }
}
if ($bsIssues -eq 0) { Write-Host "  PASS: $($bsNames.Count) old, $($newBS.Count) new - all values match" -ForegroundColor Green }
$totalIssues += $bsIssues

# ============================================================
# CHECK 3: EVENT_MENUS
# ============================================================
Write-Host "`n--- EVENT_MENUS ---" -ForegroundColor Yellow
$emNames = Get-NameToIndex $oldLines $boundaries['bm'] $boundaries['es']
$emValues = Get-IndexToValue-MenuItems $oldLines $boundaries['bm'] $boundaries['es']
$newEM = Get-NewMenuMap $newContent

$emIssues = 0
foreach ($name in $emNames.Keys) {
    $idx = $emNames[$name]
    $oldVal = ""
    if ($emValues.ContainsKey($idx)) { $oldVal = $emValues[$idx] }
    $newVal = ""
    if ($newEM.ContainsKey($name)) { $newVal = $newEM[$name] }
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx) OLD=[$oldVal] NEW=[$newVal]" -ForegroundColor Red
        $emIssues++
    }
}
foreach ($name in $newEM.Keys) {
    if (-not $emNames.ContainsKey($name)) {
        Write-Host "  EXTRA in new: $name -> [$($newEM[$name])]" -ForegroundColor Magenta
        $emIssues++
    }
}
if ($emIssues -eq 0) { Write-Host "  PASS: $($emNames.Count) old, $($newEM.Count) new - all values match" -ForegroundColor Green }
$totalIssues += $emIssues

# ============================================================
# CHECK 4: EVENT_SPECS
# ============================================================
Write-Host "`n--- EVENT_SPECS ---" -ForegroundColor Yellow
$esNames = Get-NameToIndex $oldLines $boundaries['cc'] $boundaries['bs']
$esValues = Get-IndexToValue-EventSpecs $oldLines $boundaries['cc'] $boundaries['bs']
$newES = Get-NewStringMap $newContent "EVENT_SPECS"

$esIssues = 0
foreach ($name in $esNames.Keys) {
    $idx = $esNames[$name]
    $oldVal = "initialize"  # default
    if ($esValues.ContainsKey($idx) -and $null -ne $esValues[$idx]) { $oldVal = $esValues[$idx] }
    $newVal = "initialize"  # getOrDefault default
    if ($newES.ContainsKey($name)) { $newVal = $newES[$name] }
    if ($oldVal -ne $newVal) {
        Write-Host "  MISMATCH: $name (idx=$idx) OLD=[$oldVal] NEW=[$newVal]" -ForegroundColor Red
        $esIssues++
    }
}
foreach ($name in $newES.Keys) {
    if (-not $esNames.ContainsKey($name)) {
        Write-Host "  EXTRA in new: $name -> $($newES[$name])" -ForegroundColor Magenta
        $esIssues++
    }
}
if ($esIssues -eq 0) { Write-Host "  PASS: $($esNames.Count) old, $($newES.Count) new - all values match" -ForegroundColor Green }
$totalIssues += $esIssues

# ============================================================
# CHECK 5: Duplicates in new file
# ============================================================
Write-Host "`n--- DUPLICATE CHECK ---" -ForegroundColor Yellow
$dupIssues = 0
foreach ($mapName in @("BLOCK_PARAMS", "BLOCK_SPECS", "EVENT_MENUS", "EVENT_SPECS")) {
    $keys = @{}
    $pattern = [regex]::Escape($mapName) + '\.put\("([^"]+)"'
    $allPuts = [regex]::Matches($newContent, $pattern)
    $dupes = @()
    foreach ($m in $allPuts) {
        $k = $m.Groups[1].Value
        if ($keys.ContainsKey($k)) { $dupes += $k }
        $keys[$k] = $true
    }
    if ($dupes.Count -gt 0) {
        foreach ($d in $dupes) { Write-Host "  DUPLICATE in ${mapName}: $d" -ForegroundColor Red; $dupIssues++ }
    } else {
        Write-Host "  $mapName : 0 duplicates" -ForegroundColor Green
    }
}
$totalIssues += $dupIssues

# ============================================================
# CHECK 6: Method signatures + defaults
# ============================================================
Write-Host "`n--- API CHECK ---" -ForegroundColor Yellow
$apiIssues = 0
$checks = @(
    @{Pat='public static ArrayList<String> getBlockParams\(String blockName\)'; Desc='getBlockParams signature'},
    @{Pat='public static ArrayList<String> getBlockMenuItems\(String blockName\)'; Desc='getBlockMenuItems signature'},
    @{Pat='public static String getEventSpec\(String blockName\)'; Desc='getEventSpec signature'},
    @{Pat='public static String getBlockSpec\(String blockName\)'; Desc='getBlockSpec signature'},
    @{Pat='getOrDefault\(blockName,\s*"initialize"\)'; Desc='getEventSpec default=initialize'},
    @{Pat='getOrDefault\(blockName,\s*"false"\)'; Desc='getBlockSpec default=false'}
)
foreach ($c in $checks) {
    if ($newContent -match $c.Pat) { Write-Host "  $($c.Desc): OK" -ForegroundColor Green }
    else { Write-Host "  $($c.Desc): FAIL" -ForegroundColor Red; $apiIssues++ }
}
$totalIssues += $apiIssues

# ============================================================
# FINAL
# ============================================================
Write-Host "`n================================================" -ForegroundColor Cyan
if ($totalIssues -eq 0) {
    Write-Host "  ALL CHECKS PASSED - ZERO ISSUES" -ForegroundColor Green
} else {
    Write-Host "  $totalIssues ISSUES FOUND" -ForegroundColor Red
}
Write-Host "================================================" -ForegroundColor Cyan
