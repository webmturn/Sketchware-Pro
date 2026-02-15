# a.a.a Package Refactoring - Class Naming Map

## Constraints
- `eC`, `hC`, `kC`, `iC`, `oB`, `vB`, `yB`, `mB`, `nv`, `DB`, `GB`, `Sp`, `By`, `Vs`, `Ts` etc. are in JAR files (`a.a.a-important-classes.jar` / `a.a.a-notimportant-classes.jar`) and **CANNOT be renamed**
- All renamed classes stay in `a.a.a` package for now (package rename is a separate phase)
- 685 import references across 236 files need updating per rename

## Naming Map

### Already Named (no action needed)
| Current | Status |
|---------|--------|
| `ProjectBuilder` | ✅ Already named |
| `ViewEditorFragment` | ✅ Already named |

### Code Generators
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `Fx` | `BlockInterpreter` | Processes blocks into Java code expressions | 76KB |
| `Gx` | `ClassInfo` | Represents type hierarchy (boolean, String, View, etc.) | 12KB |
| `Hx` | `EventCodeGenerator` | Organizes events for activity code generation | 18KB |
| `Ix` | `ManifestGenerator` | Generates AndroidManifest.xml | 39KB |
| `Jx` | `ActivityCodeGenerator` | Generates Java source for activities | 53KB |
| `Lx` | `ComponentCodeGenerator` | Generates component/listener source code | 175KB |
| `Ox` | `LayoutGenerator` | Generates XML layout files | 54KB |

### Project Data Management
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `jC` | `ProjectDataManager` | Singleton holder for eC/hC/kC/iC data managers | 3KB |
| `jq` | `BuildConfig` | Holds build config, permissions, library settings | 11KB |
| `lC` | `ProjectListManager` | Lists projects, handles deletion/backup | 9KB |
| `wq` | `SketchwarePaths` | Static path constants for .sketchware directories | 11KB |
| `yq` | `ProjectFilePaths` | Per-project file path organizer | 55KB |

### UI - Base Classes
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `qA` | `BaseFragment` | Base Fragment with transitions and task management | 2KB |
| `DA` | `PermissionFragment` | Fragment with storage permission handling | 4KB |
| `MA` | `BaseAsyncTask` | Abstract AsyncTask with error handling | 2KB |

### UI - Validators (MB subclasses)
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `MB` | ⚠️ IN JAR - cannot rename | Base text input validator | - |
| `NB` | `UniqueNameValidator` | Validates name not in predefined list | 1KB |
| `RB` | `LowercaseNameValidator` | Validates lowercase name pattern | 1KB |
| `SB` | `LengthRangeValidator` | Validates text length within min/max range | 1KB |
| `VB` | `VariableNameValidator` | Validates variable name (alpha + underscore) | 1KB |
| `YB` | `ActivityNameValidator` | Validates activity/file name with reserved words | 3KB |
| `ZB` | `IdentifierValidator` | Validates identifier with reserved + excluded names | 3KB |

### UI - Feature Fragments (extend qA/BaseFragment)
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `Fw` | `ViewFilesFragment` | Manages list of activity/view files | 15KB |
| `br` | `ComponentListFragment` | Component list in logic editor | 22KB |
| `fu` | `ImageCollectionFragment` | Image collection management | 9KB |
| `ow` | `SoundListFragment` | Sound list management | 19KB |
| `pu` | `ImageListFragment` | Image list management | 25KB |
| `rs` | `EventListFragment` | Event list and MoreBlock management | 33KB |
| `Yv` | `SoundImportFragment` | Sound import list | 10KB |

### UI - Dialogs & Views
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `aB` | `SketchDialog` | Custom AlertDialog builder | 6KB |
| `bB` | `SketchToast` | Custom Toast utility | 3KB |

### UI - Firebase Library Views (implement nv interface)
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `kv` | `FirebasePreviewView` | Firebase library toggle/preview | 4KB |
| `lv` | `FirebaseSettingsView` | Firebase project settings input | 3KB |
| `mv` | `FirebaseStorageView` | Firebase storage URL settings | 2KB |

### UI - Utilities
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `gB` | `AnimationUtil` | Static animation helper methods | 4KB |
| `wB` | `ViewUtil` | dp conversion and layout inflation | 1KB |
| `uy` | `WidgetPaletteIcon` | Custom widget icon in palette | 1KB |

### Logic & Data Registries
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `kq` | `BlockColorMapper` | Maps block opcodes to UI colors | 15KB |
| `mq` | `ComponentTypeMapper` | Builds ClassInfo (Gx), maps component types | 35KB |
| `oq` | `EventRegistry` | Maps event names to icons and descriptions | 22KB |
| `sq` | `SketchwareConstants` | Color palettes, view property defaults, bean data | 30KB |
| `tq` | `CompileQuizManager` | Manages compiling dialog quizzes | 17KB |
| `uq` | `BlockConstants` | Reserved words, component names, action types | 14KB |

### Interfaces
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `ay` | `SimpleCallback` | Simple void callback interface | <1KB |

### Built-in Library
| Current | Proposed Name | Role | Size |
|---------|--------------|------|------|
| `Jp` | `BuiltInLibrary` | Represents a bundled library with metadata | 2KB |

## Refactoring Priority Order
1. **Phase 1 - Small utilities** (low risk, few references): `wB`, `gB`, `bB`, `aB`, `ay`, `Jp`, `uy`
2. **Phase 2 - Validators**: `NB`, `RB`, `SB`, `VB`, `YB`, `ZB`
3. **Phase 3 - Fragments**: `qA`, `DA`, `Fw`, `br`, `fu`, `ow`, `pu`, `rs`, `Yv`
4. **Phase 4 - Firebase views**: `kv`, `lv`, `mv`
5. **Phase 5 - Data/Logic**: `jC`, `jq`, `lC`, `wq`, `yq`, `MA`
6. **Phase 6 - Code generators**: `Fx`, `Gx`, `Hx`, `Ix`, `Jx`, `Lx`, `Ox`
7. **Phase 7 - Registries**: `kq`, `mq`, `oq`, `sq`, `tq`, `uq`
