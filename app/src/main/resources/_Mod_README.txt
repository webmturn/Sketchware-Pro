★★★★★★★★★★★★★★★★★★★★★★★
★★★      Sketchware Pro      ★★★
★★★★★★★★★★★★★★★★★★★★★★★

Sketchware Pro is a community-maintained open-source fork of Sketchware, originally re-modded from "Sketchware Revolution" by Agus JCoderZ. The full source lives on GitHub and pull requests are welcome.

For the current version, release notes, active maintainers, and full credits, look at:

  - In-app: About → Team / Change Log
  - Repository: https://github.com/Sketchware-Pro/Sketchware-Pro/
  - RELEASE_NOTES.md, LICENSE.md, and docs/ at the repository root

This file is a static APK resource and is intentionally NOT the source of truth for those details — anything below is kept as a historical credits snapshot, not a live roster.


Main team (historical snapshot):

  - Aldi Sayuti
  - Hilal Saif
  - Mike Anderson (Hey! Studios)
  - Jbk0
  - Hasrat


Contributors & special thanks (historical snapshot):

  - IndoSW                (Direct Code Editor support)
  - Agus JCoderZ          (base mod, Sketchware Revolution)
  - Auwal Emptyset        (library downloading source)
  - Ani1nonly
  - Dava
  - Ilyasse Salama        (new About Sketchware Pro activity)
  - Zarzo
  - AlucardTN             (miscellaneous fixes)
  - tyron                 (.swb file Intent filter)
  - Iyxan23               (miscellaneous fixes)
  - Aliveness             (improved pretty-print & replaceable generated files)
  - khaled                (Android 11+ storage performance fix and other fixes)
  - Zirus                 (improved project backup dialog)
  - Pranav                (improved MainActivity)

For the up-to-date team, contributors, and changelog, please use the in-app About screen and the GitHub repository above rather than this file.

**** FOR MODDERS ****

The original mod was structured around heavily obfuscated classes (Fx, Jx, Lx, Ox, Dp, yq, ...) and a forest of per-contributor packages (mod.*, dev.aldi.*, id.indosw.*). That layout no longer exists. Both refactors are complete:

  - v1: all contributor namespaces (mod.agus, mod.hey, mod.hilal, mod.jbk, mod.pranav, mod.tyron, mod.bobur, mod.khaled, mod.alucard, mod.remaker, dev.aldi) folded into pro.sketchware.*
  - v2: the entire com.besome.sketch.* tree migrated into pro.sketchware.*; the obfuscated a.a.a package became pro.sketchware.core and every class/method/field was renamed to a readable name.

Today essentially all Sketchware Pro source lives under a single business package: pro.sketchware.*. The only legacy survivor on disk is com/bumptech/glide/signature/StringSignature.java, kept on purpose as a package-private reflection bridge for Glide.

Where things live now (high level):

  - pro.sketchware.SketchApplication        Application entry point
  - pro.sketchware.activities.*             All Activities/Fragments (editor, design, settings, projects, ...)
  - pro.sketchware.core.*                   Project model, code generation, build pipeline, validators, async helpers
  - pro.sketchware.core.codegen.*           Block / event / layout / manifest / component code generators
  - pro.sketchware.core.build.*             Project builder, incremental cache, key store, compiler/multidex wrappers
  - pro.sketchware.core.project.*           ProjectDataStore, ProjectFileManager, LibraryManager, ResourceManager, SketchwarePaths
  - pro.sketchware.library.*                Built-in libraries, local library, dependency resolver
  - pro.sketchware.util.*                   Shared utilities
  - pro.sketchware.widgets.* / lib.*        Custom views and editor-feature scaffolding
  - pro.sketchware.third_party.kellinwood.* Vendored ZipSigner sources

For the authoritative, up-to-date map please read:

  - docs/package-target-architecture.md     current target layout + open refactor items
  - docs/package-migration-policy.md        hard rules for where new .java files go
  - docs/package-naming-policy.md           naming conventions for new packages
  - docs/DEVELOPMENT.md                     full developer handbook

The DEX layout produced by the release build is now decided by R8/D8 sharding rather than the hand-curated classes1..classes9 split described in the previous version of this file, so per-DEX class lists here are no longer meaningful.
