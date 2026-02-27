# Verify BlockSpecRegistry refactor v2: exhaustive comparison
# Uses a completely different parsing strategy from v1

$ErrorActionPreference = "Stop"

$oldLines = git show "HEAD~1:app/src/main/java/pro/sketchware/core/BlockSpecRegistry.java"
$oldContent = $oldLines -join "`n"
$newContent = Get-Content "C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\src\main\java\pro\sketchware\core\BlockSpecRegistry.java" -Raw

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  BlockSpecRegistry Refactor Verifier v2" -ForegroundColor Cyan
Write-Host "  (Exhaustive line-by-line comparison)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# ===== Step 1: Identify method boundaries in old file =====
$methodBoundaries = @{}
$methods = @("getBlockParams", "getBlockMenuItems", "getEventSpec", "getBlockSpec")
for ($i = 0; $i -lt $oldLines.Count; $i++) {
    foreach ($m in $methods) {
        if ($oldLines[$i] -match "public static .+ $m\(") {
            $methodBoundaries[$m] = $i
        }
    }
    # Also find computeC (private helper for getEventSpec)
    if ($oldLines[$i] -match "private static String computeC\(") {
        $methodBoundaries["computeC"] = $i
    }
}
Write-Host "`nOld file method boundaries:" -ForegroundColor Gray
foreach ($k in $methodBoundaries.Keys | Sort-Object { $methodBoundaries[$_] }) {
    Write-Host "  $k : line $($methodBoundaries[$k])" -ForegroundColor Gray
}

# ===== Step 2: For each method section, extract ALL blockName.equals entries =====
function Get-AllBlockNames($lines, $startLine, $endLine) {
    $names = @{}
    for ($i = $startLine; $i -lt $endLine -and $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'blockName\.equals\("([^"]+)"\)') {
            $name = $Matches[1]
            if (-not $names.ContainsKey($name)) {
                $names[$name] = $i
            }
        }
    }
    return $names
}

# getBlockParams section: from getBlockParams to getBlockMenuItems
$bpStart = $methodBoundaries["getBlockParams"]
$bpEnd = $methodBoundaries["getBlockMenuItems"]
$oldBPNames = Get-AllBlockNames $oldLines $bpStart $bpEnd
Write-Host "`ngetBlockParams: $($oldBPNames.Count) unique blockNames in old file" -ForegroundColor Gray

# getBlockMenuItems section: from getBlockMenuItems to getEventSpec
$bmStart = $methodBoundaries["getBlockMenuItems"]
$bmEnd = $methodBoundaries["getEventSpec"]
$oldBMNames = Get-AllBlockNames $oldLines $bmStart $bmEnd
Write-Host "getBlockMenuItems: $($oldBMNames.Count) unique blockNames in old file" -ForegroundColor Gray

# getEventSpec/computeC section: from computeC to getBlockSpec
$esStart = $methodBoundaries["computeC"]
$esEnd = $methodBoundaries["getBlockSpec"]
$oldESNames = Get-AllBlockNames $oldLines $esStart $esEnd
Write-Host "getEventSpec: $($oldESNames.Count) unique blockNames in old file" -ForegroundColor Gray

# getBlockSpec section: from getBlockSpec to end
$bsStart = $methodBoundaries["getBlockSpec"]
$bsEnd = $oldLines.Count
$oldBSNames = Get-AllBlockNames $oldLines $bsStart $bsEnd
Write-Host "getBlockSpec: $($oldBSNames.Count) unique blockNames in old file" -ForegroundColor Gray

# ===== Step 3: Extract all keys from new file's HashMap =====
function Get-NewMapKeys($content, $mapName) {
    $keys = @{}
    $allPuts = [regex]::Matches($content, [regex]::Escape($mapName) + '\.put\("([^"]+)"')
    foreach ($m in $allPuts) {
        $keys[$m.Groups[1].Value] = $true
    }
    return $keys
}

$newBPKeys = Get-NewMapKeys $newContent "BLOCK_PARAMS"
$newBMKeys = Get-NewMapKeys $newContent "EVENT_MENUS"
$newESKeys = Get-NewMapKeys $newContent "EVENT_SPECS"
$newBSKeys = Get-NewMapKeys $newContent "BLOCK_SPECS"

Write-Host "`nNew file map sizes:" -ForegroundColor Gray
Write-Host "  BLOCK_PARAMS: $($newBPKeys.Count)" -ForegroundColor Gray
Write-Host "  EVENT_MENUS: $($newBMKeys.Count)" -ForegroundColor Gray
Write-Host "  EVENT_SPECS: $($newESKeys.Count)" -ForegroundColor Gray
Write-Host "  BLOCK_SPECS: $($newBSKeys.Count)" -ForegroundColor Gray

# ===== Step 4: Compare sets =====
$totalIssues = 0

function Compare-KeySets($oldNames, $newKeys, $tableName) {
    $issues = 0
    Write-Host "`n--- $tableName ---" -ForegroundColor Yellow
    Write-Host "  Old: $($oldNames.Count) keys, New: $($newKeys.Count) keys" -ForegroundColor Gray

    # Check missing
    $missing = @()
    foreach ($name in $oldNames.Keys) {
        if (-not $newKeys.ContainsKey($name)) {
            $missing += $name
            $issues++
        }
    }
    if ($missing.Count -gt 0) {
        Write-Host "  MISSING in new ($($missing.Count)):" -ForegroundColor Red
        foreach ($m in ($missing | Sort-Object)) {
            Write-Host "    - $m (old line $($oldNames[$m]))" -ForegroundColor Red
        }
    }

    # Check extra
    $extra = @()
    foreach ($name in $newKeys.Keys) {
        if (-not $oldNames.ContainsKey($name)) {
            $extra += $name
            $issues++
        }
    }
    if ($extra.Count -gt 0) {
        Write-Host "  EXTRA in new ($($extra.Count)):" -ForegroundColor Magenta
        foreach ($e in ($extra | Sort-Object)) {
            Write-Host "    - $e" -ForegroundColor Magenta
        }
    }

    if ($issues -eq 0) {
        Write-Host "  PERFECT MATCH" -ForegroundColor Green
    }
    return $issues
}

$totalIssues += Compare-KeySets $oldBPNames $newBPKeys "BLOCK_PARAMS (getBlockParams)"
$totalIssues += Compare-KeySets $oldBMNames $newBMKeys "EVENT_MENUS (getBlockMenuItems)"
$totalIssues += Compare-KeySets $oldESNames $newESKeys "EVENT_SPECS (getEventSpec)"
$totalIssues += Compare-KeySets $oldBSNames $newBSKeys "BLOCK_SPECS (getBlockSpec)"

# ===== Step 5: Deep value comparison for BLOCK_PARAMS =====
# Trace each opCode through matchIndex -> params in old file
Write-Host "`n--- BLOCK_PARAMS Deep Value Check ---" -ForegroundColor Yellow
$deepIssues = 0
# Build complete index->opCode mapping for getBlockParams section
$bpIndexToName = @{}
for ($i = $bpStart; $i -lt $bpEnd; $i++) {
    if ($oldLines[$i] -match 'blockName\.equals\("([^"]+)"\)') {
        $currentName = $Matches[1]
    }
    if ($oldLines[$i] -match '^\s*matchIndex\s*=\s*(\d+);') {
        $idx = $Matches[1]
        if ($currentName -and (-not $bpIndexToName.ContainsKey($idx))) {
            $bpIndexToName[$idx] = $currentName
        }
        $currentName = $null
    }
}

# Build matchIndex -> params list
$bpIndexToParams = @{}
$inParamSwitch = $false
$currentIdx = $null
$currentParams = @()
for ($i = $bpStart; $i -lt $bpEnd; $i++) {
    # Detect second switch (the one with sequential case numbers and params.add)
    if ($oldLines[$i] -match '^\s*case (\d+):$' -and $i -gt ($bpStart + 100)) {
        if ($currentIdx -ne $null -and $currentParams.Count -gt 0) {
            $bpIndexToParams[$currentIdx] = ($currentParams -join ",")
        }
        $currentIdx = $Matches[1]
        $currentParams = @()
    }
    if ($oldLines[$i] -match 'params\.add\("([^"]+)"\)') {
        $currentParams += $Matches[1]
    }
    if ($oldLines[$i] -match 'result\s*=\s*params') {
        if ($currentIdx -ne $null -and $currentParams.Count -gt 0) {
            $bpIndexToParams[$currentIdx] = ($currentParams -join ",")
        }
    }
}

# Now compare values
$newBPValues = @{}
$bpPuts = [regex]::Matches($newContent, 'BLOCK_PARAMS\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\)')
foreach ($m in $bpPuts) {
    $key = $m.Groups[1].Value
    $vals = $m.Groups[2].Value
    $items = [regex]::Matches($vals, '"([^"]+)"')
    $arr = @()
    foreach ($item in $items) { $arr += $item.Groups[1].Value }
    $newBPValues[$key] = ($arr -join ",")
}

foreach ($idx in $bpIndexToName.Keys) {
    $name = $bpIndexToName[$idx]
    if ($bpIndexToParams.ContainsKey($idx) -and $newBPValues.ContainsKey($name)) {
        $oldVal = $bpIndexToParams[$idx]
        $newVal = $newBPValues[$name]
        if ($oldVal -ne $newVal) {
            Write-Host "  VALUE MISMATCH: $name" -ForegroundColor Red
            Write-Host "    OLD (idx $idx): $oldVal" -ForegroundColor DarkYellow
            Write-Host "    NEW: $newVal" -ForegroundColor DarkYellow
            $deepIssues++
        }
    }
}

if ($deepIssues -eq 0) {
    Write-Host "  All traceable values match" -ForegroundColor Green
} else {
    Write-Host "  $deepIssues value mismatches found" -ForegroundColor Red
}
$totalIssues += $deepIssues

# ===== Step 6: Deep value comparison for BLOCK_SPECS =====
Write-Host "`n--- BLOCK_SPECS Deep Value Check ---" -ForegroundColor Yellow
$bsDeepIssues = 0
$bsIndexToName = @{}
$currentName = $null
for ($i = $bsStart; $i -lt $bsEnd; $i++) {
    if ($oldLines[$i] -match 'blockName\.equals\("([^"]+)"\)') {
        $currentName = $Matches[1]
    }
    if ($oldLines[$i] -match '^\s*matchIndex\s*=\s*(\d+);') {
        $idx = $Matches[1]
        if ($currentName -and (-not $bsIndexToName.ContainsKey($idx))) {
            $bsIndexToName[$idx] = $currentName
        }
        $currentName = $null
    }
}

$bsIndexToSpec = @{}
for ($i = $bsStart; $i -lt $bsEnd; $i++) {
    if ($oldLines[$i] -match '^\s*case (\d+):$') {
        $caseIdx = $Matches[1]
    }
    if ($oldLines[$i] -match 'blockName\s*=\s*"([^"]+)";\s*$') {
        if ($caseIdx) {
            $bsIndexToSpec[$caseIdx] = $Matches[1]
        }
    }
}

$newBSValues = @{}
$bsPuts = [regex]::Matches($newContent, 'BLOCK_SPECS\.put\("([^"]+)",\s*"([^"]+)"\)')
foreach ($m in $bsPuts) {
    $newBSValues[$m.Groups[1].Value] = $m.Groups[2].Value
}

foreach ($idx in $bsIndexToName.Keys) {
    $name = $bsIndexToName[$idx]
    if ($bsIndexToSpec.ContainsKey($idx) -and $newBSValues.ContainsKey($name)) {
        $oldVal = $bsIndexToSpec[$idx]
        $newVal = $newBSValues[$name]
        if ($oldVal -ne $newVal) {
            Write-Host "  VALUE MISMATCH: $name" -ForegroundColor Red
            Write-Host "    OLD (idx $idx): $oldVal" -ForegroundColor DarkYellow
            Write-Host "    NEW: $newVal" -ForegroundColor DarkYellow
            $bsDeepIssues++
        }
    }
}

if ($bsDeepIssues -eq 0) {
    Write-Host "  All traceable values match" -ForegroundColor Green
} else {
    Write-Host "  $bsDeepIssues value mismatches found" -ForegroundColor Red
}
$totalIssues += $bsDeepIssues

# ===== Final Summary =====
Write-Host "`n============================================" -ForegroundColor Cyan
if ($totalIssues -eq 0) {
    Write-Host "  VERIFICATION PASSED - Zero issues!" -ForegroundColor Green
} else {
    Write-Host "  $totalIssues ISSUES FOUND" -ForegroundColor Red
}
Write-Host "============================================" -ForegroundColor Cyan
