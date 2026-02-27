# Verify BlockSpecRegistry refactor: compare old (git HEAD~1) vs new (current)
# Extracts all mappings from both versions and reports any differences

$ErrorActionPreference = "Stop"

# Get old file from git
$oldFile = git show "HEAD~1:app/src/main/java/pro/sketchware/core/BlockSpecRegistry.java"
$oldContent = $oldFile -join "`n"

# Get new file
$newContent = Get-Content "C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\src\main\java\pro\sketchware\core\BlockSpecRegistry.java" -Raw

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BlockSpecRegistry Refactor Verifier" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$totalIssues = 0

# ===== Helper: Extract old-style mappings =====
function Extract-OldBlockParams($content) {
    # Extract opCode -> matchIndex from hashCode switch
    $indexMap = @{}
    $opMatches = [regex]::Matches($content, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
    foreach ($m in $opMatches) {
        $opCode = $m.Groups[1].Value
        $idx = $m.Groups[2].Value
        if (-not $indexMap.ContainsKey($idx)) {
            $indexMap[$idx] = $opCode
        }
    }

    # Get section before getBlockMenuItems
    $section = $content.Substring(0, $content.IndexOf("public static ArrayList<String> getBlockMenuItems"))

    # Extract matchIndex -> params
    $paramCases = [regex]::Matches($section, 'case (\d+):\s*((?:params\.add\("[^"]+"\);\s*)+)')
    $paramsData = @{}
    foreach ($pc in $paramCases) {
        $idx = $pc.Groups[1].Value
        $body = $pc.Groups[2].Value
        $paramList = [regex]::Matches($body, 'params\.add\("([^"]+)"\)')
        $pArr = @()
        foreach ($p in $paramList) { $pArr += $p.Groups[1].Value }
        $paramsData[$idx] = $pArr
    }

    $result = @{}
    foreach ($idx in $indexMap.Keys) {
        $opCode = $indexMap[$idx]
        if ($paramsData.ContainsKey($idx)) {
            $result[$opCode] = ($paramsData[$idx] -join ",")
        }
    }
    return $result
}

function Extract-OldBlockSpecs($content) {
    $section = $content.Substring($content.IndexOf("public static String getBlockSpec"))

    $indexMap = @{}
    $opMatches = [regex]::Matches($section, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
    foreach ($m in $opMatches) {
        $opCode = $m.Groups[1].Value
        $idx = $m.Groups[2].Value
        if (-not $indexMap.ContainsKey($idx)) {
            $indexMap[$idx] = $opCode
        }
    }

    $specCases = [regex]::Matches($section, 'case (\d+):\s*blockName\s*=\s*"([^"]+)";\s*break;')
    $specData = @{}
    foreach ($sc in $specCases) {
        $specData[$sc.Groups[1].Value] = $sc.Groups[2].Value
    }

    $result = @{}
    foreach ($idx in $indexMap.Keys) {
        $opCode = $indexMap[$idx]
        if ($specData.ContainsKey($idx)) {
            $result[$opCode] = $specData[$idx]
        }
    }
    return $result
}

function Extract-OldEventMenus($content) {
    $startIdx = $content.IndexOf("public static ArrayList<String> getBlockMenuItems")
    $endIdx = $content.IndexOf("public static String getEventSpec")
    $section = $content.Substring($startIdx, $endIdx - $startIdx)

    $indexMap = @{}
    $opMatches = [regex]::Matches($section, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
    foreach ($m in $opMatches) {
        $name = $m.Groups[1].Value
        $idx = $m.Groups[2].Value
        if (-not $indexMap.ContainsKey($idx)) {
            $indexMap[$idx] = $name
        }
    }

    $menuCases = [regex]::Matches($section, 'case (\d+):\s*((?:menuItems\.add\("[^"]+"\);\s*)+)')
    $menuData = @{}
    foreach ($mc in $menuCases) {
        $idx = $mc.Groups[1].Value
        $body = $mc.Groups[2].Value
        $items = [regex]::Matches($body, 'menuItems\.add\("([^"]+)"\)')
        $arr = @()
        foreach ($item in $items) { $arr += $item.Groups[1].Value }
        $menuData[$idx] = $arr
    }

    $result = @{}
    foreach ($idx in $indexMap.Keys) {
        $name = $indexMap[$idx]
        if ($menuData.ContainsKey($idx)) {
            $result[$name] = ($menuData[$idx] -join ",")
        }
    }
    return $result
}

function Extract-OldEventSpecs($content) {
    $startIdx = $content.IndexOf("private static String computeC")
    $endIdx = $content.IndexOf("public static String getBlockSpec")
    $section = $content.Substring($startIdx, $endIdx - $startIdx)

    $indexMap = @{}
    $opMatches = [regex]::Matches($section, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
    foreach ($m in $opMatches) {
        $name = $m.Groups[1].Value
        $idx = $m.Groups[2].Value
        if (-not $indexMap.ContainsKey($idx)) {
            $indexMap[$idx] = $name
        }
    }

    $returnCases = [regex]::Matches($section, 'case (\d+):\s*return\s*"([^"]+)";')
    $specData = @{}
    foreach ($rc in $returnCases) {
        $specData[$rc.Groups[1].Value] = $rc.Groups[2].Value
    }

    $result = @{}
    foreach ($idx in $indexMap.Keys) {
        $name = $indexMap[$idx]
        if ($specData.ContainsKey($idx)) {
            $result[$name] = $specData[$idx]
        }
    }
    return $result
}

# ===== Helper: Extract new-style mappings =====
function Extract-NewStringArrayMap($content, $mapName) {
    $result = @{}
    $pattern = [regex]::Escape($mapName) + '\.put\("([^"]+)",\s*new String\[\]\{([^}]*)\}\);'
    $putMatches = [regex]::Matches($content, $pattern)
    foreach ($m in $putMatches) {
        $key = $m.Groups[1].Value
        $vals = $m.Groups[2].Value
        # Extract individual strings
        $items = [regex]::Matches($vals, '"([^"]+)"')
        $arr = @()
        foreach ($item in $items) { $arr += $item.Groups[1].Value }
        $result[$key] = ($arr -join ",")
    }
    return $result
}

function Extract-NewStringMap($content, $mapName) {
    $result = @{}
    $pattern = [regex]::Escape($mapName) + '\.put\("([^"]+)",\s*"([^"]+)"\);'
    $putMatches = [regex]::Matches($content, $pattern)
    foreach ($m in $putMatches) {
        $result[$m.Groups[1].Value] = $m.Groups[2].Value
    }
    return $result
}

function Compare-Maps($oldMap, $newMap, $tableName) {
    $issues = 0
    Write-Host "`n--- $tableName ---" -ForegroundColor Yellow

    # Check for missing in new
    foreach ($key in $oldMap.Keys) {
        if (-not $newMap.ContainsKey($key)) {
            Write-Host "  MISSING in new: $key -> $($oldMap[$key])" -ForegroundColor Red
            $issues++
        } elseif ($oldMap[$key] -ne $newMap[$key]) {
            Write-Host "  MISMATCH: $key" -ForegroundColor Red
            Write-Host "    OLD: $($oldMap[$key])" -ForegroundColor DarkYellow
            Write-Host "    NEW: $($newMap[$key])" -ForegroundColor DarkYellow
            $issues++
        }
    }

    # Check for extra in new
    foreach ($key in $newMap.Keys) {
        if (-not $oldMap.ContainsKey($key)) {
            Write-Host "  EXTRA in new: $key -> $($newMap[$key])" -ForegroundColor Magenta
            $issues++
        }
    }

    if ($issues -eq 0) {
        Write-Host "  ALL OK ($($oldMap.Count) old, $($newMap.Count) new)" -ForegroundColor Green
    } else {
        Write-Host "  $issues issues found ($($oldMap.Count) old, $($newMap.Count) new)" -ForegroundColor Red
    }
    return $issues
}

# ===== Run comparisons =====
Write-Host "`nExtracting old mappings..." -ForegroundColor Gray
$oldBlockParams = Extract-OldBlockParams $oldContent
$oldBlockSpecs = Extract-OldBlockSpecs $oldContent
$oldEventMenus = Extract-OldEventMenus $oldContent
$oldEventSpecs = Extract-OldEventSpecs $oldContent

Write-Host "Extracting new mappings..." -ForegroundColor Gray
$newBlockParams = Extract-NewStringArrayMap $newContent "BLOCK_PARAMS"
$newBlockSpecs = Extract-NewStringMap $newContent "BLOCK_SPECS"
$newEventMenus = Extract-NewStringArrayMap $newContent "EVENT_MENUS"
$newEventSpecs = Extract-NewStringMap $newContent "EVENT_SPECS"

$totalIssues += Compare-Maps $oldBlockParams $newBlockParams "BLOCK_PARAMS"
$totalIssues += Compare-Maps $oldBlockSpecs $newBlockSpecs "BLOCK_SPECS"
$totalIssues += Compare-Maps $oldEventMenus $newEventMenus "EVENT_MENUS"
$totalIssues += Compare-Maps $oldEventSpecs $newEventSpecs "EVENT_SPECS"

Write-Host "`n========================================" -ForegroundColor Cyan
if ($totalIssues -eq 0) {
    Write-Host "  VERIFICATION PASSED - No issues found!" -ForegroundColor Green
} else {
    Write-Host "  VERIFICATION FAILED - $totalIssues issues found!" -ForegroundColor Red
}
Write-Host "========================================" -ForegroundColor Cyan
