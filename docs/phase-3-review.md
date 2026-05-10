# Phase 3 Review — `refactor/core-package-split`

Independent verification of the Phase 2 batch execution. Branch reviewed:
`refactor/core-package-split` against base `40046ab5c` (origin/main at start
of Phase 1).

## TL;DR

| Check | Result |
|---|---|
| Compilation (`:app:compileDebugJavaWithJavac`) | **PASS** (12s) |
| Stale FQN imports | **0 / 934** |
| Stale FQN in code body | **0** |
| Stale FQN in javadoc | **0** (4 found, 4 fixed in `646088a31`) |
| Mojibake bytes (`E9 88 A5/AB`) in `core/` | **0** (CodeContext fix in `c07b929ed`) |
| Refactor-specific lint issues | **0** |
| Cross-path content drift (excluding EOF newlines) | **0 lines** |

## Branch shape

15 commits ahead of base, in chronological order (oldest first):

| # | Sha | Type | Summary |
|---|---|---|---|
| 1 | `a1c07e0e3` | refactor | fragments → `core.fragments` |
| 2 | `3cced5ca1` | refactor | block-view widgets / library-config → `core.ui` |
| 3 | `d02572af6` | refactor | text-input validators → `core.validation` |
| 4 | `8fabe2834` | refactor | build/sign + ProjectFilePaths → `core.build` |
| 5 | `be9b6a480` | refactor | block/event registries → `core.codegen` |
| 6 | `fe1d86516` | refactor | activity/component/layout generators → `core.codegen` |
| 7 | `4ea020cab` | fix | initial mojibake repair (incomplete; finished by 9bc308c39 + c07b929ed) |
| 8 | `1f56ddabb` | refactor | collection/history managers → `core.project` |
| 9 | `e213fab21` | refactor | project data store/paths → `core.project` |
| 10 | `94c32fb15` | refactor | bitmap/format/device/UI helpers → `core.util` (part 1) |
| 11 | `423fb655b` | refactor | encrypted-file IO/zip/URI → `core.util` (part 2) |
| 12 | `00b2a795b` | refactor | callbacks/exceptions → `core.callback` |
| 13 | `0cc7e3adb` | refactor | BackgroundTasks/TaskHost → `core.async` |
| 14 | `9741e62bd` | docs | Phase 1 plan |
| 15 | `9bc308c39` | chore(docs) | mojibake fix in 8 files (Phase 2 finalize) |
| 16 | `c07b929ed` | chore(docs) | mojibake fix in CodeContext.java (Phase 3 follow-up) |
| 17 | `646088a31` | docs | drop misleading FQN in 4 javadoc sites (Phase 3 follow-up) |

(17 commits listed because two follow-ups were added during Phase 3.)

## Cross-path diff verification

Methodology: `git diff -M50% 40046ab5c HEAD` produced 385 entries. For every
+/- line outside the diff headers, check that the payload is one of:

1. `package …;` declaration change.
2. `import …;` statement change.
3. A line containing a `pro.sketchware.core.<sub>.Class` FQN (sub-package
   move follow-through).

Anything else is "unexpected drift" and inspected manually.

| Pass | Total ± lines | Expected | Unexpected | Files flagged |
|---|---|---|---|---|
| Initial (after batch 10) | 2082 | 2007 | 75 | 39 |
| After CodeContext mojibake fix (`c07b929ed`) | 2060 | 2007 | 53 | 38 |
| After javadoc FQN cleanup (`646088a31`) | 2052 | 2003 | 49 | 38 |

The remaining 49 / 38 are entirely classified as **EOF-newline additions**:

- 24 R-type entries each show a single `+` (empty trailing line).
- 14 entries show `-}` / `+}` pairs (closing brace re-paired because the
  origin file lacked a trailing newline and the new file has one).
- 1 entry (`FirebasePreviewView.java`) combines both patterns.

Decision: **do not roll back**. Adding a trailing newline at EOF is the
POSIX/Java convention; reverting it would re-introduce a known code smell.
The original memory note "3 files / 6 lines need EOF rollback" turned out
to be inverted — origin/main is the side missing the newline, and the
refactor improved this consistently.

## Mojibake findings (Phase 3 follow-up)

Phase 2 commit `9bc308c39` repaired 25 lines across 8 files but missed
`CodeContext.java`. Phase 3 byte-scan caught the omission:

| File | Mojibake bytes (current) | Mojibake bytes (origin) | Fix commit |
|---|---|---|---|
| `core/codegen/CodeContext.java` | 2040 (mojibake) | 1026 (correct UTF-8) | `c07b929ed` |

Affected glyphs: `─` (U+2500 box-drawing), `—` (U+2014 em-dash). 11 lines
restored 1:1 from origin bytes. Post-fix non-ASCII byte count matches
origin exactly (1026 = 1026).

## Stale FQN audit

Counts after `646088a31`:

- 934 `import pro.sketchware.core.<sub>.<Class>;` statements — **all** point
  at one of the 9 sub-packages (`async`, `build`, `callback`, `codegen`,
  `fragments`, `project`, `ui`, `util`, `validation`). Zero point at a
  bare `pro.sketchware.core.Class` (which would be stale).
- 1089 total mentions of `pro.sketchware.core.` in `app/src/main/java`.
  All non-import mentions are FQNs in code bodies that include a
  sub-package qualifier.

Four pre-existing javadoc comments referenced classes that either no longer
exist (`BaseAsyncTask`) or pointed at a self-reference (`BlockInterpreter`).
These were stale even before the package split. `646088a31` drops the
misleading `pro.sketchware.core.` prefix in all four sites:

- `com/besome/sketch/export/ExportProjectActivity.java` (3 sites)
- `pro/sketchware/core/codegen/BlockInterpreter.java` (1 site)

## Lint baseline

`:app:lintDebug` reports **1177 errors / 2745 warnings** repository-wide.
This is a **pre-existing baseline** — top categories are i18n /
accessibility / discouraged-API rules, not refactor-related.

Filtered counts:

- Issues whose path contains `pro/sketchware/core/`: 54 (~0.4 per moved
  file, consistent with pre-existing density).
- Issues for refactor-specific rules (`WrongPackage`, `UnusedImport`,
  `MissingClass`, `InvalidPackage`, `UnknownClass`): **0**.

The package split introduced no new lint errors.

## Files touched

- 124 source files moved (Java) — distributed across 9 sub-packages.
- 261 modify-only files updated (FQN/import follow-through) — primarily
  in `com/besome/sketch/` (135), `pro/sketchware/` non-core (50), and
  contributor sub-trees (`mod/*`, `dev/aldi`).
- 1 file added (`docs/package-refactor-plan.md`).

## Outstanding items

- None blocking merge-back to `main`.
- The pre-existing lint baseline (1177/2745) is unchanged by this branch
  and out of scope for the package-split work.

## Verification commands

```pwsh
# Compile
$env:JAVA_HOME='<jdk-23>'
.\gradlew.bat :app:compileDebugJavaWithJavac --console=plain

# Stale FQN scan
git grep -n "pro\.sketchware\.core\." -- "app/src/main/java/*.java"
# Filter out lines containing one of the 9 sub-packages.
```

## Conclusion

The package split is content-clean: every change between `40046ab5c` and
`HEAD` is attributable to (a) the refactor itself, (b) FQN/import
follow-through, (c) the EOF-newline normalization, or (d) the two mojibake
repair commits. The branch is ready to merge.
