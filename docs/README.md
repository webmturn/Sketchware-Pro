# Sketchware-Pro Documentation

This directory contains internal documentation for the Sketchware-Pro project.

## Development

| Document | Description |
|----------|-------------|
| [DEVELOPMENT.md](DEVELOPMENT.md) | Comprehensive development handbook covering project overview, environment setup, architecture, code generation, build system, and contribution guidelines |
| [sketchware-project-data-format.md](sketchware-project-data-format.md) | Data format specification for Sketchware project files (JSON schemas, file structure, quick-start checklist) |
| [upload-file-guide.md](upload-file-guide.md) | User guide for implementing image upload to remote servers using FilePicker and RequestNetwork |

## Architecture Analysis

| Document | Description |
|----------|-------------|
| [code-generation-system-analysis.md](code-generation-system-analysis.md) | Deep dive into the code generation pipeline: BlockInterpreter, ActivityCodeGenerator, EventCodeGenerator, ComponentCodeGenerator |
| [builtin-blocks-analysis.md](builtin-blocks-analysis.md) | Complete catalog of 174+ built-in block opcodes with generated Java code |
| [block-programming-bottleneck-analysis.md](block-programming-bottleneck-analysis.md) | Analysis of block programming system bottlenecks and extension capabilities |
| [ui-designer-analysis.md](ui-designer-analysis.md) | Deep dive into the UI designer: ViewEditorFragment, ViewEditor, ViewPane, LayoutGenerator |
| [notification-component-analysis.md](notification-component-analysis.md) | 7-layer architecture analysis of the Notification component implementation |

## Refactoring

| Document | Description |
|----------|-------------|
| [refactoring-naming-map.md](refactoring-naming-map.md) | Class renaming map for the `a.a.a` → `pro.sketchware.core` package refactoring |
| [refactoring-method-naming.md](refactoring-method-naming.md) | Method and field renaming map for Phase 8a classes (BlockHistoryManager, etc.) |
| [hardcoded-strings-analysis.md](hardcoded-strings-analysis.md) | Internationalization guide: hardcoded string extraction status and i18n architecture |

## Plans & Issues

| Document | Description |
|----------|-------------|
| [firebase-upgrade-plan.md](firebase-upgrade-plan.md) | Firebase libraries upgrade plan (v19.x → BOM 33.7.0) — completed in v7.0.0-beta2 |
| [local-library-issues.md](local-library-issues.md) | Local library issue audit and fixes |
| [local-library-dependency-order-solution.md](local-library-dependency-order-solution.md) | Proposed solution for local library dependency ordering via topological sort |

## Session Reviews

| Document | Description |
|----------|-------------|
| [session-modifications-review.md](session-modifications-review.md) | Review of session-specific code modifications |
