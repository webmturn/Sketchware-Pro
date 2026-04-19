# v7.0.0-beta8

## 🐛 Bug Fixes

### Local Library Manifest Merger (1 fix)
- **Attribute-level merge** — Fix `LocalLibraryManifestMerger` only merging children of `<application>`, silently dropping attributes declared on the library's `<application>` tag and on components (e.g. activities) that already exist in the generated manifest. Attributes like `android:launchMode="singleTop"`, `android:exported`, `android:supportsRtl`, `android:networkSecurityConfig` are now merged with existing-wins semantics; user / XML-command values always win, conflicts are logged. Relates to #6 (RuStore Pay SDK)

### Logic Editor (1 fix)
- **Source code viewer xmlName** — Fix `LogicEditorActivity.showSourceCode` not passing `xmlName` to `BlockInterpreter`, causing incorrect layout binding in previewed source

### Code Generation (1 fix)
- **Decompiled block parameter handling** — Harden decompiled block parameter parsing against malformed or unexpected shapes

### Add-Event UI (1 fix)
- **Collapse animation** — Fix broken collapse animation and disable item animator to prevent flicker during category switching

### General (1 fix)
- **Multiple bugfixes** — Code generation, event management, and library handling fixes collected into a single change

## ⚡ Performance

- **Background tasks IO pool** — Restore fixed IO pool with adaptive thread count for more predictable concurrency
- **Add-event startup** — Optimize safe-startup hot paths in the add-event screen

## ♻️ Refactoring

- **Codegen `definedFunc` handler** — Reuse `getParamClassInfo` helper to reduce duplication

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-07...v7.0.0-beta-08

---

# v7.0.0-beta7

## ✨ New Features

### Local Library Manifest Auto-Merge
- **Project-level toggle** — New setting in Local Library Manager to enable automatic AndroidManifest merging from local libraries
- **DOM-based manifest merger** — Merges `uses-permission`, `uses-feature`, `queries`, and application components (`provider`, `service`, `receiver`, `activity`, `activity-alias`, `meta-data`, `uses-library`) from local library manifests into the project manifest
- **Conflict detection** — Automatically detects and skips conflicting `provider` authorities; qualifies relative class names with library package names; strips `tools:*` namespace attributes
- **Placeholder support** — Handles `${applicationId}` and `${packageName}` placeholders in library manifests
- **Per-library error isolation** — A single malformed library manifest won't break the entire merge; errors are logged and other libraries proceed normally

### Build System
- **Improved incremental build cache** — Enhanced cache key computation and diagnostics logging for faster repeat builds

## 🐛 Bug Fixes

### Language / i18n (3 fixes)
- **Import handling** — Fix language override import handling so imported string overrides apply more reliably across Activities
- **Application context recursion** — Fix recursive locale context application access and preserve language override behavior
- **Resource wrapper caching** — Cache wrapped language override resources and application context to avoid recreating wrapper objects on repeated access

### Project Data & Storage (5 fixes)
- **Blocks/moreblocks loss on update** — Fix regression where saved blocks and moreblocks disappear after app updates; add `hasDistinctBackup` validation across data stores
- **Backup/save flows** — Harden exception handling and project backup/save flows
- **Resource temp backup restore** — Harden temp backup restore flow
- **Resource filename normalization** — Normalize filenames during project copy
- **Local library path normalization** — Normalize local library paths on project load

### Storage Paths (2 fixes)
- **External volume paths** — Preserve absolute external volume paths instead of incorrectly relativizing them
- **Codegen cache invalidation** — Invalidate cache for beta6.3 run updates

### Logic Editor — Favorite Collections (3 fixes)
- **Collection persistence** — Fix favorite block and moreblock collections not persisting
- **Favorite deletion cleanup** — Fix favorite block deletion UI cleanup so delete mode exits correctly
- **Collection deselection** — Fix `ManageCollectionActivity` missing `case 4` for built-in block collections

### Custom Block Manager (3 fixes)
- **Block type preview** — Fix custom block type preview showing incorrect shapes; extract `BlockTypeUtils.normalizeStoredBlockType()` to canonicalize legacy stored values
- **Palette search indexing** — Fix `ExtraPaletteBlock` search index being off, causing the wrong block to be highlighted
- **Search source index** — Fix `BlocksManager` search results using filtered adapter positions as source indices

### UI Designer — Editor Preview (3 fixes)
- **Container preview invisible** — Fix `LinearLayout`, `RelativeLayout`, `HorizontalScrollView`, `VerticalScrollView`, `CardView` previews not rendering due to missing `setWillNotDraw(false)`
- **Widget preview invisible** — Fix `ProgressBar`, `AdView`, `ListView`, `RecyclerView`, `TabLayout`, `BottomNavigationView` previews with the same drawing flag issue
- **Special widget preview invisible** — Fix `BadgeView`, `PatternLockView`, `LottieAnimationView`, `YoutubePlayerView`, `OTPView`, `CodeView` placeholder previews

### XML & Code Generation (3 fixes)
- **XmlBuilder NPE** — Fix `NullPointerException` in `AttributeBuilder.toCode()` caused by implicit mutable state
- **XmlBuilder attribute serialization** — Harden attribute serialization edge cases
- **Null-safety guards** — Add null-safety guards for editor, history, and project data paths

### Build & Command Blocks (1 fix)
- **Command block handling** — Harden temp state handling and malformed block parsing

### UI Fixes (5 fixes)
- **Event category switching** — Stabilize add-event category switching
- **Crash report clipboard** — Guard crash report clipboard copy size and fall back to truncated content
- **Resource manager null safety** — Harden resource manager screens against null or empty `sc_id`
- **4 confirmed bugs** — Fix color picker, palette, block view, and XML formatting issues
- **MaterialButton/Crashlytics** — Guard MaterialButton background and add Crashlytics null-safety

### Background Tasks (1 fix)
- **UI-only task guards** — Guard UI-only tasks with `IfAlive` to prevent crashes on destroyed Activities

## ♻️ Refactoring

- **Background tasks** — Migrate `BaseAsyncTask` and `Thread` usage to `BackgroundTasks`/`TaskHost` for consistent async handling
- **Storage paths** — Centralize storage path handling in `SketchwarePaths`; unify external-storage path handling
- **Core naming** — Clarify internal naming across core helpers and project data utilities
- **Legacy handlers** — Reduce duplication in legacy component and event handlers
- **Project data management** — Extract `saveProjectDataToFiles`, move `ProjectListManager` init to Application
- **Base activity** — Simplify base activity and harden file helpers
- **Manifest pipeline** — Unify manifest post-processing into `finalizeGeneratedManifest()` for both build and preview paths

## 📖 Documentation

- Sync and clarify project documentation

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-06...v7.0.0-beta-07

---

# v7.0.0-beta6.3

##  Bug Fixes

### Language Overrides
- **Import handling**  Fix language override import handling so imported string overrides apply more reliably across Activities.
- **Application context recursion**  Fix recursive locale context application access and preserve language override behavior when retrieving the application context.
- **Resource wrapper caching**  Cache wrapped language override resources and application context to avoid recreating wrapper objects on repeated access.

### Custom Block Manager
- **Palette actions during search**  Fix `BlocksManager` search results using filtered adapter positions as source indices, so open/edit/delete/insert actions target the correct palette.

### Logic Editor  Favorite Collections
- **Favorite deletion cleanup**  Fix favorite block deletion UI cleanup so delete mode exits correctly and the drawer removes only the matching pair of views.

### Crash Reporting
- **Clipboard size guard**  Guard crash report clipboard copy size and fall back to truncated content when the full report is too large to copy.

### Resource Management
- **Null/empty project paths**  Harden resource manager screens against null or empty `sc_id` and `dir_path` values to prevent lifecycle-order crashes.

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-06-02...v7.0.0-beta-06-03

---
# v7.0.0-beta6.2

## 馃悰 Bug Fixes

### Project Data Integrity
- **Blocks/moreblocks loss on update** 鈥?Fix regression where saved blocks and moreblocks disappear after app updates; add `hasDistinctBackup` helper that rejects empty or byte-identical backup files across `ProjectDataStore`, `ProjectFileManager`, `LibraryManager`, and `ResourceManager`; guard all `loadFromBackup` entry points to prevent clearing in-memory state from an invalid backup; fix `DesignActivity` load order to correctly respect the load strategy flag

### Logic Editor 鈥?Favorite Collections
- **Collection persistence** 鈥?Fix favorite block and moreblock collections not persisting; override `loadCollections()` in `BlockCollectionManager` and `MoreBlockCollectionManager` with line-by-line JSON parsing to match the storage format
- **Collection deselection** 鈥?Fix `ManageCollectionActivity.unselectToBeDeletedItems()` missing `case 4` for built-in block collections, causing the wrong collection type to be cleared on deletion
- **Collection name display** 鈥?Fix `DefinitionBlockView` not showing the collection name as its block spec; add `getCollectionName()` accessor for downstream use

### Custom Block Manager
- **Block type preview** 鈥?Fix custom block type preview showing incorrect shapes in `BlocksManagerCreatorActivity` and `BlocksManagerDetailsActivity`; extract `BlockTypeUtils.normalizeStoredBlockType()` to canonicalize legacy stored values (`"regular"`, localized display labels) to internal single-character codes
- **Palette search indexing** 鈥?Fix `ExtraPaletteBlock` search index being off, causing the wrong block to be highlighted when selecting a search result

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-06-01...v7.0.0-beta-06-02

---

# v7.0.0-beta6.1

## 馃悰 Bug Fixes

### XML Generation
- **XmlBuilder NPE** 鈥?Fix `NullPointerException` in `AttributeBuilder.toCode()` caused by implicit mutable state; `newlineIndent` is now passed as a local parameter, eliminating the crash on single raw attribute injection

### UI Designer 鈥?Editor Preview
- **Container preview invisible** 鈥?Fix `LinearLayout`, `RelativeLayout`, `HorizontalScrollView`, `VerticalScrollView`, `CardView` previews not rendering borders or selection overlay due to missing `setWillNotDraw(false)`
- **Widget preview invisible** 鈥?Fix `ProgressBar`, `AdView`, `ListView`, `RecyclerView`, `TabLayout`, `BottomNavigationView` previews with the same drawing flag issue
- **Special widget preview invisible** 鈥?Fix `BadgeView`, `PatternLockView`, `LottieAnimationView`, `YoutubePlayerView`, `OTPView`, `CodeView` placeholder previews not rendering selection overlay

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-06...v7.0.0-beta-06-01

---

# v7.0.0-beta6

## 鉁?New Features

### Resources Editor
- **Dimens support** 鈥?Resources Editor now supports `dimens.xml` editing with validation and UX enhancements
- **Color resources for more widgets** 鈥?Support color resources for BottomAppBar, CollapsingToolbar, CardView backgrounds

### Logic Editor
- **Continue block** 鈥?New `continue` block for loop control flow
- **Chunked block loading** 鈥?Logic editor loads blocks in chunks for improved performance with large projects
- **Block palette search optimization** 鈥?Faster search and rendering in block palette
- **Extra block metadata caching** 鈥?Cached loading for better editor responsiveness
- **Block search fix** 鈥?Search now matches visible block text correctly

### UI Designer
- **Elevation property** 鈥?New elevation property for views with live preview in designer
- **Elevation preview** 鈥?Restored elevation preview using GradientDrawable fix

### Local Library Management
- **Import package indexing** 鈥?Index and browse import packages from local libraries
- **Lazy rebuild UI** 鈥?Rebuild import package index on demand with progress UI

### Other
- **Store page states** 鈥?Remember page states and accessibility settings
- **Remember last restore directory** 鈥?File picker remembers the last used restore directory
- **Variable/list management UI** 鈥?Refactored variable and list management with references UI

## 馃悰 Bug Fixes

### Crash Prevention (30+ fixes)
- **HashMap safety** 鈥?Prevent NPE and ClassCastException from `HashMap<String,Object>.get()` across 28 files
- **String indexing guards** 鈥?Guard `charAt()`, `substring()`, `split()[index]` across BlockUtil, ComponentCodeGenerator, AndroidManifestInjector, PropertyInputItem, FileUtil, ReturnMoreblockManager, and more
- **Stale position guards** 鈥?Guard `getLayoutPosition()` against `NO_POSITION` in all click listeners
- **Editor dialog guards** 鈥?Guard stale index access in block, command, and listener editor dialogs
- **Lifecycle guards** 鈥?Guard async UI callbacks against destroyed lifecycle; guard fragment arguments and extras against null
- **Context safety** 鈥?Harden context casts and unsafe map value reads
- **Build callback guards** 鈥?Guard DesignActivity build progress and cancel callbacks

### UI Fixes
- **AsdDialog** 鈥?Enable keyboard for find/replace dialog
- **CollapsingToolbarLayout** 鈥?Restore contentScrim default to `?attr/colorPrimary`
- **Button/EditText/MaterialButton** 鈥?Restore default background in preview; fix SignInButton defaults reset inside loop
- **BlockBean Parcelable** 鈥?Add `dataAvail()` guards for new fields
- **InnerAddComponentBottomSheet** 鈥?Add missing return after `dismissAllowingStateLoss`
- **Logic editor pane** 鈥?Fix pane size updates
- **Resource ID resolution** 鈥?Eliminate `Invalid ID 0x00000000` errors
- **ManageLocalLibrary** 鈥?Fix static call and `getParentFile` null check

### Performance
- **replaceAll 鈫?replace** 鈥?Replace unnecessary regex `replaceAll()` with literal `replace()` across 15+ files for parsing safety and performance

## 鈾伙笍 Refactoring

### Custom Event System
- **POJOs for custom blocks** 鈥?Replace `HashMap<String,Object>` with typed POJOs for custom events, listeners, and components
- **Centralized paths** 鈥?Centralize custom event system file paths
- **ComponentsHandler** 鈥?Normalize naming and harden safety checks

### Code Quality
- **Unified logging** 鈥?Migrate to `LogUtil` across 23 files
- **CommandBlock cleanup** 鈥?Clean up and record maintainability review
- **ViewPane imports** 鈥?Replace FQN references with imports, remove unused BitmapFactory
- **Decompilation residue** 鈥?Rename decompilation residue variables across multiple files
- **Resource refs** 鈥?Use resource refs for colors/strings in view editor
- **Editor/palette resources** 鈥?Add editor and palette color and string resources
- **Comment cleanup** 鈥?Update stale obfuscated class name references, clarify ambiguous comments, fix stale Javadoc links

## 馃摉 Documentation

- Calibrate project data format docs against current source code
- Update hardcoded-strings analysis to use `pro.sketchware.core` package paths
- Mark completed items in block-programming bottleneck analysis, firebase, and i18n docs
- Polish project documentation and reclassify documentation index

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-05...v7.0.0-beta-06

---

# v7.0.0-beta5

## 鉁?New Features

### Block System
- **Block disable/enable** 鈥?Disable individual blocks via PopupMenu toggle; disabled blocks are skipped during code generation, drawn with diagonal stripe overlay, and serialized with Parcel/copy/isEqual support
- **Try-catch-finally blocks** 鈥?New `catchBlock`/`finallyBlock` chain blocks with backward-compatible `tryCatch`
- **Collapsible control substacks** 鈥?Control blocks can now be collapsed
- **Continue block** reverted (re-added in beta6)
- **Int variable type** (`VARIABLE_TYPE_INT = 4`) 鈥?Added then reverted due to issues

### Block Search
- **Palette search** 鈥?Search block names and specs within the palette panel
- **Canvas search** 鈥?Search placed blocks in the logic editor canvas
- **Block Manager search** 鈥?Search palette names and block names/specs in Block Manager
- **Integrated into LogicEditorActivity** with string resources

### Dependency Resolver
- **Dependency tree visualization** 鈥?View full dependency tree of resolved libraries
- **Resolution timeout notification** 鈥?`onResolutionTimeout` callback notifies user when tree resolution times out
- **3 caching optimizations** 鈥?Faster resolution with parent POM fix
- **Min API level** 鈥?`setMinApiLevel` added to `compileJarWithFallback`
- **Built-in dependency detection** 鈥?Narrowed `isBuiltInDependency` to specific androidx groupIds
- **Sub-dependency hiding** 鈥?Hide sub-dependencies from main library list, fix dependency parent field

### Crash Monitoring
- **Persistent crash log system** 鈥?Automatic crash logging with viewer in About page

### UI Modernization
- **Material 3 upgrades** 鈥?CheckBox 鈫?MaterialCheckBox (22 layouts), RadioButton 鈫?MaterialRadioButton, CardView 鈫?MaterialCardView, Spinner 鈫?AppCompatSpinner, CheckBox 鈫?MaterialSwitch in project settings (6 instances)
- **ProGuard page** 鈥?Refactored to Material 3 style with AppBarLayout + card sections
- **RTL support** 鈥?Fix `paddingLeft/Right`, `marginLeft/Right`, `layout_gravity`, FAB positioning across 20+ layouts
- **Project list** 鈥?Add ripple to items, remove NestedScrollView wrapping RecyclerView for performance
- **Compile log** 鈥?Increase font size, add copy button
- **Import XML** 鈥?Reorganized in bottom popup menu (view tab only)

## 馃悰 Bug Fixes

### Crash Prevention
- **ViewPropertyItems** 鈥?Fix #1982 ClassCastException
- **SubDepAdapter** 鈥?Fix NPE: field init calls `getResources()` before context attach
- **Gson parsing** 鈥?Null guard for `enabledLibs` after parsing
- **ImageView recycling** 鈥?Clear both Glide and Coil requests before rebinding to prevent stale SVG/bitmap conflicts
- **SwipeRefreshLayout** 鈥?Add scroll callback for correct pull-to-refresh with RecyclerView

### Build System
- **Dexing OOM** 鈥?Fix dexing out-of-memory, resolver hang, and dependency resolution performance
- **LayoutGenerator BuildConfig** 鈥?Ensure correct `sc_id` usage
- **Resource leak, CME risk, Uri inconsistency, stale cache** 鈥?Multiple build system bug fixes
- **Compiled app display issues** 鈥?Fix transparent colors, Force Dark, AAPT2
- **XmlLayoutParser** 鈥?Fix `parseColorValue` stripping alpha channel
- **Atomic file writes** 鈥?Reliable `readFileBytes` in `EncryptedFileUtil`
- **Save on success only** 鈥?Only save version code on successful save; notify user on save failure

### Dependency Resolver Fixes
- **Root artifact skipped** by `skipFilter` in `resolveDependencyTree`
- **Stale expandedBoms cache** across resolution sessions
- **Wrong androidx.graphics groupId** + duplicate entries on re-download
- **Local library dependency handling** improvements

### Other
- **Nested block disable overlay** 鈥?Propagate disabled overlay through nested blocks
- **Block preview fallback color** 鈥?From white to palette/default purple
- **Magic numbers** 鈥?Replace with named constants (WrongConstant lint)
- **Thread safety** 鈥?Fix singletons and shared state
- **ProGuard/R8 rules** 鈥?Clean up default rules

## 鈿?Performance

- **Palette panel** 鈥?Optimize opening performance (P1-P4), eliminate duplicate loading, cache file listings (P5a/P5b)
- **ViewPane** 鈥?Cache `getXmlString` XML parsing (H8), replace `findViewWithTag` with HashMap index in BlockPane (H1)
- **Save operations** 鈥?Async `saveAllBackup` in LogicEditorActivity and 5 remaining callers (H5/H5b)
- **Sound list** 鈥?Async audio metadata loading in adapters
- **7 files** 鈥?Fix performance bottlenecks and bugs

## 鈾伙笍 Refactoring

- **BlocksHandler migration** 鈥?Migrate BlocksHandler blocks to BlockCodeRegistry
- **About page** 鈥?Upgrade legacy CardView layouts (fixed UTF-8 encoding)
- **Dead code removal** 鈥?Delete BuildingDialog, QuizBoard

## 馃摉 Documentation

- Add comprehensive development manual
- Add Javadoc to bean, code generation, and core classes (batch 1-5)
- Add Sketchware project data format documentation
- Update 5 documentation files to match current codebase

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-04...v7.0.0-beta-05

---

# v7.0.0-beta4

## 馃悰 Bug Fixes

### Property Editor & View Pane
- **PropertyIndentItem** 鈥?Fix dialog not closing when `valueChangeListener` is null; move `v.dismiss()` outside listener check
- **ViewPane.findItemViewByTag** 鈥?Add null/empty check for `tag` to prevent `charAt(0)` crash
- **ViewPropertyItems** 鈥?Add `settings != null` guard before `settings.getValue()` to prevent NPE
- **ViewPropertyItems** 鈥?Add null/empty checks for `bean.id` and `currentId` before `charAt(0)`
- **ViewPropertyItems** 鈥?Add `!blockBean.parameters.isEmpty()` before `parameters.get(0)` to prevent IndexOutOfBoundsException
- **ViewPane.updateViewBeanProperties** 鈥?Replace `view.getTag().toString()` with null-safe `view.getTag() != null ? view.getTag().toString() : ""` (5 places)
- **ViewPane** 鈥?Add null/empty checks for `viewBean.id` before `charAt(0)` (3 places in `addViewAndUpdateIndex`, `moveView`, `updateItemView`)
- **ViewPane.getXmlString** 鈥?Add null checks for `map.get("key")` and `map.get("text")` to prevent NPE when parsing strings.xml

### values-night Colors
- Add `editor_*` and `palette_*` color definitions to `values-night/colors.xml` for dark theme support

### Other Modules
- **EventListFragment** 鈥?Fix `LinearLayoutManager(null)` 鈫?use `parent.getContext()`
- **SoundImportFragment** 鈥?Fix `LinearLayoutManager(null)` 鈫?use `requireContext()`
- **LogicClickListener** 鈥?Fix 4脳 `LinearLayoutManager(null)` 鈫?use `logicEditor`
- **LibrarySettingsImporter** 鈥?Fix `LinearLayoutManager(null)` 鈫?use `activity`
- **ExcludeBuiltInLibrariesActivity** 鈥?Fix `LinearLayoutManager(null)` 鈫?use `this`
- **LibraryDownloaderDialogFragment** 鈥?Fix `LinearLayoutManager(getContext())` 鈫?use `requireContext()`; add `getActivity() != null` guards before `showAnErrorOccurredDialog` (4 places) to prevent NPE when fragment is detached
- **LogicEditorActivity** 鈥?Fix undefined variable `ss`: use `fieldBlock` for `showAtLocation` anchor, use `viewBean` for `getClassInfo()` (pre-existing compilation errors)

---

# v7.0.0-beta3

## 鉁?New Features

### SQLite Database Component (type 29)
- **12 blocks** for full SQLite CRUD operations
- Create/open database, execute SQL, query with cursor, insert/update/delete rows
- `onSQLiteError` event with localized descriptions
- Chinese translations included

### Notification Component (type 26)
- **10 blocks**, 7 layers for channel-based notifications
- `POST_NOTIFICATIONS` runtime permission for Android 13+
- Proper channel management with `notifCreateChannel` / `notifSetChannel`
- Small icon, color, and builder pattern support

### Dependency Resolver (Localized)
- **Replaced remote module** (`com.github.Cosmic-Ide:DependencyResolver`) with local `:resolver` module
- **BOM support** 鈥?Automatically resolves Bill of Materials for version management
- **Dependency tree caching** 鈥?`dependency-tree.json` avoids redundant resolution
- **Built-in dependency detection** 鈥?Skips AndroidX, Firebase, Kotlin, etc. that are already bundled
- **EPERM auto-retry** 鈥?Falls back to app-specific storage on FUSE-restricted devices

### Library Management UI
- **Sub-dependency viewer** 鈥?See all transitive dependencies of a root library
- **Cascade toggling** 鈥?Enable/disable root library cascades to sub-dependencies
- **Smart deletion** 鈥?Shared sub-dependencies retained, orphan detection and cleanup
- **Version conflict detection** and built-in dependency badges

### Logic Editor
- **Variable and list rename** 鈥?Rename variables/lists across all blocks in an activity
- **New blocks**: `getClipboard`, `getExceptionMessage`

### Build System 鈥?Incremental Java Compilation
- **Skip ECJ entirely** when no Java files changed and R.java is stable 鈥?saves 2-10s per build
- **Partial recompile** 鈥?only changed Activity files recompiled; unchanged `.class` files reused
- CRC32-based hash cache (`bin/build_hashes.json`) survives across builds, reset on full clean
- **Full recompile triggered** automatically when: R.java changes (resource IDs), classpath changes, user custom Java/Broadcast/Service files change, or ProGuard is enabled
- **Stale `.class` cleanup** 鈥?deleted Activities have their `.class` and inner-class files removed from `bin/classes/` automatically
- 4 files changed: `IncrementalBuildCache` (new), `ProjectFilePaths.cleanRJavaOnly()`, `ProjectBuilder.compileJavaCode()`, `DesignActivity`
- **Fix: `computeCodeGenCacheKey()` missing system file inputs** 鈥?Custom event/listener/component definition files (`events.json`, `listeners.json`, `component.json`) now included in the code generation cache key; previously, modifying a custom event's code template wouldn't invalidate the cache

## 馃悰 Bug Fixes

### Editor Performance
- **Fix editor tab swiping lag** 鈥?Skip unnecessary full view rebuild (990 views) on tab switch; dirty-check ensures rebuild only when Activity file changes
- **Fix ViewPane NPE** 鈥?6 null/exists checks for `BitmapFactory.decodeFile` and `NinePatchDecoder.decodeFile`
- **Remove debug log storm** 鈥?`ViewEditor` was logging full stack traces for every custom-ID view

### Component & Lifecycle
- **Fix Gyroscope sensor** 鈥?Lifecycle management and type mismatch
- **Fix lifecycle event generation** 鈥?Accumulate multi-component events, preserve auto-cleanup with user events, fix ViewBinding ID resolution
- **Fix ClassCastException** in `ViewPropertyItems` for `property_text_size`

### Library Downloads
- **Fix FUSE EPERM** 鈥?Auto-retry with fallback path for Huawei/Samsung devices
- **Fix backup/restore** 鈥?Reliability improvements for local library restore
- **Fix libType enforcement** 鈥?Correct `libType` based on section key during deserialization
- **Fix DependencyResolver crash** 鈥?Catch `IOException` to prevent thread crash

### Notification Component Fixes
- 8 incremental fixes: block colors, translations, permission, channel ID, small icon, anonymous class compatibility, params size guards

### Performance
- **O(1) DEX merge estimation** 鈥?Replace O(n) method counting with header-based approach

### UI Designer
- **Fix `ViewPane.getUnknownItemView()` data corruption**
- **`ViewHistoryManager.MAX_HISTORY_STEPS`** 鈥?Hard-coded 50 extracted to named constant; easy to adjust in one place

### Logic Editor
- **`BlockHistoryManager.MAX_HISTORY_STEPS`** 鈥?Same change for consistency; both editors now share the same configurable pattern

## 鈾伙笍 Refactoring

### Code Generation System 鈥?Registry Pattern
- **`BlockCodeRegistry`** 鈥?All 200+ block opcodes migrated from `BlockInterpreter`'s 1400-line switch to a `HashMap<String, BlockCodeHandler>` registry; new blocks can be added via `BlockCodeRegistry.register()` without modifying `BlockInterpreter`
- **`EventCodeRegistry`** 鈥?All 40+ event templates migrated from `ComponentCodeGenerator.getEventCode()`'s 310-line switch to a `HashMap<String, EventCodeHandler>` registry
- **`BlockCodeHandler` / `EventCodeHandler`** 鈥?New `@FunctionalInterface` contracts for block code generation and event code templates
- **`BlockInterpreter`** 鈥?`getBlockCode()` now delegates to `BlockCodeRegistry`; legacy switch fully removed
- **`ComponentCodeGenerator`** 鈥?`getEventCode()` now delegates to `EventCodeRegistry`

- **BlockSpecRegistry** 鈥?Replace decompiled hashCode switch with HashMap
- **ProjectDataStore** 鈥?Replace decompiled enum switches, delete `ScreenOrientationConstants` and `KeyboardSettingConstants`
- **ProjectDataParser** 鈥?Replace decompiled hashCode/enum switches, clean up fields
- **StringResource** 鈥?Replace decompiled hashCode switch with Set
- **BlockPane** 鈥?Replace decompiled hashCode switches with Map lookups
- **Code style cleanup** across 30+ files (this. prefixes, FQN imports, verbose conditionals)
- **Decompiler artifact cleanup** across 17 files

## 馃寪 i18n

- Chinese translations for SQLite component, library category titles, onSQLiteError event
- Localized dependency resolver event descriptions

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-02...v7.0.0-beta-03

---

# v7.0.0-beta2

## 馃敟 Firebase Libraries Upgrade (v19.x 鈫?BOM 33.7.0)

The built-in Firebase libraries have been upgraded from severely outdated v19.x (2019-2020) to versions compatible with BOM 33.7.0:

| Library | Old Version | New Version |
|---------|------------|-------------|
| firebase-auth | 19.0.0 | 23.1.0 |
| firebase-database | 19.3.1 | 21.0.0 |
| firebase-storage | 19.0.0 | 21.0.1 |
| firebase-messaging | 19.0.0 | 24.1.0 |
| firebase-common | 19.3.0 | 21.0.0 |
| play-services-base | 17.1.0 | 18.5.0 |

- **Removed deprecated `firebase-iid`** (deleted by Google in Firebase 21+), migrated to `firebase-installations` + `FirebaseMessaging.getToken()`
- **Fixed BOM version mismatch**: ComponentCodeGenerator was generating BOM 34.1.0 while using v19.x JARs
- **Removed deprecated manifest entries**: `FirebaseInstanceIdReceiver`, `firebase.iid.Registrar`, `c2dm` permission
- **Fixed FCM crash**: `ManageEvent.java` `onCompleteRegister` template now guards `task.getResult()` with `isSuccessful()` check
- **35 libraries** total downloaded, DEX-generated, and packed into assets

## 鉁?New Features

- **Project pin feature** 鈥?Pin important projects to the top
- **Event list search & sort** 鈥?Search and sort events in the event manager
- **Material 3 slider dialogs** 鈥?Hybrid slider dialogs for numeric widget properties
- **Library downloader UX** 鈥?Advanced progress tracking and improved download experience
- **Local library sorting** 鈥?Enabled libraries shown first
- **Project options improvements** 鈥?Display project ID, project name, rearranged options by frequency
- **Theme preset reset** 鈥?Button to reset theme colors to defaults
- **Language settings** 鈥?Optimized with built-in locale picker and i18n fixes
- **SwipeRefreshLayout** 鈥?Added as a built-in library
- **MapView listeners** 鈥?Initialized by default
- **Add Event keyboard** 鈥?Adjusted for keyboard visibility
- **Project data encryption** 鈥?Toggle setting for encrypted project data storage

## 馃悰 Bug Fixes (102 fixes)

### Crash Prevention
- **109 crash-safety fixes** across the codebase:
  - All `Gson.fromJson()` calls protected against malformed JSON
  - All `Color.parseColor`, `Integer.parseInt`, `Double.parseDouble`, `Float.parseFloat` guarded
  - All `BitmapFactory.decodeFile` null-checked in `FileUtil`
  - All `split()[index]` patterns protected against `ArrayIndexOutOfBoundsException`
  - All `ExecutorService` leaks fixed (12 instances)
  - All `FileOutputStream`/`FileInputStream` resource leaks fixed
- **NPE prevention**: 40+ null-safety fixes across project data, map values, fragment states
- **ConcurrentModificationException** in `ProjectDataStore.removeFirebaseViews()`
- **ClassCastException** in `LogicEditorActivity`
- **ActivityNotFoundException** from unguarded intents (market://, http://, file://)
- **APK signing** used keystore password instead of alias password

### Code Generation
- 6 template bugs fixed in `ComponentCodeGenerator.java`
- Fixed duplicate permission nodes in generated `AndroidManifest.xml`
- Fixed ViewBinding field generation for custom views and Drawer views
- Fixed `layout_alignRight` attribute handling

### UI/UX Fixes
- Dark mode fix for `SrcCodeEditor` non-Java/Kt/XML files
- Search events search box dark mode fix
- Text clipping/truncation in ProGuard manager
- RecyclerView item confusion in `ViewSelectorActivity`
- Focus on attribute input field in `ManageAppCompatActivity`
- View not selectable when Disabled
- Removed ANR-causing font

## 鈾伙笍 Refactoring (146 changes)

- **JAR source integration**: Decompiled JAR classes integrated as Java source with proper naming
- **Class renames**: `a.a.a` package classes renamed for readability with full reference updates
- **String externalization**: ~15 commits extracting hardcoded strings to string resources for i18n
- **ViewBinding migration**: Multiple activities migrated to ViewBinding
- **Deprecated API migration**: `startActivityForResult` 鈫?`ActivityResultLauncher`, `onBackPressed` 鈫?`OnBackPressedCallback`, `getColor()` 鈫?`ContextCompat`
- **Thread safety**: `ExecutorService` migration, `volatile` singletons, synchronized access
- **Exception narrowing**: ~20 commits replacing broad `catch(Exception)` with specific exception types
- **Logging**: `printStackTrace()` 鈫?`Log.e()`, stale log tags replaced with class names
- **Removed unused code**: Facebook ads, OneSignal, DynamicLinks components removed

## 馃摝 Dependencies

- R8: 8.9.35 鈫?8.11.18
- Lottie: updated to 6.6.7
- Material: updated to 1.14.0-alpha05
- androidx.activity: 1.10.1 鈫?1.11.0
- android.jar: platform-34 added
- Built-in libraries versions updated

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-01...v7.0.0-beta-02

