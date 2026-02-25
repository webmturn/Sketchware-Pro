# pro.sketchware.core Package Refactoring - Class Naming Map

## Package History
- Original obfuscated package: `a.a.a`
- Renamed to: `pro.sketchware.core`
- All classes are source code (no JAR constraints)
- **Status: ALL class renames complete. ALL method/field renames complete.**
- `a.a.a` package fully removed from codebase.

## Naming Map

### Already Named (no action needed)
| Current | Status |
|---------|--------|
| `ProjectBuilder` | ✅ Already named |
| `ViewEditorFragment` | ✅ Already named |

### Code Generators
| Original | Name | Role | Size |
|----------|------|------|------|
| `Fx` | `BlockInterpreter` | Processes blocks into Java code expressions | 76KB |
| `Gx` | `ClassInfo` | Represents type hierarchy (boolean, String, View, etc.) | 12KB |
| `Hx` | `EventCodeGenerator` | Organizes events for activity code generation | 18KB |
| `Ix` | `ManifestGenerator` | Generates AndroidManifest.xml | 39KB |
| `Jx` | `ActivityCodeGenerator` | Generates Java source for activities | 53KB |
| `Lx` | `ComponentCodeGenerator` | Generates component/listener source code | 175KB |
| `Ox` | `LayoutGenerator` | Generates XML layout files | 54KB |

### Project Data Management
| Original | Name | Role | Size |
|----------|------|------|------|
| `jC` | `ProjectDataManager` | Singleton holder for eC/hC/kC/iC data managers | 3KB |
| `jq` | `BuildConfig` | Holds build config, permissions, library settings | 11KB |
| `lC` | `ProjectListManager` | Lists projects, handles deletion/backup | 9KB |
| `wq` | `SketchwarePaths` | Static path constants for .sketchware directories | 11KB |
| `yq` | `ProjectFilePaths` | Per-project file path organizer | 55KB |

### UI - Base Classes
| Original | Name | Role | Size |
|----------|------|------|------|
| `qA` | `BaseFragment` | Base Fragment with transitions and task management | 2KB |
| `DA` | `PermissionFragment` | Fragment with storage permission handling | 4KB |
| `MA` | `BaseAsyncTask` | Abstract AsyncTask with error handling | 2KB |

### UI - Validators (BaseValidator subclasses)
| Original | Name | Role | Size |
|----------|------|------|------|
| `MB` | `BaseValidator` | Base text input validator | 2KB |
| `NB` | `UniqueNameValidator` | Validates name not in predefined list | 1KB |
| `RB` | `LowercaseNameValidator` | Validates lowercase name pattern | 1KB |
| `SB` | `LengthRangeValidator` | Validates text length within min/max range | 1KB |
| `VB` | `VariableNameValidator` | Validates variable name (alpha + underscore) | 1KB |
| `YB` | `ActivityNameValidator` | Validates activity/file name with reserved words | 3KB |
| `ZB` | `IdentifierValidator` | Validates identifier with reserved + excluded names | 3KB |

### UI - Feature Fragments (extend BaseFragment)
| Original | Name | Role | Size |
|----------|------|------|------|
| `Fw` | `ViewFilesFragment` | Manages list of activity/view files | 15KB |
| `br` | `ComponentListFragment` | Component list in logic editor | 22KB |
| `fu` | `ImageCollectionFragment` | Image collection management | 9KB |
| `ow` | `SoundListFragment` | Sound list management | 19KB |
| `pu` | `ImageListFragment` | Image list management | 25KB |
| `rs` | `EventListFragment` | Event list and MoreBlock management | 33KB |
| `Yv` | `SoundImportFragment` | Sound import list | 10KB |

### UI - Dialogs & Views
| Original | Name | Role | Size |
|----------|------|------|------|
| `aB` | ~~`SketchDialog`~~ | *(deleted — dead code)* | — |
| `bB` | `SketchToast` | Custom Toast utility | 3KB |

### UI - Firebase Library Views (implement LibraryConfigView)
| Original | Name | Role | Size |
|----------|------|------|------|
| `kv` | `FirebasePreviewView` | Firebase library toggle/preview | 4KB |
| `lv` | `FirebaseSettingsView` | Firebase project settings input | 3KB |
| `mv` | `FirebaseStorageView` | Firebase storage URL settings | 2KB |

### UI - Utilities
| Original | Name | Role | Size |
|----------|------|------|------|
| `gB` | `AnimationUtil` | Static animation helper methods | 4KB |
| `wB` | `ViewUtil` | dp conversion and layout inflation | 1KB |
| `uy` | `WidgetPaletteIcon` | Custom widget icon in palette | 1KB |

### Logic & Data Registries
| Original | Name | Role | Size |
|----------|------|------|------|
| `kq` | `BlockColorMapper` | Maps block opcodes to UI colors | 15KB |
| `mq` | `ComponentTypeMapper` | Builds ClassInfo (Gx), maps component types | 35KB |
| `oq` | `EventRegistry` | Maps event names to icons and descriptions | 22KB |
| `sq` | `SketchwareConstants` | Color palettes, view property defaults, bean data | 30KB |
| `tq` | `CompileQuizManager` | Manages compiling dialog quizzes | 17KB |
| `uq` | `BlockConstants` | Reserved words, component names, action types | 14KB |

### Interfaces
| Original | Name | Role | Size |
|----------|------|------|------|
| `ay` | `SimpleCallback` | Simple void callback interface (method: `onCallback()`) | <1KB |

### Built-in Library
| Original | Name | Role | Size |
|----------|------|------|------|
| `Jp` | `BuiltInLibrary` | Represents a bundled library with metadata | 2KB |

### Phase 8a - Utilities & Data (23 classes)

#### Utilities
| Original | Name | Role | Size |
|----------|------|------|------|
| `zB` | `NinePatchDecoder` | Decodes bitmaps with NinePatch chunk handling | 8KB |
| `HB` | `UriPathResolver` | Converts content/document URIs to file paths | 3KB |
| `KB` | `ZipUtil` | Extract ZIP from assets, create ZIP files | 9KB |
| `nB` | `DateTimeUtil` | Date/time formatting utilities | 3KB |

#### Data Managers / History
| Original | Name | Role | Size |
|----------|------|------|------|
| `bC` | `BlockHistoryManager` | Undo/redo history for BlockBeans | 7KB |
| `cC` | `ViewHistoryManager` | Undo/redo history for ViewBeans | 6KB |
| `fC` | `KeyboardSettingConstants` | Static int array {1..8} | <1KB |

#### Block/Logic
| Original | Name | Role | Size |
|----------|------|------|------|
| `vq` | `ActivityConfigConstants` | Theme names, orientation names, keyboard modes | 1KB |
| `xq` | `VersionCodeValidator` | Validates version code range (200-600) | 1KB |

#### Interfaces
| Original | Name | Role | Size |
|----------|------|------|------|
| `Iw` | `ViewEditorCallback` | View editor refresh + view selection callback | <1KB |
| `Jw` | `FileSelectedCallback` | Single string (filename) callback | <1KB |
| `Kw` | `PropertyChangedCallback` | Key-value property change callback | <1KB |
| `Lw` | `ViewBeanCallback` | ViewBean selection callback | <1KB |
| `Qs` | `EventSelectedCallback` | EventBean selection callback | <1KB |
| `Uu` | `LibrarySettingsView` | Library settings view interface (setData, isValid, getDocUrl) | <1KB |
| `by` | ~~`ProjectFileSelectedCallback`~~ | *(deleted — dead code)* | — |
| `cy` | `BuildCallback` | Build progress callback (start, progress, complete) | <1KB |
| `ty` | ~~`ScrollableContainer`~~ | *(deleted — dead code)* | — |
| `YA` | `IntCallback` | Simple int callback | <1KB |

#### Other
| Original | Name | Role | Size |
|----------|------|------|------|
| `Cx` | `RecentHistoryManager` | Manages recent items per category (max 10, stored in DB P26) | 4KB |
| `jv` | ~~`FirebaseClickListenerLegacy`~~ | *(deleted — dead code)* | — |
| `ro` | `UserExperienceLevel` | Loads user experience level from DB U1 | 1KB |
| `zy` | `SimpleException` | Simple exception wrapper | <1KB |

### Phase 8b - Former JAR Wrapper Renames (wrappers deleted, classes merged)
| Old | New Name | Role |
|-----|----------|------|
| `FB` | `FormatUtil` | Hex conversion, file size formatting, clipboard |
| `iB` | `BitmapUtil` | Bitmap sample size, EXIF rotation |
| `uB` | `HashMapTypeToken` | Gson TypeToken for HashMap (direct rename, no wrapper) |
| `jB` | `ViewEnableRunnable` | Runnable that re-enables a View |
| `dC` | `ScreenOrientationConstants` | Static int array {1..8} |
| `gC` | `ProjectDataParser` | Parses project data JSON |
| `lq` | `BlockSpecRegistry` | Maps block opcode → params/returns (260KB) |
| `rq` | `PresetLayoutFactory` | Creates preset ViewBean layouts |
| `nA` | `ReflectiveToString` | toString via reflection |
| `yy` | `CompileException` | Exception with error list |

### Phase 9 - Previously "JAR constrained" classes (all source code)

#### Interfaces & Small Utilities
| Old | New Name | Role |
|-----|----------|------|
| `Vs` | `BlockSizeListener` | Block size change callback |
| `nv` | `LibraryConfigView` | Library config view interface |
| `Sp` | `ThrottleTimer` | 30-second cooldown tracker |
| `uB` | `HashMapTypeToken` | Gson TypeToken for HashMap |
| `vB` | `GsonMapHelper` | HashMap↔JSON serialization |
| `yB` | `MapValueHelper` | Safe map value getters |

#### Medium Utilities
| Old | New Name | Role |
|-----|----------|------|
| `DB` | `SharedPrefsHelper` | SharedPreferences wrapper |
| `mB` | `UIHelper` | UI interaction utilities |
| `wB` | `ViewUtil` | dp conversion, layout inflation |
| `GB` | `DeviceUtil` | Device info utilities |
| `PB` | `FileNameValidator` | File name validation |
| `WB` | `ResourceNameValidator` | Sound/image resource name validation |
| `QB` | `XmlNameValidator` | XML name validation |
| `OB` | `NumberRangeValidator` | Numeric range validation |
| `By` | `SketchwareException` | Build/compilation exception |
| `oB` | `EncryptedFileUtil` | Encrypted file I/O |

#### Collection Managers
| Old | New Name | Role |
|-----|----------|------|
| `Lp` | `BaseCollectionManager` | Abstract base for collection managers |
| `Mp` | `BlockCollectionManager` | Block collection manager |
| `Np` | `ImageCollectionManager` | Image resource collection manager |
| `Op` | `SoundCollectionManager` | Sound resource collection manager |
| `Pp` | `MoreBlockCollectionManager` | MoreBlock collection manager |
| `Qp` | `FontCollectionManager` | Font collection manager |
| `Rp` | `WidgetCollectionManager` | Widget collection manager |

#### View File Helpers
| Old | New Name | Role |
|-----|----------|------|
| `tw` | `ViewFileScrollListener` | Scroll listener for view files |
| `uw` | `ViewFileClickListener` | Click listener for view files |
| `vw` | `ViewFileLongClickListener` | Long-click listener for view files |
| `ww` | `ViewFileEditClickListener` | Edit click listener for view files |

#### Data Classes
| Old | New Name | Role |
|-----|----------|------|
| `hC` | `ProjectFileManager` | Project file bean management |
| `iC` | `LibraryManager` | Library bean management |
| `kC` | `ResourceManager` | Project resource management |
| `xB` | `StringResource` | String resource loader |
| `xw` | `ViewFilesAdapter` | View files list adapter |
| `iI` | `KeyStoreManager` | Keystore/certificate management |
| `hI` | `KeyStoreOutputStream` | Output stream for keystore |

#### Block View Classes
| Old | New Name | Role |
|-----|----------|------|
| `Ts` | `BaseBlockView` | Base block view (RelativeLayout) |
| `Ss` | `FieldBlockView` | Field/parameter block view |
| `Rs` | `BlockView` | Main block view |
| `Us` | `DefinitionBlockView` | Definition/header block view |

#### Core Class
| Old | New Name | Role |
|-----|----------|------|
| `eC` | `ProjectDataStore` | Core project data holder (views, blocks, events, components) |

## Summary

- **113 classes** renamed across 10 phases (109 after dead code removal)
- **17 JAR wrapper classes** deleted
- **4 dead code classes** deleted
- **68 methods + 18 fields** renamed in Phase 8a classes (1 class deleted before rename)
- Package `a.a.a` fully removed from codebase
- No remaining obfuscated class names
