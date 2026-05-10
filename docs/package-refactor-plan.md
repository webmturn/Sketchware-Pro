# pro.sketchware.core 包拆分重构方案

> 输入凭据：
> - 验收基线：`docs/package-refactor-kickoff.md` § 1.4 (124 文件 / 260 外部引用方 / 764 imports / 反射/序列化/Manifest/资源/.kt 全部 0)
> - 历史命名：`docs/refactoring-naming-map.md`
> - codegen 子系统：`docs/code-generation-system-analysis.md`
>
> 本文档只输出方案，不包含任何代码改动。所有 batch 由 Phase 2 模型按 § C 的 YAML 块逐批执行。

---

## A. 目标子包划分（9 个，覆盖 124 文件，无重复无遗漏）

> 硬约束兑现：拆分完成后 `pro.sketchware.core/` 自身不直接保留任何 `.java`，全部下沉到下面 9 个子包目录中。124 = 2 + 13 + 18 + 12 + 22 + 23 + 7 + 12 + 15。

### A.1 `pro.sketchware.core.async` — 后台执行基础设施（2 文件）

> 一句话职责：提供 IO/SERIAL Executor + 主线程 Handler 与生命周期感知的 `TaskHost`，统一的后台任务入口。

```
BackgroundTasks.java
TaskHost.java
```

### A.2 `pro.sketchware.core.callback` — 跨层小型回调与异常类型（13 文件）

> 一句话职责：纯接口/异常容器，承载层与层之间传递的回调签名，不引入业务行为。

```
BlockSizeListener.java
BuildCallback.java
EventSelectedCallback.java
FileSelectedCallback.java
IntCallback.java
PropertyChangedCallback.java
SimpleCallback.java
ViewBeanCallback.java
ViewEditorCallback.java
ViewEnableRunnable.java
CompileException.java
SimpleException.java
SketchwareException.java
```

### A.3 `pro.sketchware.core.util` — 通用工具类（18 文件）

> 一句话职责：与项目模型无关的纯工具方法（Bitmap、ZIP、设备/UI、加密 IO、格式化等）。

```
AnimationUtil.java
BitmapUtil.java
DateTimeUtil.java
DeviceUtil.java
EncryptedFileUtil.java
FormatUtil.java
GsonMapHelper.java
HashMapTypeToken.java
MapValueHelper.java
NinePatchDecoder.java
ReflectiveToString.java
SharedPrefsHelper.java
SketchToast.java
ThrottleTimer.java
UIHelper.java
UriPathResolver.java
ViewUtil.java
ZipUtil.java
```

### A.4 `pro.sketchware.core.validation` — 文本输入校验器家族（12 文件）

> 一句话职责：`BaseValidator` 及其子类，给 TextInput 控件挂载具体的命名/范围/保留字校验规则。

```
BaseValidator.java
ActivityNameValidator.java
FileNameValidator.java
IdentifierValidator.java
LengthRangeValidator.java
LowercaseNameValidator.java
NumberRangeValidator.java
ResourceNameValidator.java
UniqueNameValidator.java
VariableNameValidator.java
VersionCodeValidator.java
XmlNameValidator.java
```

### A.5 `pro.sketchware.core.project` — 项目数据模型与持久化（22 文件）

> 一句话职责：项目数据存取（视图/逻辑/资源/库/集合/历史）、路径与全局常量、`ClassInfo` 类型描述。

```
BaseCollectionManager.java
BlockCollectionManager.java
BlockHistoryManager.java
BuildConfig.java
BuiltInLibrary.java
ClassInfo.java
FontCollectionManager.java
ImageCollectionManager.java
LibraryManager.java
MoreBlockCollectionManager.java
ProjectDataManager.java
ProjectDataParser.java
ProjectDataStore.java
ProjectFileManager.java
ProjectListManager.java
RecentHistoryManager.java
ResourceManager.java
SketchwareConstants.java
SketchwarePaths.java
SoundCollectionManager.java
ViewHistoryManager.java
WidgetCollectionManager.java
```

### A.6 `pro.sketchware.core.codegen` — 代码生成器与翻译表（23 文件）

> 一句话职责：把 ProjectDataStore 中的 Block/Event/Component/View 翻译为 Java/XML/Gradle 文本片段；包含 Block/Event 注册表与翻译资源。

```
ActivityCodeGenerator.java
ActivityConfigConstants.java
BlockCodeHandler.java
BlockCodeRegistry.java
BlockColorMapper.java
BlockConstants.java
BlockInterpreter.java
BlockSpecRegistry.java
CodeContext.java
CodeFormatter.java
ComponentCodeGenerator.java
ComponentTemplates.java
ComponentTypeMapper.java
EventCodeGenerator.java
EventCodeHandler.java
EventCodeRegistry.java
EventRegistry.java
GradleFileGenerator.java
LayoutGenerator.java
ListenerCodeRegistry.java
LocalLibraryManifestMerger.java
StringResource.java
XmlLayoutParser.java
```

### A.7 `pro.sketchware.core.build` — 构建/打包/签名编排（7 文件，**枢纽**）

> 一句话职责：组合 codegen 与 project 数据驱动 APK 的整个构建—签名—增量缓存链路；包含 ProjectFilePaths（每项目路径与生成入口）与 ManifestGenerator（与 ProjectFilePaths 双向耦合，必须同包）。

```
CompileQuizManager.java
IncrementalBuildCache.java
KeyStoreManager.java
KeyStoreOutputStream.java
ManifestGenerator.java
ProjectBuilder.java
ProjectFilePaths.java
```

### A.8 `pro.sketchware.core.ui` — 编辑器内嵌的非 Fragment 视图（12 文件）

> 一句话职责：BlockView 家族 + Firebase 库 UI + 库设置接口/抽象 + 调色板/主题等可视化原子组件。

```
BaseBlockView.java
BlockView.java
DefinitionBlockView.java
FieldBlockView.java
FirebasePreviewView.java
FirebaseSettingsView.java
FirebaseStorageView.java
LibraryConfigView.java
LibrarySettingsView.java
PresetLayoutFactory.java
UserExperienceLevel.java
WidgetPaletteIcon.java
```

### A.9 `pro.sketchware.core.fragments` — Fragment 与列表辅助（15 文件）

> 一句话职责：编辑器中的 Fragment 实现以及视图文件列表 Adapter / RecyclerView 监听器。

```
BaseFragment.java
ComponentListFragment.java
EventListFragment.java
ImageCollectionFragment.java
ImageListFragment.java
PermissionFragment.java
SoundImportFragment.java
SoundListFragment.java
ViewEditorFragment.java
ViewFileClickListener.java
ViewFileEditClickListener.java
ViewFileLongClickListener.java
ViewFileScrollListener.java
ViewFilesAdapter.java
ViewFilesFragment.java
```

---

## B. 依赖规则与依赖图（DAG，无循环）

### B.1 推荐依赖方向

```
fragments ──▶ ui ──┐
       │           ├──▶ codegen ──▶ project ──▶ util ──▶ async
       ├──▶ validation ┘                  │
       └──────────────────────────────────┤
                                          ▼
                                       callback
build ──▶ codegen, project, util, async, callback
```

- **顶层（出度高，入度 0 或仅来自 fragments/外部）**：`fragments`, `build`
- **中层**：`ui`, `validation`, `codegen`
- **基础层**：`project`
- **叶子（出度 ≤ 1，入度高）**：`util`, `callback`, `async`

任何子包只能依赖位于其下方的子包。`build` 与 `fragments`/`ui` 之间彼此无依赖（外部 Activity 同时使用两者）。`util`/`callback`/`async` 三个叶子之间也彼此无依赖。

### B.2 子包间引用强度（grep 估算，class 名出现次数；统计源限定在子包对应的文件集合内）

> 数据来源：`grep_search` 在每个子包文件集合内对其它子包类名做正则匹配，去掉对自身子包类的命中。引用强度按 *使用次数* 统计而非 *import 行*，因此跨多次使用同一类的高频文件会显著抬高数值。仅用于判断方向与发现潜在循环；精确数字以 Phase 2 实施前再次抽样为准。

```
from \ to       async  callback  util  validation  project  codegen  build  ui  fragments
async             —       0        0       0         0        0       0     0      0
callback          0       —        0       0         0        0       0     0      0
util              0       0        —       0         0        0       0     0      0
validation        0       1        2       —         0       30+      0     0      0
project           ~5      ~3      ~25      0         —        0       0     0      0
codegen          ~10     ~10      ~80      0      ~150        —       0     0      0
build            ~15      ~5      ~30      0      ~120      ~80       —     0      0
ui                0       2       ~15      0       ~20       ~25      0     —      0
fragments        ~10     ~25     ~100      8       ~80       ~60      0    ~10     —
```

**关键观察**：
1. 上半三角全 0 — 无循环。
2. `project → codegen` 经审计**为 0**：`ProjectDataStore` 唯一可能跨界的引用是 `ClassInfo`，已将 `ClassInfo` 划归 `project`（见下文消解 #1）。
3. `codegen → build` 经审计**为 0**：`ManifestGenerator` 与 `ProjectFilePaths` 双向耦合（`ManifestGenerator.setProjectFilePaths(ProjectFilePaths)` + `ProjectFilePaths` 内部 `new ManifestGenerator(...)`），同样 `ProjectBuilder` 与 `ProjectFilePaths` 双向耦合 — 三者**必须同子包**。已划归 `build`（见下文消解 #2）。
4. `LayoutGenerator` 与 `ManifestGenerator` 内的 `@see ProjectBuilder` Javadoc 标记不构成编译依赖（只影响 javadoc 任务，不影响 `:app:compileDebugJavaWithJavac`）；划归后保留为非限定 `@see`，由后续文档清理批次（不在本次重构范围）处理。

### B.3 循环候选与消解

#### 消解 #1：`ProjectDataStore.getClassInfo(...)` 与 `ClassInfo`

- 现象：`ProjectDataStore`（候选 project 子包）调用 `BlockBean.getClassInfo()` 返回 `ClassInfo`，并以 `ClassInfo` 为参数键移除/查询块；`ComponentTypeMapper`（候选 codegen 子包）也大量构建 `ClassInfo`。
- 若 `ClassInfo` 划归 codegen → project 反向依赖 codegen，造成 `project ↔ codegen` 循环。
- **消解**：将 `ClassInfo` 划归 `project`。`ClassInfo` 自身只 import 了一个外部 `mod.hilal.saif.components.ComponentsHandler`，与 codegen 没有强耦合，纯粹是数据描述类，归于 `project` 语义最贴切。

#### 消解 #2：`ProjectFilePaths` ↔ `ProjectBuilder` ↔ `ManifestGenerator`

- 现象：
  - `ProjectFilePaths.finalizeGeneratedManifest()` 中 `new ProjectBuilder(SketchApplication.getAppContext(), this)`（行 1136 附近）。
  - `ProjectBuilder` 构造形参与字段中处处使用 `ProjectFilePaths`（90+ 命中）。
  - `ManifestGenerator.setProjectFilePaths(ProjectFilePaths)` 在 422–432 行。
  - `ProjectFilePaths` 多处 `new ManifestGenerator(...)`。
- 三类同时双向引用，唯一不破坏行为的方案是**三者放在同一子包**。
- **消解**：把 `ProjectBuilder` / `ProjectFilePaths` / `ManifestGenerator` 全部划归 `core.build`。codegen 中其它生成器（ActivityCodeGenerator / ComponentCodeGenerator / LayoutGenerator / GradleFileGenerator / EventCodeGenerator / Block*Registry / EventCodeRegistry / StringResource / XmlLayoutParser / LocalLibraryManifestMerger / 等）保留在 `codegen`，由 `build.ProjectFilePaths` 单向依赖。
- 顺带把 `IncrementalBuildCache`（仅被 `ProjectBuilder` 使用）和 `KeyStoreManager` / `KeyStoreOutputStream` / `CompileQuizManager` 也归 `core.build`，得到内聚的"枢纽"子包。

#### 消解 #3：`validation → codegen.StringResource`

- 现象：`XmlNameValidator` / `FileNameValidator` / `ResourceNameValidator` / `NumberRangeValidator` 等用 `StringResource.getInstance().getTranslatedString(...)` 生成本地化错误提示。
- 这是**单向**依赖（`codegen.StringResource` 不反向使用任何 validator），属于 DAG 内合法边。
- **不需要消解**，仅记录方向：`validation → codegen`。

#### 消解 #4：`ui → codegen` / `ui → project`

- `BaseBlockView` / `BlockView` / `FieldBlockView` 等 BlockView 家族使用 `ClassInfo`（已归 project）、`ComponentTypeMapper`/`BlockColorMapper`/`StringResource`（codegen）。
- 单向，DAG 合法。

---

## C. 拆分批次顺序（共 12 批，每批 ≤ 15 文件，按 Phase 2 模型机械读取）

> 顺序依据：
> 1. 优先迁移"被依赖少"的叶子（fragments/ui/validation/build），它们移动后没有其它已落地子包需要回写 import；
> 2. 中层 codegen → project 顺序处理，避免 build 早于 codegen 落地后 build 内部 import 还要二次重写；
> 3. 基础层（util/callback/async）保留在最后 — 这三类叶子被极多文件 import，单批 ripple 最大，放到最后让前面所有 batch 的引用一次性落地新坐标。
>
> **每个批次 commit 后**必须满足：
> - `:app:compileDebugJavaWithJavac` BUILD SUCCESSFUL
> - `git diff --check` 无空白错误
> - `grep_search` 全仓 `import pro\.sketchware\.core\.<旧扁平类名>;` 命中数 = 0（针对该批所有移动类）
> - `import pro\.sketchware\.core\.\*;` 通配 import 命中数 = 0

---

```
### 第 1 批：fragments（叶子）
target_package: pro.sketchware.core.fragments
files:
  - BaseFragment.java
  - ComponentListFragment.java
  - EventListFragment.java
  - ImageCollectionFragment.java
  - ImageListFragment.java
  - PermissionFragment.java
  - SoundImportFragment.java
  - SoundListFragment.java
  - ViewEditorFragment.java
  - ViewFileClickListener.java
  - ViewFileEditClickListener.java
  - ViewFileLongClickListener.java
  - ViewFileScrollListener.java
  - ViewFilesAdapter.java
  - ViewFilesFragment.java
depends_on_already_moved: []
external_import_sites_estimate: 30
commit_message: "refactor(core): split fragments and view-file listeners into core.fragments sub-package"
```

```
### 第 2 批：ui（叶子）
target_package: pro.sketchware.core.ui
files:
  - BaseBlockView.java
  - BlockView.java
  - DefinitionBlockView.java
  - FieldBlockView.java
  - FirebasePreviewView.java
  - FirebaseSettingsView.java
  - FirebaseStorageView.java
  - LibraryConfigView.java
  - LibrarySettingsView.java
  - PresetLayoutFactory.java
  - UserExperienceLevel.java
  - WidgetPaletteIcon.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
external_import_sites_estimate: 12
commit_message: "refactor(core): split block-view widgets and library-config views into core.ui sub-package"
```

```
### 第 3 批：validation（叶子）
target_package: pro.sketchware.core.validation
files:
  - ActivityNameValidator.java
  - BaseValidator.java
  - FileNameValidator.java
  - IdentifierValidator.java
  - LengthRangeValidator.java
  - LowercaseNameValidator.java
  - NumberRangeValidator.java
  - ResourceNameValidator.java
  - UniqueNameValidator.java
  - VariableNameValidator.java
  - VersionCodeValidator.java
  - XmlNameValidator.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
external_import_sites_estimate: 25
commit_message: "refactor(core): split text-input validators into core.validation sub-package"
```

```
### 第 4 批：build（枢纽 / 叶子）
target_package: pro.sketchware.core.build
files:
  - CompileQuizManager.java
  - IncrementalBuildCache.java
  - KeyStoreManager.java
  - KeyStoreOutputStream.java
  - ManifestGenerator.java
  - ProjectBuilder.java
  - ProjectFilePaths.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
external_import_sites_estimate: 50
commit_message: "refactor(core): split build/sign orchestration and ProjectFilePaths into core.build sub-package"
```

```
### 第 5 批：codegen（中游，第 1/2 部分 — Block/Event 注册表与小型工具）
target_package: pro.sketchware.core.codegen
files:
  - ActivityConfigConstants.java
  - BlockCodeHandler.java
  - BlockCodeRegistry.java
  - BlockColorMapper.java
  - BlockConstants.java
  - BlockInterpreter.java
  - BlockSpecRegistry.java
  - CodeContext.java
  - CodeFormatter.java
  - EventCodeHandler.java
  - EventCodeRegistry.java
  - EventRegistry.java
  - ListenerCodeRegistry.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
external_import_sites_estimate: 25
commit_message: "refactor(core): split block/event registries and constants into core.codegen sub-package"
```

```
### 第 6 批：codegen（中游，第 2/2 部分 — 组装器与生成器）
target_package: pro.sketchware.core.codegen
files:
  - ActivityCodeGenerator.java
  - ComponentCodeGenerator.java
  - ComponentTemplates.java
  - ComponentTypeMapper.java
  - EventCodeGenerator.java
  - GradleFileGenerator.java
  - LayoutGenerator.java
  - LocalLibraryManifestMerger.java
  - StringResource.java
  - XmlLayoutParser.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen   # 第 5 批已落地的同子包前置
external_import_sites_estimate: 30
commit_message: "refactor(core): split activity/component/layout generators and StringResource into core.codegen sub-package"
```

```
### 第 7 批：project（基础层，第 1/2 部分 — 集合与历史管理器、ClassInfo）
target_package: pro.sketchware.core.project
files:
  - BaseCollectionManager.java
  - BlockCollectionManager.java
  - BlockHistoryManager.java
  - ClassInfo.java
  - FontCollectionManager.java
  - ImageCollectionManager.java
  - MoreBlockCollectionManager.java
  - ProjectDataParser.java
  - RecentHistoryManager.java
  - SoundCollectionManager.java
  - ViewHistoryManager.java
  - WidgetCollectionManager.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
external_import_sites_estimate: 30
commit_message: "refactor(core): split collection/history managers and ClassInfo into core.project sub-package"
```

```
### 第 8 批：project（基础层，第 2/2 部分 — 数据/路径/常量/库管理）
target_package: pro.sketchware.core.project
files:
  - BuildConfig.java
  - BuiltInLibrary.java
  - LibraryManager.java
  - ProjectDataManager.java
  - ProjectDataStore.java
  - ProjectFileManager.java
  - ProjectListManager.java
  - ResourceManager.java
  - SketchwareConstants.java
  - SketchwarePaths.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
  - pro.sketchware.core.project   # 第 7 批已落地的同子包前置
external_import_sites_estimate: 130
commit_message: "refactor(core): split project data store, paths and managers into core.project sub-package"
```

```
### 第 9 批：util（叶子，第 1/2 部分 — 通用工具）
target_package: pro.sketchware.core.util
files:
  - AnimationUtil.java
  - BitmapUtil.java
  - DateTimeUtil.java
  - DeviceUtil.java
  - FormatUtil.java
  - GsonMapHelper.java
  - HashMapTypeToken.java
  - MapValueHelper.java
  - NinePatchDecoder.java
  - ReflectiveToString.java
  - SharedPrefsHelper.java
  - SketchToast.java
  - ThrottleTimer.java
  - UIHelper.java
  - ViewUtil.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
  - pro.sketchware.core.project
external_import_sites_estimate: 200
commit_message: "refactor(core): split bitmap/format/device/UI helpers into core.util sub-package (part 1)"
```

```
### 第 10 批：util（叶子，第 2/2 部分 — 加密/Zip/Uri 与剩余工具）
target_package: pro.sketchware.core.util
files:
  - EncryptedFileUtil.java
  - UriPathResolver.java
  - ZipUtil.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
  - pro.sketchware.core.project
  - pro.sketchware.core.util       # 第 9 批已落地的同子包前置
external_import_sites_estimate: 35
commit_message: "refactor(core): split encrypted-file IO, zip and URI utilities into core.util sub-package (part 2)"
```

```
### 第 11 批：callback（叶子）
target_package: pro.sketchware.core.callback
files:
  - BlockSizeListener.java
  - BuildCallback.java
  - CompileException.java
  - EventSelectedCallback.java
  - FileSelectedCallback.java
  - IntCallback.java
  - PropertyChangedCallback.java
  - SimpleCallback.java
  - SimpleException.java
  - SketchwareException.java
  - ViewBeanCallback.java
  - ViewEditorCallback.java
  - ViewEnableRunnable.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
  - pro.sketchware.core.project
  - pro.sketchware.core.util
external_import_sites_estimate: 30
commit_message: "refactor(core): split callbacks and exception types into core.callback sub-package"
```

```
### 第 12 批：async（叶子，最后一批）
target_package: pro.sketchware.core.async
files:
  - BackgroundTasks.java
  - TaskHost.java
depends_on_already_moved:
  - pro.sketchware.core.fragments
  - pro.sketchware.core.ui
  - pro.sketchware.core.validation
  - pro.sketchware.core.build
  - pro.sketchware.core.codegen
  - pro.sketchware.core.project
  - pro.sketchware.core.util
  - pro.sketchware.core.callback
external_import_sites_estimate: 5
commit_message: "refactor(core): split BackgroundTasks and TaskHost into core.async sub-package"
```

> 第 12 批合并后必须验证：`pro.sketchware.core/` 目录下 **不再直接存在任何 .java 文件**，与 § E 中的"完全完成"checklist 对齐。

---

## D. 风险点列表

### D.1 外部引用 Top-10（grep 估算，scope = 不在 `pro/sketchware/core/` 之内的 .java 文件中精确字面量 `^import pro\.sketchware\.core\.<X>;`）

> 数据采样自 Phase 1 的 `grep_search`。`SketchwarePaths` 测得为 17 (com) + 30 (mod) + 19 (pro) + 6 (dev) = 72；其余按四个根包合并近似。三位数估算的具体精度不影响计划成立，仅用于挑出最高 ripple 类便于在 Phase 2 单批内重点核对。

| Rank | Class | 子包 | 估算外部 import 数 | 备注 |
|---|---|---|---|---|
| 1 | `SketchwarePaths` | core.project | **72** | 全仓最热引用，4 个根包共享。第 8 批 ripple 最大单点。 |
| 2 | `ViewUtil` | core.util | **~75** | 仅 com/* 已 71 命中，几乎所有自定义 View 都用。第 9 批 ripple 最大单点。 |
| 3 | `UIHelper` | core.util | **~50** | dialog / 设置面板大量使用。第 9 批。 |
| 4 | `SketchToast` | core.util | **~45** | 跨层错误/成功提示。第 9 批。 |
| 5 | `BuildConfig` | core.project | **21** | 不要与 Android 框架 `BuildConfig` 混淆，分别完整 FQN。第 8 批。 |
| 6 | `ProjectListManager` | core.project | **21** | 备份/导出/项目列表入口。第 8 批。 |
| 7 | `ProjectDataManager` | core.project | **16** | 项目数据 Facade。第 8 批。 |
| 8 | `BaseValidator` | core.validation | **10** | `pro.sketchware.lib.validator.*` 大量继承。第 3 批。 |
| 9 | `BaseFragment` | core.fragments | **9** | 主要被 settings 下 Fragment 继承。第 1 批。 |
| 10 | `StringResource` | core.codegen | **7** | 翻译入口（非生成器）。第 6 批。 |

**结论**：
- 真正的"大批量 ripple"集中在 **第 8 批（project 数据/路径）** 与 **第 9 批（util 通用工具）**，两批 ripple 量均在三位数级；其它批次都是几十级别。
- Phase 2 在跑这两批时务必把 `WaitDurationSeconds`/重试预算放宽，并按 § C 的"3 次最大尝试"规则严守。

### D.2 反射 / 序列化 / 资源 XML / Manifest / .kt — 不需重复审计

直接引用 `docs/package-refactor-kickoff.md` § 1.4 *Verified baseline* 行结论：

| 路径 | 现状 | 影响 |
|---|---|---|
| `AndroidManifest.xml` 中 `pro.sketchware.core.*` 引用 | 0 | 无需改动 |
| `app/src/main/res/**` XML 中 `pro.sketchware.core.*` 引用 | 0 | 无需改动 |
| `*.gradle` / `proguard-rules.pro` | 0 | 无需改动 |
| `Class.forName("pro.sketchware.core.*")` / 字符串 FQN | 0 | 无反射坍塌风险 |
| `core/` 内反射 | 2 处仅指向外部类（`Bitmap.mNinePatchChunk`、`dx Main$Arguments`） | 与本次包重命名无关 |
| `EncryptedFileUtil` 项目数据格式 | 文本 section，**非 Java 序列化** | 包重命名不影响磁盘兼容性 |
| `core/` 内 `.kt` 文件 | 0 | 仅需处理 `.java` |
| 自动化测试 | 0 | 验证依赖 `assembleDebug` + 手测 |

### D.3 跨子包高耦合点（每批必须特别关注）

| 文件 | 所属子包 | 跨子包耦合面 | 主要风险 | 已落地的缓解 |
|---|---|---|---|---|
| `ProjectFilePaths` | core.build | 反向 import codegen 几乎全部生成器 + project 全部数据类 + util.ZipUtil | 单文件接近 50 个跨包符号；任一漏改 import 立刻编译失败 | 与 ManifestGenerator/ProjectBuilder 同包；保持 build 是最后一个移动的 *上层* 子包 |
| `ProjectBuilder` | core.build | 反向 import IncrementalBuildCache、KeyStoreManager（同包），codegen LayoutGenerator/ManifestGenerator @see（Javadoc only） | 长方法体内部多处 ProjectFilePaths 字段访问 | 与 ProjectFilePaths 同包，避免跨包字段访问可见性问题 |
| `ManifestGenerator` | core.build | `setProjectFilePaths(ProjectFilePaths)` 形参类型 | 若划入 codegen 会形成 codegen ↔ build 循环 | 同包消解（B.3 #2）|
| `ProjectDataStore` | core.project | 26 处 `ClassInfo` 调用 + 持有 LibraryBean/EventBean/BlockBean 等外部 bean | `ClassInfo` 跨包会产生 project → codegen 反向边 | `ClassInfo` 已同样划归 project（B.3 #1）|
| `ActivityCodeGenerator` | core.codegen | 跨包 `BuildConfig`、`ProjectDataManager`、`SketchwarePaths`、`StringResource`、`ManageLocalLibrary`（外部）、`ProjectSettings`（外部）等 ≥ 40 个符号 | 第 6 批最大单文件 ripple | 第 6 批已让 build / project / 同子包 codegen 第 5 批先落地 |
| `LayoutGenerator` | core.codegen | 大量项目 bean + util 调用 + `@see ProjectBuilder` Javadoc 跨包 | Javadoc 引用降级为非限定名（不影响编译）；第 6 批批次内必须复核所有 `@see` 不被错误地"补全 import" | Phase 2 step 5 diff 必须只允许 package + 必要 import 行 |
| `BaseBlockView` / `BlockView` | core.ui | `ClassInfo`（project）、`ComponentTypeMapper`/`BlockColorMapper`/`StringResource`（codegen）、外部 BlockBean | 第 2 批 commit 后这些类还引用旧扁平 codegen/project 类名，要等第 5/6/8 批回写 | 是预期行为（早期批次 ui/fragments 内的 `pro.sketchware.core.*` 旧 import 仍合法，因为 codegen/project 仍未拆分）|

> 注意：本节**不**包含 "core → com.besome.sketch.beans 113 处 import" 这一条目。该耦合是 *Verified baseline* 已明示的 out-of-scope 现状（bean POJO 留在原地），不在本次重构范围。

---

## E. 验收 checklist 模板

### E.1 引用 kickoff § 5

每批 commit 前 / 后必须满足 `docs/package-refactor-kickoff.md` § 5 中的 12 项基础 checklist。本计划不重复列出。

### E.2 各批附加回归点

| 批次 | 额外验证 | 目的 |
|---|---|---|
| 第 1 批 (fragments) | 装机后打开 `MainActivity → Settings`（覆盖 `BlockSelectorManagerFragment` 等设置 Fragment） | 验证 BaseFragment / PermissionFragment 子类继承链未破 |
| 第 2 批 (ui) | 进入 LogicEditor，确认块面板渲染正常（BaseBlockView/BlockView） | 验证 BlockView 家族构造与 ClassInfo 跨包仍可解析 |
| 第 3 批 (validation) | 在新建项目对话框输入非法包名/项目名，确认错误提示文案出现 | 验证 BaseValidator 子类与 StringResource 跨包绑定 |
| 第 4 批 (build) | 在示例工程 743 上 Run，APK 装机正常 | 覆盖 ProjectBuilder / ProjectFilePaths / ManifestGenerator 同包链路 |
| 第 5–6 批 (codegen) | 对一个含 Firebase + AdView + RecyclerView 的工程 Run，对比生成的 .java/AndroidManifest/build.gradle 与上一次 build 输出 byte-for-byte 一致 | 保证生成器迁移后输出未变化 |
| 第 7–8 批 (project) | 进入项目→修改一个 block→保存→关闭→重开，确认数据完整持久化（覆盖 ProjectDataStore/ResourceManager 备份回放） | 验证项目数据与 SketchwarePaths 路径仍正确 |
| 第 9–10 批 (util) | 装机后随便切几次 Toast / 主题；运行一次 Export Project 流程触发 ZipUtil + EncryptedFileUtil | 覆盖最大 ripple 集合；确保运行期工具类仍解析 |
| 第 11 批 (callback) | 对一个事件添加 onClick block → Run，确认事件回调代码生成正确 | 异常类与 Callback 接口在生成器/编辑器内仍可见 |
| 第 12 批 (async) | 触发任意后台任务（保存/导入），观察 LogCat 中 BackgroundTasks 标签 | 后台执行器未因包改名失联 |

> 按 kickoff § 5 规则，第 3、6、9、12 批后还必须额外跑一次 `:app:assembleDebug` + 手测打开主屏。

---

## F. 待人工决策（少量发现，不擅自纳入计划）

> 本节列出 Phase 1 调研中发现的、可能值得未来处理但**与本次包拆分无关**的事项。Phase 2 模型**不应**触碰它们。

1. **`@see ProjectBuilder` Javadoc 跨包未限定**：`LayoutGenerator.java:60` 和 `ManifestGenerator.java:49` 在第 4 批之后会变成跨包 Javadoc 引用。若希望 javadoc 任务仍干净，可在后续单独提交里把它们改为完全限定 `@see pro.sketchware.core.build.ProjectBuilder`。**当前计划中保持原样**（不破坏 `:app:compileDebugJavaWithJavac`）。
2. **`ManifestGenerator` 是否长期留在 `core.build`**：从命名直觉看 `ManifestGenerator` 更像 codegen 的成员，但因 § B.3 #2 的双向耦合而被上调到 build。如果未来希望让 build 只做编排不做生成，需要在 ProjectFilePaths 内移除 `new ManifestGenerator(...)` 直接调用（改为依赖注入或回调），那是行为级改造，**不在本次包重构范围**。
3. **`CompileQuizManager` 的归属**：该类提供"编译过程中的小测试"对话框数据，归 `core.build` 是因为它的唯一调用方在构建 UI 流程；若未来发现它被 codegen 或 settings 直接使用，可考虑独立到 `core.ui` 子包。当前 grep 仅在 build 链路内见用例，**保持现归属**。
4. **`UserExperienceLevel` 的归属**：从 DB 读取用户经验等级，含轻量 UI 提示开关，被划入 `core.ui`。如果 Phase 2 实施时发现它有"非 UI"的 project-level 调用（例如 ProjectDataStore 直接读它做行为分支），可考虑把它下沉到 `core.project`。**默认保持 ui 归属**。
5. **两个误提交的 0 字节占位 `.java` 文件**（Phase 2 第 7 批执行期间发现，与包拆分无关）：
   - `app/src/main/java/com/besome/sketch/beans/PropertyInputItem.java`（0 字节）
   - `app/src/main/java/com/besome/sketch/beans/ViewPropertyItems.java`（0 字节）

   来源：commit `8536deb7a` "Rename methods in ClassInfo - remove wrapper pattern"（2026-02-24）在修改 `com/besome/sketch/editor/property/` 下两个同名真实文件的同时，意外在 `com/besome/sketch/beans/` 新建了两个 0 字节文件（疑似 IDE 跨目录 refactor 残留）。javac 对 0 字节 `.java` 视为空翻译单元直接跳过，不报错，所以躺了 2 个多月未被发现。

   第 7 批执行期间，我第一版 PowerShell import 重写脚本在处理 0 字节文件时抛异常但 `$newContent` 变量未清零，导致按字母序的**上一个文件的内容**被写入这两个 0 字节文件（PropertyInputItem 被填入 ProjectResourceBean 的内容，ViewPropertyItems 被填入 ViewHistoryBean 的内容），触发 javac "class X is public, should be declared in a file named X.java" 报错。已用 `git checkout HEAD -- <两个文件>` 恢复为 0 字节，**与本次重构的 commit 无关**。

   **建议**：在包重构全部完成后，单开一个 `chore:` commit 删掉这两个 0 字节占位文件。不在本次重构范围，Phase 2 模型不应在任何一批里顺手删除它们。

---

## 总结

基于 Verified baseline（260 个外部 .java 文件 / 764 处 import 行），按本方案 9 子包 / 12 批 切分后，外部 import 改动总量级约为 **800–900 行**（≈ 764 行原扁平 import + 少量 fully-qualified 引用补 import + 跨子包 same-package 同包引用拆出新 import），分布在 12 个 commit 之中，其中第 8 批（project 数据层）与第 9 批（util 通用工具）单批触及百级文件，其余批次为几十级。
