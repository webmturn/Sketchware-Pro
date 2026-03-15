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
| [builtin-blocks-analysis.md](builtin-blocks-analysis.md) | Complete catalog of 174+ built-in block opcodes with generated Java code |
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
| [local-library-dependency-order-solution.md](local-library-dependency-order-solution.md) | Unimplemented proposal for local library dependency ordering via topological sort |
| [lan-web-editing-mvp-plan.md](lan-web-editing-mvp-plan.md) | MVP planning note for LAN-based Web viewing, low-risk editing, and single-writer collaboration |

## Historical Plans

| Document | Description |
|----------|-------------|
| [firebase-upgrade-plan.md](firebase-upgrade-plan.md) | Archived Firebase upgrade implementation plan; completed in v7.0.0-beta2 |

## Refactoring Archives

| Document | Description |
|----------|-------------|
| [refactoring-naming-map.md](refactoring-naming-map.md) | Historical class renaming map for the `a.a.a` → `pro.sketchware.core` refactor |
| [refactoring-method-naming.md](refactoring-method-naming.md) | Historical method/field renaming map for Phase 8a refactoring work |

## Session Notes

| Document | Description |
|----------|-------------|
| [session-modifications-review.md](session-modifications-review.md) | Session-specific modification review and notes |
