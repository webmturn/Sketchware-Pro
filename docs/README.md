# Sketchware-Pro Documentation

This directory contains internal documentation for the Sketchware-Pro project.

Documents are grouped by purpose so that long-term references, active proposals, and historical notes are easier to distinguish.

## Core Development Guides

| Document | Description |
|----------|-------------|
| [DEVELOPMENT.md](DEVELOPMENT.md) | Comprehensive development handbook covering project overview, environment setup, architecture, code generation, build system, and contribution guidelines |
| [sketchware-project-data-format.md](sketchware-project-data-format.md) | Data format specification for Sketchware project files (JSON schemas, file structure, quick-start checklist) |
| [upload-file-guide.md](upload-file-guide.md) | User guide for implementing image upload to remote servers using FilePicker and RequestNetwork |

## Architecture & Capability Analysis

| Document | Description |
|----------|-------------|
| [code-generation-system-analysis.md](code-generation-system-analysis.md) | Deep dive into the code generation pipeline: BlockInterpreter, ActivityCodeGenerator, EventCodeGenerator, ComponentCodeGenerator |
| [builtin-blocks-analysis.md](builtin-blocks-analysis.md) | Historical 2026-02-27 snapshot of the built-in block catalog and gap analysis; not a current complete capability list |
| [block-programming-bottleneck-analysis.md](block-programming-bottleneck-analysis.md) | Analysis of block programming system bottlenecks and extension capabilities |
| [ui-designer-analysis.md](ui-designer-analysis.md) | Deep dive into the UI designer: ViewEditorFragment, ViewEditor, ViewPane, LayoutGenerator |

## Feature Audits & Issue Notes

| Document | Description |
|----------|-------------|
| [notification-component-analysis.md](notification-component-analysis.md) | 7-layer architecture analysis of the Notification component implementation |
| [hardcoded-strings-analysis.md](hardcoded-strings-analysis.md) | Internationalization audit and hardcoded string extraction status |
| [local-library-issues.md](local-library-issues.md) | Local library issue audit, path strategy, existing capabilities, and known limits |

## Active Proposals

| Document | Description |
|----------|-------------|
| [package-refactor-kickoff.md](package-refactor-kickoff.md) | Multi-session kickoff pack for splitting `pro.sketchware.core` (124 flat files) into responsibility-based sub-packages; contains Phase 1/2/3 prompts and acceptance checklist |
| [package-refactor-plan.md](package-refactor-plan.md) | Phase 1 output: 9-sub-package split / 12-batch sequence / DAG dependency map for `pro.sketchware.core`; consumed by Phase 2 execution sessions |
| [package-target-architecture.md](package-target-architecture.md) | Target architecture after all six P0/P1/P2 refactors (core split + LogicEditor controllers + BuildStage chain + vendor-dx submodule + codegen Sink layer + naming policy) |
| [package-migration-plan-v2.md](package-migration-plan-v2.md) | Planning-stage proposal for the next two follow-ups left over from v1: tidying `pro.sketchware.*` sub-packages and migrating `com.besome.sketch.*` into `pro.sketchware.*`. Phases 11–15 already landed; remaining items folded into `package-migration-policy.md` § 4.2. |
| [p2a-codegen-sink-plan.md](p2a-codegen-sink-plan.md) | Draft design for P2a: abstract `ProjectFilePaths`' 26 IO call sites into a `ProjectArtifactSink` interface + `BuildDirSink` / `AndroidStudioExportSink` / `SourceViewingSink` implementations. 5 sequenced batches; pre-condition for Android Studio export. |
| [local-library-dependency-order-solution.md](local-library-dependency-order-solution.md) | Unimplemented proposal for local library dependency ordering via topological sort |
| [lan-web-editing-mvp-plan.md](lan-web-editing-mvp-plan.md) | MVP planning note for LAN-based Web viewing, low-risk editing, and single-writer collaboration |

## Policies

| Document | Description |
|----------|-------------|
| [package-migration-policy.md](package-migration-policy.md) | Active hard rules for where new `.java` files must live (`pro.sketchware.*` only), naming conventions, sub-package depth budget, migration tracker, PR review checklist, and rejected anti-patterns. Implements P2b. |
| [package-naming-policy.md](package-naming-policy.md) | Active naming policy for package layout (P2b deliverable of `package-target-architecture.md`): forbids old top-level packages from regrowing, defines where new files must land, and what reviewers/auto checks enforce. |

## Historical Plans

| Document | Description |
|----------|-------------|
| [firebase-upgrade-plan.md](firebase-upgrade-plan.md) | Archived Firebase upgrade implementation plan; completed in v7.0.0-beta2 |
| [package-migration-plan.md](package-migration-plan.md) | Historical v1 plan that eliminated `mod.*` / `dev.*` contributor namespaces (167 files into `pro.sketchware.*` / `com.besome.sketch.*`). All 9 phases merged to `main`; kept as the precondition record cited by the v2 plan. |

## Refactoring Archives

| Document | Description |
|----------|-------------|
| [refactoring-naming-map.md](refactoring-naming-map.md) | Historical class renaming map for the `a.a.a` → `pro.sketchware.core` refactor |
| [refactoring-method-naming.md](refactoring-method-naming.md) | Historical method/field renaming map for Phase 8a refactoring work |

## Session Notes

| Document | Description |
|----------|-------------|
| [session-modifications-review.md](session-modifications-review.md) | Session-specific modification review and notes |
