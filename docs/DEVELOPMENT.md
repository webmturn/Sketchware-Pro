# Sketchware Pro 开发手册

本手册基于仓库实际代码编写,用于帮助开发者理解项目架构、核心子系统与扩展方式。文中出现的所有类名、方法签名与文件路径均与代码库保持一致。

> 版本信息(取自 `app/build.gradle`):`applicationId = pro.sketchware`,`versionCode = 160`,`versionName = v7.0.0-beta9`。

---

## 1. 项目概述

Sketchware Pro 是一个运行在 Android 设备上的可视化 Android 应用构建器。用户通过拖拽视图、配置组件、编排逻辑积木(block)来搭建应用,项目最终由内置的编译管线直接在设备上打包成可安装的 APK。

核心能力可以拆成两条主线:

- **可视化编辑**:视图设计器(`DesignActivity`)与逻辑编辑器(`LogicEditorActivity`)负责把用户操作转化成结构化的项目数据(beans)。
- **代码生成与构建**:代码生成子系统(`pro.sketchware.core.codegen`)把项目数据翻译成 Java/XML 源码,构建子系统(`pro.sketchware.core.build`)再把源码编译、dex 化、打包并签名成 APK。

---

## 2. 环境搭建

### 2.1 构建工具链

| 组件 | 版本 | 来源 |
| --- | --- | --- |
| Gradle | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.12.0 | `build.gradle`(根) |
| Kotlin Gradle Plugin | 2.1.21 | `build.gradle`(根) |
| Java | 17 | `app/build.gradle` `compileOptions` |
| compileSdk | 36 | `app/build.gradle` |
| minSdk | 26 | `app/build.gradle` |
| targetSdk | 28 | `app/build.gradle`(项目不上架 Google Play,故保留旧 targetSdk) |

### 2.2 拉取与构建

```bash
git clone <repo-url>
cd Sketchware-Pro
./gradlew assembleDebug
```

> Windows 说明:项目使用自定义 `GRADLE_USER_HOME`(仓库内 `.gradle-user-home/`,已在 `.gitignore` 中忽略)。在本机命令行中若需要串联多条命令,PowerShell 使用 `;` 分隔,cmd 使用 `&`。

### 2.3 签名配置

`app/build.gradle` 中的 `signingConfigs.debug` 指向仓库根目录的 `testkey.keystore`:

```
storeFile     = ../testkey.keystore
storePassword = testkey
keyAlias      = testkey
keyPassword   = testkey
```

`release` 构建类型当前也复用了该 debug 签名(`signingConfig = signingConfigs.debug`),且 `minifyEnabled = false`。发布正式版本时应替换为独立的 release keystore。

### 2.4 可选环境变量

`SKETCHUB_API_KEY` 通过 `buildConfigField` 注入(`System.getenv("SKETCHUB_API_KEY")`),用于 SketchHub 相关联网功能。未设置时相关功能不可用,但不影响本地构建。

---

## 3. 整体架构

### 3.1 Gradle 模块

项目在 `settings.gradle` 中声明了三个模块:

```
include(":app")
include(":resolver")
include(":vendor-dx")
```

| 模块 | 命名空间 | 职责 |
| --- | --- | --- |
| `:app` | `pro.sketchware` | 主体应用:编辑器 UI、代码生成、构建管线、项目管理 |
| `:resolver` | `org.cosmic.ide.dependency.resolver` | Maven 依赖解析(基于 OkHttp + Jackson + Kotlin 协程) |
| `:vendor-dx` | `mod.agus.jcoderz.dx` | dex 工具链(DX)厂商代码 |

`:app` 通过 `implementation project(':resolver')` 与 `implementation project(':vendor-dx')` 依赖另外两个模块。

`:resolver` 的依赖(见 `resolver/build.gradle.kts`):

```
kotlinx-coroutines-android:1.10.2
okhttp:4.12.0
jackson-dataformat-xml:2.19.0
jackson-module-kotlin:2.19.0
```

`:resolver` 源码在 `resolver/src/main/kotlin/org/cosmic/ide/dependency/resolver/`(源自 Cosmic IDE,GPL v3),对外暴露的核心 API:

- 顶层声明(`utils.kt`):
  - `repositories`:`ConcurrentLinkedQueue<Repository>`,默认预置 `MavenCentral()`、`GoogleMaven()`、`Jitpack()`、`SonatypeSnapshots()`(见 `repository/` 下四个实现)。
  - `okHttpClient`:统一的 OkHttp 客户端(connect 5s、read/write 15s、call 30s 超时)。
  - `getArtifact(groupId, artifactId, version): Artifact?`:定位构件所在仓库并读取 POM。
  - `clearSessionCaches()`:清理跨解析会话不可复用的缓存(如 BOM 展开标记)。
- `api/` 包:`Artifact.kt`、`Repository.kt`、`ProjectObjectModel.kt`(POM 模型)、`MavenMetadata.kt`、`EventReciever.kt`(解析事件回调基类)。
- 传递依赖解析支持 BOM 导入(`scope=import`)、父 POM 的 `<dependencyManagement>` 继承、版本区间/属性占位符(`${...}`)与排除(`<exclusions>`),解析并发度由 `Semaphore(8)` 控制。

### 3.2 应用入口

全局 `Application` 子类为 `pro.sketchware.SketchApplication`(`app/src/main/java/pro/sketchware/SketchApplication.java`)。据其类注释,主要职责为:

- 通过 `getContext()` 提供 locale-aware 的静态应用上下文;
- 通过 lifecycle 回调跟踪当前前台 `Activity`;
- 安装全局未捕获异常处理器,崩溃时拉起 `CollectErrorActivity`(`pro.sketchware.activities.tools.CollectErrorActivity`);
- 启动时初始化 `LanguageOverrideManager` 与 `ThemeManager`。

### 3.3 `:app` 的核心包结构

主要源码位于 `app/src/main/java/pro/sketchware/`:

```
beans/                     结构化项目数据模型(bean)
core/
  codegen/                 代码生成子系统(block/event/component/activity/layout)
  build/                   构建管线(资源编译、Java 编译、dex、打包、签名)
    compiler/  dependency/  multidex/
  project/                 项目数据的加载、序列化、持久化与门面
  block/  callback/  async/  resources/  ui/  validation/  fragments/  exception/
activities/
  design/                  DesignActivity(视图设计器)
  editor/                  LogicEditorActivity(逻辑编辑器)
  main/activities/         MainActivity(项目列表)
  settings/  tools/  ...
SketchApplication.java     Application 入口
```

> 包命名策略:项目已完成从历史包名(`com.besome.sketch.*`、`mod.*`)向 `pro.sketchware.*` 的整体迁移。新增代码一律放在 `pro.sketchware.*` 下。

---

## 4. 核心子系统

### 4.1 数据模型(beans)

结构化项目数据全部定义在 `app/src/main/java/pro/sketchware/beans/`。这些 bean 是编辑器、代码生成器与持久化之间流转的通用数据结构。常用 bean 包括:

| Bean | 说明 |
| --- | --- |
| `ProjectBean` | 项目元信息 |
| `ProjectFileBean` | 单个文件(Activity/自定义视图等)信息,含 `getJavaName()` 等 |
| `ViewBean` / `ViewBeans` | 视图节点及其集合 |
| `LayoutBean` | 视图布局属性 |
| `ComponentBean` | 组件(如 Intent、SharedPreferences、Firebase 等) |
| `EventBean` | 事件(如按钮 onClick) |
| `BlockBean` | 单个逻辑积木,含 `opCode`、`spec`、`parameters`、`subStack1`、`subStack2` 等字段 |
| `ProjectLibraryBean` | 内置库(AdMob、Firebase、Google Map 等)配置 |
| `ProjectResourceBean` | 资源(图片、字体、声音)条目 |

`BlockBean` 是代码生成链路中最关键的数据单元:`opCode` 决定该积木由哪个处理器生成代码,`spec` 描述其参数插槽,`subStack1`/`subStack2` 指向嵌套子积木(用于 if/循环等控制流)。

### 4.2 代码生成链路(codegen)

代码生成子系统位于 `app/src/main/java/pro/sketchware/core/codegen/`。它把项目数据(beans)翻译成完整的 Java 源码。

#### 4.2.1 积木解释器 `BlockInterpreter`

`BlockInterpreter`(`BlockInterpreter.java`)负责把一串 `BlockBean` 翻译成 Java 代码。关键方法:

```java
public BlockInterpreter(String activityName, BuildConfig buildConfig,
                        ArrayList<BlockBean> eventBlocks, boolean isViewBindingEnabled)
public String interpretBlocks()
public final String generateBlock(BlockBean bean, String parentOpcode)
private String getBlockCode(BlockBean bean, ArrayList<String> params)
private String getCodeExtraBlock(BlockBean blockBean, ArrayList<String> resolvedParams)
```

调用链为:`interpretBlocks()` → 对每个积木调用 `generateBlock()` → `getBlockCode()`。`getBlockCode()` 的分派逻辑如下(与源码一致):

```java
BlockCodeHandler handler = BlockCodeRegistry.get(bean.opCode);
if (handler != null) {
    opcode = handler.generate(bean, params, this);
} else {
    opcode = getCodeExtraBlock(bean, params);
}
```

也就是说:**先查内置注册表 `BlockCodeRegistry`,命中则用处理器生成;未命中则回退到 `getCodeExtraBlock()`**,由后者通过 `BlockLoader.getBlockInfo(opCode)` 取出积木定义并用 `String.format` 套模板生成代码(自定义/项目级积木走这条路径)。

#### 4.2.2 积木注册表 `BlockCodeRegistry`

`BlockCodeRegistry`(`BlockCodeRegistry.java`)是内置积木 opcode 到代码生成器的映射表。核心 API:

```java
public static void register(String opcode, BlockCodeHandler handler)
public static BlockCodeHandler get(String opcode)
```

`BlockCodeHandler`(`BlockCodeHandler.java`)是函数式接口:

```java
@FunctionalInterface
public interface BlockCodeHandler {
    String generate(BlockBean bean, ArrayList<String> params, BlockInterpreter context);
}
```

注册按功能域分组进行,由一系列 `private static void registerXxxBlocks()` 方法完成,例如:`registerCoreBlocks()`、`registerOperatorBlocks()`、`registerStringBlocks()`、`registerCollectionBlocks()`、`registerControlFlowBlocks()`、`registerViewBlocks()`、`registerIntentBlocks()`、`registerFirebaseBlocks()`、`registerWebViewBlocks()`、`registerSQLiteBlocks()`,直至 `registerMiscBlocks()` 等(共约 40 个分组方法)。

#### 4.2.3 事件注册表 `EventCodeRegistry`

`EventCodeRegistry`(`EventCodeRegistry.java`)负责生成事件处理方法。核心 API:

```java
public static void register(String eventName, EventCodeHandler handler)
public static EventCodeHandler get(String eventName)
public static String generate(String targetId, String eventName, String eventLogic)
```

`generate()` 的分派逻辑(与源码一致):命中注册的处理器则调用之,否则回退到 `ManageEvent.getExtraEventCode(targetId, eventName, eventLogic)`:

```java
EventCodeHandler handler = handlers.get(eventName);
if (handler != null) {
    return handler.generate(targetId, eventLogic);
}
return ManageEvent.getExtraEventCode(targetId, eventName, eventLogic);
```

`EventCodeHandler`(`EventCodeHandler.java`)同样是函数式接口:

```java
@FunctionalInterface
public interface EventCodeHandler {
    String generate(String targetId, String eventLogic);
}
```

事件注册也按域分组:`registerLifecycleEvents()`、`registerViewEvents()`、`registerTextEvents()`、`registerListEvents()`、`registerFirebaseEvents()`、`registerBluetoothEvents()` 等。以生命周期事件为例,注册形如:

```java
register("onStart", (id, logic) ->
    "@Override\r\npublic void onStart() {\r\nsuper.onStart();\r\n" + logic + "\r\n}");
```

#### 4.2.4 内置积木定义 `BuiltInBlockDefinitions` / `ExtraBlockDefinitions`

内置积木的**元数据**(名称、类型、返回类型名、代码模板、颜色、spec)是数据驱动的:

- `BuiltInBlockDefinitions.builtInBlocks(ArrayList<HashMap<String, Object>>)` 先调用 `ExtraBlockDefinitions.extraBlocks(...)`,再追加 `CommandBlockJava`、`CommandBlockXML`、`setRecyclerViewLayoutParams` 等特殊积木。
- `ExtraBlockDefinitions.extraBlocks(...)` 通过辅助方法 `addBlock(name, type, typeName, code, color, spec)` 批量登记积木。例如:

```java
arrayList.add(addBlock("strParseInteger", "d", "", "Integer.parseInt(%s)", "#5cb722", "parse int %s"));
arrayList.add(addBlock("isEmpty", "b", "", "%s.isEmpty()", "#e1a92a", "%s isEmpty"));
```

其中 `type` 为返回类别(如 `"b"` 布尔、`"d"` 数字、`"s"` 字符串、`" "` 无返回语句、`"c"` 复合),`code` 为带 `%s`/`%d` 占位符的 Java 模板,`spec` 为积木在编辑器中的可视化拼写。

> 二者区别:`BlockCodeRegistry` 面向**需要自定义生成逻辑**的积木(用 Java lambda 生成);`ExtraBlockDefinitions`/`BuiltInBlockDefinitions` 面向**可用固定模板表达**的积木(用 `String.format` 套模板)。

#### 4.2.5 组件与整体装配

- `ComponentCodeGenerator`(`ComponentCodeGenerator.java`):生成组件字段声明、初始化代码、适配器(RecyclerView/Pager)代码,以及 `build.gradle`、`settings.gradle` 等文本。其静态方法 `getEventCode(String targetId, String eventName, String eventLogic)` 委托给 `EventCodeRegistry.generate(...)`。
- `ComponentTypeMapper`:组件类型与 import 映射。
- `ComponentTemplates`:组件模板。
- `LayoutGenerator`:生成 XML 布局。
- `ActivityCodeGenerator`(`ActivityCodeGenerator.java`):最终装配器,`generateCode(boolean isAndroidStudioExport, String sc_id)` 把变量声明、组件初始化、事件方法、逻辑积木代码等组合成一个完整的 Java 类。构造函数签名:

```java
public ActivityCodeGenerator(BuildConfig jqVar, ProjectFileBean projectFileBean, ProjectDataStore eCVar)
```

### 4.3 构建管线(build)

构建子系统位于 `app/src/main/java/pro/sketchware/core/build/`,核心类为 `ProjectBuilder`(`ProjectBuilder.java`)。整个"设备内构建"由 `DesignActivity` 的构建任务驱动,按 `step / 20` 报告进度。以 `DesignActivity` 中的真实调用顺序为准,完整流程如下:

| step | 进度文案 | 关键调用 | 说明 |
| --- | --- | --- | --- |
| 1 | Deleting temporary files... | `FileUtil.deleteFile(q.generatedFilesPath)`、`q.createBuildDirectories(...)` | 清理临时文件、创建构建目录、拷贝图标 |
| 2 | Generating source code... | `resourceManager.copyImagesToDir/copySoundsToDir/copyFontsToDir`、`new ProjectBuilder(...)`、`q.generateProjectFiles(...)` | 拷贝资源、生成项目源码、增量预检 |
| 3 | Extracting built-in libraries... | `BuiltInLibraries.extractCompileAssets(this)` | 释放内置库(含 `android.jar` 等) |
| 8–10 | AAPT2 is running... | `builder.compileResources()` | AAPT2 编译并链接资源(见 §4.3.1) |
| 11 | Generating view binding... | `builder.generateViewBinding()` | 生成 ViewBinding(另见 `ViewBindingBuilder.kt`) |
| 12 | Kotlin is compiling... | `KotlinCompilerBridge.compileKotlinCodeIfPossible(...)` | 仅当项目含 `.kt` 文件时执行 |
| 13 | Java is compiling... | `builder.compileJavaCode()` | 用 ECJ 编译 Java 源码(见 §4.3.2) |
| — | (Stringfog) | `new StringfogHandler(sc_id).start(...)` | 字符串加密(可选) |
| — | (Proguard/R8) | `new ProguardHandler(sc_id).start(...)` | 代码收缩/混淆(可选) |
| 17 | D8/Dx is running... | `builder.createDexFilesFromClasses()` | class → dex(见 §4.3.3) |
| 18 | Merging DEX files... | `builder.getDexFilesReady()` | 合并 dex |
| 19 | Building APK... | `builder.buildApk()` | 打包(内部完成 zipalign) |
| 20 | Signing APK... | `builder.signDebugApk()` | 签名,随后 `installBuiltApk()` |

> 进度文案中的 dexer 名称由 `builder.getDxRunningText()` 决定:根据 `BuildSettings.SETTING_DEXER` 返回 `"D8 is running..."` 或 `"Dx is running..."`。

每一步之间都会检查 `canceled` 标记以支持中途取消。构建异常按类型分流(见 §4.5.1)。

#### 4.3.1 资源编译(AAPT2)

`compileResources()` 委托给 `core/build/compiler/ResourceCompiler.java`。其内部 `Aapt2Compiler` 依次:编译内置库资源 → 编译本地库资源 → 编译项目资源 → 编译导入的资源 → `link()` 链接。资源以 `.zip` 形式分层缓存(内置库/本地库按修改时间判断是否需重编),链接阶段通过 `BinaryExecutor` 调用 aapt2 可执行文件,传入 `--min-sdk-version`、`--target-sdk-version`、`-I <android.jar>`、`--manifest`、`--java <R.java 输出>`、`--proguard <规则输出>` 等参数。链接失败会抛出 `SimpleException`,缺文件/目录抛出 `MissingFileException`。

#### 4.3.2 Java 编译(ECJ)与增量构建

`compileJavaCode()` 使用 ECJ(Eclipse Compiler for Java,依赖 `libs.ecj`)。是否走增量模式在 `DesignActivity` 构建任务的"增量预检"里决定,需同时满足:

- 已存在编译产物(`compiledClassesPath` 下有 `.class`);
- 存在增量缓存文件(`IncrementalBuildCache.hasCacheFile()`);
- 未启用 Proguard 收缩;
- classpath 未变化(`isClasspathChanged(...)` 为 false);
- 无需缓存迁移(`requiresFullRebuildMigration()` 为 false)。

增量模式下只清理 `R.java` 目录(`cleanRJavaOnly()`),否则全量清理(`cleanBuildCache()`)。`IncrementalBuildCache`(`IncrementalBuildCache.java`)基于内容指纹跟踪变更,配合 `ProjectBuilder` 的 `updateCacheAfterSuccessfulBuild(...)`、`deleteOldClassFiles(...)` 增删过期 class。

#### 4.3.3 Dex 与收缩

- Dex:`createDexFilesFromClasses()` 根据设置选择 D8 或 DX。D8 路径见 `core/build/compiler/DexCompiler.java`(`D8Command`,`CompilationMode.RELEASE`,`setMinApiLevel(...)`)。DX 相关厂商代码在 `:vendor-dx` 模块与 `core/build/multidex/`(`MainDexListBuilder` 等,用于生成 main dex 列表)。
- 收缩/优化(可选):`runProguard()`、`runR8()`(见 `core/build/compiler/R8Compiler.kt`,`R8Command` + Proguard 规则/映射输出)、`runStringfog()`(字符串加密)。
- 是否启用由 `BuildSettings` 与 `ProguardHandler`/`StringfogHandler` 控制。

#### 4.3.4 其它辅助类

- `BuildSettings`(`BuildSettings.java`):项目级构建设置的键定义,如 `SETTING_DEXER`(取值 `SETTING_DEXER_D8` / `SETTING_DEXER_DX`)、`SETTING_ANDROID_JAR_PATH`、`SETTING_CLASSPATH`、`SETTING_JAVA_VERSION` 等;继承自 `ProjectSettings`。
- `KeyStoreManager` / `KeyStoreOutputStream`:签名相关。
- `ManifestGenerator`:生成 `AndroidManifest.xml`。
- `ProjectFilePaths`:构建过程中的各类路径(`binDirectoryPath`、`compiledClassesPath`、`resDirectoryPath`、`rJavaDirectoryPath` 等)。
- `AppBundleCompiler`:生成 AAB(配合 `setBuildAppBundle(true)`)。

### 4.4 项目数据管理(project)

项目数据的加载、序列化与持久化位于 `app/src/main/java/pro/sketchware/core/project/`。

#### 4.4.1 门面 `ProjectDataManager`

`ProjectDataManager`(`ProjectDataManager.java`)是一组 `static synchronized` 方法构成的门面,按项目 id(`sc_id`)获取四类管理器:

```java
public static ProjectDataStore   getProjectDataManager(String sc_id)
public static ProjectDataStore   getProjectDataManager(String sc_id, boolean load)
public static ProjectFileManager getFileManager(String sc_id)
public static LibraryManager     getLibraryManager(String sc_id)
public static ResourceManager    getResourceManager(String sc_id)
```

以及生命周期管理:`clearAll()`、`discardAll()`、`closeDataManager()`、`closeFileManager()`、`closeLibraryManager()`、`closeResourceManager()`。带 `boolean load` 的重载用于控制是否立即从磁盘加载数据。

#### 4.4.2 数据存储 `ProjectDataStore`

`ProjectDataStore`(`ProjectDataStore.java`)持有单个项目的视图/逻辑/组件数据,负责读写与序列化。与代码生成对接的关键方法:

```java
public ArrayList<BlockBean> getBlocks(String fileName, String blockKey)
public void putBlocks(String fileName, String blockKey, ArrayList<BlockBean> blocks)
public final void serializeLogicData(StringBuilder buffer)
public final void serializeViewData(StringBuilder buffer)
public boolean saveAllData()
```

它还提供大量针对视图、组件、变量、事件、more block 的增删查改方法(如 `addView`、`addComponent`、`addEvent`、`getViews`、`getComponents`、`getEvents`、`renameVariable` 等),是编辑器对项目内容进行操作的主要入口。

其余职责类:`ProjectFileManager`、`ProjectDataParser`、`ResourceManager`、`LibraryManager`、`SketchwarePaths`、`SketchwareConstants`,以及各类集合管理器(`ImageCollectionManager`、`SoundCollectionManager`、`FontCollectionManager`、`BlockCollectionManager`、`MoreBlockCollectionManager`、`WidgetCollectionManager`)。

### 4.5 编辑器入口

#### 4.5.1 视图设计器 `DesignActivity`

`DesignActivity`(`app/src/main/java/pro/sketchware/activities/design/DesignActivity.java`)是视图设计器,同时承载“构建 APK”流程。其内部构建任务按进度逐步调用 `ProjectBuilder`(共 20 步,进度显示为 `step / 20`):

```java
onProgress("AAPT2 is running...", 8);        builder.compileResources();
onProgress("Generating view binding...", 11); builder.generateViewBinding();
onProgress("Java is compiling...", 13);       builder.compileJavaCode();
onProgress(builder.getDxRunningText(), 17);    builder.createDexFilesFromClasses();
onProgress("Building APK...", 19);             builder.buildApk();
onProgress("Signing APK...", 20);              builder.signDebugApk();
```

构建失败时按异常类型分流处理:`MissingFileException`(缺文件/目录,可弹窗创建)、`SimpleException`(编译错误,`indicateCompileErrorOccurred(...)`),以及兜底 `Throwable`(打印堆栈)。

#### 4.5.2 逻辑编辑器 `LogicEditorActivity`

`LogicEditorActivity`(`app/src/main/java/pro/sketchware/activities/editor/LogicEditorActivity.java`)是逻辑积木编辑器。它通过 `blockKey = id + "_" + eventName` 与 `ProjectDataStore` 桥接积木数据。加载逻辑见 `loadEventBlocks(Runnable onComplete)`:

```java
ArrayList<BlockBean> eventBlocks = ProjectDataManager
        .getProjectDataManager(scId)
        .getBlocks(projectFile.getJavaName(), id + "_" + eventName);
```

保存时相应调用 `putBlocks(fileName, blockKey, blocks)` 写回。

### 4.6 依赖解析(resolver 模块 + DependencyResolver)

用户在项目中添加 Maven 依赖时,由 `:app` 侧的 `DependencyResolver`(`core/build/dependency/DependencyResolver.kt`)驱动,底层调用 `:resolver` 模块(`org.cosmic.ide.dependency.resolver`,源自 Cosmic IDE)。

`:resolver` 模块的公开 API(`resolver/src/main/kotlin/org/cosmic/ide/dependency/resolver/`):

- `utils.kt`:顶层入口 `getArtifact(groupId, artifactId, version): Artifact?`、全局可变的 `repositories` 队列(默认含 `MavenCentral`、`GoogleMaven`、`Jitpack`、`SonatypeSnapshots`)、`eventReciever`、共享的 `okHttpClient`(连接 5s / 读写 15s / 整体 30s 超时),以及 `ProjectObjectModel.resolveDependencies(...)`(带 BOM / `dependencyManagement` / 父 POM 继承展开与并发解析,`parallelForEach` 使用 `Semaphore(8)` 限流)。
- `api/`:`Artifact.kt`、`Repository.kt`、`EventReciever.kt`、`MavenMetadata.kt`、`ProjectObjectModel.kt`。
- `repository/`:四个内置仓库实现。

`DependencyResolver` 在 `:app` 侧承担下载、解压与 dex 化:

- 构造参数:`groupId`、`artifactId`、`version`、`skipDependencies`、`buildSettings`。核心方法 `resolveDependency(callback: DependencyResolverCallback)`(`runBlocking(Dispatchers.IO)`)。
- 下载目录默认 `SketchwarePaths.getLocalLibsDir()`;遇到存储权限错误(`EPERM`/`Operation not permitted`)自动切换到 `getLocalLibsFallbackDir()`。
- AAR 会被解压出 `classes.jar` 并从 `AndroidManifest.xml` 解析包名写入 `config` 文件;JAR/解压后的 JAR 通过 `compileJarWithFallback(...)` 用 D8 转 `classes.dex`(先无 classpath 快路径,失败再带完整 classpath 重试以支持脱糖)。
- 传递依赖解析有 300 秒超时(`withTimeout(300_000L)`),超时则仅保留主库;结果缓存为 `dependency-tree.json`。
- `isBuiltInDependency(...)` 会跳过已内置的传递依赖(如大部分 `androidx.*`、`com.google.firebase`、`com.google.android.gms`、`org.jetbrains.kotlin` 等),对 OkHttp/Okio 等有破坏性大版本变更的库按主版本号判断(`parseMajorVersion`)。

`DependencyResolverCallback` 暴露 `dexing`、`unzipping`、`onResolutionComplete`、`onDownloadError`、`dexingFailed`、`onResolutionTimeout`、`onTaskCompleted` 等回调供 UI 展示进度。

### 4.7 存储路径与项目布局

所有磁盘路径由 `SketchwarePaths`(`core/project/SketchwarePaths.java`)集中管理,是一组静态方法。常用示例:

| 方法 | 含义 |
| --- | --- |
| `getDataPath(sc_id)` | 项目数据根目录 |
| `getProjectJavaPath(sc_id)` | 自定义 Java 源码目录 |
| `getProjectResourcePath(sc_id)` | 导入的资源目录 |
| `getProjectBuildConfigPath(sc_id)` | 构建设置文件(`BuildSettings.getPath()` 使用) |
| `getLocalLibsDir()` / `getLocalLibsFallbackDir()` | 本地库缓存(依赖解析下载目标) |
| `getRepositoriesJsonPath()` | 自定义 Maven 仓库列表 |
| `getSignedApkPath()` / `getSignedAabPath()` | 构建产物 |
| `getDebugLogPath()` / `getLogcatPath(packageName)` | 调试日志 |
| `getKeystoreFilePath()` | keystore |

新增涉及磁盘的功能时,应在 `SketchwarePaths` 中新增方法而不是硬编码路径字符串。

---

## 5. 扩展指南

以下扩展点均给出真实文件路径与接口签名。

### 5.1 新增一个逻辑积木(block)

分两种情况:

**A. 固定模板即可表达** —— 在 `ExtraBlockDefinitions.extraBlocks(...)`(`core/codegen/ExtraBlockDefinitions.java`)中新增一行:

```java
arrayList.add(addBlock(
    "myOpcode",        // 唯一 opCode
    "s",               // 返回类别:" " 语句 / "b" 布尔 / "d" 数字 / "s" 字符串 / "c" 复合
    "",                // typeName(自定义返回类型名,可留空)
    "%1$s.myMethod(%2$s)", // Java 模板,占位符对应参数
    "#5cb722",         // 积木颜色
    "%s myMethod %s"   // spec:编辑器中的可视化拼写
));
```

**B. 需要自定义生成逻辑** —— 在 `BlockCodeRegistry`(`core/codegen/BlockCodeRegistry.java`)对应的 `registerXxxBlocks()` 分组里注册一个 `BlockCodeHandler`:

```java
register("myOpcode", (bean, params, context) -> {
    // 可访问 context.resolveBlock(...)、context.activityName、context.buildConfig 等
    return params.get(0) + ".myMethod(" + params.get(1) + ");";
});
```

未在注册表命中的 opcode 会走 `BlockInterpreter.getCodeExtraBlock(...)` 的模板回退路径。

### 5.2 新增一个事件(event)

在 `EventCodeRegistry`(`core/codegen/EventCodeRegistry.java`)对应的 `registerXxxEvents()` 分组里注册 `EventCodeHandler`:

```java
register("onMyEvent", (id, logic) ->
    "@Override\r\npublic void onMyEvent() {\r\n" + logic + "\r\n}");
```

未注册的事件名会回退到 `ManageEvent.getExtraEventCode(targetId, eventName, eventLogic)`。组件侧如需生成事件代码,统一走 `ComponentCodeGenerator.getEventCode(targetId, eventName, eventLogic)`(其内部委托 `EventCodeRegistry.generate(...)`)。

### 5.3 新增一个组件(component)

组件相关生成逻辑集中在:

- `ComponentTypeMapper`(`core/codegen/ComponentTypeMapper.java`):登记组件类型到 Java 类型与 import 的映射。
- `ComponentTemplates`(`core/codegen/ComponentTemplates.java`):组件模板。
- `ComponentCodeGenerator`(`core/codegen/ComponentCodeGenerator.java`):字段声明(`getFieldDeclaration(...)`)、初始化(`getComponentInitializerCode(...)`)、适配器代码(`recyclerViewAdapter(...)`、`pagerAdapter(...)`)等。

数据侧还需在 `ProjectDataStore` / `ComponentBean` 层面确保新组件类型能被序列化与读取。

### 5.4 扩展点小结

| 目标 | 主要文件 | 关键 API |
| --- | --- | --- |
| 模板型积木 | `ExtraBlockDefinitions.java` | `addBlock(name,type,typeName,code,color,spec)` |
| 逻辑型积木 | `BlockCodeRegistry.java` | `register(opcode, BlockCodeHandler)` |
| 事件 | `EventCodeRegistry.java` | `register(eventName, EventCodeHandler)` |
| 组件 | `ComponentTypeMapper` / `ComponentCodeGenerator` | 类型映射 + 生成方法 |
| 构建步骤 | `ProjectBuilder.java` | 见 §4.3 管线顺序 |

---

## 6. 编码规范

- **包名**:新代码统一置于 `pro.sketchware.*`。历史包名(`com.besome.sketch.*`、`mod.*`)已完成迁移,不要再引入。
- **代码生成优先注册表**:内置积木/事件优先通过 `BlockCodeRegistry` / `EventCodeRegistry` 注册,按功能域放入对应的 `registerXxx()` 分组,保持分组内聚。
- **数据经由门面访问**:读写项目数据统一通过 `ProjectDataManager` 获取管理器,再操作 `ProjectDataStore` 等,不要绕过门面直接持有实例。
- **Java 17 / Android**:遵循 `compileOptions` 的 Java 17 语言级别;注意 `minSdk = 26`。
- **函数式接口**:`BlockCodeHandler`、`EventCodeHandler` 为单方法接口,注册时优先用 lambda。

---

## 7. 构建与发布

- 本地调试构建:`./gradlew assembleDebug`。
- 设备内构建:即 `DesignActivity` 触发的 `ProjectBuilder` 全流程(§4.3、§4.5.1),产物为已签名的调试 APK。
- 发布注意:`release` 构建目前复用 debug 签名且未开启混淆(`minifyEnabled = false`)。对外发布前应配置独立 release keystore,并评估是否开启 `runProguard()` / `runR8()`。

---

## 8. 调试

- **崩溃日志**:`SketchApplication` 安装了全局未捕获异常处理器,崩溃时跳转 `CollectErrorActivity`(`pro.sketchware.activities.tools.CollectErrorActivity`),并由 `CrashLogManager` 记录。
- **编译错误**:设备内构建时,Java 编译错误以 `SimpleException` 抛出并展示在界面上(`indicateCompileErrorOccurred(...)`);`CompileErrorSaver`(`core/build/CompileErrorSaver.java`)负责持久化编译错误。构建阶段的资源错误(AAPT2)同样以 `SimpleException` 抛出(见 `ResourceCompiler`),缺失文件/目录则抛 `MissingFileException`。
- **增量构建问题**:若怀疑增量缓存导致的编译异常,关注 `IncrementalBuildCache` 与 `ProjectBuilder.deleteOldClassFiles(...)` 的行为。`DesignActivity` 的构建任务会打印 `Incremental build precheck` 日志,列出 `compiledClassesAvailable`、`cacheFileExists`、`classpathChanged` 等判定项,可据此定位为何未走增量路径。必要时通过工程设置切换回全量重建。
- **构建耗时**:`DesignActivity$BuildTask` 会用 `Log.d` 打印各阶段耗时(如 `Step 2 timing: ...`),用于定位构建瓶颈。
- **logcat 抓取**:`BuildSettings.SETTING_ENABLE_LOGCAT` 控制是否为构建出的应用启用 logcat 采集,日志路径见 `SketchwarePaths.getLogcatPath(packageName)`。

---

## 附:关键文件索引

| 领域 | 路径 |
| --- | --- |
| 应用入口 | `app/src/main/java/pro/sketchware/SketchApplication.java` |
| 数据模型 | `app/src/main/java/pro/sketchware/beans/` |
| 积木解释器 | `app/src/main/java/pro/sketchware/core/codegen/BlockInterpreter.java` |
| 积木注册表 | `app/src/main/java/pro/sketchware/core/codegen/BlockCodeRegistry.java` |
| 事件注册表 | `app/src/main/java/pro/sketchware/core/codegen/EventCodeRegistry.java` |
| 内置积木定义 | `app/src/main/java/pro/sketchware/core/codegen/BuiltInBlockDefinitions.java` / `ExtraBlockDefinitions.java` |
| Activity 装配 | `app/src/main/java/pro/sketchware/core/codegen/ActivityCodeGenerator.java` |
| 组件生成 | `app/src/main/java/pro/sketchware/core/codegen/ComponentCodeGenerator.java` |
| 构建管线 | `app/src/main/java/pro/sketchware/core/build/ProjectBuilder.java` |
| 资源编译(AAPT2) | `app/src/main/java/pro/sketchware/core/build/compiler/ResourceCompiler.java` |
| DEX 编译(D8) | `app/src/main/java/pro/sketchware/core/build/compiler/DexCompiler.java` |
| R8 编译 | `app/src/main/java/pro/sketchware/core/build/compiler/R8Compiler.kt` |
| Kotlin 编译桥接 | `app/src/main/java/pro/sketchware/core/build/compiler/KotlinCompilerBridge.java` |
| 构建设置常量 | `app/src/main/java/pro/sketchware/core/build/BuildSettings.java` |
| 增量编译缓存 | `app/src/main/java/pro/sketchware/core/build/IncrementalBuildCache.java` |
| 依赖解析(app 侧) | `app/src/main/java/pro/sketchware/core/build/dependency/DependencyResolver.kt` |
| 依赖解析(resolver 模块) | `resolver/src/main/kotlin/org/cosmic/ide/dependency/resolver/` |
| 存储路径 | `app/src/main/java/pro/sketchware/core/project/SketchwarePaths.java` |
| 数据门面 | `app/src/main/java/pro/sketchware/core/project/ProjectDataManager.java` |
| 数据存储 | `app/src/main/java/pro/sketchware/core/project/ProjectDataStore.java` |
| 视图设计器 | `app/src/main/java/pro/sketchware/activities/design/DesignActivity.java` |
| 逻辑编辑器 | `app/src/main/java/pro/sketchware/activities/editor/LogicEditorActivity.java` |
