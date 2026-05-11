# Package Migration Policy

> **Status**: Active rule. PRs adding `.java` files outside the rules below may be asked to relocate.
>
> **Scope**: `app/src/main/java/**` only. The `:vendor-dx` Gradle module (delivered by P1b) and any `pro.sketchware.third_party.*` vendored sources (`kellinwood/` lives there now) are out of scope.

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
| Future logic-editor controllers (P0b output) | `pro.sketchware.activities.editor.logic.*` (extends existing supporting classes such as `BlockPane`, `LogicTopMenu`) |
| Future view-editor controllers (P0c output) | `pro.sketchware.activities.editor.view.*` (extends existing migrated `view/`, `view/item/`, `view/palette/`) |

### 1.2 `com.besome.sketch.*` is ELIMINATED

- v2 Phase 11–15 fully removed `com.besome.sketch.*`. The directory no longer exists in `app/src/main/java/`.
- **No** new `.java` file may be created under `com.besome.sketch.*`. Re-introducing the package (even for "compat" or "to keep diffs small") is rejected on sight.
- The earlier "`com.besome.sketch.beans.*` is permanent" carve-out is **withdrawn** — the bean layer now lives at `pro.sketchware.beans.*` (see § 4.1, Phase 13).
- The only legacy file that physically remains outside `pro.sketchware.*` is `com/bumptech/glide/signature/StringSignature.java`, kept on purpose as a Glide package-private reflection bridge. Do not add anything else next to it.

### 1.3 `mod.*` is FROZEN to mainline contributions

- `mod.agus.jcoderz/`, `mod.hey.studios/`, `mod.hilal.saif/`, `mod.bobur/`, `mod.jbk/`, `mod.pranav/`, `mod.alucard/`, `mod.khaled/`, `mod.remaker/`, `mod.tyron/` are reserved for their respective branch contributors.
- Mainline (non-`mod.*`) PRs **must not** add new `.java` files inside `mod.*`.
- Bug fixes in existing `mod.*` files are allowed; widening their API surface is not.

### 1.4 `dev.aldi.sayuti/` and root-level `kellinwood/` are ELIMINATED; `pro.sketchware.third_party.*` is FROZEN

- Root-level `kellinwood/` was relocated to `pro.sketchware.third_party.kellinwood/` in v2 Phase 11; root-level `dev.aldi.sayuti/` was removed in v1.
- `pro.sketchware.third_party.*` is the new home for vendored / third-author code (currently the kellinwood ZipSigner sources). Treat the whole sub-tree as read-only — touch only for bug fixes against the existing API; do not refactor identifiers, package names, or layout.
- New vendored drops MUST land under `pro.sketchware.third_party.<vendor>/` rather than at the `java/` root.

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

As of mid-2026, both the v1 (`mod.*` / `dev.*` → `pro.sketchware.*`) and v2 (`com.besome.sketch.*` + `kellinwood/` → `pro.sketchware.*`) plans have fully landed. The only legacy file that physically remains outside `pro.sketchware.*` is `com/bumptech/glide/signature/StringSignature.java` (Glide package-private reflection bridge, permanently kept). All rows below marked "✅ Done" are historical and retained for audit / blame context.

### 4.1 Completed migrations

| Legacy location | Target location | Tracking item | Key commit(s) |
|---|---|---|---|
| `pro.sketchware.core.*` (124 flat files) | `pro.sketchware.core.{async,build,callback,codegen,exception,fragments,project,ui,util,validation}` | P0a (`package-refactor-plan.md`, 12 batches) | `a1c07e0e3`, `3cced5ca1`, `d02572af6`, `8fabe2834`, `be9b6a480`, `fe1d86516`, `1f56ddabb`, `e213fab21`, `94c32fb15`, `423fb655b`, `00b2a795b`, `0cc7e3adb` |
| `mod.agus.jcoderz.{dx,dex}/` | `:vendor-dx` Gradle module | P1b | `f9ba8eaf3` |
| `pro.sketchware.lib.validator.*` (9) | `pro.sketchware.core.validation.*` | Path B step 5 | `48c6468ac` |
| `pro.sketchware.utility.*` (25) | `pro.sketchware.util.*` | Path B step 6 | `de5c04f74` |
| `pro.sketchware.{model,listeners,xml,managers}/` + `pro.sketchware.lib.DebouncedClickListener` (8 files / 4 micro-packages) | `pro.sketchware.beans.*` (3 POJOs), `pro.sketchware.util.*` (2 listeners), `pro.sketchware.util.xml.*` (2 builders), `pro.sketchware.tools.*` (1 manager) | Path B step 7 | `34eb36581`, `e2c6b5029`, `c371c0222` |
| `pro.sketchware.core.callback.{Compile,Simple,Sketchware}Exception` (3) | `pro.sketchware.core.exception.*` | Path D step 1 | `bb37d7b19` |
| `kellinwood/` (44 files) | `pro.sketchware.third_party.kellinwood/` | v2 Phase 11 | `0e4b0e1e1` |
| `com.besome.sketch.lib.*` | `pro.sketchware.activities.base.*` + `pro.sketchware.widgets.*` | v2 Phase 12 | `3700c25a5` |
| `com.besome.sketch.beans.*` (29) | `pro.sketchware.beans.*` (unified POJO layer) | v2 Phase 13 | `779902e0b`, `b32cbd171` (zero-byte cleanup) |
| `com.besome.sketch.editor.view.*` (61) | `pro.sketchware.activities.editor.view.*` (incl. `item/` + `palette/`) | v2 Phase 14 | `a0d145938`, `0b7083e7a` |
| `com.besome.sketch.editor.{event,logic,makeblock}` | `pro.sketchware.activities.editor.{event,logic,makeblock}` | v2 Phase 14 | `cc575e45d` |
| `com.besome.sketch.editor.{component,property}` | `pro.sketchware.activities.editor.{component,property}` | v2 Phase 14 | `31dab2dfb` |
| `com.besome.sketch.editor.manage` (excl. library) | `pro.sketchware.activities.editor.manage` | v2 Phase 14 | `ef35aea03` |
| `com.besome.sketch.editor.manage.library` | `pro.sketchware.activities.editor.manage.library` | v2 Phase 14 | `c25bbefcd` |
| `com.besome.sketch.editor.{block,makeblock}` management screens | `pro.sketchware.activities.editor.{block,makeblock}` management screens | v2 Phase 14 | `f6f6d02a8` |
| `com.besome.sketch.editor.{manifest,permission,resource, …}` misc editor screens | `pro.sketchware.activities.editor.*` | v2 Phase 14 | `d1d94b47d` |
| `com.besome.sketch.editor.{settings, dialogs, code editor UI}` | `pro.sketchware.activities.settings.*` / `pro.sketchware.dialogs.*` / consolidated code-editor widgets | v2 Phase 14 | `c793755f2`, `b6946d3b5` |
| `com.besome.sketch.editor.{adapters,ctrls,design,export,help,lib,projects,tools}` | matching `pro.sketchware.<theme>.*` under `pro.sketchware.activities.*` and/or `pro.sketchware.adapters` etc. | v2 Phase 15 | same chain as Phase 14 |
| Residual obfuscated / old-style `util`, `graphics`, custom block/event/component handlers | `pro.sketchware.util.*`, `pro.sketchware.graphics.*`, `pro.sketchware.core.codegen.*` handlers | v2 Phase 15 follow-ups | `3651947e6`, `abe7f6a9d`, `787a90f0c`, `f218ac163`, `bf58e13ee`, `532afd536`, `311530b79`, `0c2054118` |

### 4.2 Still pending

| Legacy location | Target location | Status | Tracking item |
|---|---|---|---|
| `com.besome.sketch.editor.LogicEditorActivity` (now at `pro.sketchware.activities.editor.LogicEditorActivity`, ~147 KB) | `pro.sketchware.activities.editor.logic.*` controllers (Activity stays as thin host) | ⏳ Planned | P0b |
| `pro.sketchware.activities.design.DesignActivity` (same god-class shape) | Same controller-split pattern as P0b | 🟡 Candidate | P0c (not yet scheduled) |
| `pro.sketchware.core.build.ProjectBuilder` (~70 KB god class) | `pro.sketchware.core.build.stage.*` chain | ⏳ Planned | P1a |
| `pro.sketchware.core.build.ProjectFilePaths` 26 IO call sites (writeProjectFile / generateGradleFiles / createLauncherIconXml / copyAppIcon / generateDebugFiles / secrets.xml / codegen cache) | `pro.sketchware.core.codegen.sink.{ProjectArtifactSink,ArtifactKind}` interfaces + `pro.sketchware.core.build.sink.{BuildDir,AndroidStudioExport,SourceViewing,InMemory}Sink` impls | ⏳ Planned | P2a — design in [`p2a-codegen-sink-plan.md`](p2a-codegen-sink-plan.md) |

Legend: ✅ Done · ⏳ Planned (has design doc) · 🟡 Candidate (low-risk, mechanical) · ⏸ Deferred

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
