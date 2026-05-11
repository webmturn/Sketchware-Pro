# P2a — codegen → sink 接口化设计方案

> **状态**：draft（设计文档，尚未执行）。等待用户 review 后进入实现批次。
>
> **输入凭据**：
> - 范围归属：`docs/package-migration-policy.md` § 4.2 "Still pending" 中的 P2a 行
> - 父级架构：`docs/package-target-architecture.md`
> - 命名规则：`docs/package-naming-policy.md`
>
> **本文档只输出方案**，不包含任何代码改动。所有 batch 由后续执行 session 按 § 5 的清单逐批落地。

---

## 1. 调研结论 — P2a 的实际形状与文档描述的偏差

policy § 4.2 把 P2a 描述为："`pro.sketchware.core.codegen.*` direct file writes → `core.codegen.sink.*` interfaces + `core.build.sink.*` impls"。本次调研把这条假设拆开来对照现状：

### 1.1 codegen 包内的直接 IO（grep `FileUtil.write|EncryptedFileUtil.write|new FileWriter/FileOutputStream/PrintWriter/BufferedWriter|Files.write`）

只命中 3 处真实写盘点，全部是边界文件：

| 文件 | 行 | 写什么 |
|---|---|---|
| `pro.sketchware.core.codegen.LogicHandler` | 104 | 写系统 temp Java 文件（调试辅助路径） |
| `pro.sketchware.core.codegen.CommandBlock` | 193 / 198 | 写/删 `TEMP_COMMANDS_PATH`（命令块进程通信） |
| `pro.sketchware.core.codegen.AndroidManifestInjector` | 182 | 写 launcher activity 配置文件 |

其余命中（`ComponentTemplates`、`BlockCodeRegistry`）都是**字符串模板里**的 `FileWriter/FileOutputStream`，是生成给目标 APK 的代码，不是 codegen 自己的 IO。

**结论**：codegen 包基本不做实际写盘，policy 的 "direct file writes" 描述过度估计了 codegen 的耦合度。

### 1.2 codegen 主入口签名

| 类 | 关键方法 | 返回 |
|---|---|---|
| `ActivityCodeGenerator` | `generateCode(boolean isAndroidStudioExport, String sc_id, boolean applyFormatting)` | `String` |
| `ActivityCodeGenerator` | `applyCommands(String formattedCode)` | `String` |
| `LayoutGenerator` | `toXmlString()` | `String` |
| `ManifestGenerator` | `generateManifest()` | `String`（位于 `core.build.ManifestGenerator`） |
| `ComponentCodeGenerator` | `getBuildGradleString(...)`, `getSettingsGradle()`, `getTopLevelBuildGradle(...)`, `formatCode(...)` | `String` |
| `GradleFileGenerator` | `getSettingsGradle()`, `getBuildGradleString(...)`, `getTopLevelBuildGradle(...)` | `String`（全部 `static`） |

**结论**：codegen 主路径已经是纯函数风格（输入 bean/setting，输出 String）。这是 P2a 的福报，意味着不需要重写 codegen 自身。

### 1.3 真正的写盘集中点 — `core.build.ProjectFilePaths`（25 处 IO）

| 方法 | 写盘动作 |
|---|---|
| `generateGradleFiles()` | `app/build.gradle` + `settings.gradle` + 顶层 `build.gradle` + `gradle.properties`（4 个文件） |
| `copyAppIcon(iconPath)` | 拷贝到 `mipmap-xhdpi/ic_launcher.png` |
| `copyMipmapFolder(iconPath)` | 整目录 copy 到 `res/` |
| `createLauncherIconXml(content)` | 写 `mipmap-anydpi-v26/ic_launcher.xml` |
| `generateDebugFiles(Context)` | 写 `DebugActivity.java` / 自定义 Application 类 / `SketchLogger.java` |
| `writeProjectFile(fileName, fileContent)` | **switch-by-suffix 路由表**：`.java` → `java/<pkg>/`、`AndroidManifest.xml` → `androidManifestPath`、`{colors,styles,strings}.xml` → `res/values/`、`provider_paths.xml` → `res/xml/`、其余 → `layout/` |
| `generateProjectFiles(...)` / `generateSourceCodeBeans(...)` | 每个 Activity 生成 → `FileUtil.writeFile(codegenCacheDir/<javaName>.code)`（codegen 缓存） |
| `generateProjectFiles(...)` | `res/values/secrets.xml`（行 821） |
| `generateProjectFiles(...)` | 触发 ViewBinding：先把布局 xml 写到 `context.getCacheDir()`（行 1021），再调 `ViewBindingBuilder.generateBindingForLayout(...)` |

### 1.4 ProjectBuilder 内的 6 处 IO

属于**构建过程文件**，非 codegen 输出：
- `copyAssetFile(...)`：资源解压
- `PrintWriter` 包 ECJ stdout/stderr 流
- DEX merge fingerprint `Files.write(fingerprintFile, ...)`
- `proguardAddRjavaRules`：写 ProGuard rule 文件
- `ZipAlign` 用 `FileOutputStream` 写对齐后的 zip

**结论**：这些不属于 P2a，应放到 **P1a (build stage chain)** 的范围。

### 1.5 ExportType 与 sink 选择的天然映射

`ProjectFilePaths.ExportType` 已经枚举了 5 种导出模式：

```java
public enum ExportType {
    AAB, SIGN_APP, DEBUG_APP, ANDROID_STUDIO, SOURCE_CODE_VIEWING
}
```

当前已经用 `isAndroidStudioExport = (exportingType == ANDROID_STUDIO)` 派生出一个 boolean，分散在 4 处条件分支里影响 codegen 行为（imports、manifest merge、cache key、source viewing 是否复用磁盘文件）。这正是 sink 抽象要解决的"多目标输出"问题。

---

## 2. 重新定义 P2a 范围

基于调研，把 P2a 的边界精确化：

### 2.1 在范围内

- 把 `ProjectFilePaths` 中"生成内容 → 写盘到 `.sketchware/mysc/<sc_id>/...`"的 26 个写盘点抽象成 `ProjectArtifactSink` 接口。
- 提供两个具体实现：
  - `BuildDirSink` — 复刻当前行为，写到 `.sketchware/mysc/<sc_id>/`
  - `AndroidStudioExportSink` — 写到 Android Studio 工程目录结构（gradle 项目布局）
- 把 `isAndroidStudioExport` boolean 和 `getXMLString/getXMLColor/getXMLStyle` 里散布的 `exportingType == SOURCE_CODE_VIEWING` 条件分支收敛到 sink 选择上。
- 保留 `writeProjectFile(String fileName, String fileContent)` 作为兼容 facade（内部委托 sink），不强行删除外部调用方。

### 2.2 不在范围内（明确排除）

- **codegen 本身**：`ActivityCodeGenerator` / `LayoutGenerator` / `ManifestGenerator` / `ComponentCodeGenerator` 不动签名、不改实现。
- **`ProjectBuilder` 的 6 处过程 IO**：DEX fingerprint、ECJ writer、ProGuard rules、ZipAlign —— 归 P1a。
- **`LogicHandler` / `CommandBlock` / `AndroidManifestInjector` 的 temp 文件 IO**：这是命令块进程通信的副作用，不是项目产物。留作单独 follow-up。
- **`ProjectFilePaths.initializeMetadata/createBuildDirectories/prepareBuildDirectories`**：目录创建，不涉及内容生成。

### 2.3 与现有 docs/package-migration-policy.md § 4.2 P2a 行的关系

policy 行原文：

> `pro.sketchware.core.codegen.*` direct file writes | `pro.sketchware.core.codegen.sink.*` interfaces + `pro.sketchware.core.build.sink.*` impls

需要在 § 4.2 把 "Tracking item" 列指向本文档（`p2a-codegen-sink-plan.md`），并把 Legacy 列从 "codegen.* direct file writes" 修正为 "ProjectFilePaths writeProjectFile / generateGradleFiles / createLauncherIconXml / generateDebugFiles" 等具体方法。

---

## 3. 目标架构

### 3.1 接口（位于 `pro.sketchware.core.codegen.sink`）

```java
package pro.sketchware.core.codegen.sink;

/**
 * One write of a generated project artifact.
 *
 * <p>codegen 输出 (relativePath, content) 元组，sink 决定最终落到哪个文件系统位置、
 * 以哪种编码、是否加密、是否合并到 zip。
 */
public interface ProjectArtifactSink {

    /** 写文本类产物（.java / .xml / .gradle / .properties） */
    void writeText(ArtifactKind kind, String relativePath, String content) throws IOException;

    /** 从源文件 copy（icon / mipmap 等已存在于磁盘的二进制） */
    void copyFrom(ArtifactKind kind, String sourcePath, String relativePath) throws IOException;

    /** 整目录 copy（mipmap 文件夹） */
    void copyDirectoryFrom(ArtifactKind kind, String sourceDir, String relativeTargetDir) throws IOException;

    /** 删除指定相对路径（清空旧产物） */
    void delete(String relativePath);

    /** sink 标识，用于诊断/日志 */
    String describe();
}
```

`ArtifactKind` 是一个枚举，让 sink 决定路径前缀与编码策略：

```java
public enum ArtifactKind {
    JAVA_SOURCE,        // pro.sketchware/.../<pkg>/<Foo>.java
    LAYOUT_XML,         // res/layout/<foo>.xml
    VALUES_XML,         // res/values/<foo>.xml (colors/strings/styles/secrets)
    DRAWABLE_XML,       // res/drawable-*/...xml
    MIPMAP_XML,         // res/mipmap-anydpi-v26/ic_launcher.xml
    MIPMAP_BITMAP,      // res/mipmap-*/ic_launcher.png
    XML_RESOURCE,       // res/xml/provider_paths.xml
    ANDROID_MANIFEST,   // AndroidManifest.xml
    GRADLE_SCRIPT,      // build.gradle / settings.gradle / gradle.properties
    CODEGEN_CACHE       // bin/.codegen_cache/<JavaName>.code (sink 可以 NO-OP)
}
```

### 3.2 实现类（位于 `pro.sketchware.core.build.sink`）

```
pro.sketchware.core.build.sink/
├── BuildDirSink.java              // 当前行为：写到 .sketchware/mysc/<sc_id>/
├── AndroidStudioExportSink.java   // 新增：写到 AS 工程结构（app/src/main/...）
├── SourceViewingSink.java         // 派生 BuildDirSink，遇到 VALUES_XML 优先复用磁盘
└── InMemorySink.java              // 测试 / dry-run（仅记录调用，不落盘）
```

每个 sink 的差异点：

| ArtifactKind | BuildDirSink | AndroidStudioExportSink | SourceViewingSink |
|---|---|---|---|
| JAVA_SOURCE | `mysc/<sc_id>/app/src/main/java/<pkg>/...` | `<exportRoot>/app/src/main/java/<pkg>/...` | 同 BuildDirSink |
| LAYOUT_XML | `mysc/<sc_id>/app/src/main/res/layout/...` | `<exportRoot>/app/src/main/res/layout/...` | 同 BuildDirSink |
| ANDROID_MANIFEST | `mysc/<sc_id>/app/src/main/AndroidManifest.xml`，merge local libs | 同左路径但跳过 local libs merge（AGP 自己 merge） | 同 BuildDirSink，不 merge |
| GRADLE_SCRIPT | 写 4 个 gradle 文件 | 同左 | NO-OP（不重新生成） |
| CODEGEN_CACHE | 写 `bin/.codegen_cache/...` | NO-OP | NO-OP |
| VALUES_XML | 总是覆盖 | 总是覆盖 | 若磁盘已有则跳过（保留用户/源码查看版本） |

ExportType → Sink 选择策略集中到一个工厂方法：

```java
static ProjectArtifactSink forExport(ExportType type, ProjectFilePaths paths, String exportRoot) {
    return switch (type) {
        case DEBUG_APP, SIGN_APP, AAB -> new BuildDirSink(paths);
        case ANDROID_STUDIO            -> new AndroidStudioExportSink(paths, exportRoot);
        case SOURCE_CODE_VIEWING       -> new SourceViewingSink(paths);
    };
}
```

### 3.3 改造后的 `ProjectFilePaths` 入口

```java
public class ProjectFilePaths {
    private ProjectArtifactSink sink;   // 由 initializeMetadata(ExportType) 注入

    public void writeProjectFile(String fileName, String fileContent) {
        ArtifactKind kind = classifyByName(fileName);
        sink.writeText(kind, relativizeByKind(kind, fileName), fileContent);
    }

    public void generateGradleFiles() {
        sink.writeText(GRADLE_SCRIPT, "app/build.gradle",     ComponentCodeGenerator.getBuildGradleString(...));
        sink.writeText(GRADLE_SCRIPT, "settings.gradle",      ComponentCodeGenerator.getSettingsGradle());
        sink.writeText(GRADLE_SCRIPT, "build.gradle",         ComponentCodeGenerator.getTopLevelBuildGradle(...));
        sink.writeText(GRADLE_SCRIPT, "gradle.properties",    DEFAULT_GRADLE_PROPERTIES);
    }

    public void copyAppIcon(String iconPath)             { sink.copyFrom(MIPMAP_BITMAP, iconPath, "res/mipmap-xhdpi/ic_launcher.png"); }
    public void copyMipmapFolder(String iconPath)        { sink.copyDirectoryFrom(MIPMAP_BITMAP, iconPath, "res/"); }
    public void createLauncherIconXml(String content)    { sink.writeText(MIPMAP_XML, "res/mipmap-anydpi-v26/ic_launcher.xml", content); }
}
```

`writeProjectFile` 的 switch-by-suffix 路由表被分类函数 `classifyByName(fileName)` + 相对路径函数 `relativizeByKind(kind, fileName)` 取代。`fileUtil` 字段不再直接被 ProjectFilePaths 调用，但保留给 sink 用。

---

## 4. 依赖与约束

### 4.1 依赖方向

```
codegen.sink (interface)
    ↑              ↑
    │              │
ProjectFilePaths   build.sink.{BuildDir,AndroidStudioExport,SourceViewing,InMemory}
                          ↓
                   util / project / async
```

- `codegen.sink` 只能依赖 JDK 和 `core.exception`；不准引 `core.build`、`core.project`、`util`。
- `build.sink.*` 实现可以引 `EncryptedFileUtil`、`FileUtil`、`ProjectFilePaths`（用其路径常量）、Android Context。
- `ProjectFilePaths` 持有 sink 引用，但不知道具体实现类型。

### 4.2 与 codegen 现有 boolean 参数的关系

- `ActivityCodeGenerator.generateCode(boolean isAndroidStudioExport, ...)` 暂时保留这个 boolean（避免一次性改动太大）。
- 在 P2a 完成后的 follow-up 中可以把它换成 `ArtifactKind` 上下文或 `ExportType` 注入，但**不在本 plan 范围**。
- 同理 `ProjectFilePaths.finalizeGeneratedManifest()` 里的 `if (!isAndroidStudioExport)` 暂时不动，等 sink 接管 ANDROID_MANIFEST 写入后再下放到 `AndroidStudioExportSink` 内部。

---

## 5. 拆分批次（共 5 批）

每批独立编译通过 + `git diff --check` 干净 + 手动验证一次 build。

### 第 1 批：引入接口与默认实现（叶子，零行为变化）

新增文件：
- `pro/sketchware/core/codegen/sink/ProjectArtifactSink.java`
- `pro/sketchware/core/codegen/sink/ArtifactKind.java`
- `pro/sketchware/core/build/sink/BuildDirSink.java`（包装当前 `EncryptedFileUtil.writeText` / `FileUtil.copyFile` / `FileUtil.copyDirectory` 调用）
- `pro/sketchware/core/build/sink/InMemorySink.java`

不改 `ProjectFilePaths`。验收：编译通过；新类有 unit-test 级别的 simple smoke（如可选）。

### 第 2 批：路由表与文件名分类

新增私有工具：
- `ProjectFilePaths.classifyByName(String fileName) → ArtifactKind`
- `ProjectFilePaths.relativizeByKind(ArtifactKind kind, String fileName) → String`

不改写盘行为，但用日志在 `writeProjectFile` 入口打印分类结果，验证 100% 覆盖现状 5 个分支。验收：build 一个示例项目，日志显示所有写盘都被正确分类。

### 第 3 批：`ProjectFilePaths` 切换到 `BuildDirSink`（行为等价）

- 给 `ProjectFilePaths` 加 `private ProjectArtifactSink sink` 字段。
- `initializeMetadata(ExportType)` 末尾根据 type 创建 sink，目前只支持 `DEBUG_APP/SIGN_APP/AAB/SOURCE_CODE_VIEWING` 全部用 `BuildDirSink`（暂时 `ANDROID_STUDIO` 也用 BuildDirSink，第 4 批再换）。
- `writeProjectFile` / `generateGradleFiles` / `createLauncherIconXml` / `copyAppIcon` / `copyMipmapFolder` / `generateDebugFiles` / secrets.xml 写入 / codegen cache 写入 改为通过 sink。
- `getXMLString/getXMLColor/getXMLStyle` 暂不动（它们是 read，不是 write）。

验收：debug build / sign build 完全等价（产物 byte-for-byte 一致或 timestamp-only 差异）。

### 第 4 批：`AndroidStudioExportSink` 与 `SourceViewingSink`

- 实现 `AndroidStudioExportSink`，接收 `exportRoot` 路径，输出 AS 工程布局。
- 实现 `SourceViewingSink`，对 `VALUES_XML` 优先复用磁盘文件（取代当前 `getXMLString/getXMLColor/getXMLStyle` 里的 `if (FileUtil.isExistFile(...) && exportingType == SOURCE_CODE_VIEWING)` 分支）。
- 工厂方法 `forExport(...)` 落地，`initializeMetadata` 用它选择 sink。
- `ProjectFilePaths.finalizeGeneratedManifest` 的 `if (!isAndroidStudioExport)` local-library-merge 分支下沉到 `BuildDirSink.writeText(ANDROID_MANIFEST, ...)` 内部，`AndroidStudioExportSink` 不做 merge。

验收：导出 Android Studio 工程 + 源码查看 + debug 三条路径都和重构前等价。

### 第 5 批：清理与文档

- 删除 `isAndroidStudioExport` boolean 字段（迁移到通过 sink 类型查询）。**仅限 ProjectFilePaths 内部**；`ActivityCodeGenerator.generateCode(boolean isAndroidStudioExport, ...)` 的参数留作 follow-up。
- 更新 `docs/package-migration-policy.md` § 4.1 把 P2a 移到 Completed 表，列引用 commit。
- 更新 `docs/package-target-architecture.md` 的 P2a 段落，标记完成。
- 删除本文档头部的 "draft" 标记，改为 "✅ Completed"。

---

## 6. 风险点 / 回归检查

| 风险 | 缓解 |
|---|---|
| `writeProjectFile` 的 5 个 suffix 分支有遗漏（如 `provider_paths.xml` 大小写） | 第 2 批先用日志验证 100% 命中再切换；保留 fallback 分支 `OTHER → layout/` 行为 |
| sink 切换后 `EncryptedFileUtil` 的加密行为意外改变 | `BuildDirSink` 直接复用 `ProjectFilePaths.fileUtil` 字段，不重新构造 EncryptedFileUtil |
| `generateSourceCodeBeans` 里的并行写 codegen cache 在多线程下经 sink 是否仍线程安全 | sink 接口契约写明"实现必须线程安全或文档警告 not thread-safe"；`BuildDirSink` 的 codegen cache 写直接复用现有 `FileUtil.writeFile`（已被现状证明线程安全） |
| `finalizeGeneratedManifest` 的 local library merge 下沉到 sink 后路径处理出错 | 第 4 批仅下沉这一处分支；保留单元化日志，能 1:1 对比重构前的 final manifest |
| ANDROID_STUDIO 导出工程 `gradle.properties` 内容可能需要与 Sketchware 内置 build 不同（如不需要 `android.useAndroidX=true` 这种 build-time only 设置） | 第 4 批的 `AndroidStudioExportSink` 提供 override 钩子；产出 diff 由人工 review |

---

## 7. 退出条件

- [ ] `pro.sketchware.core.codegen.sink.ProjectArtifactSink` 与 `ArtifactKind` 存在并被使用
- [ ] `pro.sketchware.core.build.sink/` 下至少 3 个实现（BuildDir / AndroidStudioExport / SourceViewing）
- [ ] `ProjectFilePaths` 中没有任何 `fileUtil.writeText(...)` 或 `FileUtil.writeFile(...)` 直接调用（除 sink 内部转发）
- [ ] `isAndroidStudioExport` 字段在 `ProjectFilePaths` 中不再被业务分支读取（剩余仅 cache key 拼接和 ActivityCodeGenerator 参数透传可保留）
- [ ] 3 条 build 路径（DEBUG / SIGN / ANDROID_STUDIO 导出）+ 源码查看 在实机上均工作如旧
- [ ] `docs/package-migration-policy.md` § 4 表已更新
- [ ] `:app:assembleDebug` BUILD SUCCESSFUL，`git diff --check` 干净

---

## 8. 不在范围内 / 后续 follow-up

- **codegen 包的 temp file IO** (`LogicHandler` / `CommandBlock` / `AndroidManifestInjector`)：独立 follow-up，预计 P3 级。
- **`ActivityCodeGenerator.generateCode(boolean isAndroidStudioExport, ...)` 参数清理**：等 P2a 完成后单独一个 micro-commit。
- **ProjectBuilder 的 6 处过程 IO**：P1a 范围。
- **多模块拆分**（`:core` / `:codegen` / `:builder` / `:vendor` Gradle modules）：与 P2a 无关，是独立的架构议题。

---

## 9. 总结

P2a 的实际工作量被 policy 文档高估了：codegen 主路径已经是纯函数，真正需要抽象的写盘耦合点都集中在 `ProjectFilePaths` 一个类的约 26 行 IO 上。本方案提议一个轻量的 `ProjectArtifactSink + ArtifactKind` 接口，加 3 个具体 sink 实现，分 5 个批次落地，每批独立可回滚。完成后获得的能力：

1. **导出 Android Studio 工程**这一长期 deferred 的特性有了清晰的扩展点（只需替换 sink）。
2. `isAndroidStudioExport` / `exportingType` 散布在 5 处的条件分支收敛到 1 处 sink 选择。
3. 后续若想加 "导出到 zip"、"导出到 LAN web preview"、"导出到 Gradle Composite Build" 等任何新出口，只需新增一个 sink 实现，无需触碰任何 codegen 或 builder 代码。