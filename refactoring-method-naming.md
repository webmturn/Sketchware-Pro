# Method & Field Renaming Map — Phase 8a Classes

Only Phase 8a classes (no JAR references) have methods/fields safe to rename.
Phase 8b classes have JAR wrappers, so their methods CANNOT be renamed (JAR bytecode calls them by letter).

---

## 1. BlockHistoryManager (was `bC`) — 14 methods, 4 fields

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` (static) | `instance` | `BlockHistoryManager` | Singleton instance |
| `b` | `positionMap` | `Map<String, Integer>` | Current undo/redo position per key |
| `c` | `historyMap` | `Map<String, ArrayList<HistoryBlockBean>>` | History entries per key |
| `d` | `scId` | `String` | Project ID |

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static String a(String, String, String)` | `buildKey` | Concatenates 3 strings with `_` separator |
| `static void a()` | `clearInstance` | Nullifies singleton and clears data |
| `static BlockHistoryManager d(String)` | `getInstance` | Lazy singleton factory |
| `void a(String)` | `trimFutureHistory` | Removes history entries after current position |
| `void a(String, HistoryBlockBean)` | `addHistoryEntry` | Appends entry to history list (max 50) |
| `void a(String, BlockBean, BlockBean)` | `recordUpdate` | Records a block update action |
| `void a(String, BlockBean, int, int, BlockBean, BlockBean)` | `recordAdd` | Records single block add action |
| `void a(String, ArrayList, int, int, BlockBean, BlockBean)` | `recordAddMultiple` | Records multiple blocks add action |
| `void a(String, ArrayList, ArrayList, int, int, int, int, BlockBean, BlockBean, BlockBean, BlockBean)` | `recordMove` | Records block move action |
| `void b(String)` | `removeHistory` | Removes all history for a key |
| `void b(String, ArrayList, int, int, BlockBean, BlockBean)` | `recordRemove` | Records block remove action |
| `void c(String)` | `decrementPosition` | Moves position backward (for undo) |
| `void e(String)` | `incrementPosition` | Moves position forward (for redo) |
| `void f(String)` | `initHistory` | Initializes empty history for a key |
| `boolean g(String)` | `canRedo` | Returns true if redo is possible |
| `boolean h(String)` | `canUndo` | Returns true if undo is possible |
| `HistoryBlockBean i(String)` | `redo` | Performs redo, returns history entry |
| `HistoryBlockBean j(String)` | `undo` | Performs undo, returns history entry |

---

## 2. ViewHistoryManager (was `cC`) — 12 methods, 4 fields

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` (static) | `instance` | `ViewHistoryManager` | Singleton instance |
| `b` | `positionMap` | `Map<String, Integer>` | Current undo/redo position per key |
| `c` | `historyMap` | `Map<String, ArrayList<HistoryViewBean>>` | History entries per key |
| `d` | `scId` | `String` | Project ID |

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static void a()` | `clearInstance` | Nullifies singleton |
| `static ViewHistoryManager c(String)` | `getInstance` | Lazy singleton factory |
| `void a(String)` | `trimFutureHistory` | Removes entries after current position |
| `void a(String, HistoryViewBean)` | `addHistoryEntry` | Appends entry to list (max 50) |
| `void a(String, ViewBean)` | `recordAdd` | Records single view add |
| `void a(String, ViewBean, ViewBean)` | `recordUpdate` | Records view update |
| `void a(String, ArrayList<ViewBean>)` | `recordAddMultiple` | Records multiple views add |
| `void b(String)` | `decrementPosition` | Moves position backward |
| `void b(String, ViewBean)` | `recordMove` | Records view move |
| `void b(String, ArrayList<ViewBean>)` | `recordRemove` | Records view remove |
| `void d(String)` | `incrementPosition` | Moves position forward |
| `void e(String)` | `initHistory` | Initializes empty history |
| `boolean f(String)` | `canRedo` | Returns true if redo possible |
| `boolean g(String)` | `canUndo` | Returns true if undo possible |
| `HistoryViewBean h(String)` | `redo` | Performs redo |
| `HistoryViewBean i(String)` | `undo` | Performs undo |

---

## 3. ZipUtil (was `KB`) — 7 methods, 0 fields

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static void a(Context, String, String)` | `extractAssetZip` | Extracts ZIP from assets to directory |
| `void a(InputStream, String)` | `extractZipStream` | Extracts ZIP from InputStream |
| `void a(String, String)` | `extractZipFile` | Extracts ZIP file to directory |
| `int a(String, File, ZipOutputStream, ArrayList)` | `addDirectoryToZip` | Recursively adds directory to ZIP |
| `void a(String, ArrayList, ArrayList)` | `createZipFile` | Creates ZIP from list of paths |
| `boolean a(String, String, ZipOutputStream)` | `addFileToZip` | Adds single file to ZIP stream |
| `byte[] a(String)` | `readFileToBytes` | Reads file into byte array |

---

## 4. DateTimeUtil (was `nB`) — 7 methods, 0 fields

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `long a()` | `currentTimeMillis` | Returns current time in milliseconds |
| `String a(long, String)` | `formatTimestamp` | Formats timestamp with given pattern |
| `String a(String)` | `formatCurrentTime` | Formats current time with pattern |
| `String a(String, String)` | `convertTimezone` | Converts date string with timezone offset |
| `String a(String, String, String)` | `reformatDate` | Parses and reformats date string |
| `long b(String, String)` | `parseToMillis` | Parses date string to milliseconds |
| `String b(String)` | `formatCurrentTimeGmt` | Formats current time in GMT |

---

## 5. RecentHistoryManager (was `Cx`) — 6 methods, 4 fields

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` (static) | `instance` | `RecentHistoryManager` | Singleton |
| `b` (static) | `maxItems` | `int` | Maximum items per category (10) |
| `c` | `recentMap` | `HashMap<String, ArrayList<String>>` | Recent items per category |
| `d` | `database` | `DB` | SharedPreferences DB (P26) |

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static RecentHistoryManager a()` | `getInstance` | Lazy singleton factory |
| `ArrayList<String> a(String)` | `getRecentItems` | Gets recent items for category |
| `void a(Context)` | `initialize` | Initializes maps and DB |
| `void a(String, String)` | `addRecentItem` | Adds item to recent list (MRU order) |
| `void b()` | `saveToDatabase` | Saves all recent items to DB |
| `void b(String)` | `loadFromDatabase` | Loads recent items from DB |

---

## 6. NinePatchDecoder (was `zB`) — 6 methods, 0 fields

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static Bitmap a(InputStream)` | `decode` | Decodes 9-patch from InputStream |
| `static Bitmap a(String)` | `decodeFile` | Decodes 9-patch from file path |
| `static void a(Bitmap, byte[])` | `extractPadding` | Extracts padding data from bitmap |
| `static void a(OutputStream, int)` | `writeIntLE` | Writes int as little-endian bytes |
| `static void a(byte[], int, int)` | `putIntLE` | Puts int as little-endian in byte array |
| `static byte[] a(Bitmap)` | `buildNinePatchChunk` | Builds the 9-patch chunk data |

---

## 7. UriPathResolver (was `HB`) — 5 methods, 0 fields

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static String a(Context, Uri)` | `resolve` | Resolves URI to file path (main entry) |
| `static String a(Context, Uri, String, String[])` | `queryDataColumn` | Queries content resolver for `_data` |
| `static boolean a(Uri)` | `isDownloadsDocument` | Checks downloads provider authority |
| `static boolean b(Uri)` | `isExternalStorageDocument` | Checks external storage authority |
| `static boolean c(Uri)` | `isMediaDocument` | Checks media provider authority |

---

## 8. VersionCodeValidator (was `xq`) — 2 methods, 0 fields

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static boolean a(String)` | `isValid` | Always returns true (stub) |
| `static boolean b(String)` | `isInRange` | Checks if version code is between 200-600 |

---

## 9. ActivityConfigConstants (was `vq`) — 1 method, 2 fields

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` | `THEME_OPTIONS` | `String[]` | {"Default", "NoActionBar", "FullScreen"} |
| `b` | `ORIENTATION_OPTIONS` | `String[]` | {"Portrait", "Landscape", "Both"} |

### Methods
| Current Signature | Proposed Name | Purpose |
|-------------------|---------------|---------|
| `static String a(int)` | `getKeyboardSettingName` | Returns keyboard setting name for int code |

---

## 10. UserExperienceLevel (was `ro`) — 0 methods, 3 fields

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` | `database` | `DB` | SharedPreferences DB (U1) |
| `b` | `level` | `int` | Raw experience level |
| `c` | `score` | `int` | Computed score (level * 20, capped at 60) |

---

## 11. KeyboardSettingConstants (was `fC`) — 0 methods, 1 field

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` | `VALUES` | `String[]` | Keyboard setting string values |

---

## 12. FirebaseClickListenerLegacy (was `jv`) — 0 methods, 1 field

### Fields
| Current | Proposed | Type | Purpose |
|---------|----------|------|---------|
| `a` | `noOpClickListener` | `View.OnClickListener` | No-op click listener |

---

## Summary

| Class | Methods to Rename | Fields to Rename |
|-------|-------------------|------------------|
| BlockHistoryManager | 18 | 4 |
| ViewHistoryManager | 16 | 4 |
| ZipUtil | 7 | 0 |
| DateTimeUtil | 7 | 0 |
| RecentHistoryManager | 6 | 4 |
| NinePatchDecoder | 6 | 0 |
| UriPathResolver | 5 | 0 |
| VersionCodeValidator | 2 | 0 |
| ActivityConfigConstants | 1 | 2 |
| UserExperienceLevel | 0 | 3 |
| KeyboardSettingConstants | 0 | 1 |
| FirebaseClickListenerLegacy | 0 | 1 |
| **Total** | **68** | **19** |
