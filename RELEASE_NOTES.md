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
