# Package Migration Policy

> **Status**: Active rule. PRs adding `.java` files outside the rules below may be asked to relocate.
>
> **Scope**: `app/src/main/java/**` only. The `vendor-dx` module (planned by P1b) and `kellinwood/`, `dev/aldi/sayuti/`, `mod/agus/jcoderz/dx/` vendored sources are out of scope.

This document is the implementation of the P2b item in [`package-target-architecture.md`](package-target-architecture.md). It exists so a fresh contributor can answer "where does my new file go?" in under a minute, and so reviewers have a single citation when the answer is "not here".

---

## 1. Hard rules for new code

### 1.1 New code MUST live under `pro.sketchware.*`

Any new top-level `.java` (Activity, Fragment, Manager, Controller, View, Util, Bean, Dialog, Listener, etc.) **must** be created in a sub-package of `pro.sketchware.*`.

Acceptable destinations (by responsibility):

| Responsibility | Target package |
|---|---|
| Project data model / store / parser | `pro.sketchware.core.project.*` |
| Code generation (Java / XML / Gradle) | `pro.sketchware.core.codegen.*` |
| APK build pipeline (resources / Java / DEX / sign) | `pro.sketchware.core.build.*` |
| Background task helpers | `pro.sketchware.core.async.*` |
| Generic utilities (no project / Android specific dependency) | `pro.sketchware.core.util.*` |
| Input validators (extending `BaseValidator`) | `pro.sketchware.core.validation.*` |
| Editor-internal fragments | `pro.sketchware.core.fragments.*` |
| Block / palette UI views | `pro.sketchware.core.ui.*` |
| Callbacks / listener interfaces / exceptions used by `core.*` | `pro.sketchware.core.callback.*` |
| Top-level Activities (App-level entry points) | `pro.sketchware.activities.<theme>.*` |
| App-level Settings fragments | `pro.sketchware.fragments.settings.*` |
| Dialogs not tied to Logic/View editor | `pro.sketchware.dialogs.*` |
| Future logic-editor controllers (P0b output) | `pro.sketchware.editor.logic.*` |
| Future view-editor controllers | `pro.sketchware.editor.view.*` |

### 1.2 `com.besome.sketch.*` is FROZEN

- **No** new `.java` file may be added under `com.besome.sketch.*`.
- Bug fixes that add a method to an existing class **are allowed**.
- Renaming, splitting, or extending the API of an existing class **must** put the new file in `pro.sketchware.*` (the new file then references the legacy class until that is migrated separately).
- **Exception**: `com.besome.sketch.beans.*` is the cross-package POJO layer. New `.java` files here are still discouraged; prefer `pro.sketchware.beans/`. New beans here are accepted only when an existing legacy bean must be subclassed/extended in place.

### 1.3 `mod.*` is FROZEN to mainline contributions

- `mod.agus.jcoderz/`, `mod.hey.studios/`, `mod.hilal.saif/`, `mod.bobur/`, `mod.jbk/`, `mod.pranav/`, `mod.alucard/`, `mod.khaled/`, `mod.remaker/`, `mod.tyron/` are reserved for their respective branch contributors.
- Mainline (non-`mod.*`) PRs **must not** add new `.java` files inside `mod.*`.
- Bug fixes in existing `mod.*` files are allowed; widening their API surface is not.

### 1.4 `dev.aldi.sayuti/` and `kellinwood.*` are FROZEN entirely

These are single-author or vendored libraries. Treat them as read-only. Touch only for bug fixes against the existing API.

---

## 2. Naming conventions

| Element | Convention | Example |
|---|---|---|
| Package segment | lowercase, single word, **no underscores**, **no `utility`** (use `util`) | `pro.sketchware.core.codegen` |
| `.java` file under `pro.sketchware.*` | PascalCase, descriptive (no obfuscated single letters) | `BlockSpecRegistry.java` |
| Manager / Controller / Validator | `Foo + Manager` / `Foo + Controller` / `Foo + Validator` suffix | `LibraryManager.java`, `LogicPaletteController.java`, `IdentifierValidator.java` |
| Util / Helper | `Foo + Util.java` (preferred) or `Foo + Helper.java` (when stateful) | `FormatUtil.java`, `MapValueHelper.java` |
| Bean / POJO | `Foo + Bean.java` | `BlockBean.java`, `EventBean.java` |
| Callback / Listener interface | `Foo + Callback.java` / `Foo + Listener.java` | `BuildCallback.java`, `BlockSizeListener.java` |

---

## 3. Sub-package depth budget

- Top-level `pro.sketchware/` should keep **≤ 12** direct sub-packages.
- A new top-level sub-package is acceptable only when:
  1. It will hold **≥ 3** files at landing time, AND
  2. None of the existing sub-packages fit by responsibility.
- A 1-file sub-package created "for future expansion" is rejected. Park the file in the closest existing sub-package and split later when files accumulate.

---

## 4. Migration tracker

Tracks the legacy → modern migration progress. **Do not** edit ad hoc; update only when a sub-package finishes a planned refactor batch.

| Legacy location | Target location | Status | Tracking item |
|---|---|---|---|
| `pro.sketchware.core.*` (124 flat) | `pro.sketchware.core.{async,build,callback,codegen,fragments,project,ui,util,validation}` | ✅ Done | P0a (`package-refactor-plan.md`) |
| `com.besome.sketch.editor.LogicEditorActivity` | `pro.sketchware.editor.logic.*` controllers (Activity stays) | ⏳ Planned | P0b |
| `com.besome.sketch.editor.DesignActivity` | Same pattern as P0b | 🟡 Candidate | P0c (not yet scheduled) |
| `mod.agus.jcoderz.dx/` | `:vendor-dx` Gradle module | ⏳ Planned | P1b |
| `pro.sketchware.core.build.ProjectBuilder` | `pro.sketchware.core.build.stage.*` chain | ⏳ Planned | P1a |
| `pro.sketchware.core.codegen.*` direct file writes | `pro.sketchware.core.codegen.sink.*` interfaces + `pro.sketchware.core.build.sink.*` impls | ⏳ Planned | P2a |
| `pro.sketchware.lib.validator.*` (9) | `pro.sketchware.core.validation.*` | ✅ Done (commit `48c6468ac`) | Path B step 5 |
| `pro.sketchware.utility.*` (25) | `pro.sketchware.util.*` | 🟡 Candidate | Path B step 6 |
| `com.besome.sketch.beans.*` (29) | **Permanent**; cross-package POJO layer | 🛡 Frozen | Per § 1.2 exception |
| `com.besome.sketch.editor.view.*` (61) | Long-term: `pro.sketchware.editor.view.*` | ⏸ Deferred | Post-P0b/P0c |
| `com.besome.sketch.editor.{adapters,ctrls,design,export,help,lib,projects,tools}` | Long-term: merge into matching `pro.sketchware.<theme>.*` | ⏸ Deferred | Post-P0c |

Legend: ✅ Done · ⏳ Planned (has design doc) · 🟡 Candidate (low-risk, mechanical) · ⏸ Deferred · 🛡 Frozen

---

## 5. PR review checklist

Reviewers should mentally tick these for any PR that adds new `.java` files:

- [ ] No new `.java` file under `com.besome.sketch.*` (except `com.besome.sketch.beans/` per § 1.2 exception).
- [ ] No new `.java` file under `mod.*`, `dev.aldi.sayuti/`, or `kellinwood.*`.
- [ ] New file lives in a sub-package matching its responsibility per § 1.1 table.
- [ ] No new top-level sub-package under `pro.sketchware/` that ships with fewer than 3 files (§ 3).
- [ ] File name follows the suffix conventions in § 2 (`*Manager`, `*Controller`, `*Util`, `*Bean`, `*Callback`, `*Validator`, …).
- [ ] No "obfuscation-style" identifiers (single letters, numeric suffixes like `Foo1`/`Foo2`) introduced.
- [ ] If the PR is a refactor that **moves** files, update the relevant row in § 4 in the same PR.

---

## 6. Anti-patterns (rejected on sight)

- Creating `pro.sketchware.helpers/`, `pro.sketchware.utilities/`, `pro.sketchware.misc/`, `pro.sketchware.common/`, or any other "everything goes here" bucket.
- Mirroring an existing sub-package name with a singular/plural variant (`util` vs `utils`, `validator` vs `validation`, `model` vs `models`).
- Adding a class named `Foo.java` that is solely a wrapper / facade over `pro.sketchware.core.Foo.java` for "compat".
- Re-introducing the `a.a.a` style of single-letter file names (the 2026 deobfuscation campaign explicitly removed all of them; see [`refactoring-naming-map.md`](refactoring-naming-map.md)).
- Putting a new `.java` directly inside `pro.sketchware.core/` (the root must stay empty per P0a's exit condition; new code goes to one of the 9 sub-packages).

---

## 7. References

- [`package-target-architecture.md`](package-target-architecture.md) — full P0/P1/P2 target architecture
- [`package-refactor-kickoff.md`](package-refactor-kickoff.md) — multi-session refactor kickoff pack
- [`package-refactor-plan.md`](package-refactor-plan.md) — P0a Phase 1 plan output (12 batches, 9 sub-packages, DAG)
- [`refactoring-naming-map.md`](refactoring-naming-map.md) — historical `a.a.a` → readable name map
- Top-level [`README.md`](../README.md) `Contributing` section — informal hint pointing here for the formal rule
