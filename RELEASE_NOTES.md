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
