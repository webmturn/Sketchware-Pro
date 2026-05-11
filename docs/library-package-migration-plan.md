# `pro.sketchware.library` 后续迁移方案

## 背景

当前目标是继续清理 Sketchware-Pro 中历史遗留的 `pro.sketchware.library` 包，把其中实际属于构建、工具或编辑器 UI 的类迁移到更准确的包中，降低 `core.build` / `core.codegen` / `project` 对 UI/legacy 包的反向依赖。

已完成并提交的基线：

```text
72dc40418 Move local library core utilities to util.library
```

该提交已将本地库核心类迁移到：

```text
pro.sketchware.util.library
```

已迁移类：

```text
ManageLocalLibrary
LocalLibrariesUtil
LocalLibraryImportPackageIndex
LocalLibrary
LocalLibrariesComparator
```

迁移后验证结果：

```text
git diff --check
.\gradlew.bat :app:compileDebugJavaWithJavac
```

均已通过。

## 当前剩余 `library` 包文件

当前工作区完成 Phase 6 后，`app/src/main/java/pro/sketchware/library` 下不再剩余源文件：

```text
无
```

## 当前外部引用点

源码中仍引用 `pro.sketchware.library.*` 的主要位置：

```text
无
```

## 目标包边界

### `pro.sketchware.util.library`

用于库相关的纯工具、配置、规则和数据模型。

适合放入：

```text
BuiltInLibraries
BuiltInLibraryManager
BuiltInLibraryUtils
ExcludeBuiltInLibrariesConfig
LocalLibrariesUtil
LocalLibrary
LocalLibraryImportPackageIndex
ManageLocalLibrary
ExtLibSelected
```

### `pro.sketchware.core.build.dependency`

用于 Maven 依赖解析、下载、AAR 解包、D8 dexing、依赖树缓存等构建流程逻辑。

适合放入：

```text
DependencyResolver
```

### `pro.sketchware.activities.editor.manage.library.*`

用于库管理相关 UI：Activity、Fragment、Adapter、列表项 View、UI helper。

适合放入：

```text
ManageLocalLibraryActivity
SubDependenciesActivity
LibraryDownloaderDialogFragment
DependencyDownloadAdapter
DependencyDownloadItem
ExcludeBuiltInLibrariesActivity
ExcludeBuiltInLibrariesLibraryItemView
LibrarySettingsImporter
```

## 迁移总顺序

推荐顺序：

```text
1. ExtLibSelected
2. DependencyResolver
3. Dependency downloader UI
4. Local library management UI
5. Built-in library exclusion UI
6. LibrarySettingsImporter
7. Final cleanup
```

排序原则：

- 先消除 `core.build` 对 legacy `library` 包的反向依赖。
- 再移动非 UI 的构建引擎逻辑。
- 再移动 Fragment / Adapter 等 UI 组件。
- 最后移动 Activity 和 Manifest 相关项。
- 每批独立验证、独立提交。

## Phase 1：移动 `ExtLibSelected`

### 目标

消除 `core.build.ProjectBuilder` 对 `pro.sketchware.library` 的最后一个直接依赖。

### 移动

从：

```text
pro.sketchware.library.ExtLibSelected
```

到：

```text
pro.sketchware.util.library.ExtLibSelected
```

### 更新调用点

```text
ProjectBuilder
```

从：

```java
import pro.sketchware.library.ExtLibSelected;
```

改为：

```java
import pro.sketchware.util.library.ExtLibSelected;
```

### 风险

低。

`ExtLibSelected` 只根据 `ConstVarComponent` 给 `BuiltInLibraryManager` 添加内置库，没有 UI、Manifest、Activity 生命周期风险。

### 验证

```text
git diff --check
.\gradlew.bat :app:compileDebugJavaWithJavac
```

额外检查：

```text
app/src/main/java/pro/sketchware/core 下不应再有 import pro.sketchware.library.*
```

### 建议提交

```text
Move ExtLibSelected to util.library
```

## Phase 2：移动 `DependencyResolver.kt`

### 目标

把 Maven 依赖解析、下载、AAR 解包、D8 dexing 等构建逻辑从 legacy `library` 包移出。

### 移动

从：

```text
pro.sketchware.library.DependencyResolver
```

到：

```text
pro.sketchware.core.build.dependency.DependencyResolver
```

### 不放入 `util.library` 的原因

`DependencyResolver` 依赖：

```text
BuildSettings
D8 / R8
SketchwarePaths
FilePathUtil
BuiltInLibraries
```

它更像构建依赖解析引擎，而不是普通 util。

### 更新调用点

```text
LibraryDownloaderDialogFragment
```

从：

```java
import pro.sketchware.library.DependencyResolver;
```

改为：

```java
import pro.sketchware.core.build.dependency.DependencyResolver;
```

### 风险

中低。

主要风险：

- Kotlin 文件 package 声明必须同步更新。
- `.kt` 文件路径和 `package` 声明必须同时移动到新包，避免 IDE / Gradle 缓存掩盖旧包残留。
- Java 调 Kotlin 的 import 必须同步更新。
- 需要覆盖 `compileDebugKotlin`。

### 验证

```text
git diff --check
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

残留检查：

```text
pro.sketchware.library.DependencyResolver 应归零
```

### 建议提交

```text
Move dependency resolver to core build dependency package
```

## Phase 3：移动依赖下载 UI 三件套

### 目标

把依赖下载弹窗和其 RecyclerView 模型 / Adapter 移入编辑器库管理 UI 包。

### 移动

从：

```text
pro.sketchware.library.LibraryDownloaderDialogFragment
pro.sketchware.library.DependencyDownloadAdapter
pro.sketchware.library.DependencyDownloadItem
```

到：

```text
pro.sketchware.activities.editor.manage.library.downloader.LibraryDownloaderDialogFragment
pro.sketchware.activities.editor.manage.library.downloader.DependencyDownloadAdapter
pro.sketchware.activities.editor.manage.library.downloader.DependencyDownloadItem
```

三者必须同一批移动，避免 `LibraryDownloaderDialogFragment`、`DependencyDownloadAdapter`、`DependencyDownloadItem` 之间出现半迁移状态。

### 更新调用点

主要更新：

```text
ManageLocalLibraryActivity
```

移动后需要显式 import：

```java
import pro.sketchware.activities.editor.manage.library.downloader.LibraryDownloaderDialogFragment;
```

`DependencyDownloadAdapter` 和 `DependencyDownloadItem` 只由 `LibraryDownloaderDialogFragment` 使用，跟随 Fragment 一起移动即可。

### 风险

中。

风险点：

- 当前三者与 `ManageLocalLibraryActivity` 同包，移动后要确认没有 package-private 访问。
- Fragment 不是 Manifest Activity，无 Manifest 注册风险。
- Adapter / Item 是 UI 状态模型，不应迁到 `util.library`。

### 验证

```text
git diff --check
.\gradlew.bat :app:compileDebugJavaWithJavac
```

残留检查：

```text
pro.sketchware.library.LibraryDownloaderDialogFragment
pro.sketchware.library.DependencyDownloadAdapter
pro.sketchware.library.DependencyDownloadItem
```

均应归零。

### 建议提交

```text
Move dependency downloader UI to manage library package
```

## Phase 4：移动本地库管理 UI

### 目标

把本地库管理 Activity 从 legacy `library` 包移到编辑器库管理 UI 包。

### 移动

从：

```text
pro.sketchware.library.ManageLocalLibraryActivity
pro.sketchware.library.SubDependenciesActivity
```

到：

```text
pro.sketchware.activities.editor.manage.library.local.ManageLocalLibraryActivity
pro.sketchware.activities.editor.manage.library.local.SubDependenciesActivity
```

### 更新调用点

需要更新：

```text
AppSettings
ManageLibraryActivity
AndroidManifest.xml
ManageLocalLibraryActivity 内部对 SubDependenciesActivity 的引用
```

如果 Phase 3 已先移动 downloader UI，`ManageLocalLibraryActivity` 中的 `LibraryDownloaderDialogFragment` import 应保持指向 downloader 子包。

Manifest 从：

```xml
<activity
    android:name="pro.sketchware.library.ManageLocalLibraryActivity"
    android:configChanges="orientation|screenSize" />
<activity
    android:name="pro.sketchware.library.SubDependenciesActivity"
    android:configChanges="orientation|screenSize" />
```

改为：

```xml
<activity
    android:name="pro.sketchware.activities.editor.manage.library.local.ManageLocalLibraryActivity"
    android:configChanges="orientation|screenSize" />
<activity
    android:name="pro.sketchware.activities.editor.manage.library.local.SubDependenciesActivity"
    android:configChanges="orientation|screenSize" />
```

### 风险

中。

风险点：

- Activity FQN 改变，Manifest 必须同步。
- 设置页和库管理页的跳转必须同步更新。
- 如果外部保存了旧 Activity FQN 的快捷方式或显式 Intent，可能失效；当前已知调用点主要是代码内 class 引用。

### 验证

```text
git diff --check
.\gradlew.bat :app:processDebugMainManifest :app:compileDebugJavaWithJavac
```

残留检查：

```text
pro.sketchware.library.ManageLocalLibraryActivity
pro.sketchware.library.SubDependenciesActivity
```

源码和 Manifest 中都应归零。

### 建议提交

```text
Move local library management UI to editor manage package
```

## Phase 5：移动内置库排除 UI

### 目标

把“排除内置库”的 Activity 和列表项 View 移到编辑器库管理 UI 包。

### 移动

从：

```text
pro.sketchware.library.ExcludeBuiltInLibrariesActivity
pro.sketchware.library.ExcludeBuiltInLibrariesLibraryItemView
```

到：

```text
pro.sketchware.activities.editor.manage.library.builtin.ExcludeBuiltInLibrariesActivity
pro.sketchware.activities.editor.manage.library.builtin.ExcludeBuiltInLibrariesLibraryItemView
```

### 更新调用点

需要更新：

```text
ManageLibraryActivity
AndroidManifest.xml
```

Manifest 从：

```xml
<activity android:name="pro.sketchware.library.ExcludeBuiltInLibrariesActivity" />
```

改为：

```xml
<activity android:name="pro.sketchware.activities.editor.manage.library.builtin.ExcludeBuiltInLibrariesActivity" />
```

### 风险

中低。

风险点：

- Activity Manifest 需要同步。
- `ExcludeBuiltInLibrariesLibraryItemView` 被 `ManageLibraryActivity` 直接使用，import 要更新。
- `BuiltInLibraryManager` 的 Javadoc 中存在裸 `ExcludeBuiltInLibrariesActivity` 链接；它不是源码 import 残留，普通编译不受影响，可在本批顺手改为纯文本或新 FQN。

### 验证

```text
git diff --check
.\gradlew.bat :app:processDebugMainManifest :app:compileDebugJavaWithJavac
```

残留检查：

```text
pro.sketchware.library.ExcludeBuiltInLibrariesActivity
pro.sketchware.library.ExcludeBuiltInLibrariesLibraryItemView
```

源码和 Manifest 中都应归零。

### 建议提交

```text
Move built-in library exclusion UI to manage library package
```

## Phase 6：移动 `LibrarySettingsImporter`

### 目标

把 Firebase / AdMob / GoogleMap 的库设置导入 UI helper 移到库管理 UI 包。

### 移动

从：

```text
pro.sketchware.library.LibrarySettingsImporter
```

到：

```text
pro.sketchware.activities.editor.manage.library.LibrarySettingsImporter
```

### 理由

`LibrarySettingsImporter` 是 UI dialog/helper，依赖：

```text
Activity
RecyclerView
LottieAnimationView
MaterialAlertDialogBuilder
ProjectComparator
LibraryManager
ProjectListManager
```

其中 `ProjectComparator` 已在：

```text
pro.sketchware.activities.editor.manage.library
```

所以放入该父级包最合适。

### 更新调用点

```text
AdmobActivity
FirebaseActivity
ManageGoogleMapActivity
```

从：

```java
import pro.sketchware.library.LibrarySettingsImporter;
```

改为：

```java
import pro.sketchware.activities.editor.manage.library.LibrarySettingsImporter;
```

### 风险

低。

无 Manifest，主要是 UI helper 包名变化。

### 验证

```text
git diff --check
.\gradlew.bat :app:compileDebugJavaWithJavac
```

残留检查：

```text
pro.sketchware.library.LibrarySettingsImporter
```

应归零。

### 建议提交

```text
Move library settings importer to manage library package
```

## Phase 7：最终清理旧 `library` 包

### 目标

确认 `pro.sketchware.library` 源码包完全消失。

### 检查项

源码中应全部归零：

```text
package pro.sketchware.library
import pro.sketchware.library.
import static pro.sketchware.library.
android:name="pro.sketchware.library.
```

只检查源码目录即可：

```text
app/src/main
```

不要把迁移计划文档本身当成残留问题：

```text
docs/library-package-migration-plan.md
```

不要把构建产物当成问题：

```text
app/build/intermediates/...
```

它们会在下一次构建后刷新。

### 验证

```text
git diff --check
.\gradlew.bat :app:processDebugMainManifest :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

如果希望更保险，最后运行：

```text
.\gradlew.bat :app:assembleDebug
```

### 建议提交

如果 Phase 6 后旧包已经自然为空，可以不单独提交。

如果还需要删除空目录或调整极少量引用，可提交：

```text
Remove remaining legacy library package references
```

## 每批通用安全规则

### 文件移动

使用：

```text
git mv
```

不要手动复制后删除，避免 Git 无法识别 rename。

### 编码安全

禁止使用 PowerShell 默认 `Set-Content` 重写 Java/Kotlin 文件。

原因：默认编码可能破坏已有非 ASCII 注释或 Javadoc 字符，导致 `javac` 报：

```text
编码 UTF-8 的不可映射字符
```

推荐方式：

- 使用 IDE 重构。
- 使用 UTF-8 safe 脚本。
- 或使用 binary-safe replacement，只替换 ASCII package/import 行。

### 替换顺序

如需批量替换 FQN，必须长名字优先，避免前缀误替换。

例如，下面两个名字有前缀关系：

```text
LocalLibrary
LocalLibraryImportPackageIndex
```

短名先替换可能破坏长名。

### 每批验证三件套

每个 phase 至少执行：

```text
git diff --check
.\gradlew.bat :app:compileDebugJavaWithJavac
```

涉及 Kotlin 的 phase 还要执行：

```text
.\gradlew.bat :app:compileDebugKotlin
```

涉及 Activity / Manifest 的 phase 还要执行：

```text
.\gradlew.bat :app:processDebugMainManifest
```

## 最终完成标准

全部迁移完成后，应满足：

```text
app/src/main/java/pro/sketchware/library 不再存在源码文件
app/src/main/AndroidManifest.xml 不再引用 pro.sketchware.library.*
app/src/main/java 中没有 import pro.sketchware.library.*
app/src/main/java 中没有 package pro.sketchware.library
core/build/codegen/project 不再依赖 legacy library 包
compileDebugKotlin 通过
compileDebugJavaWithJavac 通过
processDebugMainManifest 通过
git diff --check 通过
```

## 推荐下一步

下一步优先执行：

```text
Phase 1：移动 ExtLibSelected 到 pro.sketchware.util.library
```

这是最小、最低风险、收益最明确的一步，可以立即消除 `core.build` 对 legacy `library` 包的剩余直接 import。
