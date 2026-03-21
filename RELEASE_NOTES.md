# v7.0.0-beta6

## ✨ New Features

### Resources Editor
- **Dimens support** — Resources Editor now supports `dimens.xml` editing with validation and UX enhancements
- **Color resources for more widgets** — Support color resources for BottomAppBar, CollapsingToolbar, CardView backgrounds

### Logic Editor
- **Continue block** — New `continue` block for loop control flow
- **Chunked block loading** — Logic editor loads blocks in chunks for improved performance with large projects
- **Block palette search optimization** — Faster search and rendering in block palette
- **Extra block metadata caching** — Cached loading for better editor responsiveness
- **Block search fix** — Search now matches visible block text correctly

### UI Designer
- **Elevation property** — New elevation property for views with live preview in designer
- **Elevation preview** — Restored elevation preview using GradientDrawable fix

### Local Library Management
- **Import package indexing** — Index and browse import packages from local libraries
- **Lazy rebuild UI** — Rebuild import package index on demand with progress UI

### Other
- **Store page states** — Remember page states and accessibility settings
- **Remember last restore directory** — File picker remembers the last used restore directory
- **Variable/list management UI** — Refactored variable and list management with references UI

## 🐛 Bug Fixes

### Crash Prevention (30+ fixes)
- **HashMap safety** — Prevent NPE and ClassCastException from `HashMap<String,Object>.get()` across 28 files
- **String indexing guards** — Guard `charAt()`, `substring()`, `split()[index]` across BlockUtil, ComponentCodeGenerator, AndroidManifestInjector, PropertyInputItem, FileUtil, ReturnMoreblockManager, and more
- **Stale position guards** — Guard `getLayoutPosition()` against `NO_POSITION` in all click listeners
- **Editor dialog guards** — Guard stale index access in block, command, and listener editor dialogs
- **Lifecycle guards** — Guard async UI callbacks against destroyed lifecycle; guard fragment arguments and extras against null
- **Context safety** — Harden context casts and unsafe map value reads
- **Build callback guards** — Guard DesignActivity build progress and cancel callbacks

### UI Fixes
- **AsdDialog** — Enable keyboard for find/replace dialog
- **CollapsingToolbarLayout** — Restore contentScrim default to `?attr/colorPrimary`
- **Button/EditText/MaterialButton** — Restore default background in preview; fix SignInButton defaults reset inside loop
- **BlockBean Parcelable** — Add `dataAvail()` guards for new fields
- **InnerAddComponentBottomSheet** — Add missing return after `dismissAllowingStateLoss`
- **Logic editor pane** — Fix pane size updates
- **Resource ID resolution** — Eliminate `Invalid ID 0x00000000` errors
- **ManageLocalLibrary** — Fix static call and `getParentFile` null check

### Performance
- **replaceAll → replace** — Replace unnecessary regex `replaceAll()` with literal `replace()` across 15+ files for parsing safety and performance

## ♻️ Refactoring

### Custom Event System
- **POJOs for custom blocks** — Replace `HashMap<String,Object>` with typed POJOs for custom events, listeners, and components
- **Centralized paths** — Centralize custom event system file paths
- **ComponentsHandler** — Normalize naming and harden safety checks

### Code Quality
- **Unified logging** — Migrate to `LogUtil` across 23 files
- **CommandBlock cleanup** — Clean up and record maintainability review
- **ViewPane imports** — Replace FQN references with imports, remove unused BitmapFactory
- **Decompilation residue** — Rename decompilation residue variables across multiple files
- **Resource refs** — Use resource refs for colors/strings in view editor
- **Editor/palette resources** — Add editor and palette color and string resources
- **Comment cleanup** — Update stale obfuscated class name references, clarify ambiguous comments, fix stale Javadoc links

## 📖 Documentation

- Calibrate project data format docs against current source code
- Update hardcoded-strings analysis to use `pro.sketchware.core` package paths
- Mark completed items in block-programming bottleneck analysis, firebase, and i18n docs
- Polish project documentation and reclassify documentation index

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-05...v7.0.0-beta-06

---

# v7.0.0-beta5

## ✨ New Features

### Block System
- **Block disable/enable** — Disable individual blocks via PopupMenu toggle; disabled blocks are skipped during code generation, drawn with diagonal stripe overlay, and serialized with Parcel/copy/isEqual support
- **Try-catch-finally blocks** — New `catchBlock`/`finallyBlock` chain blocks with backward-compatible `tryCatch`
- **Collapsible control substacks** — Control blocks can now be collapsed
- **Continue block** reverted (re-added in beta6)
- **Int variable type** (`VARIABLE_TYPE_INT = 4`) — Added then reverted due to issues

### Block Search
- **Palette search** — Search block names and specs within the palette panel
- **Canvas search** — Search placed blocks in the logic editor canvas
- **Block Manager search** — Search palette names and block names/specs in Block Manager
- **Integrated into LogicEditorActivity** with string resources

### Dependency Resolver
- **Dependency tree visualization** — View full dependency tree of resolved libraries
- **Resolution timeout notification** — `onResolutionTimeout` callback notifies user when tree resolution times out
- **3 caching optimizations** — Faster resolution with parent POM fix
- **Min API level** — `setMinApiLevel` added to `compileJarWithFallback`
- **Built-in dependency detection** — Narrowed `isBuiltInDependency` to specific androidx groupIds
- **Sub-dependency hiding** — Hide sub-dependencies from main library list, fix dependency parent field

### Crash Monitoring
- **Persistent crash log system** — Automatic crash logging with viewer in About page

### UI Modernization
- **Material 3 upgrades** — CheckBox → MaterialCheckBox (22 layouts), RadioButton → MaterialRadioButton, CardView → MaterialCardView, Spinner → AppCompatSpinner, CheckBox → MaterialSwitch in project settings (6 instances)
- **ProGuard page** — Refactored to Material 3 style with AppBarLayout + card sections
- **RTL support** — Fix `paddingLeft/Right`, `marginLeft/Right`, `layout_gravity`, FAB positioning across 20+ layouts
- **Project list** — Add ripple to items, remove NestedScrollView wrapping RecyclerView for performance
- **Compile log** — Increase font size, add copy button
- **Import XML** — Reorganized in bottom popup menu (view tab only)

## 🐛 Bug Fixes

### Crash Prevention
- **ViewPropertyItems** — Fix #1982 ClassCastException
- **SubDepAdapter** — Fix NPE: field init calls `getResources()` before context attach
- **Gson parsing** — Null guard for `enabledLibs` after parsing
- **ImageView recycling** — Clear both Glide and Coil requests before rebinding to prevent stale SVG/bitmap conflicts
- **SwipeRefreshLayout** — Add scroll callback for correct pull-to-refresh with RecyclerView

### Build System
- **Dexing OOM** — Fix dexing out-of-memory, resolver hang, and dependency resolution performance
- **LayoutGenerator BuildConfig** — Ensure correct `sc_id` usage
- **Resource leak, CME risk, Uri inconsistency, stale cache** — Multiple build system bug fixes
- **Compiled app display issues** — Fix transparent colors, Force Dark, AAPT2
- **XmlLayoutParser** — Fix `parseColorValue` stripping alpha channel
- **Atomic file writes** — Reliable `readFileBytes` in `EncryptedFileUtil`
- **Save on success only** — Only save version code on successful save; notify user on save failure

### Dependency Resolver Fixes
- **Root artifact skipped** by `skipFilter` in `resolveDependencyTree`
- **Stale expandedBoms cache** across resolution sessions
- **Wrong androidx.graphics groupId** + duplicate entries on re-download
- **Local library dependency handling** improvements

### Other
- **Nested block disable overlay** — Propagate disabled overlay through nested blocks
- **Block preview fallback color** — From white to palette/default purple
- **Magic numbers** — Replace with named constants (WrongConstant lint)
- **Thread safety** — Fix singletons and shared state
- **ProGuard/R8 rules** — Clean up default rules

## ⚡ Performance

- **Palette panel** — Optimize opening performance (P1-P4), eliminate duplicate loading, cache file listings (P5a/P5b)
- **ViewPane** — Cache `getXmlString` XML parsing (H8), replace `findViewWithTag` with HashMap index in BlockPane (H1)
- **Save operations** — Async `saveAllBackup` in LogicEditorActivity and 5 remaining callers (H5/H5b)
- **Sound list** — Async audio metadata loading in adapters
- **7 files** — Fix performance bottlenecks and bugs

## ♻️ Refactoring

- **BlocksHandler migration** — Migrate BlocksHandler blocks to BlockCodeRegistry
- **About page** — Upgrade legacy CardView layouts (fixed UTF-8 encoding)
- **Dead code removal** — Delete BuildingDialog, QuizBoard

## 📖 Documentation

- Add comprehensive development manual
- Add Javadoc to bean, code generation, and core classes (batch 1-5)
- Add Sketchware project data format documentation
- Update 5 documentation files to match current codebase

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-04...v7.0.0-beta-05

---

# v7.0.0-beta4

## 🐛 Bug Fixes

### Property Editor & View Pane
- **PropertyIndentItem** — Fix dialog not closing when `valueChangeListener` is null; move `v.dismiss()` outside listener check
- **ViewPane.findItemViewByTag** — Add null/empty check for `tag` to prevent `charAt(0)` crash
- **ViewPropertyItems** — Add `settings != null` guard before `settings.getValue()` to prevent NPE
- **ViewPropertyItems** — Add null/empty checks for `bean.id` and `currentId` before `charAt(0)`
- **ViewPropertyItems** — Add `!blockBean.parameters.isEmpty()` before `parameters.get(0)` to prevent IndexOutOfBoundsException
- **ViewPane.updateViewBeanProperties** — Replace `view.getTag().toString()` with null-safe `view.getTag() != null ? view.getTag().toString() : ""` (5 places)
- **ViewPane** — Add null/empty checks for `viewBean.id` before `charAt(0)` (3 places in `addViewAndUpdateIndex`, `moveView`, `updateItemView`)
- **ViewPane.getXmlString** — Add null checks for `map.get("key")` and `map.get("text")` to prevent NPE when parsing strings.xml

### values-night Colors
- Add `editor_*` and `palette_*` color definitions to `values-night/colors.xml` for dark theme support

### Other Modules
- **EventListFragment** — Fix `LinearLayoutManager(null)` → use `parent.getContext()`
- **SoundImportFragment** — Fix `LinearLayoutManager(null)` → use `requireContext()`
- **LogicClickListener** — Fix 4× `LinearLayoutManager(null)` → use `logicEditor`
- **LibrarySettingsImporter** — Fix `LinearLayoutManager(null)` → use `activity`
- **ExcludeBuiltInLibrariesActivity** — Fix `LinearLayoutManager(null)` → use `this`
- **LibraryDownloaderDialogFragment** — Fix `LinearLayoutManager(getContext())` → use `requireContext()`; add `getActivity() != null` guards before `showAnErrorOccurredDialog` (4 places) to prevent NPE when fragment is detached
- **LogicEditorActivity** — Fix undefined variable `ss`: use `fieldBlock` for `showAtLocation` anchor, use `viewBean` for `getClassInfo()` (pre-existing compilation errors)

---

# v7.0.0-beta3

## ✨ New Features

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
- **BOM support** — Automatically resolves Bill of Materials for version management
- **Dependency tree caching** — `dependency-tree.json` avoids redundant resolution
- **Built-in dependency detection** — Skips AndroidX, Firebase, Kotlin, etc. that are already bundled
- **EPERM auto-retry** — Falls back to app-specific storage on FUSE-restricted devices

### Library Management UI
- **Sub-dependency viewer** — See all transitive dependencies of a root library
- **Cascade toggling** — Enable/disable root library cascades to sub-dependencies
- **Smart deletion** — Shared sub-dependencies retained, orphan detection and cleanup
- **Version conflict detection** and built-in dependency badges

### Logic Editor
- **Variable and list rename** — Rename variables/lists across all blocks in an activity
- **New blocks**: `getClipboard`, `getExceptionMessage`

### Build System — Incremental Java Compilation
- **Skip ECJ entirely** when no Java files changed and R.java is stable — saves 2-10s per build
- **Partial recompile** — only changed Activity files recompiled; unchanged `.class` files reused
- CRC32-based hash cache (`bin/build_hashes.json`) survives across builds, reset on full clean
- **Full recompile triggered** automatically when: R.java changes (resource IDs), classpath changes, user custom Java/Broadcast/Service files change, or ProGuard is enabled
- **Stale `.class` cleanup** — deleted Activities have their `.class` and inner-class files removed from `bin/classes/` automatically
- 4 files changed: `IncrementalBuildCache` (new), `ProjectFilePaths.cleanRJavaOnly()`, `ProjectBuilder.compileJavaCode()`, `DesignActivity`
- **Fix: `computeCodeGenCacheKey()` missing system file inputs** — Custom event/listener/component definition files (`events.json`, `listeners.json`, `component.json`) now included in the code generation cache key; previously, modifying a custom event's code template wouldn't invalidate the cache

## 🐛 Bug Fixes

### Editor Performance
- **Fix editor tab swiping lag** — Skip unnecessary full view rebuild (990 views) on tab switch; dirty-check ensures rebuild only when Activity file changes
- **Fix ViewPane NPE** — 6 null/exists checks for `BitmapFactory.decodeFile` and `NinePatchDecoder.decodeFile`
- **Remove debug log storm** — `ViewEditor` was logging full stack traces for every custom-ID view

### Component & Lifecycle
- **Fix Gyroscope sensor** — Lifecycle management and type mismatch
- **Fix lifecycle event generation** — Accumulate multi-component events, preserve auto-cleanup with user events, fix ViewBinding ID resolution
- **Fix ClassCastException** in `ViewPropertyItems` for `property_text_size`

### Library Downloads
- **Fix FUSE EPERM** — Auto-retry with fallback path for Huawei/Samsung devices
- **Fix backup/restore** — Reliability improvements for local library restore
- **Fix libType enforcement** — Correct `libType` based on section key during deserialization
- **Fix DependencyResolver crash** — Catch `IOException` to prevent thread crash

### Notification Component Fixes
- 8 incremental fixes: block colors, translations, permission, channel ID, small icon, anonymous class compatibility, params size guards

### Performance
- **O(1) DEX merge estimation** — Replace O(n) method counting with header-based approach

### UI Designer
- **Fix `ViewPane.getUnknownItemView()` data corruption**
- **`ViewHistoryManager.MAX_HISTORY_STEPS`** — Hard-coded 50 extracted to named constant; easy to adjust in one place

### Logic Editor
- **`BlockHistoryManager.MAX_HISTORY_STEPS`** — Same change for consistency; both editors now share the same configurable pattern

## ♻️ Refactoring

### Code Generation System — Registry Pattern
- **`BlockCodeRegistry`** — All 200+ block opcodes migrated from `BlockInterpreter`'s 1400-line switch to a `HashMap<String, BlockCodeHandler>` registry; new blocks can be added via `BlockCodeRegistry.register()` without modifying `BlockInterpreter`
- **`EventCodeRegistry`** — All 40+ event templates migrated from `ComponentCodeGenerator.getEventCode()`'s 310-line switch to a `HashMap<String, EventCodeHandler>` registry
- **`BlockCodeHandler` / `EventCodeHandler`** — New `@FunctionalInterface` contracts for block code generation and event code templates
- **`BlockInterpreter`** — `getBlockCode()` now delegates to `BlockCodeRegistry`; legacy switch fully removed
- **`ComponentCodeGenerator`** — `getEventCode()` now delegates to `EventCodeRegistry`

- **BlockSpecRegistry** — Replace decompiled hashCode switch with HashMap
- **ProjectDataStore** — Replace decompiled enum switches, delete `ScreenOrientationConstants` and `KeyboardSettingConstants`
- **ProjectDataParser** — Replace decompiled hashCode/enum switches, clean up fields
- **StringResource** — Replace decompiled hashCode switch with Set
- **BlockPane** — Replace decompiled hashCode switches with Map lookups
- **Code style cleanup** across 30+ files (this. prefixes, FQN imports, verbose conditionals)
- **Decompiler artifact cleanup** across 17 files

## 🌐 i18n

- Chinese translations for SQLite component, library category titles, onSQLiteError event
- Localized dependency resolver event descriptions

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-02...v7.0.0-beta-03

---

# v7.0.0-beta2

## 🔥 Firebase Libraries Upgrade (v19.x → BOM 33.7.0)

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

## ✨ New Features

- **Project pin feature** — Pin important projects to the top
- **Event list search & sort** — Search and sort events in the event manager
- **Material 3 slider dialogs** — Hybrid slider dialogs for numeric widget properties
- **Library downloader UX** — Advanced progress tracking and improved download experience
- **Local library sorting** — Enabled libraries shown first
- **Project options improvements** — Display project ID, project name, rearranged options by frequency
- **Theme preset reset** — Button to reset theme colors to defaults
- **Language settings** — Optimized with built-in locale picker and i18n fixes
- **SwipeRefreshLayout** — Added as a built-in library
- **MapView listeners** — Initialized by default
- **Add Event keyboard** — Adjusted for keyboard visibility
- **Project data encryption** — Toggle setting for encrypted project data storage

## 🐛 Bug Fixes (102 fixes)

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

## ♻️ Refactoring (146 changes)

- **JAR source integration**: Decompiled JAR classes integrated as Java source with proper naming
- **Class renames**: `a.a.a` package classes renamed for readability with full reference updates
- **String externalization**: ~15 commits extracting hardcoded strings to string resources for i18n
- **ViewBinding migration**: Multiple activities migrated to ViewBinding
- **Deprecated API migration**: `startActivityForResult` → `ActivityResultLauncher`, `onBackPressed` → `OnBackPressedCallback`, `getColor()` → `ContextCompat`
- **Thread safety**: `ExecutorService` migration, `volatile` singletons, synchronized access
- **Exception narrowing**: ~20 commits replacing broad `catch(Exception)` with specific exception types
- **Logging**: `printStackTrace()` → `Log.e()`, stale log tags replaced with class names
- **Removed unused code**: Facebook ads, OneSignal, DynamicLinks components removed

## 📦 Dependencies

- R8: 8.9.35 → 8.11.18
- Lottie: updated to 6.6.7
- Material: updated to 1.14.0-alpha05
- androidx.activity: 1.10.1 → 1.11.0
- android.jar: platform-34 added
- Built-in libraries versions updated

---

**Full Changelog**: https://github.com/webmturn/Sketchware-Pro/compare/v7.0.0-beta-01...v7.0.0-beta-02
