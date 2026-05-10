# Sketchware-Pro 目标架构（v2 落地后续重构规划）

> 本文档描述 **v2 包迁移完成后** 的实际仓库布局，以及后续 5 项大型重构（P0b / P0c / P1a / P2a / P2b）的目标态与退出条件。
>
> **本次重写背景**：v2 包迁移（Phase 10–15，13 commit）合入 main 后，原文档（基于 P0a 时代）已与现状大幅偏离，故全面重写。已删除 / 更新的内容：
> - 删除 P0a 段落（已完成）
> - 删除 `mod.*` 顶层包相关段落（v1 已清空）
> - 删除"`com.besome.sketch.beans.*` 永久保留"段落（v2 已合并到 `pro.sketchware.beans/`）
> - 删除 P1b（`mod.agus.jcoderz.dx → :vendor-dx`）—— `:vendor-dx` 子模块已存在
> - 更新所有 com.besome.sketch.* 路径为 pro.sketchware.*
> - 修正 P0b 目标包路径（旧规划写 `pro.sketchware.editor/`，但 v2 后实际位置为 `pro.sketchware.activities.editor.logic/`）
> - 新增 P0c（`DesignActivity` 拆分，与 P0b 同模式）

---

## 0. 起点（v2 落地后的实际状态）

### 0.1 已完成的工作

- **v1 包迁移**（167 文件 / 9 Phase）：`mod.*` / `dev.*` → `pro.sketchware.*`，`a.a.a` → `pro.sketchware.core`
- **v2 包迁移**（13 commit / ~330 文件）：`com.besome.sketch.*` → `pro.sketchware.*`，`kellinwood/` → `pro.sketchware.third_party.kellinwood/`
- **vendor-dx 子模块**：`mod.agus.jcoderz.dx` 已抽到独立 Gradle 模块 `:vendor-dx/`

参考：`docs/package-migration-plan.md`（v1）、`docs/package-migration-plan-v2.md`（v2）

### 0.2 当前 java/ 顶层包

| 顶层包 | 文件 | 状态 |
|--------|-----|------|
| `pro/sketchware/` | 678 | **唯一业务命名空间** |
| `com/bumptech/glide/signature/` | 1 | Glide 反射桥接（package-private 约束，永久保留） |
| `mod/` | — | 不存在（v1 已清空） |
| `kellinwood/` | — | 不存在（v2 已迁到 `pro.sketchware.third_party.kellinwood/`） |
| `com/besome/sketch/` | — | 不存在（v2 已全部迁移） |

### 0.3 pro.sketchware/ 一级子包（22 个 + 2 顶层文件）

| 包 | 文件 | 角色 |
|----|-----|------|
| `activities/` | 300 | 全部 Activity（按业务模块分子包） |
| `core/` | 154 | 业务核心层（async / build / callback / codegen / ctrls / exception / fragments / project / ui / validation） |
| `util/` | 60 | 工具类（顶层 44 + 7 子包 16：apk/format/io/library/relativelayout/theme/xml） |
| `third_party/` | 44 | vendored 第三方（kellinwood/） |
| `beans/` | 32 | 跨包共享 POJO |
| `widgets/` | 18 | UI 组件（含 widgets.base/ 抽象基类 3 个） |
| `library/` | 16 | 项目库管理 |
| `project/` | 13 | 项目实体 |
| `lib/` | 12 | 编辑器子系统基础设施（code_editor/highlighter/iconcreator/base） |
| `dialogs/` | 8 | Dialog |
| `tools/` | 5 | 非 Activity 工具类 |
| `graphics/` | 5 | 图形处理（BitmapUtil/AnimationUtil/NinePatchDecoder/VectorDrawable*） |
| `control/` | 3 | 控件 |
| `adapters/` | 2 | RecyclerView Adapter |
| `blocks/` | 2 | 块相关 |
| `menu/` | 2 | 菜单 |
| `common/` | 1 | （仅 SrcViewerActivity，候选合入 activities.tools/） |
| `firebase/` | 1 | （候选并入 library/ 或 core/） |
| 顶层 | `Config.java`, `SketchApplication.java` | Config 候选并入 `core.build.BuildDefaults` |

### 0.4 pro.sketchware.core 内部分布（154 文件）

| 子包 | 文件 |
|------|-----|
| `codegen/` | 39 |
| `build/` | 27 |
| `project/` | 22 |
| `validation/` | 21 |
| `fragments/` | 15 |
| `ui/` | 12 |
| `callback/` | 10 |
| `exception/` | 4 |
| `async/` | 2 |
| `ctrls/` | 2 |

### 0.5 pro.sketchware.activities.editor 内部分布（212 文件）

| 子包 | 文件 |
|------|-----|
| `view/`（含 palette 52 + item 45） | 113 |
| `manage/`（含 font/image/sound/view 子包 + library/ 子包 18） | 42 |
| `property/` | 16 |
| `makeblock/` | 8 |
| `component/` | 6 |
| `logic/` | 5 |
| `block/` | 4 |
| `event/` | 2 |
| `command/` | 2 |
| `manifest/` | 2 |
| `code/` | 1 |
| 顶层 | 11（含 `LogicEditorActivity`、`PropertyActivity`、`LogicEditorDrawer` 等） |

---

## 1. 五项后续重构概览

| 编号 | 项目 | 核心动作 | 工作量 | 依赖前置 |
|---|---|---|---|---|
| **P0b** | 拆 `LogicEditorActivity`（143.8 KB） | 抽 6 个 Controller | 5–7 commit | v2 完成 ✅ |
| **P0c** | 拆 `DesignActivity`（80.2 KB） | 同 P0b 模式 | 4–5 commit | v2 完成 ✅ |
| **P1a** | `ProjectBuilder` → BuildStage 链 | 抽 5 个 Stage + Context | 6–8 commit | v2 完成 ✅ |
| **P2a** | codegen Sink 接口层 | 4 个 Sink 接口 + 4 个 File*Sink 实现 | 4–5 commit | P1a |
| **P2b** | PR review checklist + 命名硬规则 | 新文档 + GitHub Action | 1–2 commit | — |

> **建议执行顺序**：`P2b`（最快，建立护栏）→ `P0c`（小且独立，先练手）→ `P0b` → `P1a` → `P2a`。
>
> P0b/P0c 独立无依赖，理论上可并行；为避免一个 PR 周期内两个 Activity 同时大改导致 review 困难，建议串行。

---

## 2. 实际仓库布局（v2 完成后）

```
Sketchware-Pro/
├── settings.gradle                          # 含 :app, :resolver, :vendor-dx
├── app/
│   └── src/main/java/
│       ├── com/bumptech/glide/signature/StringSignature.java   # Glide 反射桥接（永久保留）
│       └── pro/sketchware/
│           ├── Config.java                  # 2 静态常量（候选并入 SketchApplication 或 core.build）
│           ├── SketchApplication.java
│           │
│           ├── activities/                  # 300 文件
│           │   ├── about/   appcompat/   base/   common/(?)
│           │   ├── design/                  # 含 DesignActivity (80.2 KB) ←── P0c 目标
│           │   ├── editor/                  # 212 文件
│           │   │   ├── LogicEditorActivity.java    # 143.8 KB ←── P0b 目标
│           │   │   ├── PropertyActivity.java
│           │   │   ├── LogicEditorDrawer.java
│           │   │   ├── block/   code/   command/   component/   event/
│           │   │   ├── logic/(5)            # P0b 控制器入此包
│           │   │   ├── makeblock/   manage/ (含 library/)   manifest/   property/   view/(113)
│           │   ├── export/   help/   iconcreator/   importicon/
│           │   ├── main/                    # 含 MainDrawer.java
│           │   ├── preview/   projects/   resourceseditor/   settings/   tools/
│           │
│           ├── core/                        # 154 文件
│           │   ├── async/(2)   build/(27)   callback/(10)   codegen/(39)
│           │   │                            # ProjectBuilder.java (68.5 KB) ←── P1a 目标
│           │   │                            # ProjectFilePaths.java (81.1 KB)
│           │   ├── ctrls/(2)   exception/(4)   fragments/(15)
│           │   ├── project/(22)             # ProjectDataStore.java (67.9 KB)
│           │   ├── ui/(12)   validation/(21)
│           │
│           ├── adapters/   beans/(32)   blocks/   common/   control/
│           ├── dialogs/(8)   firebase/   graphics/(5)
│           ├── lib/(12) — base/ code_editor/ highlighter/ iconcreator/
│           ├── library/(16)   menu/   project/(13)
│           ├── third_party/                 # vendored 第三方
│           │   └── kellinwood/(44)          # logging/ + security/zipsigner/ + zipio/
│           ├── tools/(5)                    # 非 Activity 工具类
│           ├── util/(60)                    # 顶层 44 + apk/format/io/library/relativelayout/theme/xml
│           └── widgets/(18)
│               └── base/(3)                 # BaseWidget / CollapsibleLayout / CollapsibleViewHolder
│
├── vendor-dx/                                # 已存在的 Gradle 子模块
│   └── src/main/java/mod/agus/jcoderz/dx/
│
└── resolver/                                 # 已存在的 Gradle 子模块
```

---

## 3. P0b — LogicEditorActivity 控制器抽出

### 3.1 当前状态

- **文件**：`@app/src/main/java/pro/sketchware/activities/editor/LogicEditorActivity.java`，**143.8 KB**
- **职责混杂**：palette 构造 / block 生命周期 / field 交互 / 上下文菜单 / 变量列表 / 历史撤销 / drag 跟踪 / color picker callback / view binding
- **风格参考**：`ViewPane` 拆分（已完成）— Activity 仅做 wiring，逻辑下沉到 Controller

### 3.2 控制器划分

| Controller | 主要职责 | 关键方法 |
|---|---|---|
| `LogicPaletteController` | 块面板构造、分类 | `createPaletteBlockWith*`, `addPaletteCategory`, `addPaletteLabel*`, `setPaletteBuildInterceptor` |
| `LogicBlockController` | block 生命周期 | `addBlockBeans`, `connectBlock`, `deleteBlock`, `trackDragSource`, `createBlockView`, `isBlockValid`, `hitTestCopy`, `setCopyActive`, `hitTestIconDelete`, `activeIconDelete` |
| `LogicFieldController` | 字段交互 | `setFieldValue`, `pickImage`, `showColorPicker`, `onColorPicked`, `onResourceColorPicked`, `showNumberOrStringInput`, `handleBlockFieldClick` |
| `LogicMenuController` | 上下文菜单 + 收藏 | `showBlockContextMenu`, `toggleBlockDisabled`, `toggleBlockCollapsed`, `showSaveToFavorites`, `saveBlockToCollection`, `isStatementBlock` |
| `LogicVariableController` | 变量与列表声明 | `addVariable`, `addListVariable`, 相关 dialog wiring |
| `LogicHistoryController` | undo/redo 栈 | 调度 `pro.sketchware.core.project.BlockHistoryManager` |

### 3.3 包归宿

- **新代码**位于 `pro/sketchware/activities/editor/logic/`（与现有 `BlockPane.java`、`LogicTopMenu.java`、`PaletteBlock.java`、`PaletteSelector.java`、`PaletteSelectorAdapter.java` 同包）
- **Activity 本身保留在 `pro.sketchware.activities.editor.LogicEditorActivity`**（v2 已迁移，路径与 Manifest 同步无问题）
- Activity 只保留：`onCreate` / 生命周期 / 各 Controller 的 wiring（构造 + 注入依赖 + 绑定 listener）/ 必要的 `findViewById` 与 layout inflate

> ⚠️ **目标包路径修正**：原 `package-target-architecture.md` 曾把目标定为 `pro/sketchware/editor/logic/`，但 v2 后此路径不再合理。**新顶级 `pro.sketchware.editor/` 包不应创建**（会与 `pro.sketchware.activities.editor/` 形成"两个 editor 包"混淆）。Controller 应直接挂在 `pro.sketchware.activities.editor.logic/` 现有子包下。

### 3.4 期望收益

- LogicEditorActivity 从 143.8 KB → ~10–15 KB（仅 wiring）
- 单个 controller 平均 ~20 KB，可独立读懂
- 后续给 logic 编辑器加新功能（如新的 block 类型 UI）只需触碰对应 controller

### 3.5 退出条件

- [ ] `LogicEditorActivity.java` ≤ 20 KB
- [ ] 6 个 controller 文件全部位于 `pro/sketchware/activities/editor/logic/`
- [ ] `:app:assembleDebug` 通过
- [ ] 装机后打开示例工程的 Logic 编辑器，能完整执行：拖块 → 连接 → 改字段 → undo/redo → 保存
- [ ] git diff 表明 Activity 本身没有新增逻辑分支（仅 wiring 移动）

---

## 4. P0c — DesignActivity 控制器抽出（与 P0b 同模式）

### 4.1 当前状态

- **文件**：`@app/src/main/java/pro/sketchware/activities/design/DesignActivity.java`，**80.2 KB**
- **职责混杂**：tab 管理 / ViewPane wiring / property panel / event panel / component panel / drawer 控制 / preview 切换

### 4.2 控制器划分（草案，需在启动 P0c 时按实测方法签名细化）

| Controller | 范围 |
|---|---|
| `DesignTabController` | 4 个主 tab 切换、状态保存、tab 间数据同步 |
| `DesignViewController` | ViewPane wiring + drag/drop + 控件层级 |
| `DesignPanelController` | property/event/component 三个右侧面板的统一管理 |
| `DesignDrawerController` | DesignDrawer 菜单交互（保存 / 预览 / 设置 / 帮助） |

### 4.3 包归宿

`pro.sketchware.activities.design.controller/`（新建子包）。Activity 保持原位 `pro.sketchware.activities.design.DesignActivity`。

### 4.4 退出条件

- [ ] `DesignActivity.java` ≤ 15 KB
- [ ] 4 个 controller 位于 `pro/sketchware/activities/design/controller/`
- [ ] `:app:assembleDebug` 通过
- [ ] 装机验证：打开项目 → 切换 4 个 tab → 拖控件 → 改属性 → 添加事件 → 保存

---

## 5. P1a — ProjectBuilder → BuildStage 链

### 5.1 当前状态

- **文件**：`@app/src/main/java/pro/sketchware/core/build/ProjectBuilder.java`，**68.5 KB**
- 实测方法簇与目标 Stage 映射：

| 当前方法 | 目标 Stage |
|---|---|
| `compileResources`, `maybeExtractAapt2`, `buildBuiltInLibraryInformation` | `ResourceStage` |
| `compileJavaCode`, `runEclipseCompiler`, `collectAllSourcePaths`, `getCustomJavaDirectories`, `isCustomJavaSourcePath`, `deleteOldClassFiles`, `getCompiledClassBasePathCandidates`, `extractPackageName`, `getSourcePathDerivedClassBasePath`, `getManagedJavaSourceRoot`, `updateCacheAfterSuccessfulBuild`, `generateViewBinding` | `JavaCompileStage` |
| `createDexFilesFromClasses`, `getDexFilesReady`, `dexLibraries`, `mergeDexes`, `computeDexMergeFingerprint`, `isD8Enabled`, `getDxRunningText` | `DexStage`（D8 / dx 选择封在内部，读 `BuildSettings.SETTING_DEXER`） |
| `runR8`, `runProguard`, `runStringfog`, `proguardAddLibConfigs`, `proguardAddRjavaRules`, `getRJavaRules`, `getProguardClasspath` | `ShrinkStage` |
| `signDebugApk`, `runZipalign` | `SignStage` |
| `buildApk`, `setBuildAppBundle` | 留在 `ProjectBuilder`（编排器） |

### 5.2 目标结构

```java
// pro.sketchware.core.build.BuildStage
public interface BuildStage {
    String name();                         // 用于日志/进度
    boolean shouldRun(BuildContext ctx);   // 例如 ShrinkStage 仅当启用 minify 时跑
    void execute(BuildContext ctx) throws BuildException;
}

// pro.sketchware.core.build.BuildContext
public final class BuildContext {
    public final ProjectFilePaths paths;
    public final BuildSettings settings;
    public final IncrementalBuildCache cache;
    public final BuildProgressReceiver progress;
    // mutable 输出
    public ResourceCompileOutput resources;
    public JavaCompileOutput javaOutput;
    public DexOutput dex;
    public Path signedApk;
}

// pro.sketchware.core.build.BuildPipeline
public final class BuildPipeline {
    private final List<BuildStage> stages;
    public BuildPipeline(BuildSettings settings) {
        stages = List.of(
            new ResourceStage(),
            new JavaCompileStage(),
            settings.kotlinEnabled ? new KotlinCompileStage() : null,
            new DexStage(),                // 内部判断 D8/dx
            settings.shrinkEnabled ? new ShrinkStage() : null,
            new SignStage()
        ).stream().filter(Objects::nonNull).toList();
    }
    public void run(BuildContext ctx) throws BuildException {
        for (BuildStage s : stages) {
            if (!s.shouldRun(ctx)) continue;
            ctx.progress.onStage(s.name());
            s.execute(ctx);
        }
    }
}
```

### 5.3 设计原则

- **Stage 之间通过 `BuildContext` 单向传递**：上游只写自己的输出字段；下游只读上游已写字段。无 Stage ↔ Stage 直接调用
- **D8 vs dx 选择封装在 `DexStage` 内部**：读现有的 `BuildSettings.SETTING_DEXER`。**P1a 严格不改默认值**，也不删除 dx 分支
- **可观察**：`BuildProgressReceiver.onStage(name)` 让 UI 显示当前阶段
- **行为零变化**：P1a 是纯结构重构；同一工程、同一 BuildSettings、同一 D8/dx 选项，产出 APK 必须 byte-for-byte 一致

### 5.4 退出条件

- [ ] `ProjectBuilder.java` ≤ 15 KB（仅 ctx 构造 + `buildApk` 入口）
- [ ] 6 个 Stage 文件位于 `pro/sketchware/core/build/stage/`，每个 ≤ 20 KB
- [ ] `BuildSettings.SETTING_DEXER` 默认值与 P1a 之前一致
- [ ] 默认（dx）路径与 D8 路径都能成功打包
- [ ] 同一工程 + 同一 BuildSettings 下生成 APK byte-for-byte 一致

### 5.5 不在 P1a 范围内（独立后续决策）

- **切默认 dx → D8**：P1a 后由于 D8/dx 分支已集中在 `DexStage`，回归面小、可单测覆盖
- **下线 dx 路径**：删 `DexStage` 中 dx 分支 + 删 `:vendor-dx` 子模块
- **为 `DexStage` 加单测**：依赖项目添加测试基础设施

---

## 6. P2a — codegen Sink 接口层

### 6.1 当前耦合

- codegen 内的 `LayoutGenerator` / `ManifestGenerator` / `ActivityCodeGenerator` / `GradleFileGenerator` 等直接 `new FileWriter(projectFilePaths.getXxxPath())` 写盘
- 这导致：
  - codegen 与 build（`ProjectFilePaths`）紧耦合，难以单测
  - 测试时无法把生成结果 capture 到内存做断言

### 6.2 目标接口（4 个 Sink）

```java
// pro.sketchware.core.codegen.sink.LayoutSink
public interface LayoutSink {
    void writeLayout(String layoutName, String xmlContent) throws IOException;
}

// pro.sketchware.core.codegen.sink.ManifestSink
public interface ManifestSink {
    void writeManifestFragment(String activityFqn, String xmlSnippet) throws IOException;
    void writeFinalManifest(String fullXml) throws IOException;
}

// pro.sketchware.core.codegen.sink.JavaSink
public interface JavaSink {
    void writeJava(String packagePath, String simpleClassName, String source) throws IOException;
}

// pro.sketchware.core.codegen.sink.GradleSink
public interface GradleSink {
    void writeBuildGradle(String moduleName, String content) throws IOException;
    void writeSettingsGradle(String content) throws IOException;
}
```

### 6.3 文件 Sink 实现（位于 build）

```java
// pro.sketchware.core.build.sink.FileLayoutSink
public final class FileLayoutSink implements LayoutSink {
    private final ProjectFilePaths paths;
    public FileLayoutSink(ProjectFilePaths paths) { this.paths = paths; }
    public void writeLayout(String name, String xml) throws IOException {
        Files.writeString(Path.of(paths.getLayoutPath(name)), xml);
    }
}
// FileManifestSink / FileJavaSink / FileGradleSink 同模式
```

### 6.4 调用方改造

- `LayoutGenerator` 构造由 `new LayoutGenerator(projectFilePaths)` 改为 `new LayoutGenerator(layoutSink)`
- `ManifestGenerator`/`ActivityCodeGenerator`/`GradleFileGenerator` 同样
- `ProjectBuilder`（编排器）负责构造 4 个 `File*Sink` 实例并注入

### 6.5 退出条件

- [ ] 4 个 Sink 接口位于 `pro/sketchware/core/codegen/sink/`
- [ ] 4 个 File 实现位于 `pro/sketchware/core/build/sink/`
- [ ] codegen 内部生成器**没有任何**对 `ProjectFilePaths` 的直接 import（仅通过注入的 Sink）
- [ ] 在示例工程上 Run，生成的 `.java`/layout XML/AndroidManifest/build.gradle 与 P2a 之前 byte-for-byte 一致

---

## 7. P2b — PR review checklist + 命名硬规则

### 7.1 现状

- v2 之后，业务代码已 100% 在 `pro.sketchware.*`（`com/bumptech/glide/signature/` 例外，Glide 反射桥接保留）
- **风险**：未来 PR 可能再次引入 `com.besome.sketch.*` 路径（粘贴遗留代码、IDE 自动补全失误、从分支贡献者代码合并）

### 7.2 硬规则

新增 `docs/package-naming-policy.md`，含：

**新代码硬规则**：

1. 任何新增 Activity / Fragment / Manager / Controller / Util 必须放在 `pro.sketchware.*`
2. **禁止**新建 `com.besome.sketch.*` 文件
3. **禁止**新建 `com.bumptech.glide.signature/` 之外的 `com.*` 文件
4. **禁止**新建 `mod.*` 文件（保留给历史，不接受新代码）
5. **禁止**新建 `kellinwood.*` 文件（vendor 代码，第三方更新走 vendor 同步流程）
6. 新 controller/manager/util 命名遵循 `pro.sketchware.<theme>.<purpose>`

**自动化（可选）**：

- **GitHub Action**：PR 中扫描 `app/src/main/java/{com/besome/,mod/,kellinwood/}` 是否含新文件，存在则失败
- **detekt/checkstyle 规则**：禁止新 `import com.besome.sketch.*` / `import mod.*` / 裸 `import kellinwood.*`

### 7.3 退出条件

- [ ] `docs/package-naming-policy.md` 已提交
- [ ] `docs/README.md` 索引含此条目（如有 README index）
- [ ] CI workflow（如启用）含包路径扫描步骤

---

## 8. 跨项依赖与排期建议

```
[v2 已完成]
    │
    ├──► P2b（独立护栏，建议最先做）
    │
    ├──► P0c（DesignActivity 80 KB，小且独立）──┐
    │                                          │
    ├──► P0b（LogicEditor 144 KB，主体重构）─────┤
    │                                          │
    └──► P1a（BuildStage 链，68 KB）─────────────┴──► P2a（Sink 接口）
```

**并行机会**：
- P2b 与 P0c/P0b/P1a 完全独立，可并行
- P0b 与 P0c 同模式但作用于不同 Activity，理论上可并行（不建议同时进行，review 困难）
- P2a 必须等 P1a 完成（P2a 是 P1a 的逻辑延伸）

---

## 9. 完成后的关键改善指标

| 指标 | v2 后实测 | 五项完成后 | 改善 |
|---|---|---|---|
| `LogicEditorActivity.java` | 143.8 KB | ~15 KB | **-90%** |
| `DesignActivity.java` | 80.2 KB | ~15 KB | **-81%** |
| `ProjectBuilder.java` | 68.5 KB | ~15 KB | **-78%** |
| codegen → build 反向依赖 | 双向耦合 | 单向 | 接口解耦 |
| god class（>50 KB）数量 | ≥ 8 | ≤ 5 | 显著降低 |
| 新代码归宿决策成本 | 中（无硬规则） | 低（policy + CI） | 团队规模化前置 |
| 旧包路径再生风险 | 无护栏 | CI 拦截 | 规模化可持续 |

---

## 10. 不在本文档范围内（明示）

为避免范围蔓延，以下事项**不在**当前 5 项规划内，留给未来周期：

- **`ProjectFilePaths` 内部拆分**（81.1 KB）：依赖 P2a 完全解耦后再考虑，否则会重新引入循环
- **`ProjectDataStore` 内部拆分**（67.9 KB）：要拆需要先有 schema 测试基础设施
- **`BlocksHandler` 拆分**（123.1 KB）、**`ExtraPaletteBlock` 拆分**（106 KB）、**`ComponentTemplates` 拆分**（95.9 KB）：需要单独排期
- **加单测/集成测套件**：当前 baseline 测试基础设施薄弱，建议在 P1a 完成后单独立项
- **Kotlin 化某些 controller / stage**：定为 P3+ 备选（保持每个重构周期"只挪逻辑、不换语言"，降低风险面）
- **`pro.sketchware.tools/` 与 `pro.sketchware.activities.tools/` 合并**：当前职责清晰（前者放工具类，后者放 Activity），不需合并
- **v2 后审查发现的若干清理项**（迁移 OldResourceIdMapper、合并稀疏单文件包、`Config.java` 归并等）：作为独立 chore commits 处理，不计入 5 项重构范围

---

## 附录 A — 引用文档

- v1 包迁移：`docs/package-migration-plan.md`
- v2 包迁移：`docs/package-migration-plan-v2.md`
- 通用迁移策略：`docs/package-migration-policy.md`
- 上游架构记录：`docs/DEVELOPMENT.md` / `docs/code-generation-system-analysis.md`
- 既往审计：`docs/android-development-tool-audit-review.md`
- 命名映射（去混淆历史）：`docs/refactoring-naming-map.md`
