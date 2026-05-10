# Sketchware-Pro 目标架构（六项 P0/P1/P2 全部完成后的最终形态）

> 这是一份"完成态"文档：合并 P0a 包拆分（`docs/package-refactor-plan.md` 已落地）、P0b LogicEditor 控制器抽出、P1a BuildStage 链化、P1b dx 抽子模块、P2a codegen Sink 接口层、P2b 命名策略硬规则之后，仓库布局应当是什么样。
>
> 本文档**只描述目标态与每项的退出条件**，不包含具体批次步骤。每项都是一个独立的后续重构周期，可以独立排期。

---

## 0. 起点（已经落地的部分）

- **P0a 包拆分（plan 已产出）**：`pro.sketchware.core/` 124 文件按职能分到 9 个子包（async/callback/util/validation/project/codegen/build/ui/fragments），DAG 依赖、零循环、`core/` 自身为空。详见 `docs/package-refactor-plan.md`。
- 本文档假定 P0a 已经合回 `main`。后续 5 项在 P0a 之上叠加，**不重复 P0a 的内容**。

---

## 1. 六项概览

| 编号 | 项目 | 核心动作 | 工作量 | 依赖前置 |
|---|---|---|---|---|
| P0a | `pro.sketchware.core` 拆子包 | 包级移动 | 12 commit | — |
| **P0b** | **拆 LogicEditorActivity** | 抽 6 个 Controller | 5–7 commit | P0a |
| **P1a** | **`ProjectBuilder` → BuildStage 链** | 抽 5 个 Stage + Context | 6–8 commit | P0a |
| **P1b** | **`mod.agus.jcoderz.dx` → 子模块** | 新 Gradle 模块 `:vendor-dx` | 1–2 commit | — (可与 P0a/P0b 并行) |
| **P2a** | **codegen Sink 接口层** | 4 个 Sink 接口 + 4 个 File*Sink 实现 | 4–5 commit | P0a, 建议在 P1a 之后 |
| **P2b** | **命名策略硬规则文档化** | 新文档 + RELEASE_NOTES 引用 | 1 commit | — (可与任意并行) |

> **建议执行顺序**：`P1b` (并行) → `P0a` → `P2b` → `P0b` → `P1a` → `P2a`。前两项与 P2b 都是低风险、可在 main 直接做；P0b/P1a/P2a 都需要 P0a 先落地，且按列出顺序避免后两项触碰前一项刚动过的文件。

---

## 2. 最终仓库布局（全部完成后）

```
Sketchware-Pro/
├── settings.gradle                         # 加入 :vendor-dx
├── app/                                    # 主模块
│   ├── build.gradle                        # 加 implementation project(':vendor-dx')
│   └── src/main/java/
│       ├── com/besome/sketch/              # ❄ 冻结：仅修 bug，不接受新功能（P2b）
│       │   ├── Config.java
│       │   ├── MainDrawer.java
│       │   ├── beans/                      # 跨包共享 POJO（保留，永久）
│       │   ├── editor/
│       │   │   ├── LogicEditorActivity.java   # ⬅ P0b 后：~300 行 wiring-only
│       │   │   ├── DesignActivity.java        # 未来 P0c 候选
│       │   │   ├── ViewEditor.java
│       │   │   └── ... (legacy)
│       │   ├── adapters/  ctrls/  design/  export/
│       │   ├── help/  lib/  projects/  tools/
│       ├── pro/sketchware/
│       │   ├── SketchApplication.java
│       │   ├── activities/   beans/   blocks/   control/
│       │   ├── core/                       # P0a 完成后，此目录下无任何 .java
│       │   │   ├── async/                  # 2 文件
│       │   │   ├── callback/               # 13 文件
│       │   │   ├── util/                   # 18 文件
│       │   │   ├── validation/             # 12 文件
│       │   │   ├── project/                # 22 文件
│       │   │   ├── codegen/                # 23 文件
│       │   │   │   ├── ActivityCodeGenerator.java
│       │   │   │   ├── ComponentCodeGenerator.java
│       │   │   │   ├── LayoutGenerator.java
│       │   │   │   ├── GradleFileGenerator.java
│       │   │   │   ├── ... (existing 23)
│       │   │   │   └── sink/               # ⬅ P2a NEW
│       │   │   │       ├── LayoutSink.java         # interface
│       │   │   │       ├── ManifestSink.java       # interface
│       │   │   │       ├── JavaSink.java           # interface
│       │   │   │       └── GradleSink.java         # interface
│       │   │   ├── build/                  # ⬅ P1a 完成后扩展
│       │   │   │   ├── ProjectBuilder.java         # 仅做 Stage 编排（~300 行）
│       │   │   │   ├── ProjectFilePaths.java
│       │   │   │   ├── ManifestGenerator.java
│       │   │   │   ├── BuildContext.java           # ⬅ P1a NEW
│       │   │   │   ├── BuildStage.java             # ⬅ P1a NEW (interface)
│       │   │   │   ├── BuildPipeline.java          # ⬅ P1a NEW (顺序编排)
│       │   │   │   ├── KeyStoreManager.java
│       │   │   │   ├── KeyStoreOutputStream.java
│       │   │   │   ├── IncrementalBuildCache.java
│       │   │   │   ├── CompileQuizManager.java
│       │   │   │   ├── stage/              # ⬅ P1a NEW
│       │   │   │   │   ├── ResourceStage.java       # AAPT2
│       │   │   │   │   ├── JavaCompileStage.java    # ECJ + ViewBinding
│       │   │   │   │   ├── KotlinCompileStage.java  # 可选，已有 KotlinCompiler 桥接
│       │   │   │   │   ├── DexStage.java            # D8 / dx 二选，读 BuildSettings.SETTING_DEXER（默认 dx）
│       │   │   │   │   ├── ShrinkStage.java         # R8 + ProGuard + Stringfog
│       │   │   │   │   └── SignStage.java           # apksigner + zipalign
│       │   │   │   └── sink/               # ⬅ P2a NEW (Sink 实现)
│       │   │   │       ├── FileLayoutSink.java
│       │   │   │       ├── FileManifestSink.java
│       │   │   │       ├── FileJavaSink.java
│       │   │   │       └── FileGradleSink.java
│       │   │   ├── ui/                     # 12 文件
│       │   │   └── fragments/              # 15 文件
│       │   ├── editor/                     # ⬅ P0b NEW（新代码，按 P2b 一律放这）
│       │   │   └── logic/
│       │   │       ├── LogicPaletteController.java
│       │   │       ├── LogicBlockController.java
│       │   │       ├── LogicFieldController.java
│       │   │       ├── LogicMenuController.java
│       │   │       ├── LogicVariableController.java
│       │   │       └── LogicHistoryController.java
│       │   ├── dialogs/   firebase/   fragments/   lib/   listeners/
│       │   ├── managers/  menu/   model/   tools/   util/   utility/
│       │   ├── widgets/   xml/
│       └── mod/                            # 第三方/分支贡献者代码
│           ├── agus/jcoderz/
│           │   ├── beans/  dex/  editor/  handle/  multidex/   # 保留
│           │   └── (dx/  ⬅ 已迁出到 :vendor-dx)
│           ├── hey/  hilal/  bobur/  jbk/  remaker/
│           └── ...
└── vendor-dx/                              # ⬅ P1b NEW Gradle 子模块
    ├── build.gradle                        # Java library 模块
    ├── proguard-rules.pro                  # 如需
    └── src/main/java/
        └── mod/agus/jcoderz/dx/            # 384 文件原样保留 (~3 MB)
            ├── command/
            ├── cf/  dex/  io/  merge/  rop/  ssa/  util/
            └── ...
```

> 数字说明：当前 `mod/agus/jcoderz/dx/` 实测 375 个 `.java` 文件 / 3011 KB（含子目录）。P1b 抽出后主模块少 375 文件 / 3 MB，主模块全量编译时间预计降低 15–25%（粗略估计，按 Java 文件数权重）。

---

## 3. P0b — LogicEditorActivity 控制器抽出

### 3.1 当前状态

- 文件：`com/besome/sketch/editor/LogicEditorActivity.java`，~147 KB / 60+ 公开方法
- 职责混杂：palette 构造 / block 生命周期 / field 交互 / 上下文菜单 / 变量列表 / 历史撤销 / drag 跟踪 / color picker callback / view binding
- 风格参考：`ViewPane` 拆分（已完成）— Activity 仅做 wiring，逻辑下沉到 Controller

### 3.2 控制器划分（基于实测方法签名）

| Controller | 主要职责 | 抽出方法（示例） |
|---|---|---|
| `LogicPaletteController` | 块面板构造、分类、palette 钩子 | `createPaletteBlockWith*`, `addPaletteCategory`, `addPaletteLabel*`, `setPaletteBuildInterceptor` |
| `LogicBlockController` | block 生命周期：增/连/删/迁移 | `addBlockBeans`, `connectBlock`, `deleteBlock`, `trackDragSource`, `createBlockView`, `isBlockValid`, `hitTestCopy`, `setCopyActive`, `hitTestIconDelete`, `activeIconDelete` |
| `LogicFieldController` | 字段交互：颜色/图片/输入 | `setFieldValue`, `pickImage`, `showColorPicker`, `onColorPicked`, `onResourceColorPicked`, `showNumberOrStringInput`, `handleBlockFieldClick` |
| `LogicMenuController` | 上下文菜单 + 收藏 | `showBlockContextMenu`, `toggleBlockDisabled`, `toggleBlockCollapsed`, `toggleBlockCollapsed2`, `showSaveToFavorites`, `saveBlockToCollection`, `isStatementBlock` |
| `LogicVariableController` | 变量与列表声明 | `addVariable`, `addListVariable`, 相关 dialog wiring |
| `LogicHistoryController` | undo/redo 栈、历史视图 | （结合现有 `BlockHistoryManager`，作为 Activity 层 controller，调度 `pro.sketchware.core.project.BlockHistoryManager`）|

### 3.3 包归宿

- **新代码**位于 `pro/sketchware/editor/logic/`，遵循 P2b 命名策略
- **Activity 本身保持在 `com.besome.sketch.editor.LogicEditorActivity`**：因为：
  - Manifest 与外部 Intent 跳转都引用此 FQN，移动会引入兼容性问题（用户的 Logic 入口快捷方式、生成代码中的 `.class` 引用等）
  - 行为上仍属于"legacy editor surface"
- Activity 只保留：
  - `onCreate` / 生命周期
  - 各 Controller 的 wiring（构造、注入依赖、绑定 listener）
  - 必要的 `findViewById` 与 layout inflate

### 3.4 期望收益

- LogicEditorActivity 从 ~147 KB → ~10–15 KB（仅 wiring）
- 单个 controller 平均 ~20 KB，可独立读懂
- 后续给 logic 编辑器加新功能（如新的 block 类型 UI）只需触碰对应 controller，不再触碰 god class

### 3.5 退出条件

- [ ] `LogicEditorActivity.java` ≤ 20 KB
- [ ] 6 个 controller 文件全部位于 `pro/sketchware/editor/logic/`
- [ ] `:app:assembleDebug` 通过
- [ ] 装机后打开示例工程的 Logic 编辑器，能完整执行：拖块 → 连接 → 改字段 → undo/redo → 保存
- [ ] git diff 表明 Activity 本身没有新增逻辑分支（仅 wiring 移动）

---

## 4. P1a — ProjectBuilder → BuildStage 链

### 4.1 当前状态

- 文件：`pro.sketchware.core.build.ProjectBuilder`（P0a 完成后所在位置），~70 KB
- 实测方法簇（一一对应到目标 Stage）：

| 当前方法 | 目标 Stage |
|---|---|
| `compileResources`, `maybeExtractAapt2`, `buildBuiltInLibraryInformation` | `ResourceStage` |
| `compileJavaCode`, `runEclipseCompiler`, `collectAllSourcePaths`, `getCustomJavaDirectories`, `isCustomJavaSourcePath`, `deleteOldClassFiles`, `getCompiledClassBasePathCandidates`, `addCompiledClassBasePathCandidate`, `getCurrentCompiledClassBasePath`, `extractPackageName`, `getSourcePathDerivedClassBasePath`, `getManagedJavaSourceRoot`, `updateCacheAfterSuccessfulBuild`, `generateViewBinding` | `JavaCompileStage` |
| `createDexFilesFromClasses`, `getDexFilesReady`, `dexLibraries`, `mergeDexes`, `computeDexMergeFingerprint`, `isD8Enabled`, `getDxRunningText` | `DexStage`（D8 / dx 二选逻辑封在内部，读 `BuildSettings.SETTING_DEXER`。**P1a 不改默认值**，只调代码位置） |
| `runR8`, `runProguard`, `runStringfog`, `proguardAddLibConfigs`, `proguardAddRjavaRules`, `getRJavaRules`, `getProguardClasspath` | `ShrinkStage` |
| `signDebugApk`, `runZipalign` | `SignStage` |
| `buildApk`, `setBuildAppBundle` | 留在 `ProjectBuilder`（编排器） |

### 4.2 目标结构

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
    // ...
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

// pro.sketchware.core.build.ProjectBuilder（精简后）
public final class ProjectBuilder {
    private final BuildContext ctx;
    private final BuildPipeline pipeline;
    public ProjectBuilder(Context android, ProjectFilePaths paths, BuildSettings s) {
        this.ctx = new BuildContext(android, paths, s, ...);
        this.pipeline = new BuildPipeline(s);
    }
    public void buildApk() throws BuildException {
        pipeline.run(ctx);
    }
}
```

### 4.3 设计原则

- **Stage 之间通过 `BuildContext` 单向传递**：上游只写自己的输出字段；下游只读上游已写字段。无 Stage ↔ Stage 直接调用
- **D8 vs dx 选择封装在 `DexStage` 内部**：外部不感知；仍读现有的 `BuildSettings.SETTING_DEXER`。**P1a 严格不改默认值**，也不删除 dx 分支。“切默认”与“下线 dx”是两个独立的后续决策，与 P1a 架构改造无关
- **可测试性**：每个 Stage 独立类，可 mock `BuildContext` 单测（前提是 P0a 完成后增加测试基础设施 — 当前 baseline `Test source code: 0`）
- **可观察**：`BuildProgressReceiver.onStage(name)` 让 UI 显示当前阶段
- **行为零变化**：P1a 是纯结构重构；同一工程、同一 BuildSettings、同一 D8/dx 选项，产出 APK 必须 byte-for-byte 一致

### 4.4 退出条件

- [ ] `ProjectBuilder.java` ≤ 15 KB（仅 ctx 构造 + `buildApk` 入口）
- [ ] 6 个 Stage 文件位于 `pro/sketchware/core/build/stage/`，每个 ≤ 20 KB
- [ ] `BuildSettings.SETTING_DEXER` 默认值与 P1a 之前一致（当前为 `SETTING_DEXER_DX`，P1a 不动它）
- [ ] 在示例工程上跑两道路径验证都能走通：默认（dx）路径打包成功 + 手动打开 D8 开关后路径仍成功打包
- [ ] 生成的 APK byte-for-byte 与 P1a 之前一致（同一工程 + 同一 BuildSettings + 同一快照下 reproducible build 比对）

### 4.5 不在 P1a 范围内（独立后续决策）

以下事项是 P1a 完成后**可以变得容易**、但并不随 P1a 一同交付：

- **切默认 dx → D8**：仅需改一行 `BuildSettings.SETTING_DEXER_DX` → `SETTING_DEXER_D8`；P1a 后由于 D8/dx 分支已集中在 `DexStage`，回归面小、可单测覆盖。切不切以及何时切，由用户反馈 + 产品节奏决定。
- **下线 dx 路径**：删 `DexStage` 中 dx 分支 + 删 `:vendor-dx` 子模块（P1b 产物）。必须在默认已切 D8 且运行多个版本无回归后才考虑。
- **为 `DexStage` 加单测**：依赖项目添加测试基础设施（当前 `Test source code: 0`）。

这些不作为 P1a 退出条件。P1a 只交付“架构改造 + 行为零变化”。

---

## 5. P1b — `mod.agus.jcoderz.dx` 抽 `:vendor-dx` 子模块

### 5.1 实测耦合面

| 维度 | 数值 | 含义 |
|---|---|---|
| dx 内文件 | 375 个 .java / ~3 MB | 抽出体积 |
| dx 外部 Java caller | **4 个文件** | `mod.agus.jcoderz.multidex.{ClassReferenceListBuilder, MainDexListBuilder, Path}` + `pro.sketchware.core.ProjectBuilder` |
| `app/build.gradle` 中 dx 引用 | **0** | 纯源码依赖，无需改构建配置 |
| 测试代码引用 dx | **0** | 无测试连带 |
| dx 子包数 | 9 个内部子包（command/cf/dex/io/merge/rop/ssa/util/...） | 一并迁出 |

### 5.2 操作步骤（高层）

1. 新建顶层目录 `vendor-dx/`，内含：
   - `vendor-dx/build.gradle`：声明为纯 Java library（无 Android 依赖，因为 dx 只处理 .class → .dex 字节码）
   - `vendor-dx/src/main/java/mod/agus/jcoderz/dx/`：保留**完全相同**的包结构与文件
2. `git mv app/src/main/java/mod/agus/jcoderz/dx vendor-dx/src/main/java/mod/agus/jcoderz/dx`（保留 git 历史）
3. `settings.gradle` 加 `include ':vendor-dx'`
4. `app/build.gradle` 加 `implementation project(':vendor-dx')`
5. 4 个外部 caller 不需要改 import（包名未变），只是依赖关系变成跨模块

### 5.3 收益

- 主模块少 375 文件，**增量编译时间 -15–25%**（粗估）
- 主模块 lint / Qodana 扫描跳过 dx 的 3 MB 历史代码（dx 是历史 fork，与主代码风格不同）
- `:vendor-dx` 可设独立 lint 规则（更宽松）
- 未来若 D8 路径稳定后想完全弃用 dx，只需 `removeAll(:vendor-dx)` 一步

### 5.4 退出条件

- [ ] `app/src/main/java/mod/agus/jcoderz/dx/` 不存在
- [ ] `vendor-dx/src/main/java/mod/agus/jcoderz/dx/` 含 375 文件
- [ ] `settings.gradle` 含 `include ':vendor-dx'`
- [ ] `:app:assembleDebug` 与 `:vendor-dx:assemble` 双双通过
- [ ] 在真机上：装新 APK → 用 dx 路径跑一次工程构建（验证跨模块运行期反射调用 `Main$Arguments` 仍工作）
- [ ] `:app:lint` 报告中 `mod.agus.jcoderz.dx.*` 警告全部消失（已不在主模块扫描范围）

### 5.5 风险点

- **`ProjectBuilder` 的反射调用 `Main$Arguments`**：实测 `ProjectBuilder.java:358` 处 `Main.Arguments.class.getDeclaredMethod("parse", String[].class)`。Gradle 跨模块依赖默认是 `implementation`，运行期类加载器仍能解析，反射不受影响。**风险评级：低**。
- **dx 用到的 Java 8+ 语法在 `:vendor-dx` 模块的 `targetCompatibility`**：当前主模块 `JavaCompile.targetCompatibility = JavaVersion.VERSION_17`，`:vendor-dx` 沿用即可。

---

## 6. P2a — codegen Sink 接口层

### 6.1 当前耦合

- codegen 内的 `LayoutGenerator` / `ManifestGenerator` / `ActivityCodeGenerator` / `GradleFileGenerator` 等直接 `new FileWriter(projectFilePaths.getXxxPath())` 写盘
- 这导致：
  - codegen 与 build（`ProjectFilePaths`）紧耦合，难以单测（也是为什么 `ManifestGenerator` 在 P0a 中被迫归到 build 的根因）
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

### 6.5 收益

- codegen 子包**不再依赖 build**（依赖关系：`build → codegen + sink interfaces`）
- 单测时可换 `MemoryLayoutSink` 等 stub，断言生成内容
- `ManifestGenerator` 的双向耦合被打破——长远可考虑把它从 build 移回 codegen（但本次仍保持 P0a 的归宿，避免再做一次包级移动）

### 6.6 退出条件

- [ ] 4 个 Sink 接口位于 `pro/sketchware/core/codegen/sink/`
- [ ] 4 个 File 实现位于 `pro/sketchware/core/build/sink/`
- [ ] codegen 内部生成器**没有任何**对 `ProjectFilePaths` 的直接 import（仅通过注入的 Sink）
- [ ] 在示例工程上 Run，生成的 `.java`/layout XML/AndroidManifest/build.gradle 与 P2a 之前 byte-for-byte 一致

---

## 7. P2b — 命名策略硬规则文档化

### 7.1 现状盘点

| 顶层包 | 文件量级（粗略） | 角色 |
|---|---|---|
| `com.besome.sketch.editor` | ~135 文件 | 历史编辑器 Activity / View / 控件 |
| `com.besome.sketch.beans` | 29 文件 | 跨包共享 POJO（永久保留） |
| `com.besome.sketch.{adapters,ctrls,design,export,help,lib,projects,tools}` | 共 ~40 文件 | 历史辅助类 |
| `pro.sketchware.*` | 已有 18 子包 + `core/` 9 子包 | 现代代码，新功能落地处 |
| `mod.*` | ~250+ 文件 | 各分支贡献者代码（独立维护） |

### 7.2 硬规则（写进 docs）

新文档：`docs/package-migration-policy.md`，含：

1. **新代码硬规则**：
   - 任何**新增** Activity / Fragment / Manager / Controller / Util 必须放在 `pro.sketchware.*` 下
   - 不允许在 `com.besome.sketch.*` 下创建新 `.java`（注：bug 修复时给现有类加 method 是允许的）
   - `com.besome.sketch.beans.*` 是**例外**：当确实需要新增跨层共享 POJO 时，可放此处。但优先考虑放在 `pro.sketchware.beans`
   - `mod.*` 不接受任何来自主线的新代码（保留给分支贡献者）

2. **迁移完成度跟踪表**（在 policy 文档中维护）：

| `com.besome.sketch.*` 子包 | 迁移目标 | 状态 | 备注 |
|---|---|---|---|
| `editor.LogicEditorActivity` | 拆 Controller，Activity 留原位（P0b） | 进行中 | Manifest 引用此 FQN |
| `editor.DesignActivity` | 同上模式（候选 P0c） | 未启动 | |
| `editor.ViewEditor` / `ViewPane` | 已部分拆分到 `pro.sketchware.editor.view.*` | 部分完成 | 历史 ViewPane 重构 |
| `beans.*` | **不迁移**（永久共享） | N/A | 跨包 POJO |
| `adapters` / `ctrls` / `design` / `export` / `help` / `lib` / `projects` / `tools` | 长期目标：合并到 `pro.sketchware.{对应主题}` | 未启动 | 工作量大，无紧迫性 |

3. **PR review checklist**（policy 文档末尾）：
   - [ ] 新文件不位于 `com.besome.sketch.*`（除 `beans/` 例外）
   - [ ] 新代码不位于 `mod.*`
   - [ ] 新 controller/manager/util 命名遵循 `pro.sketchware.<theme>.<purpose>`

### 7.3 退出条件

- [ ] `docs/package-migration-policy.md` 已提交
- [ ] `docs/README.md` 索引含此条目
- [ ] `RELEASE_NOTES.md` 在最近 beta 段落引用此规则（一句话提示）
- [ ] 主仓 `README.md` 的 "Contributing" 段落（如有）链到 policy 文档

---

## 8. 跨项依赖与排期建议

```
时间线（每段不预设具体日期，仅排序）：

  ┌─────────┐
  │  P1b    │  vendor-dx 抽出（独立，可任何时候做）
  └─────────┘
       │
       ▼
  ┌─────────┐
  │  P2b    │  命名策略文档（独立，可任何时候做）
  └─────────┘
       │
  ┌─────────┐
  │  P0a    │  core 包拆分（plan 已有，等待 Phase 2 执行）
  └─────────┘
       │
       ▼
  ┌─────────┐
  │  P0b    │  LogicEditorActivity 控制器抽出（依赖 P0a 的 core.project / core.codegen 子包稳定）
  └─────────┘
       │
       ▼
  ┌─────────┐
  │  P1a    │  BuildStage 链（依赖 P0a 的 core.build 子包稳定）
  └─────────┘
       │
       ▼
  ┌─────────┐
  │  P2a    │  codegen Sink 接口（依赖 P1a 的 BuildStage 已抽出，方便 Sink 注入）
  └─────────┘
```

**并行机会**：
- P1b 与 P2b 完全独立，可与 P0a 并行
- P0b 与 P1a 在 P0a 之后并行（一个动 LogicEditor，一个动 Build）
- P2a 必须等 P1a 完成（P2a 是 P1a 的逻辑延伸）

---

## 9. 总览：完成后的关键改善指标

| 指标 | 当前 | 全部完成后 | 改善 |
|---|---|---|---|
| `pro.sketchware.core/` 直接 .java 文件 | 124 | 0 | 完全分类 |
| `LogicEditorActivity.java` 大小 | ~147 KB | ~15 KB | -90% |
| `ProjectBuilder.java` 大小 | ~70 KB | ~15 KB | -78% |
| 主模块 Java 文件总数（受 dx 抽出影响） | 当前 | 当前 - 375 | -15–25% 增量编译时间 |
| codegen → build 反向依赖 | 双向耦合 | 单向 | 接口解耦 |
| "新代码该放哪" 决策成本 | 高（多个候选） | 低（policy 文档） | 团队规模化前置条件 |
| god class（>50 KB）数量 | ≥ 4（LogicEditorActivity / ProjectFilePaths / ProjectBuilder / ProjectDataStore） | ≤ 1（ProjectFilePaths 留作 P3 候选） | 显著降低 |

---

## 10. 不在本次 6 项范围内（明示）

为避免范围蔓延，以下事项**明确不在**当前规划内，留给未来周期：

- **`com.besome.sketch.editor.DesignActivity` / `ViewProperty` 拆分**：与 P0b 同模式，建议作为 P0c 单独排期
- **`ProjectFilePaths` 内部拆分**：82 KB 单文件，但拆它需要先把 codegen ↔ build 通过 P2a 完全解耦，否则会重新引入循环
- **`ProjectDataStore` 内部拆分**：70 KB，但它是项目持久化的中心；要拆需要先有 schema 测试基础设施
- **`com.besome.sketch.editor.*` 历史代码完全迁移到 `pro.sketchware.editor.*`**：~135 文件级别的迁移，应等 P0b/P0c 等"按 Activity 单位逐个拆"完成后再考虑批量迁移
- **加单测/集成测套件**：当前 baseline `Test source code: 0`，建议在 P1a 完成后单独立项
- **Kotlin 化某些 controller / stage**：考虑过在 P0b 中直接用 Kotlin 写新 controller，但为了保持 P0b 风险面纯粹（仅"挪逻辑"，不"换语言"），定为后续 P3+ 备选
- **`com.besome.sketch.beans.*` 迁出**：永久不迁，作为跨包共享层，由 P2b policy 文档明示

---

## 附录 A — 引用文档

- 起手包（约束 + 验证基线）：`docs/package-refactor-kickoff.md`
- P0a 详细批次：`docs/package-refactor-plan.md`
- 上游架构记录：`docs/DEVELOPMENT.md` / `docs/code-generation-system-analysis.md`
- 既往审计：`docs/android-development-tool-audit-review.md`
- 命名映射（去混淆历史）：`docs/refactoring-naming-map.md`
