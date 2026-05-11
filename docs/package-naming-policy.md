# Package Naming Policy

> **Status**: active. Applies to all new code merged to `main` after this document lands.
>
> **Audience**: contributors, reviewers, automated PR checks.
>
> **Origin**: Defined as the P2b deliverable of `docs/package-target-architecture.md`. See that document for context on the v1 / v2 package migrations that brought the codebase to its current state.

---

## 1. Background — why this policy exists

The codebase has gone through two large package migrations:

- **v1** (167 files): `mod.*` / `dev.*` → `pro.sketchware.*`, `a.a.a` → `pro.sketchware.core`
- **v2** (~330 files, 13 commits): `com.besome.sketch.*` → `pro.sketchware.*`, `kellinwood/` → `pro.sketchware.third_party.kellinwood/`

After v2 the entire business codebase is unified under **`pro.sketchware.*`**. The only legacy survivor is `com/bumptech/glide/signature/StringSignature.java` (Glide package-private reflection bridge, permanently kept).

Without an explicit policy, the old top-level packages can re-grow through:

- Pull requests pasting code from older forks / branches
- IDE auto-import completing to a stale FQN
- Copy-paste from old StackOverflow answers using the original package layout
- Vendor third-party drops landing at the root of `java/` instead of under `third_party/`

This document declares the hard rules and reviewer checklist that prevent regression.

---

## 2. Hard rules for new code

These rules apply to every new file added to `app/src/main/java/`. Violations should block PR merge.

### 2.1 Allowed top-level packages

A new `.java` or `.kt` file MUST be placed under exactly one of:

| Top-level | Purpose |
|-----------|---------|
| `pro.sketchware.*` | All new business / UI / utility code |
| `com.bumptech.glide.signature.*` | **Frozen** — extension only by explicit Glide upgrade decision |

### 2.2 Forbidden top-level packages (no new files)

| Top-level | Status | Why forbidden |
|-----------|--------|---------------|
| `com.besome.sketch.*` | Removed by v2 | Re-introducing would split the namespace again |
| `mod.*` | Removed by v1 | Same |
| `dev.*` | Removed by v1 | Same |
| `kellinwood.*` (bare) | Relocated by v2 | Use `pro.sketchware.third_party.kellinwood.*` |
| `a.*`, `b.*`, `c.*`, …, `aaa.*` | Removed by deobfuscation | Obfuscation artifact; never re-add |

### 2.3 Sub-package conventions inside `pro.sketchware.*`

| Pattern | Use for |
|---------|---------|
| `pro.sketchware.activities.<feature>` | Activities / Fragments owned by a specific feature module |
| `pro.sketchware.activities.<feature>.<role>` | Controllers, adapters, dialogs internal to one feature |
| `pro.sketchware.core.<layer>` | Headless business logic. `<layer>` ∈ `{async, build, callback, codegen, ctrls, exception, fragments, project, ui, validation}` |
| `pro.sketchware.util.<kind>` | Stateless helpers. `<kind>` ∈ `{apk, format, io, library, relativelayout, theme, xml}`. Top-level `util/` accepted but discouraged for new files |
| `pro.sketchware.widgets` | Concrete reusable UI widgets (instantiable, possibly XML-tag consumed) |
| `pro.sketchware.widgets.base` | Abstract base widget classes (subclassed, not instantiated) |
| `pro.sketchware.lib.<subsystem>` | Editor-subsystem-internal infrastructure (`code_editor`, `highlighter`, `iconcreator`) |
| `pro.sketchware.beans` | Cross-package shared POJOs (data classes only) |
| `pro.sketchware.third_party.<vendor>` | Vendored third-party source. New vendor drops MUST land under a `<vendor>` sub-package |

> When in doubt about the right sub-package, see the per-package guidance in `package-info.java` files (`lib/`, `widgets/`, `widgets/base/`) and the architecture overview in `docs/package-target-architecture.md` § 0.3 – § 0.5.

### 2.4 Naming style (within a package)

- Public classes: `UpperCamelCase`
- Controllers: `<Theme>Controller` (e.g. `LogicPaletteController`)
- Stages (build pipeline): `<Theme>Stage` (e.g. `DexStage`)
- Sinks (codegen output): `<Theme>Sink` interface + `File<Theme>Sink` implementation
- Managers (long-lived state): `<Theme>Manager` (e.g. `BlockHistoryManager`)
- Helpers (stateless static): `<Theme>Util` or `<Theme>Helper`

Avoid:

- Generic names: `Helper`, `Util`, `Manager`, `Common`, `Misc`, `Stuff` — always prefix with the theme
- Single-letter or 2-letter classes — obfuscation residue, never re-add
- `XxxImpl` suffix unless there is a genuine `Xxx` interface in the same package

---

## 3. Exceptions (case-by-case, must be documented)

| Exception | Reason | Required action |
|-----------|--------|-----------------|
| New file under `com.bumptech.glide.signature.*` | Genuine Glide upgrade extending the signature API surface | Include rationale in commit message; mention this policy |
| New file under `pro.sketchware.third_party.<existing-vendor>` | Vendor source update | Include vendor version in commit message |
| New file under `pro.sketchware.third_party.<new-vendor>` | New vendor drop | Update `docs/package-target-architecture.md` § 0.3 table |
| Modifying a `com/bumptech/glide/...` file in place | Bug fix / Glide version sync | Allowed; no new files added |

Anything else requires:

1. Explicit reviewer sign-off
2. A commit-message paragraph explaining why the standard rules do not apply
3. Updating this policy if the exception sets a new precedent

---

## 4. Reviewer checklist

When reviewing a PR that touches `app/src/main/java/`, verify:

- [ ] No new files outside `pro.sketchware.*` (sole exception: documented Glide signature additions)
- [ ] No new `import com.besome.sketch.*`, `import mod.*`, `import dev.*`, or bare `import kellinwood.*`
- [ ] Sub-package choice follows § 2.3 (run a mental walk: which existing class is the closest neighbour?)
- [ ] Class naming follows § 2.4 (no generic helpers, no obfuscation residue)
- [ ] If a new sub-package is created with only 1 file, the author justifies why it is semantically independent (see audit finding on `firebase/`, `common/` in v2 cleanup)
- [ ] If a class exceeds 50 KB, flag for follow-up split (god-class threshold; see `package-target-architecture.md` § 9)

A passing PR is allowed to introduce new files; a failing PR should be sent back with a quoted reference to the violated rule(s).

---

## 5. Automation (optional, recommended)

The rules in this document are reviewable by humans, but automation prevents regression at scale.

### 5.1 GitHub Action (recommended)

A workflow that runs on every PR and fails if forbidden paths gain new files:

```yaml
# .github/workflows/package-naming.yml (sketch — not yet committed)
name: Package naming policy
on: [pull_request]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Fail if forbidden top-level packages gained new files
        run: |
          BASE=${{ github.event.pull_request.base.sha }}
          HEAD=${{ github.event.pull_request.head.sha }}
          FORBIDDEN=$(git diff --name-only --diff-filter=A "$BASE..$HEAD" \
            | grep -E '^app/src/main/java/(com/besome/sketch|mod|dev|kellinwood)/' || true)
          if [ -n "$FORBIDDEN" ]; then
            echo "::error::New files in forbidden top-level packages:"
            echo "$FORBIDDEN"
            exit 1
          fi
```

### 5.2 Local pre-commit hook (optional)

Same idea but on `git commit`:

```bash
# .git/hooks/pre-commit (developer-installed)
forbidden=$(git diff --cached --name-only --diff-filter=A \
  | grep -E '^app/src/main/java/(com/besome/sketch|mod|dev|kellinwood)/' || true)
if [ -n "$forbidden" ]; then
  echo "Forbidden new files (see docs/package-naming-policy.md):"
  echo "$forbidden"
  exit 1
fi
```

### 5.3 Static analysis (long-term)

If detekt or Checkstyle is adopted, mirror the rules:

- detekt: `ForbiddenImport` rule listing `com.besome.sketch.*`, `mod.*`, `dev.*`
- Checkstyle: `IllegalImport` module with the same list

---

## 6. Rule lifecycle

This policy is not eternal:

- **2026 (current)**: Hard rules ban any new file in forbidden packages
- **After P0b / P0c land**: Controllers consolidated; § 2.3 may add `pro.sketchware.activities.<feature>.controller` as a recommended sub-package for new feature work
- **After P1a / P2a land**: § 2.3 will list `pro.sketchware.core.build.stage` and `pro.sketchware.core.codegen.sink` as canonical locations for Stage / Sink contributions
- **Annual**: Re-validate that no v1 / v2 era assumptions still hold and that no rule is dead-weight

Updates to this policy require:

1. A commit dedicated to the rule change
2. A note in the commit message describing what is added / loosened / tightened
3. A corresponding update in `docs/package-target-architecture.md` § 7 if a structural rule changes

---

## 7. References

- Architecture overview: `docs/package-target-architecture.md`
- v1 plan (history): `docs/package-migration-plan.md`
- v2 plan (history): `docs/package-migration-plan-v2.md`
- Migration policy (procedural, predates this naming policy): `docs/package-migration-policy.md`
- Deobfuscation map: `docs/refactoring-naming-map.md`
- Audit history: `docs/android-development-tool-audit-review.md`
