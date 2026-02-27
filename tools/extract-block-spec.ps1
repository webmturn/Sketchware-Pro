# Extract all mappings from BlockSpecRegistry.java
# Parses the decompiled hashCode-based switch statements

$file = Get-Content "C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\src\main\java\pro\sketchware\core\BlockSpecRegistry.java" -Raw

# ===== Extract getBlockParams mappings =====
# Step 1: Build opCode -> matchIndex from hashCode switch
$blockParamsMap = @{}  # matchIndex -> opCode
$matches = [regex]::Matches($file, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
foreach ($m in $matches) {
    $opCode = $m.Groups[1].Value
    $idx = $m.Groups[2].Value
    if (-not $blockParamsMap.ContainsKey($idx)) {
        $blockParamsMap[$idx] = $opCode
    }
}

Write-Host "Found $($blockParamsMap.Count) unique opCode->matchIndex mappings"

# Step 2: Extract matchIndex -> params from second switch
# Pattern: case NNN: params.add("..."); ... result = params; break;
$getBlockParamsSection = $file.Substring(0, $file.IndexOf("public static ArrayList<String> getBlockMenuItems"))

# Find all case blocks with params
$paramCases = [regex]::Matches($getBlockParamsSection, 'case (\d+):\s*((?:params\.add\("[^"]+"\);\s*)+)')
Write-Host "Found $($paramCases.Count) param case blocks"

$paramsData = @{} # matchIndex -> param list
foreach ($pc in $paramCases) {
    $idx = $pc.Groups[1].Value
    $body = $pc.Groups[2].Value
    $paramList = [regex]::Matches($body, 'params\.add\("([^"]+)"\)')
    $params = @()
    foreach ($p in $paramList) {
        $params += $p.Groups[1].Value
    }
    $paramsData[$idx] = $params
}

# Merge: opCode -> params
Write-Host "`n===== BLOCK_PARAMS ====="
$blockParams = @{}
foreach ($idx in $blockParamsMap.Keys) {
    $opCode = $blockParamsMap[$idx]
    if ($paramsData.ContainsKey($idx)) {
        $blockParams[$opCode] = $paramsData[$idx]
    }
}

# Sort and output
$sortedKeys = $blockParams.Keys | Sort-Object
foreach ($key in $sortedKeys) {
    $params = $blockParams[$key] -join '", "'
    Write-Host "BLOCK_PARAMS.put(`"$key`", new String[]{`"$params`"});"
}
Write-Host "Total: $($blockParams.Count)"

# ===== Extract getBlockSpec mappings =====
Write-Host "`n===== BLOCK_SPECS ====="
$getBlockSpecSection = $file.Substring($file.IndexOf("public static String getBlockSpec"))

# Build opCode -> matchIndex from the getBlockSpec hashCode switch
$specIndexMap = @{} # matchIndex -> opCode
$specMatches = [regex]::Matches($getBlockSpecSection, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
foreach ($m in $specMatches) {
    $opCode = $m.Groups[1].Value
    $idx = $m.Groups[2].Value
    if (-not $specIndexMap.ContainsKey($idx)) {
        $specIndexMap[$idx] = $opCode
    }
}

# Extract matchIndex -> specKey from second switch
$specCases = [regex]::Matches($getBlockSpecSection, 'case (\d+):\s*blockName\s*=\s*"([^"]+)";\s*break;')
$specData = @{} # matchIndex -> specKey
foreach ($sc in $specCases) {
    $idx = $sc.Groups[1].Value
    $specKey = $sc.Groups[2].Value
    $specData[$idx] = $specKey
}

# Merge
$blockSpecs = @{}
foreach ($idx in $specIndexMap.Keys) {
    $opCode = $specIndexMap[$idx]
    if ($specData.ContainsKey($idx)) {
        $blockSpecs[$opCode] = $specData[$idx]
    }
}

$sortedSpecKeys = $blockSpecs.Keys | Sort-Object
foreach ($key in $sortedSpecKeys) {
    Write-Host "BLOCK_SPECS.put(`"$key`", `"$($blockSpecs[$key])`");"
}
Write-Host "Total: $($blockSpecs.Count)"

# ===== Extract getBlockMenuItems (events) =====
Write-Host "`n===== EVENT_MENUS ====="
$menuSection = $file.Substring(
    $file.IndexOf("public static ArrayList<String> getBlockMenuItems"),
    $file.IndexOf("public static String getEventSpec") - $file.IndexOf("public static ArrayList<String> getBlockMenuItems")
)

$eventIndexMap = @{}
$eventMatches = [regex]::Matches($menuSection, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
foreach ($m in $eventMatches) {
    $eventName = $m.Groups[1].Value
    $idx = $m.Groups[2].Value
    if (-not $eventIndexMap.ContainsKey($idx)) {
        $eventIndexMap[$idx] = $eventName
    }
}

$menuCases = [regex]::Matches($menuSection, 'case (\d+):\s*((?:menuItems\.add\("[^"]+"\);\s*)+)')
$menuData = @{}
foreach ($mc in $menuCases) {
    $idx = $mc.Groups[1].Value
    $body = $mc.Groups[2].Value
    $items = [regex]::Matches($body, 'menuItems\.add\("([^"]+)"\)')
    $menuList = @()
    foreach ($item in $items) {
        $menuList += $item.Groups[1].Value
    }
    $menuData[$idx] = $menuList
}

$eventMenus = @{}
foreach ($idx in $eventIndexMap.Keys) {
    $eventName = $eventIndexMap[$idx]
    if ($menuData.ContainsKey($idx)) {
        $eventMenus[$eventName] = $menuData[$idx]
    }
}

$sortedEventKeys = $eventMenus.Keys | Sort-Object
foreach ($key in $sortedEventKeys) {
    $items = $eventMenus[$key] -join '", "'
    Write-Host "EVENT_MENUS.put(`"$key`", new String[]{`"$items`"});"
}
Write-Host "Total: $($eventMenus.Count)"

# ===== Extract getEventSpec =====
Write-Host "`n===== EVENT_SPECS ====="
$eventSpecSection = $file.Substring(
    $file.IndexOf("private static String computeC"),
    $file.IndexOf("public static String getBlockSpec") - $file.IndexOf("private static String computeC")
)

$eventSpecIndexMap = @{}
$esMatches = [regex]::Matches($eventSpecSection, 'blockName\.equals\("([^"]+)"\)\s*\)\s*\{\s*matchIndex\s*=\s*(\d+);')
foreach ($m in $esMatches) {
    $eventName = $m.Groups[1].Value
    $idx = $m.Groups[2].Value
    if (-not $eventSpecIndexMap.ContainsKey($idx)) {
        $eventSpecIndexMap[$idx] = $eventName
    }
}

# return statements
$returnCases = [regex]::Matches($eventSpecSection, 'case (\d+):\s*return\s*"([^"]+)";')
$eventSpecData = @{}
foreach ($rc in $returnCases) {
    $idx = $rc.Groups[1].Value
    $specKey = $rc.Groups[2].Value
    $eventSpecData[$idx] = $specKey
}

$eventSpecs = @{}
foreach ($idx in $eventSpecIndexMap.Keys) {
    $eventName = $eventSpecIndexMap[$idx]
    if ($eventSpecData.ContainsKey($idx)) {
        $eventSpecs[$eventName] = $eventSpecData[$idx]
    }
}

$sortedESKeys = $eventSpecs.Keys | Sort-Object
foreach ($key in $sortedESKeys) {
    Write-Host "EVENT_SPECS.put(`"$key`", `"$($eventSpecs[$key])`");"
}
Write-Host "Total: $($eventSpecs.Count)"
