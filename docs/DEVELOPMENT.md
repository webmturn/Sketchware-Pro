# Sketchware-Pro 项目开发手册

> 版本：1.0  
> 最后更新：2026-03-05  
> 适用分支：`main`

---

## 目录

- [第一章：项目概览](#第一章项目概览)
- [第二章：环境搭建与构建](#第二章环境搭建与构建)
- [第三章：应用生命周期与导航](#第三章应用生命周期与导航)
- [第四章：核心模块详解](#第四章核心模块详解)
- [第五章：代码生成系统](#第五章代码生成系统)
- [第六章：构建系统](#第六章构建系统)
- [第七章：扩展系统](#第七章扩展系统)
- [第八章：数据格式与存储](#第八章数据格式与存储)
- [第九章：已知问题与技术债务](#第九章已知问题与技术债务)
- [第十章：开发规范与贡献指南](#第十章开发规范与贡献指南)

---

# 第一章：项目概览

## 1.1 项目简介

Sketchware Pro 是一款运行在 Android 设备上的**可视化 Android 应用开发工具**。用户通过拖拽 UI 组件设计界面、通过拼接逻辑积木块（Blocks）编写程序逻辑，最终在设备上直接编译生成可安装的 APK/AAB 文件。

项目 fork 自原版 Sketchware（已停止维护），由社区驱动开发，主要扩展包括：
- 自定义 Block/组件/事件系统
- ViewBinding 支持
- Kotlin 编译支持
- Material Design 3 组件
- Firebase BOM 33.7.0 集成
- 本地库管理与 Maven 依赖解析
- 源代码编辑器（Sora Editor）
- 暗色模式与多语言支持

## 1.2 技术栈

| 层面 | 技术 |
|------|------|
| **语言** | Java 17（主体）、Kotlin（ViewBinding/R8/依赖解析）|
| **最低 API** | Android 8.0（API 26）|
| **编译 SDK** | API 36 |
| **目标 SDK** | API 28（不上架 Google Play，故未升级）|
| **构建工具** | Gradle 8.13、AGP 8.12.0 |
| **UI 框架** | AndroidX、Material Design、ViewBinding |
| **代码编辑器** | Sora Editor |
| **图片加载** | Coil + Glide |
| **JSON** | Gson |
| **网络** | OkHttp |
| **Firebase** | BOM 33.7.0（Auth / Realtime Database / Storage / FCM / Crashlytics）|
| **签名** | kellinwood ZipSigner（内嵌）|
| **Java 编译** | ECJ（Eclipse Compiler for Java）|
| **Kotlin 编译** | kotlin-compiler（内嵌）|
| **DEX** | 内嵌 dx 工具链 + DexMerger |
| **资源编译** | AAPT2（sdklib）|
| **混淆** | R8（内嵌）|
| **脱糖** | coreLibraryDesugaring（desugar_jdk_libs_nio）|

## 1.3 包结构总览

项目源码位于 `app/src/main/java/`，按来源分为五大顶级包：

```
app/src/main/java/
├── com/besome/sketch/      # 原版 Sketchware 核心（反编译 + 重构）
│   ├── beans/              #   数据模型（ViewBean, BlockBean, EventBean...）
│   ├── design/             #   项目编辑主界面（DesignActivity）
│   ├── editor/             #   编辑器（Logic/View/Property/Event）
│   ├── export/             #   APK/AAB 导出
│   ├── lib/                #   基础 UI 组件
│   ├── projects/           #   项目列表与设置
│   ├── tools/              #   工具（崩溃收集、编译日志、KeyStore）
│   └── common/             #   源代码查看器
│
├── pro/sketchware/         # 社区重构后的核心模块
│   ├── core/               #   核心引擎（代码生成、构建、数据管理、Block渲染）
│   ├── activities/         #   新增 Activity（资源编辑器、设置、图标创建器等）
│   ├── blocks/             #   Extra Block 逻辑
│   ├── control/            #   逻辑编辑器控制器（点击监听、权限管理）
│   ├── dialogs/            #   底部弹窗（构建设置、添加组件）
│   ├── firebase/           #   FCM 服务
│   ├── fragments/          #   设置页 Fragment（外观、语言、Block管理、事件管理）
│   ├── lib/                #   UI 库（高亮、图标创建器、验证器）
│   ├── managers/           #   注入管理器
│   ├── menu/               #   扩展菜单
│   ├── utility/            #   工具类（FileUtil, FileResConfig, ThemeManager...）
│   └── widgets/            #   自定义 Widget 系统
│
├── dev/aldi/sayuti/        # 社区贡献：扩展系统
│   ├── block/              #   ExtraPaletteBlock（调色板填充）
│   └── editor/             #   注入编辑器（AppCompat/Manifest）、本地库管理
│
├── mod/                    # 社区贡献：各开发者模块
│   ├── agus/jcoderz/       #   DEX/DX 工具链、事件模板、Block/权限/资源管理
│   ├── hilal/saif/         #   BlocksHandler（代码生成）、组件/事件处理器、设置页
│   ├── hey/studios/        #   源代码编辑器、构建设置、MoreBlock、文件管理器
│   ├── jbk/                #   BuiltInLibraries、编译器（AAPT2/D8/资源）、导入器
│   ├── pranav/             #   ViewBinding、R8、依赖解析
│   ├── khaled/             #   Logcat 查看器
│   ├── tyron/              #   备份/恢复
│   ├── bobur/              #   VectorDrawable 解析
│   ├── remaker/            #   自定义属性视图
│   └── alucard/            #   APK 签名
│
└── kellinwood/             # 第三方：ZIP I/O 与签名库
    ├── security/           #   ZipSigner
    └── zipio/              #   ZIP 流处理
```

## 1.4 核心架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户界面层                                │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │ 项目列表  │  │ DesignActivity│  │ 设置/关于    │  │ 导出    │ │
│  └────┬─────┘  └──────┬───────┘  └──────────────┘  └─────────┘ │
│       │               │                                         │
│       │    ┌──────────┼──────────────┐                          │
│       │    │          │              │                          │
│  ┌────▼────▼┐  ┌──────▼──────┐  ┌───▼────────┐                │
│  │ ViewEditor│  │LogicEditor  │  │EventList   │                │
│  │ ViewPane  │  │ BlockPane   │  │ComponentList│               │
│  └────┬─────┘  └──────┬──────┘  └───┬────────┘                │
└───────┼───────────────┼──────────────┼─────────────────────────┘
        │               │              │
┌───────▼───────────────▼──────────────▼─────────────────────────┐
│                      数据管理层                                  │
│  ┌─────────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │ProjectDataStore │  │ResourceManager│  │ProjectFileManager│  │
│  │ (view/logic/    │  │ (图片/音频/   │  │ (Java/布局/       │  │
│  │  event/block)   │  │  字体/资源)   │  │  AndroidManifest) │  │
│  └────────┬────────┘  └──────┬───────┘  └────────┬──────────┘  │
└───────────┼──────────────────┼───────────────────┼─────────────┘
            │                  │                   │
┌───────────▼──────────────────▼───────────────────▼─────────────┐
│                      代码生成层                                  │
│  ┌────────────────┐  ┌─────────────────┐  ┌────────────────┐   │
│  │BlockInterpreter│  │ActivityCode     │  │LayoutGenerator │   │
│  │ (Block→Java)   │  │  Generator      │  │ManifestGenerator│  │
│  └────────┬───────┘  └────────┬────────┘  └───────┬────────┘   │
│           │    ┌──────────────┤                    │            │
│  ┌────────▼────▼───┐  ┌──────▼──────┐  ┌─────────▼────────┐   │
│  │EventCode       │  │ComponentCode│  │GradleFile       │   │
│  │  Generator     │  │  Generator  │  │  Generator      │   │
│  └────────────────┘  └─────────────┘  └────────────────────┘   │
└────────────────────────────┬───────────────────────────────────┘
                             │
┌────────────────────────────▼───────────────────────────────────┐
│                        构建层                                    │
│  ┌────────┐  ┌─────┐  ┌──────────┐  ┌────────┐  ┌──────────┐  │
│  │ AAPT2  │→│ ECJ  │→│ D8/Merger │→│ R8     │→│ ZipSigner│  │
│  │(资源)  │  │(Java)│  │  (DEX)   │  │(混淆)  │  │ (签名)   │  │
│  └────────┘  └─────┘  └──────────┘  └────────┘  └──────────┘  │
│                                                    ↓            │
│                                              APK / AAB          │
└─────────────────────────────────────────────────────────────────┘
```

---

# 第二章：环境搭建与构建

## 2.1 前置条件

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| **JDK** | 17 | 推荐 Microsoft OpenJDK 或 Adoptium |
| **Android Studio** | Ladybug+ | 或任何支持 Gradle 8.x 的 IDE |
| **Git** | 2.x | 用于克隆和版本管理 |
| **磁盘空间** | ~5 GB | 包含 Gradle 缓存和构建产物 |

> **注意**：项目不需要 Android SDK 手动安装，Gradle 会自动下载 compileSdk 36 的 SDK。

## 2.2 克隆与首次构建

```bash
# 1. 克隆仓库
git clone https://github.com/Sketchware-Pro/Sketchware-Pro.git
cd Sketchware-Pro

# 2. 设置 JAVA_HOME（Windows PowerShell 示例）
$env:JAVA_HOME = "C:\path\to\jdk-17"

# 3. 构建 Debug APK
./gradlew assembleDebug

# 构建产物位于：
# app/build/outputs/apk/debug/app-debug.apk
```

首次构建会：
1. 自动生成 mock `google-services.json`（见 2.4 节）
2. 下载所有 Gradle 依赖（约 1-2 GB）
3. 编译并打包 APK

## 2.3 签名配置

项目使用内置的 `testkey.keystore` 进行 Debug 和 Release 签名：

```groovy
// app/build.gradle
signingConfigs {
    debug {
        storeFile = file("../testkey.keystore")
        storePassword = "testkey"
        keyAlias = "testkey"
        keyPassword = "testkey"
    }
}
```

Release 构建也使用同一签名配置（`signingConfig = signingConfigs.debug`），因为项目不上架 Google Play。

## 2.4 google-services.json 处理

项目依赖 Firebase，但不要求贡献者拥有 Firebase 项目。构建系统通过 `createMockGoogleServices` task 自动处理：

1. **有环境变量 `GOOGLE_SERVICES_JSON`**：直接写入 `app/google-services.json`（用于 CI/CD）
2. **无环境变量**：自动生成 mock 文件到 `app/src/debug/google-services.json`

mock 文件包含虚假的 project_number、firebase_url、api_key 等，足以让编译通过，但 Firebase 功能在运行时不会工作。

## 2.5 构建辅助脚本

`scripts/` 目录包含三个 PowerShell 脚本，用于维护项目的内嵌资产：

| 脚本 | 用途 |
|------|------|
| `download-firebase-libs.ps1` | 从 Maven 下载 Firebase BOM 指定版本的所有 AAR/JAR，打包为内嵌资产 |
| `generate-dex.ps1` | 将内嵌 JAR 库预编译为 DEX 文件，加速用户项目构建 |
| `pack-assets.ps1` | 将内嵌资源（模板、库文件等）打包到 APK 的 assets 目录 |

> 这些脚本仅在**更新内嵌依赖版本**时需要运行，日常开发不需要。

## 2.6 常见构建问题排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `Could not determine java version` | JAVA_HOME 未设置或指向错误版本 | 设置 `JAVA_HOME` 指向 JDK 17+ |
| `google-services.json not found` | mock 文件未生成 | 运行 `./gradlew createMockGoogleServices` |
| `Out of memory` | Gradle 堆内存不足 | `gradle.properties` 中调整 `org.gradle.jvmargs=-Xmx4096m` |
| `Unsupported class file major version 65` | 使用了 JDK 21+ 编译但配置了 Java 17 | 确保 `sourceCompatibility` 与 JDK 版本一致 |
| `Duplicate class javax.inject` | 依赖冲突 | 已在 `build.gradle` 中通过 `exclude` 处理 |

---

# 第三章：应用生命周期与导航

## 3.1 启动流程

```
Application.onCreate()
  ├── SketchApplication.onCreate()
  │   ├── 注册 ActivityLifecycleCallbacks（跟踪当前 Activity）
  │   ├── 设置全局 UncaughtExceptionHandler → CollectErrorActivity
  │   ├── LanguageOverrideManager.init()（初始化语言覆盖）
  │   └── ThemeManager.applyTheme()（应用当前主题）
  │
  └── 启动 MainActivity
      └── ProjectsFragment（项目列表）
          ├── 显示已创建的项目
          ├── FAB → 创建新项目
          └── 点击项目 → DesignActivity
```

**关键类**：
- `SketchApplication`：全局 Application，管理 Context 获取、语言切换、崩溃收集
- `LanguageOverrideManager`：自定义语言覆盖机制（非 Android 原生 `values-xx/` 方案）
- `ThemeManager`：暗色/亮色主题切换

## 3.2 项目编辑流程

`DesignActivity` 是项目编辑的主入口，包含三个 Tab：

```
DesignActivity
├── [Tab 1] View（视图设计器）
│   ├── ViewFilesFragment     → 文件列表（main.xml, 各 Activity 布局）
│   ├── ViewEditorFragment    → 可视化布局编辑器（ViewEditor + ViewPane）
│   └── ViewProperty          → 属性编辑面板
│
├── [Tab 2] Event（事件）
│   ├── EventListFragment     → 事件/组件列表
│   └── ComponentListFragment → 组件管理
│
├── [Tab 3] (Source Code View)
│   └── SrcViewerActivity     → 查看生成的 Java 源代码
│
├── [Bottom Menu]
│   ├── Build Settings        → 构建配置
│   ├── Clean Temp            → 清理临时文件
│   ├── Show Last Error       → 查看编译错误日志
│   ├── Show Source           → 查看当前 Activity 源码
│   ├── Install APK           → 安装已构建 APK
│   ├── Show Signatures       → 查看 APK 签名
│   ├── XML Editor            → XML 代码编辑器
│   └── Import XML            → 导入 XML 布局
│
└── [Top Bar]
    ├── Run (▶)               → 触发构建流程
    └── Save                  → 保存项目
```

### 保存流程

```
用户点击"Save" 或切换 Activity
  └── ProjectSaver.execute()（后台线程）
      ├── ResourceManager.cleanupAllResources()
      ├── 并行保存（4线程池）：
      │   ├── ProjectDataStore.saveAllData()     → 逻辑/视图/事件数据
      │   ├── ProjectFileManager.saveToData()    → Java/XML 文件
      │   ├── ResourceManager.saveToData()       → 图片/音频/字体资源
      │   └── LibraryManager.saveToData()        → 库配置
      └── 成功后：
          ├── saveVersionCodeInformationToProject()
          └── resetBackupState()
```

## 3.3 项目构建与安装流程

```
用户点击 Run(▶)
  └── ProjectBuilder（后台线程）
      ├── [1] 生成 Java 源代码
      │   ├── ActivityCodeGenerator → 各 Activity .java
      │   ├── LayoutGenerator → XML 布局文件
      │   ├── ManifestGenerator → AndroidManifest.xml
      │   └── GradleFileGenerator → build.gradle
      │
      ├── [2] AAPT2 资源编译
      │   └── 编译 XML/图片/字体 → .flat → R.java + resources.ap_
      │
      ├── [3] Java/Kotlin 编译
      │   ├── ECJ（Eclipse Compiler）→ .class
      │   └── KotlinCompiler（可选）→ .class
      │
      ├── [4] DEX 化
      │   ├── D8 → .dex
      │   └── DexMerger → 合并多 DEX
      │
      ├── [5] R8 混淆（可选）
      │
      ├── [6] APK 打包 + 签名
      │   ├── ZipSigner（testkey 或用户 keystore）
      │   └── 或 AAB 导出
      │
      └── [7] 安装到设备
          ├── Root：pm install
          └── 非 Root：PackageInstaller Intent
```

## 3.4 应用设置系统

设置分布在多个 Activity/Fragment 中：

| 入口 | 类 | 功能 |
|------|------|------|
| 主设置页 | `AppSettings` | Block/组件/事件管理器入口、工作目录、APK 签名 |
| 应用设置 | `ConfigActivity` | 自动保存、震动、Block 显示、备份格式等开关 |
| 构建设置 | `BuildSettingsBottomSheet` | 项目级：Java 版本、混淆、ViewBinding 等 |
| 外观设置 | `SettingsAppearanceFragment` | 暗色模式切换 |
| 语言设置 | `LanguageSettingsFragment` | 界面语言选择 |

`ConfigActivity` 使用 SharedPreferences 存储开关状态，通过静态方法 `isSettingEnabled(String key)` 全局访问。

## 3.5 主题与暗色模式

```java
// ThemeManager.java
public static void applyTheme(Context context, int theme) {
    AppCompatDelegate.setDefaultNightMode(theme);
}
```

主题值存储在 SharedPreferences 中，支持：
- `MODE_NIGHT_NO`（亮色）
- `MODE_NIGHT_YES`（暗色）
- `MODE_NIGHT_FOLLOW_SYSTEM`（跟随系统）

## 3.6 语言覆盖机制

项目**不使用** Android 原生的 `values-xx/` 多语言目录，而是通过自定义机制实现：

```
LanguageOverrideManager
├── init(Application) → 从 SharedPreferences 读取用户选择的语言
├── 使用 AppCompatDelegate.setApplicationLocales() 设置应用级 Locale
└── Helper.getResString(R.string.xxx) → 全局获取本地化字符串
```

**关键约束**：
- 所有用户可见文本应使用 `Helper.getResString(R.string.xxx)` 而非 `context.getString()`
- `context.getString()` 绕过了语言覆盖机制，在某些场景下会返回错误语言的文本
- 当前 `strings.xml` 有 ~3370+ 条目，其中 ~1246 处使用 `Helper.getResString()`

## 3.7 崩溃收集与诊断

```
未捕获异常
  └── UncaughtExceptionHandler
      └── CollectErrorActivity
          ├── 显示崩溃堆栈
          ├── 用户可选择上报（Firebase Crashlytics）
          └── 杀死进程并重启

编译错误
  └── CompileErrorSaver.writeLogsToFile(error)
      └── CompileLogActivity
          ├── 显示上次编译错误日志
          └── 可调整字体大小
```

---

# 第四章：核心模块详解

## 4.1 UI 设计器

UI 设计器由三个核心类组成，负责可视化布局编辑：

### ViewEditor（52KB）

视图编辑器的顶层容器，处理用户交互：

```
ViewEditor extends FrameLayout
├── 拖拽手势处理（onTouchEvent）
│   ├── 长按 → 进入拖拽模式（震动反馈）
│   ├── 拖拽中 → 实时移动 ViewDummy（占位指示器）
│   └── 释放 → 确定放置位置，更新 ViewBean
├── Widget 调色板（PaletteWidget / PaletteFavorite）
│   ├── 内置组件：LinearLayout, Button, TextView, ImageView...
│   └── 收藏组件：用户保存的 Widget Collection
├── 视图操作
│   ├── 选中 → 高亮边框 + 显示属性面板
│   ├── 删除 → 移除 ViewBean 及子视图
│   └── 撤销/重做 → ViewHistoryManager
└── 与 DesignActivity 的回调通信
```

### ViewPane（77KB，项目第二大文件）

真正的"画布"，负责将 `ViewBean` 数据渲染为可视化 Android 视图：

| 职责 | 方法 |
|------|------|
| 创建视图 | `addView(ViewBean)` → 根据 type 创建对应 Item 组件 |
| 更新属性 | `updateView(ViewBean)` → 同步 Bean 属性到实际视图 |
| 布局渲染 | `updateLayout()` → 应用 width/height/margin/padding |
| 文本处理 | `getXmlString(key)` → 解析 `@string/` 引用（带缓存） |
| 图片加载 | 支持普通图片、9-patch、VectorDrawable、SVG |
| 颜色解析 | 支持 `@color/` 引用、Material 3 动态颜色 |

**VectorDrawable / SVG 支持**：
- `VectorDrawableLoader`（`mod/bobur/`）：解析 Android vector XML → `Drawable`
- `SvgUtils.kt`（`pro/sketchware/utility/`）：解析 SVG 文件 → `PictureDrawable`

### ViewDummy

拖拽过程中的占位指示器，显示为半透明矩形，标识目标放置位置。

## 4.2 逻辑编辑器

逻辑编辑器是项目中**最复杂的模块**，`LogicEditorActivity.java`（132KB）是全项目最大的文件。

### 核心类关系

```
LogicEditorActivity (132KB)
├── BlockPane extends RelativeLayout
│   ├── blockIndex: HashMap<Integer, BlockView>    # Block 索引（O(1) 查找）
│   ├── dragBlock: BlockView                       # 当前拖拽的 Block
│   ├── activeBlock: BaseBlockView                 # 吸附指示器
│   ├── addBlock() / addBlockNoLayout()            # 添加 Block
│   ├── dropBlock()                                # 放置 Block（处理吸附）
│   ├── computeSnapPoints() / collectSnapPoints()  # 计算吸附点
│   ├── hasListReference() / hasMapReference()     # 引用检测
│   └── getBlocks()                                # 序列化为 BlockBean 列表
│
├── PaletteSelector extends RecyclerView
│   ├── allPalettes: ArrayList<PaletteSelectorRecord>  # 所有分类
│   ├── initializePalettes()                            # 初始化分类列表
│   ├── performClickPalette(id)                         # 选中指定分类
│   └── PaletteSelectorAdapter                          # 分类列表适配器
│
├── PaletteBlock extends LinearLayout
│   ├── addBlock(BlockView)                        # 添加可拖拽的 Block 模板
│   ├── addCategoryHeader()                        # 添加分类标题
│   └── clearAll()                                 # 清空所有 Block
│
├── ExtraPaletteBlock
│   ├── setBlock(paletteId, color)                 # 根据分类填充 Block
│   ├── blockVariables() / blockComponents()       # 变量/组件相关 Block
│   ├── blockCustomViews()                         # 自定义视图 Block
│   └── moreBlocks()                               # MoreBlock 列表
│
└── LogicClickListener (29KB)
    ├── 处理 Block 点击事件
    ├── 弹出参数编辑对话框
    └── 管理变量/列表/组件的添加与编辑
```

### Block 渲染

每个 Block 是一个自定义 View，由 `BaseBlockView` → `BlockView` 继承链实现：

```
BaseBlockView (23KB)
├── 绘制 Block 形状（带凹槽/凸起的拼图形状）
├── density: float（屏幕密度）
├── childViews: ArrayList<View>（参数视图列表）
└── 测量与布局（onMeasure / onLayout）

BlockView (23KB) extends BaseBlockView
├── spec: String                    # Block 规格字符串（如 "set %s to %s"）
├── opCode: String                  # 操作码（如 "setVarInt"）
├── blockType: int                  # 类型（语句/表达式/帽子块等）
├── nextBlock / subStack1/2: int    # 链接关系
├── parameters: ArrayList<String>   # 参数值
├── layoutChain()                   # 级联布局（更新整条链的位置）
└── getAllChildren()                 # 获取所有子 Block（递归）
```

### Block 历史/撤销系统

```
BlockHistoryManager（单例，按项目 sc_id 隔离）
├── historyMap: Map<String, ArrayList<HistoryBlockBean>>
│   # key = "javaName_eventId_eventName"
│   # value = 历史快照列表
├── positionMap: Map<String, Integer>
│   # 当前历史位置（支持撤销/重做）
├── MAX_HISTORY_STEPS = 50
├── recordAdd() / recordMove() / recordDelete()
├── undo() / redo()
└── trimFutureHistory()  # 新操作时丢弃"未来"历史
```

## 4.3 事件系统

事件系统连接 UI 组件与逻辑 Block：

### 事件注册表

```
EventRegistry (23KB)
├── 定义所有内置事件类型
├── 事件分类：
│   ├── Type 1: Activity 生命周期事件（onCreate, onResume...）
│   ├── Type 2: View 交互事件（onClick, onLongClick...）
│   ├── Type 3: Component 事件（onResponse, onError...）
│   └── Type 4: 自定义视图事件
└── 每个事件定义：eventName, eventType, targetId, parameters

ManageEvent (52KB, mod/agus/jcoderz/)
├── 定义扩展事件的代码模板
├── 为每种组件类型生成监听器代码
└── 处理 Firebase/网络/传感器等复杂事件模板
```

### 事件数据模型

```java
EventBean {
    String targetId;    // 目标组件 ID（如 "button1"）
    String eventName;   // 事件名（如 "onClick"）
    int eventType;      // 1=Activity, 2=View, 3=Component, 4=ViewType
    // ... 序列化为 JSON 存入 logic 文件
}
```

## 4.4 组件系统

组件是非可视化的功能单元（如 Timer、Dialog、Firebase 等）：

```
ComponentBean (23KB)
├── componentId: String     # 组件实例名（如 "timer1"）
├── type: int               # 组件类型编号
├── param1/2/3: String      # 配置参数
└── classInfo: ClassInfo    # 类信息（全限定名、导入语句）

ComponentTypeMapper (37KB)
├── 映射 type → 类名/图标/描述
├── 支持 40+ 种组件类型：
│   Timer, Dialog, MediaPlayer, Camera, FilePicker,
│   SharedPreferences, Firebase, Gyroscope, Vibrator,
│   AsyncTask, OkHttpClient, Notification, ...
└── getComponentName(type) / getComponentIcon(type)

ComponentTemplates (98KB)
├── 为每种组件类型定义：
│   ├── 字段声明代码
│   ├── onCreate 初始化代码
│   ├── 事件监听器代码模板
│   └── 辅助方法代码
└── 全部以 String.format() 模板形式存储
```

## 4.5 数据管理

### ProjectDataStore（58KB）

项目数据的**内存仓库**，所有编辑器共享同一实例：

```
ProjectDataStore
├── viewMap: Map<filename, List<ViewBean>>                    # 视图数据
├── blockMap: Map<filename, Map<event, List<BlockBean>>>      # Block 数据
├── variableMap: Map<filename, List<Pair<type,name>>>         # 变量
├── listMap: Map<filename, List<Pair<type,name>>>             # 列表变量
├── moreBlockMap: Map<filename, List<Pair<name,spec>>>        # MoreBlock
├── componentMap: Map<filename, List<ComponentBean>>          # 组件
├── eventMap: Map<filename, List<EventBean>>                  # 事件
├── fabMap: Map<filename, ViewBean>                           # FAB 配置
│
├── 序列化：serializeLogicData(StringBuilder)  → 文本格式
├── 反序列化：ProjectDataParser.parseData()     → 从加密文件读取
├── 保存：saveAllData() / saveAllBackup()       → 写入 data/backup
└── 线程安全：saveAllBackup() 需 synchronized(this)
```

### ProjectDataManager（静态工厂）

```java
ProjectDataManager.getProjectDataManager(sc_id)   // → ProjectDataStore
ProjectDataManager.getFileManager(sc_id)           // → ProjectFileManager
ProjectDataManager.getResourceManager(sc_id)       // → ResourceManager
ProjectDataManager.getLibraryManager(sc_id)        // → LibraryManager
```

## 4.6 资源管理

```
ResourceManager (19KB)
├── 管理项目的图片/音频/字体资源
├── 资源路径：.sketchware/data/{sc_id}/files/resource/
│   ├── images/          # PNG/JPG 图片
│   ├── sounds/          # MP3/WAV 音频
│   ├── fonts/           # TTF/OTF 字体
│   └── values/          # strings.xml, colors.xml, styles.xml
├── syncImages() / syncSounds() / syncFonts()
│   → 清理已删除资源的引用
└── saveToData() / saveToBackup()
```

## 4.7 资源编辑器

`ResourcesEditorActivity` 提供对 `values/` 资源文件的可视化编辑：

| 编辑器 | Fragment | 管理的文件 |
|--------|----------|-----------|
| Strings | `StringsEditor` | `values/strings.xml` |
| Colors | `ColorsEditor` | `values/colors.xml` |
| Styles | `StylesEditor` | `values/styles.xml` |

编辑器支持增删改查操作，修改后通过 `invalidateXmlStringCache()` 通知相关缓存失效。

## 4.8 源代码查看/编辑器

| 类 | 功能 |
|----|------|
| `SrcViewerActivity` | 只读查看生成的 Java 源代码 |
| `SrcCodeEditor`（25KB） | 完整的代码编辑器（基于 Sora Editor），支持语法高亮、查找替换 |
| `ViewCodeEditorActivity` | XML 布局代码编辑器，支持直接编辑 XML 源码 |
| `CodeViewerActivity` | 轻量代码查看器 |

`SrcCodeEditor` 使用 `SyntaxScheme` 提供 Java/XML 语法高亮配色。

## 4.9 Logcat 查看器

```
LogReaderActivity (18KB, mod/khaled/)
├── 实时读取设备 Logcat 输出
├── 按包名过滤（支持多包名，逗号分隔）
├── 按日志级别着色（V/D/I/W/E）
├── 自动滚动 + 手动暂停
└── 清空日志
```

## 4.10 Collection 管理

Collection 系统允许用户保存和复用 Block 组合、Widget 布局、MoreBlock 定义：

```
ManageCollectionActivity (49KB)
├── Block Collection     → BlockCollectionManager
│   ├── 保存：选中 Block 链 → 序列化为 JSON
│   └── 加载：反序列化 → 放置到 BlockPane
├── Widget Collection    → WidgetCollectionManager
│   ├── 保存：选中 View 子树 → 序列化为 JSON
│   └── 加载：反序列化 → 添加到 ViewEditor
├── MoreBlock Collection → MoreBlockCollectionManager
│   ├── 保存：MoreBlock spec + 关联 Block
│   └── 导入/导出
└── 存储路径：.sketchware/collection/{type}/
```

## 4.11 Asset/Java/NativeLib 文件管理器

`mod/hey/studios/activity/managers/` 提供三个文件管理器：

| 管理器 | 管理路径 | 功能 |
|--------|----------|------|
| Assets Manager | `assets/` | 管理项目的原始资源文件 |
| Java Manager | `java/` | 管理自定义 Java 源文件（直接编译） |
| NativeLib Manager | `jniLibs/` | 管理 .so 原生库文件 |

用户可通过这些管理器直接向项目添加文件，构建时会被包含到 APK 中。

## 4.12 自定义 Widget

```
WidgetsCreatorManager (26KB)
├── 管理用户自定义的 View 组件类型
├── 定义新 Widget 的属性：
│   ├── 类名（如 "com.example.MyWidget"）
│   ├── 父类类型（View/ViewGroup/TextView...）
│   ├── 可用属性集
│   └── 调色板图标
├── 导入/导出为 JSON 文件
└── 存储路径：.sketchware/resources/widgets/
```

---

# 第五章：代码生成系统

代码生成是 Sketchware Pro 的核心——将用户的可视化设计（视图 + 逻辑积木）转换为可编译的 Java/XML/Gradle 源代码。

## 5.1 生成链路总览

```
用户项目数据（ViewBean + BlockBean + EventBean + ComponentBean）
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│                   代码生成阶段                              │
│                                                           │
│  [1] BlockInterpreter                                     │
│      └── 遍历 BlockBean 链表                               │
│          └── 查询 BlockCodeRegistry / BlocksHandler        │
│              └── opCode → Java 代码片段                    │
│                                                           │
│  [2] EventCodeGenerator / EventCodeRegistry               │
│      └── 为每个事件生成监听器方法框架                        │
│          └── 内部调用 BlockInterpreter 生成方法体           │
│                                                           │
│  [3] ComponentCodeGenerator / ComponentTemplates           │
│      └── 为每个组件生成：                                   │
│          ├── 字段声明（private Timer timer1;）              │
│          ├── 初始化代码（timer1 = new Timer();）            │
│          └── 事件绑定代码                                   │
│                                                           │
│  [4] ActivityCodeGenerator                                │
│      └── 组装完整的 Activity.java：                        │
│          ├── package / import 语句                         │
│          ├── 类声明 + 字段                                 │
│          ├── onCreate() + 初始化                           │
│          ├── 事件方法                                      │
│          ├── MoreBlock 方法                                │
│          └── 辅助方法                                      │
│                                                           │
│  [5] LayoutGenerator → XML 布局文件                       │
│  [6] ManifestGenerator → AndroidManifest.xml              │
│  [7] GradleFileGenerator → build.gradle                   │
└───────────────────────────────────────────────────────────┘
        │
        ▼
  完整的 Android 项目源代码（可直接编译）
```

## 5.2 BlockInterpreter：opcode → Java 代码

`BlockInterpreter`（12KB）是代码生成的**最底层**，负责将单个 Block 转换为 Java 代码：

```java
// 简化的处理流程
String generateCode(BlockBean block) {
    String template = BlockCodeRegistry.getCode(block.opCode);
    // template 例如: "%s.setText(%s)"
    String[] resolvedParams = resolveParameters(block.parameters);
    return String.format(template, resolvedParams);
}
```

**参数解析**：
- 表达式 Block（如 `add`）→ 递归生成代码（如 `(a + b)`）
- 字面值参数 → 直接嵌入（字符串自动加引号）
- 变量引用 → 直接使用变量名

## 5.3 BlockCodeRegistry / BlockSpecRegistry

这两个注册表是 Block 系统的**数据字典**：

| 注册表 | 大小 | 内容 |
|--------|------|------|
| `BlockSpecRegistry`（62KB） | ~1500+ 条目 | Block 的**显示规格**：`opCode → spec 字符串`（如 `"set %s to %s"`）|
| `BlockCodeRegistry`（73KB） | ~1500+ 条目 | Block 的**代码模板**：`opCode → Java 代码片段`（如 `"%s = %s"`）|

每种 Block 类型通过 `opCode` 关联：
```
opCode: "setVarInt"
├── BlockSpecRegistry: "set %d.v to %d"       → 用户看到的积木文本
└── BlockCodeRegistry: "%s = (int)(%s)"        → 生成的 Java 代码
```

## 5.4 ComponentCodeGenerator

`ComponentCodeGenerator`（43KB）为项目中的每个组件生成代码：

```
ComponentCodeGenerator
├── generateFieldDeclaration(component)
│   → "private Timer timer1;"
├── generateOnCreate(component)
│   → "timer1 = new Timer();"
├── generateEventCode(component, event)
│   → 监听器注册 + 回调方法
├── generateHelperMethods(component)
│   → 辅助方法（如 initializeWebView()）
└── generateBuildGradleDependency(component)
    → "implementation 'com.google.firebase:firebase-auth'"
```

**关键设计**：所有代码模板存储在 `ComponentTemplates`（98KB），使用 `String.format()` 的 `%s` 占位符，通过 `resolveParam()` 自动处理引号包裹。

## 5.5 BlocksHandler 扩展代码生成

`BlocksHandler.java`（122KB，`mod/hilal/saif/blocks/`）是项目第二大文件（仅次于 `LogicEditorActivity` 132KB），负责扩展 Block 的代码生成：

```
BlocksHandler
├── getCode(opCode, params)
│   → 根据 opCode 返回 Java 代码片段
├── 覆盖类别：
│   ├── View 操作（setText, setImage, setVisibility...）
│   ├── 文件操作（readFile, writeFile, deleteFile...）
│   ├── 网络操作（httpRequest, downloadFile...）
│   ├── 加密/解密（AES, Base64...）
│   ├── JSON 操作（parseJson, createJson...）
│   ├── 日期/时间
│   ├── 数学运算
│   └── 字符串处理
└── 与 CommandBlock.java (21KB) 配合
    └── 提供 Block 的 spec（显示文本）定义
```

## 5.6 EventsHandler / ComponentsHandler

```
EventsHandler (28KB, mod/hilal/saif/events/)
├── 为扩展事件类型生成监听器代码
├── 覆盖：自定义 View 事件、传感器事件、通知事件等
└── getListener(component, event) → 完整的监听器代码块

ComponentsHandler (27KB, mod/hilal/saif/components/)
├── 为扩展组件类型生成代码
├── 补充 ComponentCodeGenerator 不覆盖的组件
└── getComponentCode(component) → 字段/初始化/事件代码

ComponentExtraCode (4KB)
└── 额外的组件辅助代码（如日期格式化方法）
```

## 5.7 LayoutGenerator / ManifestGenerator

### LayoutGenerator（56KB）

将 `ViewBean` 树转换为 Android XML 布局文件：

```
LayoutGenerator
├── 构造器需要：BuildConfig（含 sc_id）+ ProjectFileBean
│   └── 初始化 InjectRootLayoutManager + AppCompatInjection
├── generateXml(List<ViewBean>) → String
│   ├── 根据 ViewBean.type 生成 XML 标签
│   ├── 应用属性（layout_width, text, src...）
│   ├── 处理 @string/@color/@drawable 引用
│   ├── 注入 AppCompat 属性（如 app:srcCompat）
│   └── 注入根布局属性（如 tools:context）
└── 支持的视图类型：50+ 种
```

**重要**：`BuildConfig.sc_id` 必须正确设置，否则 `InjectRootLayoutManager` 和 `AppCompatInjection` 会使用错误的资源路径。

### ManifestGenerator（41KB）

生成 `AndroidManifest.xml`：

```
ManifestGenerator
├── 基本结构（package, versionCode, permissions）
├── Activity 声明（启动器 + 其他 Activity）
├── Service/Receiver 声明
├── Firebase 配置（如有）
├── 权限声明（根据使用的组件自动添加）
└── 支持用户自定义 Manifest 注入（AndroidManifestInjector）
```

## 5.8 ViewBinding 代码生成

```
ViewBindingBuilder.kt (8KB, mod/pranav/viewbinding/)
├── 为每个布局文件生成 ViewBinding 类
├── 生成流程：
│   ├── 解析 XML 布局，提取所有带 id 的 View
│   ├── 生成 XxxBinding.java 类
│   │   ├── 字段声明（public final TextView textView1;）
│   │   ├── inflate() 静态方法
│   │   └── bind() 方法（findViewById 绑定）
│   └── 修改 Activity 代码使用 binding 对象
└── 开关：BuildSettings 中的 ViewBinding 选项
```

## 5.9 GradleFileGenerator

```
GradleFileGenerator (10KB)
├── 生成 app/build.gradle：
│   ├── compileSdk / minSdk / targetSdk
│   ├── applicationId / versionCode / versionName
│   ├── dependencies（根据使用的组件/库添加）
│   │   ├── 内置库（Firebase, AdMob, Glide...）
│   │   └── 本地库（用户添加的 AAR/JAR）
│   ├── Java 版本设置
│   └── ProGuard 配置
└── 生成 settings.gradle / gradle.properties
```

---

# 第六章：构建系统

## 6.1 ProjectBuilder 构建流程

`ProjectBuilder`（54KB）是构建引擎的核心，在后台线程中执行完整的 Android 项目构建：

```
ProjectBuilder.build()
│
├── [阶段 1] 准备阶段
│   ├── 清理临时目录
│   ├── 复制项目资源到构建目录
│   ├── 生成 Java/XML/Manifest/Gradle 源代码（调用第五章的各 Generator）
│   └── 复制用户自定义 Java/Assets/JNI 文件
│
├── [阶段 2] AAPT2 资源编译
│   ├── aapt2 compile → 将 XML/图片/9-patch 编译为 .flat 文件
│   ├── aapt2 link → 链接资源，生成：
│   │   ├── R.java（资源 ID 常量）
│   │   ├── resources.ap_（打包后的资源）
│   │   └── proguard rules（如启用混淆）
│   └── AAPT2 通过 sdklib 库内嵌执行（非外部命令）
│
├── [阶段 3] Java 编译
│   ├── ECJ（Eclipse Compiler for Java）编译 .java → .class
│   ├── classpath 包含：android.jar + 所有依赖库
│   ├── 支持 Java 8-17 源代码级别
│   └── 错误输出重定向到 CompileLogActivity
│
├── [阶段 4] Kotlin 编译（可选）
│   ├── 检测项目中是否有 .kt 文件
│   ├── 使用内嵌 kotlin-compiler 编译
│   └── 输出 .class 文件到同一目录
│
├── [阶段 5] DEX 化
│   ├── D8 → 将 .class 转换为 .dex
│   ├── dexLibraries() → 合并库的 DEX 文件
│   │   ├── 按 64K 方法限制分批
│   │   └── DexMerger 合并多个 .dex
│   └── 支持 MultiDex
│
├── [阶段 6] APK 打包
│   ├── 合并 resources.ap_ + classes.dex
│   ├── 添加 assets/jniLibs
│   └── zipalign 对齐
│
├── [阶段 7] 签名
│   ├── 默认使用 testkey（内嵌）
│   ├── 支持用户自定义 KeyStore
│   └── ZipSigner（kellinwood 库）执行签名
│
└── [阶段 8] 安装
    ├── Root 模式：pm install -r
    └── 非 Root：PackageInstaller Intent
```

## 6.2 增量构建

```
IncrementalBuildCache (7KB)
├── 记录上次成功构建的文件哈希
├── 比对当前文件哈希，跳过未变化的文件
├── 缓存路径：.sketchware/build_cache/{sc_id}/
├── 仅对 AAPT2 compile 阶段生效
│   └── 未变化的资源文件不重新编译
└── 手动清除：DesignActivity → Clean Temp
```

## 6.3 嵌入式 DX/DEX 工具链

项目内嵌了完整的 `dx` 工具链（`mod/agus/jcoderz/dex/` + `dx/`），用于 DEX 文件处理：

```
mod/agus/jcoderz/
├── dex/                    # DEX 文件格式解析器
│   ├── Dex.java (30KB)     #   DEX 文件的内存表示
│   ├── ClassDef.java       #   类定义
│   ├── MethodId.java       #   方法引用
│   ├── FieldId.java        #   字段引用
│   ├── Code.java           #   字节码
│   ├── TableOfContents.java#   文件结构目录
│   └── DexFormat.java      #   DEX 格式常量
│
├── dx/                     # DX 编译器核心
│   ├── cf/                 #   .class 文件解析
│   ├── rop/                #   中间表示（Register Oriented Programming）
│   ├── ssa/                #   SSA 优化
│   ├── dex/                #   DEX 文件写入
│   ├── merge/              #   DexMerger（DEX 合并器）
│   └── command/            #   命令行入口
│
└── multidex/               # MultiDex 支持
    ├── MainDexListBuilder.java    # 主 DEX 类列表构建
    └── ClassReferenceListBuilder.java  # 类引用分析
```

## 6.4 DEX 合并与内存管理

DexMerger 负责将多个库的 DEX 文件合并：

```
dexLibraries() 流程：
1. 收集所有库的 .dex 文件
2. 按 64K 方法数限制分批
3. 每批调用 DexMerger.merge()
4. 输出 classes.dex / classes2.dex / ...
```

**已知内存问题**：
- 峰值内存 ~65-90MB（15 个 DEX 文件）
- 所有 DEX 同时加载到内存（ByteBuffer）
- DexMerger 悲观预分配（1.25×-2× 膨胀因子）
- 已优化：LinkedList O(n²) → O(1) 头部计数

**未来优化方向**：
1. 内存限制批处理（~10 行改动，低风险）
2. 替换为 D8（~200 行改动，高风险但根治）
3. 延迟加载 + 头部预检（~50 行改动，中风险）

## 6.5 Kotlin 编译支持

```
Kotlin 编译流程：
1. 检测 java/ 目录下是否有 .kt 文件
2. 使用内嵌 kotlin-compiler 编译
3. classpath = android.jar + 依赖库 + Java .class 输出
4. 输出 .class 到同一目录
5. 后续与 Java .class 一起 DEX 化
```

## 6.6 ProGuard / R8

```
R8Compiler.kt (1KB, mod/pranav/build/)
├── 使用内嵌 R8 库执行代码混淆/优化
├── 输入：所有 .class 文件 + ProGuard 规则
├── 输出：优化后的 .class（或直接 .dex）
└── 开关：BuildSettings → Enable ProGuard

JarTask.kt (2KB, mod/pranav/build/)
├── 将 .class 文件打包为 JAR
└── 用于中间构建阶段
```

## 6.7 APK 签名

签名使用两套实现：

```
kellinwood/
├── security/zipsigner/     # ZipSigner 主实现
│   ├── 支持 v1 签名（JAR 签名）
│   ├── 使用 PKCS#7 / SHA-256
│   └── 可使用自定义 KeyStore
└── zipio/                  # ZIP I/O 工具
    └── 流式 ZIP 处理（避免全量加载到内存）

mod/alucard/tn/apksigner/  # APK 签名工具
├── 封装 kellinwood ZipSigner
└── 提供简化的 API 调用
```

## 6.8 APK/AAB 导出

```
ExportProjectActivity (41KB)
├── 导出为签名 APK
│   ├── 选择 KeyStore（testkey 或自定义）
│   ├── 构建 Release APK
│   └── 保存到用户指定路径
├── 导出为 AAB（Android App Bundle）
│   ├── 使用 bundletool 库
│   └── 生成 .aab 文件
└── 导出项目源码
    ├── 导出为 ZIP（可在 Android Studio 中打开）
    └── 包含完整的 Gradle 项目结构
```

## 6.9 KeyStore 管理

```
NewKeyStoreActivity (10KB)
├── 创建新的 .keystore 文件
│   ├── 填写：别名、密码、有效期
│   ├── 填写：组织、城市、国家等
│   └── 使用 Java KeyStore API 生成
└── 存储路径：用户自选

KeyStoreManager (6KB)
├── 加载/验证 KeyStore 文件
├── 提取证书信息
└── 用于构建时的签名配置
```

## 6.10 BuiltInLibraries 注册表

```
BuiltInLibraries.java (48KB, mod/jbk/build/)
├── 定义所有内置库的元数据：
│   ├── 库名、版本
│   ├── Maven 坐标
│   ├── 依赖关系
│   ├── 预编译 DEX 路径
│   └── ProGuard 规则
├── 覆盖库类别：
│   ├── Firebase（Auth, Database, Storage, FCM, Analytics...）
│   ├── Google（AdMob, Maps, Auth）
│   ├── AndroidX（AppCompat, RecyclerView, CardView...）
│   ├── 第三方（Glide, OkHttp, Gson, Lottie...）
│   └── Material Design
└── 库的 DEX 文件预存在 assets/ 中
    └── 构建时直接合并，无需重新编译
```

## 6.11 Maven 依赖解析

```
DependencyResolver.kt (25KB, mod/pranav/dependency/resolver/)
├── 从 Maven Central / JitPack / Google 仓库解析依赖
├── 解析流程：
│   ├── 解析 POM 文件获取传递依赖
│   ├── 版本冲突解析（最高版本优先）
│   ├── 下载 AAR/JAR 文件
│   └── 缓存已下载的依赖
├── 支持的仓库：
│   ├── Maven Central
│   ├── Google Maven
│   ├── JitPack
│   └── Sonatype Snapshots
└── 与 LibraryDownloaderDialogFragment 配合
    └── UI 显示下载进度

resolver/ 子模块
├── Kotlin 实现的依赖版本解析器
├── 独立 Gradle 模块（build.gradle.kts）
└── 被主模块通过 implementation project(':resolver') 引用
```

---

# 第七章：扩展系统

Sketchware Pro 的核心竞争力在于其强大的扩展系统，允许用户和开发者添加自定义 Block、组件、事件和视图。

## 7.1 Extra Blocks

```
ExtraBlocks.java (10KB, pro/sketchware/blocks/)
├── 加载用户自定义 Block 定义
├── Block 定义来源：
│   ├── .sketchware/resources/block/My Block/ 目录
│   └── 每个目录包含 block.json（定义）+ palette.json（分类）
├── 缓存机制：
│   ├── customVarTypes: HashSet<String>（缓存自定义变量类型名）
│   └── 正则解析 Block spec 中的 %m.xxx 提取类型
└── hasCustomVariable(typeName) → 判断是否为自定义类型
```

### ExtraPaletteBlock

```
ExtraPaletteBlock.java (dev/aldi/sayuti/block/)
├── 将 Extra Block 填充到逻辑编辑器的调色板
├── setBlock(paletteId, color)
│   ├── 读取对应分类的 block.json
│   ├── 创建 BlockView 实例
│   ├── 使用缓存的 LayoutGenerator（复用 BuildConfig + ProjectFile）
│   └── 添加到 PaletteBlock 容器
├── 特殊分类处理：
│   ├── blockVariables() → 变量操作 Block
│   ├── blockComponents() → 组件操作 Block
│   ├── blockCustomViews() → 自定义视图 Block
│   └── moreBlocks() → 用户定义的 MoreBlock
└── 性能优化：缓存 XML 字符串、文件列表
```

### ExtraBlockFile

```
ExtraBlockFile.java (dev/aldi/sayuti/block/)
├── Block 定义文件的 I/O
├── 读取/写入 block.json 和 palette.json
└── 支持导入/导出 Block 定义包
```

## 7.2 自定义组件

```
ManageCustomComponentActivity (18KB)
├── 管理用户自定义的组件类型
├── 组件定义包含：
│   ├── 组件名称/图标
│   ├── 类全限定名
│   ├── 构造方法参数
│   ├── 可用的事件列表
│   └── 生成的代码模板
├── 导入/导出为 JSON
└── 存储路径：.sketchware/resources/components/

AddCustomComponentActivity (12KB)
├── 添加/编辑单个自定义组件
├── 支持参数类型：String, int, boolean, color, view
└── CollapsibleCustomComponentLayout → 可折叠的组件分类
```

## 7.3 自定义事件

```
EventsManagerFragment (pro/sketchware/fragments/settings/events/)
├── 管理自定义事件定义
├── 事件定义包含：
│   ├── 事件名称
│   ├── 目标组件类型
│   ├── 参数列表（名称+类型）
│   ├── 监听器代码模板
│   └── 关联的 Block 列表
├── EventsManagerCreatorFragment → 创建新事件
└── EventsManagerDetailsFragment → 编辑事件详情
```

## 7.4 AppCompat 注入 + AndroidManifest 注入

### AppCompat 注入

```
AppCompatInjection.java (dev/aldi/sayuti/editor/injection/)
├── 构造器接收 BuildConfig（需要 sc_id）
├── 读取用户定义的 AppCompat 属性
├── 在 LayoutGenerator 生成 XML 时注入额外属性
│   ├── app:srcCompat（替换 android:src）
│   ├── app:tint
│   ├── app:backgroundTint
│   └── 其他 AppCompat 命名空间属性
└── ManageAppCompatActivity → 管理注入规则

InjectRootLayoutManager.java (pro/sketchware/managers/inject/)
├── 构造器使用 SketchwarePaths.getDataPath(sc_id)
├── 为根布局注入额外属性
│   ├── xmlns:app 命名空间声明
│   ├── tools:context
│   └── 自定义根属性
└── 读取配置文件：.sketchware/data/{sc_id}/injection/
```

### AndroidManifest 注入

```
AndroidManifestInjector.java (14KB, mod/hilal/saif/android_manifest/)
├── 在 ManifestGenerator 生成后注入额外 XML 片段
├── 支持注入位置：
│   ├── <application> 标签内
│   ├── <activity> 标签内
│   ├── <manifest> 顶级
│   └── 自定义 Intent Filter
├── 注入内容来源：
│   ├── 用户在 AndroidManifest 编辑器中定义
│   └── 存储在 .sketchware/data/{sc_id}/injection/manifest/
└── ManageAndroidManifestActivity → 可视化编辑注入规则
```

## 7.5 本地库管理

```
ManageLocalLibraryActivity (41KB, dev/aldi/sayuti/editor/manage/)
├── 管理用户添加的本地 AAR/JAR 库
├── 库结构（每个库一个目录）：
│   ├── classes.jar        # Java 类
│   ├── classes.dex        # 预编译 DEX
│   ├── res/               # 资源文件
│   ├── AndroidManifest.xml
│   ├── config              # 库配置（包名、依赖等）
│   └── proguard.txt       # ProGuard 规则
├── 导入方式：
│   ├── 从设备文件系统选择 AAR/JAR
│   ├── 通过 Maven 坐标下载
│   └── 从其他项目复制
└── 存储路径：.sketchware/libs/local_libs/

LibraryDownloaderDialogFragment (21KB)
├── Maven 依赖下载 UI
├── 输入：groupId:artifactId:version
├── 自动解析传递依赖
├── 显示下载进度和大小
└── 下载完成后自动添加到本地库列表

SubDependenciesActivity (8KB)
├── 显示库的传递依赖树
├── 允许用户排除不需要的传递依赖
└── 防止依赖冲突

LocalLibrariesUtil (10KB)
├── 本地库工具类
├── 列举/读取/验证本地库
├── 合并库的资源和 DEX
└── 生成库依赖的 Gradle 配置
```

## 7.6 XML Command 系统

```
ManageXMLCommandActivity (22KB)
├── 管理自定义 XML 命令块
├── 命令定义：
│   ├── 命令名称
│   ├── XML 模板（带占位符）
│   ├── 参数类型和默认值
│   └── 所属调色板分类
├── 生成对应的 Block 和代码模板
└── 存储路径：.sketchware/resources/block/
```

## 7.7 自定义 Widget 创建

```
WidgetsCreatorManager (26KB, pro/sketchware/widgets/)
├── 在 ViewEditor 调色板中注册自定义视图类型
├── Widget 定义：
│   ├── type ID（唯一编号）
│   ├── 显示名称和图标
│   ├── 对应的 Android 类名
│   ├── 可配置属性列表
│   │   ├── 标准属性（width, height, margin, padding）
│   │   └── 自定义属性（通过 CustomAttributeView）
│   └── 默认属性值
├── 注册后可在 ViewEditor 中拖拽使用
├── 代码生成时使用完整类名
└── 导入/导出为 JSON

CustomAttributeView.java (mod/remaker/view/)
└── 自定义属性的编辑视图
```

---

# 第八章：数据格式与存储

## 8.1 项目文件结构

Sketchware 的所有项目数据存储在设备的 `.sketchware/` 目录下：

```
.sketchware/
├── mysc/                           # 项目元数据
│   ├── list/0                      #   所有项目的列表（加密）
│   └── {sc_id}/                    #   单个项目
│       └── project                 #     项目配置（名称、包名、版本等）
│
├── data/                           # 项目数据（核心）
│   └── {sc_id}/
│       ├── view                    #   视图数据（ViewBean JSON，加密）
│       ├── logic                   #   逻辑数据（自定义格式，加密）
│       ├── resource                #   资源引用列表（加密）
│       ├── library                 #   库配置（加密）
│       ├── file                    #   文件列表（Activity/布局定义）
│       └── files/
│           ├── resource/
│           │   ├── images/         #     图片资源
│           │   ├── sounds/         #     音频资源
│           │   ├── fonts/          #     字体资源
│           │   └── values/         #     strings.xml, colors.xml
│           ├── java/               #     用户自定义 Java 文件
│           ├── assets/             #     Assets 文件
│           ├── jniLibs/            #     Native 库
│           └── injection/          #     AppCompat/Manifest 注入配置
│
├── backup/                         # 自动备份
│   └── {sc_id}/
│       ├── view                    #   备份的视图数据
│       └── logic                   #   备份的逻辑数据
│
├── collection/                     # 用户收藏
│   ├── block/                      #   Block Collection
│   ├── widget/                     #   Widget Collection
│   └── moreblock/                  #   MoreBlock Collection
│
├── resources/                      # 扩展资源
│   ├── block/                      #   自定义 Block 定义
│   ├── components/                 #   自定义组件定义
│   ├── widgets/                    #   自定义 Widget 定义
│   └── events/                     #   自定义事件定义
│
├── libs/
│   └── local_libs/                 # 用户添加的本地库
│
└── build_cache/                    # 增量构建缓存
    └── {sc_id}/
```

## 8.2 逻辑数据格式

逻辑数据使用**自定义文本格式**（非 JSON），每个数据段以 `@` 前缀标识：

```
@MainActivity.java_var
0 name            // 类型编号 变量名
1 count

@MainActivity.java_list
0 dataList

@MainActivity.java_func
%b.btnAction onClick  // spec 名称

@MainActivity.java_events
{"targetId":"button1","eventName":"onClick","eventType":2,...}

@MainActivity.java_components
{"componentId":"timer1","type":5,"param1":"","param2":"","param3":""}

@MainActivity.java_onClick_blocks
{"id":"1","opCode":"addSourceDirectly","spec":"...","color":-11893762,...}
```

**变量类型编号**：
| 编号 | 类型 | Java 声明 |
|------|------|-----------|
| 0 | boolean | `boolean name = false;` |
| 1 | int | `int count = 0;` |
| 2 | String | `String text = "";` |
| 3 | Map | `HashMap<String, Object> map = new HashMap<>();` |

## 8.3 视图数据格式

视图数据为 JSON 数组，每个元素为一个 `ViewBean`：

```json
[
  {
    "id": "linear1",
    "type": 0,
    "parent": "root",
    "parentType": 0,
    "index": 0,
    "layout": {
      "width": -1,
      "height": -2,
      "gravity": 0,
      "weight": 0,
      "marginLeft": 0,
      "paddingLeft": 8
    },
    "text": {
      "text": "",
      "textSize": 12,
      "textColor": -16777216,
      "hint": ""
    },
    "image": {
      "resName": ""
    },
    "convert": "LinearLayout"
  }
]
```

**type 编号映射**（部分）：
| type | 视图 | type | 视图 |
|------|------|------|------|
| 0 | LinearLayout | 10 | Spinner |
| 1 | RelativeLayout | 11 | CheckBox |
| 2 | HScrollView | 12 | ScrollView(V) |
| 3 | Button | 13 | Switch |
| 4 | TextView | 14 | SeekBar |
| 5 | EditText | 15 | CalendarView |
| 6 | ImageView | 16 | FAB |
| 7 | WebView | 17 | AdView |
| 8 | ProgressBar | 18 | MapView |
| 9 | ListView | 19+ | 见 ViewBeans.java |

## 8.4 加密与备份

### EncryptedFileUtil

项目数据文件使用**私有加密**存储，防止直接文本编辑破坏数据完整性：

```
EncryptedFileUtil (9KB)
├── encrypt(String data) → byte[]
│   └── AES-128-CBC 加密（固定密钥 "sketchwaresecure"）
├── decrypt(byte[] data) → String
│   └── 对应的解密算法
├── writeEncryptedFile(path, data) → 原子写入
│   ├── 先写入临时文件 (.tmp)
│   ├── 成功后重命名为目标文件
│   └── 失败时保留原文件不变
└── readEncryptedFile(path) → String
```

### 备份机制

```
自动备份触发时机：
├── LogicEditorActivity.onSaveInstanceState() → 异步备份
├── DesignActivity 切换 Tab → 异步备份
└── 定期自动保存（ConfigActivity 开关）

备份恢复：
├── BackupRestoreManager (mod/tyron/backup/)
│   ├── SingleCopyTask.kt → 复制项目到备份目录
│   └── 支持导出为 .swb 文件
└── 恢复时从 backup/ 目录读取
```

## 8.5 Block Collection / Widget Collection

Collection 数据为 JSON 文件，存储完整的 Block 链或 View 树：

```
.sketchware/collection/block/001
├── block.json     # BlockBean 列表（完整的 Block 链快照）
├── info           # 收藏名称和描述
└── screenshot.png # 缩略图

.sketchware/collection/widget/001
├── widget.json    # ViewBean 列表（完整的视图子树）
└── info           # 收藏名称和描述
```

## 8.6 备份与恢复

```
mod/tyron/backup/
├── SingleCopyTask.kt (4KB)
│   ├── 异步复制项目目录到指定路径
│   ├── 支持进度回调
│   └── 递归复制所有子目录和文件
└── CallBackTask.java
    └── 回调接口定义

导出格式：
├── .swb（Sketchware Backup）→ ZIP 压缩的项目完整数据
└── 可通过 ExportProjectActivity 导出
```

---

# 第九章：已知问题与技术债务

## 9.1 已修复的关键问题清单

截至当前版本，已修复 **124** 个 Bug，分多批提交：

### 批次 1-2：51 个修复
- **ExecutorService 泄漏**（12 处）：未关闭的线程池 → 添加 `pool.shutdown()`
- **代码生成模板错误**（6 处 in `ComponentCodeGenerator`）
- **构建系统修复**：`build.gradle` plugins DSL 现代化
- **工具类修复**：`JarCheck`、`FileUtils`、`BackupFactory`、`VectorDrawableLoader`

### 批次 3：Gson 反序列化保护（#52-70）
- 所有 `Gson.fromJson(FileUtil.readFile(...))` 调用添加 try-catch
- 覆盖 19 个类：`IconSelectorDialog`、`ProguardHandler`、`FileResConfig`、`AndroidManifestInjection` 等

### 批次 4：综合修复（#71-109）
- **更多 Gson 保护**：`ExtraBlockFile`、`AppCompatInjection`、`EventsManagerFragment` 等
- **数值解析保护**：
  - `Color.parseColor` → `IllegalArgumentException` 防护
  - `Integer.parseInt` / `Long.parseLong` / `Double.parseDouble` / `Float.parseFloat`
  - `BlocksManager`：5 处 `Double.parseDouble` → 安全 `getPaletteValue()`
  - `PropertyInputItem`：10 处 `Float.parseFloat` → 安全 `safeParseFloat()`
- **空指针防护**：`FileUtil` 10 处 `BitmapFactory.decodeFile` null 检查
- **资源泄漏**：`IconCreatorActivity` FileOutputStream
- **数组越界**：`split()[index]` 模式 3 处修复
- **暗色模式**：搜索事件输入框背景修复

## 9.2 未修复的已知问题

| 问题 | 影响范围 | 原因 |
|------|----------|------|
| **39 处 `getExternalStorageDirectory`** | 全项目 | 需要完整的 Scoped Storage 迁移，工作量大 |
| **511 处静默异常捕获** | 全项目 | `catch (Exception e) {}` 吞掉异常，需逐一评估 |
| **BaseBlockView/NinePatchDecoder Canvas 坐标变量** | 绘图方法 | 来自反编译，与路径数组结构耦合，需完整重写 |
| **废弃布局中的硬编码颜色** | `menu_activity.xml` | 文件未使用，不影响运行 |

## 9.3 性能优化记录

### H 系列（高优先级热路径优化）

| 编号 | 优化内容 | 效果 |
|------|----------|------|
| H1 | `BlockPane.findViewWithTag` → `HashMap` 索引 | 查找 O(n) → O(1) |
| H5 | `onSaveInstanceState` 同步备份 → 异步 | 主线程不再阻塞 |
| H5b | 其余 5 处调用方也改为异步 | 全面消除备份阻塞 |
| H8 | `ViewPane.getXmlString` 重复解析 → 缓存 | 避免每次 XML 解析 |

### P 系列（调色板面板优化）

| 编号 | 优化内容 | 效果 |
|------|----------|------|
| P1 | 自定义变量类型缓存（regex + HashSet） | 避免重复正则解析 |
| P2 | `ExtraPaletteBlock` XML 字符串缓存 | 减少重复磁盘 I/O |
| P3 | `LayoutGenerator` 实例复用 | 避免重复构造 |
| P4 | `FileResConfig` 目录列表缓存 | 减少磁盘 I/O |
| P5a | 防止重复加载 palette blocks | `paletteBlocksInitialized` 标志 |
| P5b | `FileResConfig.getDirFileNames` 缓存 | O(1) 返回缓存结果 |

## 9.4 DEX 合并内存问题

详细分析见第 6.4 节。核心问题：

- **问题 1**（已修复）：LinkedList O(n²) → O(1) 头部计数
- **问题 2**（未修复，低优先级）：内置库与本地库无去重
- **问题 3**（未修复）：DexMerger 峰值内存 65-90MB
- **问题 4**（未修复）：批次内所有 DEX 同时加载

推荐首先实施**内存限制批处理**（~10 行改动，低风险）。

---

# 第十章：开发规范与贡献指南

## 10.1 代码风格与命名规范

### 通用规则
- **缩进**：4 空格（Java/Kotlin/XML）
- **大括号**：K&R 风格（左大括号不换行）
- **行宽**：无硬性限制，但建议 120 字符以内
- **导入**：不使用通配符导入（`import java.util.*`）

### 命名规范

| 元素 | 风格 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `LogicEditorActivity` |
| 方法名 | lowerCamelCase | `generateCode()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_HISTORY_STEPS` |
| 字段 | lowerCamelCase | `blockIndex` |
| XML ID | snake_case | `button_submit` |
| 布局文件 | snake_case | `activity_main.xml` |
| 字符串资源 | snake_case + 模块前缀 | `common_message_save` |

### 反编译代码

`com.besome.sketch` 包下的代码来自原版 Sketchware 的反编译，部分仍保留原始混淆名：
- **已完成的反混淆**：参数/局部变量级别的 `[a-z][0-9]` 模式已全部重命名
- **保留未改名**：`NinePatchDecoder.buildNinePatchChunk`（寄存器复用，需完整重写）、`BaseBlockView` 绘图方法中的 Canvas 坐标变量

## 10.2 Git 提交规范

使用 **Conventional Commits** 格式：

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### 类型（type）

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `perf` | 性能优化 |
| `refactor` | 代码重构（不改变行为） |
| `ui` | UI/UX 改进 |
| `chore` | 构建/工具变更 |
| `docs` | 文档更新 |

### 示例

```
fix: ensure LayoutGenerator BuildConfig uses correct sc_id

perf: replace findViewWithTag with HashMap index in BlockPane (H1)

feat(blocks): add custom variable type caching in ExtraBlocks

ui: fix RTL in common_dialog_layout (padding, margin, gravity)
```

## 10.3 审查清单

提交代码前，请检查以下项目：

### 安全性
- [ ] `Gson.fromJson()` 调用是否有 try-catch 保护？
- [ ] 数值解析（`parseInt`、`parseFloat`、`parseColor`）是否有异常处理？
- [ ] `File.listFiles()` 返回值是否检查了 null？
- [ ] `BitmapFactory.decodeFile()` 返回值是否检查了 null？
- [ ] `split()[index]` 是否检查了数组长度？

### 资源管理
- [ ] `ExecutorService` 是否调用了 `shutdown()`？
- [ ] `FileOutputStream` / `FileInputStream` 是否在 finally 中关闭？
- [ ] 是否使用了 try-with-resources 或适当的资源释放？

### 线程安全
- [ ] 共享状态的访问是否有适当的同步？
- [ ] 后台线程的 UI 更新是否通过 `runOnUiThread()` 执行？
- [ ] `CompletableFuture` 的错误是否被正确处理？

### UI/UX
- [ ] 是否支持 RTL 布局？（使用 `Start`/`End` 替代 `Left`/`Right`）
- [ ] 是否适配暗色模式？（使用主题颜色而非硬编码）
- [ ] 用户可见文本是否使用 `Helper.getResString(R.string.xxx)`？

### 性能
- [ ] 是否存在主线程上的磁盘 I/O？
- [ ] 是否有可以缓存的重复计算？
- [ ] 大列表操作是否考虑了 O(n) 复杂度？

## 10.4 国际化指南

### 核心规则

1. **必须使用** `Helper.getResString(R.string.xxx)` 获取本地化文本
2. **禁止使用** `context.getString(R.string.xxx)`（绕过语言覆盖机制）
3. **禁止硬编码**用户可见文本
4. 字符串资源 key 使用 `模块_功能_描述` 格式

### 示例

```java
// ✅ 正确
SketchToast.toast(context, 
    Helper.getResString(R.string.common_message_complete_save),
    SketchToast.TOAST_NORMAL).show();

// ❌ 错误 - 硬编码
Toast.makeText(context, "Save completed", Toast.LENGTH_SHORT).show();

// ❌ 错误 - 使用 context.getString
Toast.makeText(context, 
    context.getString(R.string.common_message_complete_save),
    Toast.LENGTH_SHORT).show();
```

### 语言覆盖机制详解

```
用户选择语言
  └── LanguageSettingsFragment
      └── AppCompatDelegate.setApplicationLocales(localeList)
          └── SketchApplication.getContext()
              ├── 优先返回 currentActivity（已应用 Locale）
              └── 回退：创建 ConfigurationContext（缓存 Locale）
                  └── Helper.getResString() 使用此 Context 获取字符串
```

## 10.5 常见开发模式

### 模式 1：缓存失效

```java
// 适用场景：磁盘数据的内存缓存
private HashMap<String, String> cache;
private String cacheKey;

public String getCachedValue(String key) {
    if (cache == null || !currentKey.equals(cacheKey)) {
        cache = new HashMap<>();
        cacheKey = currentKey;
        // ... 加载数据到 cache
    }
    return cache.get(key);
}

public void invalidateCache() {
    cache = null;
}
```

### 模式 2：异步 I/O

```java
// 适用场景：后台保存，避免阻塞主线程
CompletableFuture.runAsync(() -> {
    synchronized (dataStore) {
        dataStore.saveAllBackup();
    }
});
```

### 模式 3：安全数值解析

```java
// 适用场景：用户输入或外部数据的数值解析
private float safeParseFloat(String value, float defaultValue) {
    try {
        return Float.parseFloat(value);
    } catch (NumberFormatException e) {
        return defaultValue;
    }
}
```

### 模式 4：Gson 安全反序列化

```java
// 适用场景：从文件读取 JSON 数据
try {
    List<MyBean> list = new Gson().fromJson(
        FileUtil.readFile(path),
        new TypeToken<List<MyBean>>(){}.getType()
    );
    if (list != null) {
        // 使用 list
    }
} catch (Exception e) {
    // JSON 解析失败，使用默认值
    list = new ArrayList<>();
}
```

### 模式 5：原子文件写入

```java
// 适用场景：防止写入中断导致数据损坏
File tmpFile = new File(targetPath + ".tmp");
// ... 写入 tmpFile
if (tmpFile.renameTo(new File(targetPath))) {
    // 成功
} else {
    // 回退处理
}
```

---

# 附录

## A. 关键文件大小排名（Top 15）

| 排名 | 文件 | 大小 | 模块 |
|------|------|------|------|
| 1 | `LogicEditorActivity.java` | 132 KB | 逻辑编辑器 |
| 2 | `BlocksHandler.java` | 122 KB | 扩展代码生成 |
| 3 | `ComponentTemplates.java` | 98 KB | 组件模板 |
| 4 | `ViewPane.java` | 77 KB | 视图画布 |
| 5 | `DesignActivity.java` | 73 KB | 项目编辑主界面 |
| 6 | `BlockCodeRegistry.java` | 73 KB | Block 代码注册表 |
| 7 | `ProjectFilePaths.java` | 70 KB | 文件路径定义 |
| 8 | `BlockSpecRegistry.java` | 62 KB | Block 规格注册表 |
| 9 | `ProjectDataStore.java` | 58 KB | 数据存储 |
| 10 | `ActivityCodeGenerator.java` | 57 KB | Activity 代码生成 |
| 11 | `LayoutGenerator.java` | 56 KB | XML 布局生成 |
| 12 | `ProjectBuilder.java` | 54 KB | 构建引擎 |
| 13 | `ViewEditor.java` | 52 KB | 视图编辑器 |
| 14 | `ManageEvent.java` | 52 KB | 事件模板 |
| 15 | `ManageCollectionActivity.java` | 49 KB | Collection 管理 |

## B. 项目统计

| 指标 | 值 |
|------|------|
| Java 文件数 | ~350+ |
| Kotlin 文件数 | ~10 |
| XML 布局文件数 | ~200+ |
| 字符串资源数 | ~3370+ |
| 内置组件类型 | 40+ |
| 内置 Block 类型 | 1500+ |
| 内置事件类型 | 200+ |

## C. Block Spec 格式规范

Block 的显示文本（spec）使用特殊占位符定义参数类型和交互方式：

### 参数占位符

| 占位符 | 含义 | 用户交互 | 生成代码中的类型 |
|--------|------|----------|------------------|
| `%s` | 字符串参数 | 文本输入框 / 可嵌套字符串表达式 Block | `String` |
| `%s.inputOnly` | 纯文本输入 | 仅文本输入框（不接受嵌套 Block） | `String` |
| `%d` | 数值参数 | 数字输入框 / 可嵌套数值表达式 Block | `int` / `double` |
| `%d.v` | 变量名（数值型） | 文本输入框 | 变量名 |
| `%b` | 布尔参数 | 可嵌套布尔表达式 Block | `boolean` |
| `%m.xxx` | 菜单/下拉选择 | 下拉菜单或组件选择器 | 取决于具体类型 |

### `%m.xxx` 菜单类型详解

| 菜单类型 | 含义 | 用户选择 |
|----------|------|----------|
| `%m.view` | View 组件 | 当前布局中的所有 View ID |
| `%m.varInt` | int 变量 | 已定义的 int 变量列表 |
| `%m.varStr` | String 变量 | 已定义的 String 变量列表 |
| `%m.varBool` | boolean 变量 | 已定义的 boolean 变量列表 |
| `%m.varMap` | Map 变量 | 已定义的 Map 变量列表 |
| `%m.list` | 任意列表 | 已定义的列表变量 |
| `%m.listInt` | int 列表 | 已定义的 int 列表变量 |
| `%m.listStr` | String 列表 | 已定义的 String 列表变量 |
| `%m.listMap` | Map 列表 | 已定义的 Map 列表变量 |
| `%m.intent` | Intent 组件 | 已添加的 Intent 组件 |
| `%m.dialog` | Dialog 组件 | 已添加的 Dialog 组件 |
| `%m.timer` | Timer 组件 | 已添加的 Timer 组件 |
| `%m.firebase` | Firebase 组件 | 已添加的 Firebase 组件 |
| `%m.firebaseauth` | FirebaseAuth 组件 | 已添加的 FirebaseAuth 组件 |
| `%m.firebasestorage` | FirebaseStorage 组件 | 已添加的 FirebaseStorage 组件 |
| `%m.camera` | Camera 组件 | 已添加的 Camera 组件 |
| `%m.filepicker` | FilePicker 组件 | 已添加的 FilePicker 组件 |
| `%m.calendar` | Calendar 组件 | 已添加的 Calendar 组件 |
| `%m.adview` | AdView 组件 | 已添加的 AdView 组件 |
| `%m.requestnetwork` | RequestNetwork 组件 | 已添加的 RequestNetwork 组件 |
| `%m.bluetoothconnect` | BluetoothConnect 组件 | 已添加的 BluetoothConnect 组件 |
| `%m.activity` | Activity 选择 | 项目中的 Activity 列表 |
| `%m.resource` | 资源名 | 项目图片/音频/字体资源 |
| `%m.color` | 颜色值 | 颜色选择器 |
| `%m.visibility` | 可见性 | VISIBLE / INVISIBLE / GONE |
| `%m.calendarField` | Calendar 字段 | YEAR / MONTH / DAY_OF_MONTH 等 |

### Block 类型（blockType）

| 类型值 | 名称 | 形状 | 用途 |
|--------|------|------|------|
| `" "` (空格) | 语句块 | 带凹槽的矩形 | 执行操作（如 `setText`） |
| `"b"` | 布尔表达式 | 六边形 | 返回 true/false（如 `=`, `<`） |
| `"d"` | 数值表达式 | 圆角矩形 | 返回数值（如 `+`, `lengthList`） |
| `"s"` | 字符串表达式 | 圆角矩形 | 返回字符串（如 `getText`, `stringJoin`） |
| `"c"` | C 形块 | 带子栈的 C 形 | 包含子 Block（如 `if`, `repeat`） |
| `"e"` | E 形块 | 带两个子栈的 E 形 | if-else 结构 |
| `"f"` | 结束块 | 无下凹槽 | 终止链（如 `break`, `return`） |
| `"h"` | 帽子块 | 顶部圆弧 | 事件入口（如 `onEvent`） |
| `"v"` | View 类型 | 圆角矩形 | 返回 View（用于自定义视图） |
| `"p"` | 组件类型 | 圆角矩形 | 返回组件引用 |
| `"l"` | 列表类型 | 圆角矩形 | 返回列表引用 |

### Block 颜色分配

Block 颜色由 `BlockColorMapper` 按 opCode 类别分配（使用 Material You 和谐色）：

| 颜色（十六进制） | 类别 |
|------------------|------|
| `#4a6cd4` | View 操作 |
| `#ee7d16` | 变量操作 |
| `#cc5b22` | 列表操作 |
| `#e1a92a` | 控制流（if/repeat/forever） |
| `#5cb722` | 运算符/字符串 |
| `#23b9a9` | 数学运算 |
| `#c88330` | 帽子块（事件入口） |
| `#2ca5e2` | 组件引用 |

## D. 实战教程

### 教程 1：添加一个新的内置 Block

**目标**：添加一个 `showSnackbar` Block，在 View 上显示 Snackbar。

**步骤 1**：在 `BlockSpecRegistry.java` 注册 spec：
```java
// initBlockSpecs() 方法中
BLOCK_SPECS.put("showSnackbar", "show Snackbar on %m.view message %s duration %m.snackbarDuration");

// initBlockParams() 方法中
BLOCK_PARAMS.put("showSnackbar", new String[]{"%m.view", "%s", "%m.snackbarDuration"});
```

**步骤 2**：在 `BlockCodeRegistry.java` 注册代码模板：
```java
// initBlockCode() 方法中
BLOCK_CODE.put("showSnackbar",
    "com.google.android.material.snackbar.Snackbar.make(%s, %s, %s).show()");
```

**步骤 3**：在 `BlockColorMapper.java` 添加颜色（可选，默认颜色即可）：
```java
// 在 View 操作 case 列表中添加
case "showSnackbar" -> harmonizeWithPrimary(context, 0xff4a6cd4);
```

**步骤 4**：在 `BlockConstants.java` 添加下拉选项（如需要）：
```java
public static final String[] SNACKBAR_DURATION = {
    "LENGTH_SHORT", "LENGTH_LONG", "LENGTH_INDEFINITE"
};
```

**步骤 5**：在调色板中显示该 Block — 修改 `LogicEditorActivity` 中对应分类的 Block 填充逻辑。

### 教程 2：添加一个新的组件类型

**目标**：添加一个 `CountDownTimer` 组件。

**步骤 1**：在 `ComponentTypeMapper.java` 注册类型：
```java
// 添加类型编号（选择一个未使用的编号，如 50）
case 50 -> "CountDownTimer";
```

**步骤 2**：在 `ComponentTemplates.java` 定义代码模板：
```java
// 字段声明
"private CountDownTimer %s;"

// onCreate 初始化
"%s = new CountDownTimer(%s, %s) {\n" +
"    @Override\n" +
"    public void onTick(long millisUntilFinished) {\n" +
"        %s\n" +
"    }\n" +
"    @Override\n" +
"    public void onFinish() {\n" +
"        %s\n" +
"    }\n" +
"};"
```

**步骤 3**：在 `ComponentCodeGenerator.java` 注册代码生成逻辑。

**步骤 4**：在 `EventRegistry.java` 或 `ManageEvent.java` 中定义关联事件（`onTick`, `onFinish`）。

**步骤 5**：添加对应的 Block（用于 `start()`, `cancel()` 等操作）。

### 教程 3：添加自定义 Manifest 注入

**步骤**：
1. 在 `.sketchware/data/{sc_id}/injection/manifest/` 下创建配置文件
2. 通过 `AndroidManifestInjector.java` 在 Manifest 生成后注入
3. 可通过 `ManageAndroidManifestActivity` 可视化配置

## E. 调试技巧

### 调试 Sketchware Pro 自身

```
1. 使用 Android Studio 直接运行 Debug 变体
   → Run → Debug 'app'
   → 设置断点在感兴趣的代码处

2. 常见调试入口点：
   ├── LogicEditorActivity.onCreate()     → 逻辑编辑器初始化
   ├── ProjectBuilder.build()             → 构建流程
   ├── ActivityCodeGenerator.generate()   → 代码生成
   ├── ViewPane.addView(ViewBean)         → 视图渲染
   └── ProjectDataStore.saveAllData()     → 数据保存

3. 使用 Logcat 过滤：
   → 包名：pro.sketchware
   → 标签：SketchwarePro（如有 Log.d 调用）
```

### 调试用户项目的构建问题

```
1. 查看编译错误日志：
   → DesignActivity → 底部菜单 → Show Last Error
   → 或查看 CompileLogActivity 显示的日志

2. 查看生成的源代码：
   → DesignActivity → 底部菜单 → Show Source
   → 检查生成的 Java 代码是否正确

3. 常见构建错误排查：
   ├── "cannot find symbol" → 检查组件/变量是否正确声明
   ├── "method does not override" → 检查事件模板参数
   ├── AAPT2 error → 检查 XML 资源格式
   └── DexMerger OOM → 减少库数量或增大堆内存
```

### 调试 Block 代码生成

```java
// 在 BlockInterpreter 中添加临时日志
Log.d("BlockDebug", "opCode=" + block.opCode +
      ", params=" + Arrays.toString(block.parameters.toArray()) +
      ", generated=" + generatedCode);
```

## F. `mod/` 贡献者索引

| 包路径 | 贡献者 | 主要贡献 |
|--------|--------|----------|
| `mod/agus/jcoderz/` | Agus Jcoderz | DEX/DX 工具链、事件模板（ManageEvent 52KB）、Block/权限/资源管理 UI |
| `mod/hilal/saif/` | Hilal Saif | **BlocksHandler（122KB）**、ComponentsHandler、EventsHandler、AppSettings、BlocksManager |
| `mod/hey/studios/` | Hey Studios | 源代码编辑器（SrcCodeEditor）、MoreBlock 系统、构建设置、文件管理器（Assets/Java/NativeLib） |
| `mod/jbk/` | JBK | **BuiltInLibraries（48KB）**、编译器（AAPT2/D8/资源）、Block 导入器 |
| `mod/pranav/` | Pranav | ViewBinding（Kotlin）、R8 编译器、**DependencyResolver（25KB）** |
| `mod/khaled/` | Khaled | Logcat 查看器（LogReaderActivity） |
| `mod/tyron/` | Tyron | 备份/恢复系统（SingleCopyTask） |
| `mod/bobur/` | Bobur | VectorDrawable 解析器 |
| `mod/remaker/` | Remaker | 自定义属性视图（CustomAttributeView） |
| `mod/alucard/` | Alucard | APK 签名工具 |
| `dev/aldi/sayuti/` | Aldi Sayuti | ExtraPaletteBlock、AppCompat/Manifest 注入编辑器、本地库管理 |

## G. CI/CD 工作流

项目使用 **GitHub Actions** 实现自动化构建和分发：

### 工作流文件：`.github/workflows/android.yml`

```
触发条件：
├── push 到非 wip/** 分支
├── 路径匹配：app/**, gradle/**, build.gradle, settings.gradle 等
└── 手动触发（workflow_dispatch）

Job 1: notifyTelegram
├── 通知 Telegram 群组有新 push
└── 使用 notify_telegram.py

Job 2: build
├── 设置 JDK 17 (Temurin)
├── 注入 GOOGLE_SERVICES_JSON（从 Secrets）
├── 运行 gradle assembleRelease
├── 重命名 APK（附加 commit hash）
└── 上传为 GitHub Artifact

Job 3: aggregateAndSend（依赖 build）
├── 下载构建产物
├── 通过 Telethon 发送 APK 到 Telegram 群组
└── 包含 commit 作者和消息

Job 4: updateAppData
├── 运行 update_app_data.py
├── 更新 about.json（应用关于页数据）
└── 推送到 host 分支
```

### Secrets 配置

| Secret 名称 | 用途 |
|-------------|------|
| `GOOGLE_SERVICES_JSON` | Firebase 配置文件内容 |
| `SKETCHUB_API_KEY` | SketchHub API 密钥 |
| `TELEGRAM_BOT_TOKEN` | Telegram 机器人 Token |
| `TELEGRAM_CHAT_ID` | Telegram 群组 ID |
| `TELEGRAM_TOPIC_ID` | Telegram 话题 ID |
| `TELEGRAM_API_ID` | Telegram API ID（Telethon） |
| `TELEGRAM_API_HASH` | Telegram API Hash（Telethon） |
| `GH_ABOUT_APP_WORKFLOW_TOKEN` | GitHub Token（推送 host 分支） |

### Issue 模板

项目配置了结构化 Issue 模板：
- `bug_report.yml`：Bug 报告（设备信息、复现步骤、期望行为）
- `feature_request.yml`：功能请求（描述、用例、替代方案）

## H. 术语表（Glossary）

| 术语 | 含义 | 出现位置 |
|------|------|----------|
| **sc_id** | 项目唯一标识符（如 `"601"`），用于构造所有项目相关路径（`.sketchware/data/{sc_id}/`） | 全项目 |
| **opCode** | Block 操作码（如 `"setVarInt"`、`"doToast"`），是 Block 的唯一标识，关联 spec 和代码模板 | `BlockCodeRegistry` / `BlockSpecRegistry` |
| **spec** | Block 规格字符串（如 `"set %d.v to %d"`），定义 Block 的显示文本和参数插槽 | `BlockSpecRegistry` / `BlockBean.spec` |
| **BlockBean** | Block 的数据模型，序列化为 JSON 存入 logic 文件。包含 `id`、`opCode`、`spec`、`color`、`nextBlock`、`subStack1/2`、`parameters` | `com.besome.sketch.beans` |
| **ViewBean** | 视图组件的数据模型，包含 `id`、`type`、`parent`、`layout`、`text`、`image` 等属性 | `com.besome.sketch.beans` |
| **EventBean** | 事件的数据模型，包含 `targetId`、`eventName`、`eventType`、参数列表 | `com.besome.sketch.beans` |
| **ComponentBean** | 非可视化组件的数据模型，包含 `componentId`、`type`（编号）、`param1/2/3` | `com.besome.sketch.beans` |
| **ProjectFileBean** | Activity/布局文件的描述，包含 `fileName`、`fileType`、`orientation` 等 | `com.besome.sketch.beans` |
| **MoreBlock** | 用户自定义的可复用函数，在逻辑编辑器中以"define"帽子块定义，在调色板中显示为可调用 Block | 第 4.2 节 / 附录 I |
| **Collection** | 用户保存的 Block 链 / Widget 子树 / MoreBlock 的可复用快照 | 第 4.10 节 |
| **Extra Block** | 社区扩展的自定义 Block，定义在 `.sketchware/resources/block/` 目录下的 JSON 文件中 | 第 7.1 节 |
| **palette** | 逻辑编辑器左侧的 Block 分类面板（如"变量"、"控制"、"运算"等） | `PaletteSelector` / `PaletteBlock` |
| **BlockPane** | 逻辑编辑器的 Block 工作区画布，用户在此拼接 Block | `com.besome.sketch.editor.logic` |
| **ViewPane** | 视图编辑器的可视化画布，将 ViewBean 渲染为真实 Android 视图 | `com.besome.sketch.editor.view` |
| **ViewDummy** | 拖拽过程中的半透明占位指示器 | `com.besome.sketch.editor.view` |
| **BuildConfig** | 项目构建配置（非 Android 的 `BuildConfig`），包含 `sc_id`、包名、版本号等字段 | `pro.sketchware.core` |
| **ProjectDataStore** | 项目数据的内存仓库，所有编辑器通过 `ProjectDataManager` 获取同一实例 | `pro.sketchware.core` |
| **EncryptedFileUtil** | 项目数据文件的加密/解密工具，使用自定义字节移位算法 | `pro.sketchware.core` |
| **LayoutGenerator** | 将 ViewBean 树转换为 Android XML 布局文件的生成器 | `pro.sketchware.core` |
| **BlockInterpreter** | 将 BlockBean 链转换为 Java 代码片段的解释器 | `pro.sketchware.core` |
| **inject / injection** | 在生成的 XML/Manifest 中注入额外属性或标签（AppCompat 属性、Manifest 标签等） | `dev.aldi.sayuti.editor.injection` |
| **DEX** | Dalvik Executable，Android 的字节码格式。`.class` 文件需经 D8/DX 转换为 `.dex` 才能在 Android 上运行 | 第 6 章 |
| **DexMerger** | 将多个 `.dex` 文件合并为一个的工具（解决 64K 方法限制） | `mod.agus.jcoderz.dx.merge` |
| **ECJ** | Eclipse Compiler for Java，项目内嵌的 Java 编译器，替代 javac | 第 6.1 节 |
| **AAPT2** | Android Asset Packaging Tool 2，编译和链接 Android 资源 | 第 6.1 节 |
| **testkey** | 项目内置的签名密钥（`testkey.keystore`），用于 Debug 和 Release 签名 | 第 2.3 节 |

## I. MoreBlock 机制详解

MoreBlock 是用户创建的**自定义可复用函数**，是 Sketchware 中最强大的用户级特性之一。

### MoreBlock Spec 格式

MoreBlock 的 spec 定义了函数名、参数和返回类型：

```
格式：funcName [returnType] %参数类型.参数名 %参数类型.参数名 ...

示例：
├── calculateSum %d.a %d.b              → void calculateSum(double a, double b)
├── getMessage %s.name %d.age           → void getMessage(String name, double age)
├── isValid[boolean] %s.input           → boolean isValid(String input)
├── getData[String] %s.key              → String getData(String key)
└── getMap[a|Map] %s.key %s.value       → HashMap<String,Object> getMap(String key, String value)
```

### 返回类型编码

返回类型通过方括号 `[type]` 附加在函数名后：

| spec 中的标记 | 返回类型（Java） | Block 形状 |
|---------------|-----------------|-----------|
| 无 `[]` | `void` | 语句块（矩形） |
| `[String]` | `String` | 字符串表达式（圆角） |
| `[double]` | `double` | 数值表达式（圆角） |
| `[boolean]` | `boolean` | 布尔表达式（六边形） |
| `[a\|Map]` | `HashMap<String, Object>` | 表达式（圆角） |
| `[l\|List String]` | `ArrayList<String>` | 表达式（圆角） |
| `[l\|List Map]` | `ArrayList<HashMap<String, Object>>` | 表达式（圆角） |
| `[v\|View]` | `View` | View 类型（圆角） |

### 参数类型

| spec 占位符 | Java 参数类型 |
|-------------|--------------|
| `%d.paramName` | `double` |
| `%s.paramName` | `String` |
| `%b.paramName` | `boolean` |

### 代码生成

MoreBlock 在代码生成时：

```java
// 定义端 → 生成方法声明
public void calculateSum(double _a, double _b) {
    // 用户在 define 块内放置的 Block 生成的代码
}

// 带返回值的 MoreBlock
public String getData(String _key) {
    // ...
    return _result;  // 自动插入 return 语句
}

// 调用端 → 生成方法调用
calculateSum(10, 20);
String result = getData("name");
```

### 关键实现类

```
ReturnMoreblockManager (7KB, mod/hey/studios/moreblock/)
├── getMbType(spec) → 提取返回类型标记（如 "String", "a|Map"）
├── getMbTypeCode(spec) → 转换为 Java 类型（如 "HashMap<String, Object>"）
├── getMbName(spec) → 提取函数名（去掉 [type] 部分）
├── getMoreblockChar(spec) → 返回类型字符（"s"/"d"/"b"/" "）
├── injectMbType(name, mbName, typeChar) → 重新组合完整 spec
└── listMoreblocks(iterator, activity) → 在调色板中列出所有 MoreBlock

MoreblockValidator (4KB)
├── 验证 MoreBlock 名称合法性
├── 检查：保留关键字、重复名称、事件名冲突
├── 名称规则：字母开头、仅字母数字下划线、≤60 字符
└── 支持返回类型后缀：funcName[type]
```

### MoreBlock 数据存储

```
逻辑数据文件中：
@MainActivity.java_func
%b.btnAction onClick        # spec: %b = boolean 参数, .btnAction = 参数名
calculateSum %d.a %d.b      # void 函数，两个 double 参数
getData[String] %s.key       # 返回 String 的函数

@MainActivity.java_calculateSum_moreBlock_blocks
{"id":"1","opCode":"...","spec":"...","parameters":[...]}
# 函数体的 Block 数据
```

## J. 快速查找表

### "我想做 X → 看文件 Y"

| 我想做的事 | 关键文件 | 章节 |
|-----------|----------|------|
| **了解项目整体架构** | 本手册第 1 章 | 1.3-1.4 |
| **搭建开发环境** | `build.gradle`, `gradle.properties` | 2.1-2.6 |
| **理解应用启动流程** | `SketchApplication.java` | 3.1 |
| **理解项目编辑主界面** | `DesignActivity.java` (73KB) | 3.2 |
| **修改视图编辑器** | `ViewEditor.java` (52KB), `ViewPane.java` (77KB) | 4.1 |
| **修改逻辑编辑器** | `LogicEditorActivity.java` (132KB) | 4.2 |
| **修改 Block 拖拽/吸附逻辑** | `BlockPane.java` (23KB) | 4.2 |
| **修改 Block 外观/渲染** | `BaseBlockView.java` (23KB), `BlockView.java` (23KB) | 4.2 |
| **修改调色板面板** | `PaletteSelector.java`, `PaletteBlock.java`, `ExtraPaletteBlock.java` | 4.2 |
| **添加新的内置 Block** | `BlockSpecRegistry.java`, `BlockCodeRegistry.java`, `BlockColorMapper.java` | 5.3, 附录 D |
| **添加新的组件类型** | `ComponentTypeMapper.java`, `ComponentTemplates.java`, `ComponentCodeGenerator.java` | 4.4, 附录 D |
| **添加新的事件类型** | `EventRegistry.java`, `ManageEvent.java` (52KB) | 4.3 |
| **修改 Activity 代码生成** | `ActivityCodeGenerator.java` (57KB) | 5.1 |
| **修改 XML 布局生成** | `LayoutGenerator.java` (56KB) | 5.7 |
| **修改 Manifest 生成** | `ManifestGenerator.java` (41KB) | 5.7 |
| **修改构建流程** | `ProjectBuilder.java` (54KB) | 6.1 |
| **修改 DEX 合并** | `mod/agus/jcoderz/dx/merge/DexMerger.java` | 6.4 |
| **修改 Kotlin 编译** | `mod/pranav/build/` | 6.5 |
| **修改 R8 混淆** | `mod/pranav/build/R8Compiler.kt` | 6.6 |
| **修改 APK 签名** | `kellinwood/security/`, `mod/alucard/tn/apksigner/` | 6.7 |
| **修改 APK 导出** | `ExportProjectActivity.java` (41KB) | 6.8 |
| **添加/修改扩展 Block 代码生成** | `BlocksHandler.java` (122KB, mod/hilal/saif/) | 5.5 |
| **添加/修改扩展事件代码** | `EventsHandler.java` (28KB, mod/hilal/saif/) | 5.6 |
| **添加/修改扩展组件代码** | `ComponentsHandler.java` (27KB, mod/hilal/saif/) | 5.6 |
| **修改内置库定义** | `BuiltInLibraries.java` (48KB, mod/jbk/) | 6.10 |
| **修改 Maven 依赖解析** | `DependencyResolver.kt` (25KB, mod/pranav/) | 6.11 |
| **修改本地库管理** | `ManageLocalLibraryActivity.java` (41KB) | 7.5 |
| **修改 AppCompat 注入** | `AppCompatInjection.java`, `InjectRootLayoutManager.java` | 7.4 |
| **修改 Manifest 注入** | `AndroidManifestInjector.java` (14KB) | 7.4 |
| **修改 ViewBinding 生成** | `ViewBindingBuilder.kt` (8KB) | 5.8 |
| **修改项目数据序列化** | `ProjectDataStore.java` (58KB), `ProjectDataParser.java` | 4.5 |
| **修改资源管理** | `ResourceManager.java` (19KB), `FileResConfig.java` | 4.6 |
| **修改暗色模式** | `ThemeManager.java` (pro/sketchware/utility/theme/) | 3.5 |
| **修改语言覆盖** | `LanguageOverrideManager.java`, `LanguageSettingsFragment.java` | 3.6 |
| **修改崩溃收集** | `CollectErrorActivity.java`, `SketchApplication.java` | 3.7 |
| **修改源码编辑器** | `SrcCodeEditor.java` (25KB) | 4.8 |
| **修改 Logcat 查看器** | `LogReaderActivity.java` (18KB) | 4.9 |
| **修改 Collection 管理** | `ManageCollectionActivity.java` (49KB) | 4.10 |
| **修改自定义 Widget** | `WidgetsCreatorManager.java` (26KB) | 4.12 |
| **修改备份/恢复** | `mod/tyron/backup/SingleCopyTask.kt` | 8.6 |
| **了解 Block spec 格式** | 本手册附录 C | 附录 C |
| **了解 MoreBlock 机制** | 本手册附录 I | 附录 I |
| **了解数据文件格式** | 本手册第 8 章 | 8.1-8.5 |
| **查看审查清单** | 本手册 10.3 节 | 10.3 |

---

## 附录 K：`pro.sketchware.core` 文件分类索引

> 该包含 120+ 文件，以下按职责分类。每个类的详细说明见源码 Javadoc。

### K.1 Block 系统
| 文件 | 职责 |
|---|---|
| `BaseBlockView` | Block 视图基类（绘制、测量、路径） |
| `BlockView` | 普通 Block 视图（拼接、子栈、参数） |
| `DefinitionBlockView` | MoreBlock 定义头部视图 |
| `FieldBlockView` | Block 内参数输入框视图 |
| `BlockInterpreter` | Block→Java 代码转换引擎 |
| `BlockCodeRegistry` / `BlockCodeHandler` | Block opCode→代码生成处理器注册表 |
| `BlockSpecRegistry` | Block/Event opCode→spec/参数类型映射 |
| `BlockColorMapper` | Block opCode→Material You 颜色映射 |
| `BlockHistoryManager` | Block 编辑 undo/redo 历史 |
| `BlockCollectionManager` | Block 收藏管理 |
| `BlockConstants` | Block 相关常量（保留关键字等） |
| `BlockSizeListener` | Block 尺寸变化回调接口 |

### K.2 代码生成
| 文件 | 职责 |
|---|---|
| `ActivityCodeGenerator` | Activity Java 源文件完整生成 |
| `ComponentCodeGenerator` | 组件字段声明、初始化、onActivityResult 代码 |
| `ComponentTypeMapper` | 组件类型→Java 类名/import 映射 |
| `ComponentTemplates` | 组件初始化代码模板（98KB，最大文件） |
| `EventCodeGenerator` | 事件监听器代码生成 |
| `EventCodeRegistry` / `EventCodeHandler` | 事件名→代码生成处理器注册表 |
| `ListenerCodeRegistry` | Listener 代码片段注册表 |
| `EventRegistry` | 事件定义注册表 |
| `LayoutGenerator` | ViewBean 树→XML 布局文件生成 |
| `ManifestGenerator` | AndroidManifest.xml 生成 |
| `GradleFileGenerator` | build.gradle / settings.gradle 生成 |
| `CodeContext` | 代码生成上下文（包名、绑定模式等） |
| `CodeFormatter` | Java 代码格式化（缩进、换行） |

### K.3 项目数据管理
| 文件 | 职责 |
|---|---|
| `ProjectDataManager` | 项目级单例工厂（DataStore/FileManager/Library/Resource） |
| `ProjectDataStore` | 内存中项目数据存储（view/event/block/component/variable） |
| `ProjectDataParser` | JSON→Bean 反序列化（JsonReader 流式解析） |
| `ProjectFileManager` | Activity/CustomView 文件列表管理 |
| `ProjectFilePaths` | 项目文件路径常量与 sc_id→路径映射 |
| `ProjectListManager` | 项目列表加载与排序 |
| `ProjectBuilder` | 构建流水线（AAPT2→ECJ→D8→APK→签名） |
| `BuildConfig` | 构建配置 Bean（包名、SDK 版本、功能开关） |
| `IncrementalBuildCache` | 增量编译缓存（文件 hash 比对） |

### K.4 验证器
| 文件 | 职责 |
|---|---|
| `BaseValidator` | 验证器基类 |
| `ActivityNameValidator` | Activity/文件名验证 |
| `FileNameValidator` | 通用文件名验证 |
| `XmlNameValidator` | XML 资源名验证 |
| `IdentifierValidator` | Java 标识符验证 |
| `ResourceNameValidator` | 资源名验证 |
| `VariableNameValidator` | 变量名验证 |
| `LengthRangeValidator` | 长度范围验证 |
| `NumberRangeValidator` | 数值范围验证 |
| `LowercaseNameValidator` | 小写名验证 |
| `UniqueNameValidator` | 唯一名验证 |
| `VersionCodeValidator` | 版本号验证 |

### K.5 UI Fragment / View
| 文件 | 职责 |
|---|---|
| `EventListFragment` | 事件列表页 |
| `ComponentListFragment` | 组件列表页 |
| `ViewEditorFragment` | 视图编辑器 Fragment |
| `ViewFilesFragment` / `ViewFilesAdapter` | 项目文件列表 |
| `ImageListFragment` / `ImageCollectionFragment` | 图片资源管理 |
| `SoundListFragment` / `SoundImportFragment` | 音频资源管理 |
| `PermissionFragment` | 权限选择页 |
| `FirebasePreviewView` / `FirebaseSettingsView` / `FirebaseStorageView` | Firebase 配置 UI |

### K.6 Collection 管理器
| 文件 | 职责 |
|---|---|
| `BaseCollectionManager` | 收藏管理基类 |
| `BlockCollectionManager` | Block 收藏 |
| `MoreBlockCollectionManager` | MoreBlock 收藏 |
| `WidgetCollectionManager` | Widget 收藏 |
| `ImageCollectionManager` | 图片收藏 |
| `FontCollectionManager` | 字体收藏 |
| `SoundCollectionManager` | 音频收藏 |

### K.7 工具类
| 文件 | 职责 |
|---|---|
| `EncryptedFileUtil` | AES-128 加密文件读写 |
| `AnimationUtil` | 视图动画工具 |
| `BitmapUtil` | 图片裁剪/缩放 |
| `DateTimeUtil` | 日期时间格式化 |
| `DeviceUtil` | 设备信息获取 |
| `FormatUtil` | 数字/大小格式化 |
| `NinePatchDecoder` | 9-patch 图片解析 |
| `ZipUtil` | ZIP 压缩/解压 |
| `UriPathResolver` | Uri→文件路径转换 |
| `ViewUtil` | View 操作工具 |
| `UIHelper` | UI 通用工具 |
| `SketchToast` | Toast 封装 |
| `SharedPrefsHelper` | SharedPreferences 封装 |
| `XmlLayoutParser` | XML 布局解析（预览用） |
| `StringResource` | Block 文案国际化 |

### K.8 其他
| 文件 | 职责 |
|---|---|
| `ClassInfo` | View/组件→Java 类信息映射 |
| `PresetLayoutFactory` | 预设布局模板工厂 |
| `SketchwareConstants` | 全局常量 |
| `SketchwarePaths` | .sketchware 路径工具 |
| `LibraryManager` / `ResourceManager` | 库/资源管理器 |
| `KeyStoreManager` | 签名密钥管理 |
| `MapValueHelper` / `GsonMapHelper` / `HashMapTypeToken` | Gson 辅助 |
| `ReflectiveToString` | Bean toString 基类 |
| `ViewHistoryManager` / `RecentHistoryManager` | 视图/最近历史 |
| `CompileQuizManager` | 编译提示管理 |
| 回调接口 | `BuildCallback`, `SimpleCallback`, `IntCallback`, `ViewBeanCallback` 等 |
| 异常类 | `CompileException`, `SketchwareException`, `SimpleException` |

