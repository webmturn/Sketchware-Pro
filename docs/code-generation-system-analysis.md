# 代码生成系统深度分析

> 分析日期：2026-03-03
> 涉及模块：BlockInterpreter · ActivityCodeGenerator · EventCodeGenerator · ComponentCodeGenerator · BlockLoader

---

## 目录

1. [系统架构概览](#1-系统架构概览)
2. [BlockInterpreter 分析](#2-blockinterpreter-分析)
3. [ActivityCodeGenerator 分析](#3-activitycodegenerator-分析)
4. [EventCodeGenerator 分析](#4-eventcodegenerator-分析)
5. [ComponentCodeGenerator 分析](#5-componentcodegenerator-分析)
6. [Extra Block 扩展系统](#6-extra-block-扩展系统)
7. [完整代码生成链路](#7-完整代码生成链路)
8. [系统限制与问题](#8-系统限制与问题)
9. [改进建议](#9-改进建议)

---

## 1. 系统架构概览

### 1.1 核心数据流

```
用户拖拽 Block → BlockBean 列表 (JSON)
                    ↓
            EventCodeGenerator
            (按事件分类处理)
                    ↓
            BlockInterpreter
            (Block → Java 代码片段)
                    ↓
            ActivityCodeGenerator
            (组装完整 .java 文件)
                    ↓
            ComponentCodeGenerator
            (build.gradle / 字段声明 / 格式化)
```

### 1.2 关键文件

| 文件 | 行数 | 职责 |
|------|------|------|
| `BlockInterpreter.java` | 383 | 将单个 Block 的 opcode 翻译为 Java 代码片段 |
| `ActivityCodeGenerator.java` | 1202 | 组装完整的 Activity/Fragment Java 源文件 |
| `EventCodeGenerator.java` | 458 | 管理事件分类（View/Component/Activity/Drawer）并生成监听器 |
| `ComponentCodeGenerator.java` | 919 | build.gradle 生成、字段声明、事件代码模板、代码格式化 |
| `BlockLoader.java` | 283 | 加载 Extra Block 自定义块定义 |
| `ExtraBlockInfo.java` | 60 | Extra Block 数据模型 |

## 2. BlockInterpreter 分析

**文件**: `pro/sketchware/core/BlockInterpreter.java` (383 行)

### 2.1 核心职责

`BlockInterpreter` 是整个代码生成链路的 **最底层翻译器**，负责将用户拖拽的 Block（以 `BlockBean` 列表表示）递归解析为 Java 代码字符串。

### 2.2 关键方法

#### `interpretBlocks()` — 入口
- 遍历 `BlockBean` 列表，找到起始 block（id = 根节点）
- 调用 `resolveBlock()` 开始递归解析

#### `resolveBlock(String blockId, String parentOpcode)` — 递归核心
- 根据 `blockId` 查找对应的 `BlockBean`
- 调用 `getBlockCode(bean)` 获取当前 block 的 Java 代码
- 如果有 `nextBlock`，递归拼接后续代码
- 返回完整的代码字符串

#### `getBlockCode(BlockBean bean)` — 巨型 switch（~1100 行）
- 根据 `bean.opCode` 匹配对应的代码生成模板
- 使用 `String.format()` 将参数嵌入模板
- 支持 **200+ 内置 opcode**，涵盖：
  - **UI 操作**: setText, setVisible, setAlpha, setRotate 等
  - **Intent**: intentSetAction, intentSetScreen, startActivity 等
  - **文件操作**: fileSetFileName, fileGetData, fileutilread 等
  - **Firebase**: firebaseAdd, firebasePush, firebaseGetChildren 等
  - **日历**: calendarGetNow, calendarFormat, calendarDiff 等
  - **媒体**: mediaplayerCreate, soundpoolCreate 等
  - **网络**: requestnetworkStartRequestNetwork 等
  - **蓝牙**: bluetoothConnectStartConnection 等
  - **动画**: objectanimatorSetTarget, objectanimatorStart 等
  - **SQLite**: sqliteOpen, sqliteExecSQL, sqliteRawQuery 等
  - **通知**: notifCreateChannel, notifShow 等

#### `resolveParam()` — 参数解析
- 处理嵌套 block 参数（block-in-block）
- 递归调用 `getBlockCode()` 解析子 block
- 处理操作符（+、-、*、/、>、<、==、&&、|| 等）

#### `getCodeExtraBlock()` — Extra Block 处理（第 1657-1728 行）
- 当 `getBlockCode()` 的 switch 没有匹配时，进入 `default` 分支
- 调用 `BlockLoader.getBlockInfo(opCode)` 查找自定义块
- 若全局也找不到，调用 `BlockLoader.getBlockFromProject(sc_id, opCode)` 查找项目级别自定义块
- 使用 `String.format(blockInfo.getCode(), parameters...)` 生成代码

### 2.3 参数类型系统

```java
// getBlockType() 返回值:
0 = boolean  → 默认 "true"
1 = double   → 默认 "0"
2 = String   → 默认 "\"\""
3 = 其他     → 默认 ""
```

### 2.4 架构特点

- **纯字符串拼接**: 全部通过 `String.format()` 模板生成代码
- **递归解析**: `resolveBlock` → `getBlockCode` → `resolveParam` → `resolveBlock` 形成递归链
- **无 AST**: 不生成抽象语法树，直接从 Block 到文本
- **ViewBinding 感知**: `isViewBindingEnabled` 影响 View ID 引用方式（`binding.xxx` vs `xxx`）

## 3. ActivityCodeGenerator 分析

**文件**: `pro/sketchware/core/ActivityCodeGenerator.java` (1202 行)

### 3.1 核心职责

`ActivityCodeGenerator` 是 **Java 源文件的组装器**，负责将所有代码片段拼接成一个完整的 `.java` 文件。它是代码生成链路的最顶层。

### 3.2 生成流程（`generateCode()` 方法）

组装顺序严格固定：

```
1. package 声明
2. import 语句（handleAppCompat() 收集）
3. 类声明（extends AppCompatActivity / Activity / Fragment）
4. 字段声明区域：
   ├── 常量（REQUEST_CODE 等）
   ├── 带静态初始化器的字段（InterstitialAd 等）
   ├── 变量字段（int/String/boolean/double）
   ├── List 字段
   ├── View 字段
   └── Component 字段
5. onCreate() / onCreateView()
   ├── setContentView / inflate
   ├── Firebase 初始化
   ├── AdMob 初始化
   └── 权限检查
6. initialize() 方法
   ├── View.findViewById 绑定
   ├── Component 初始化
   ├── View 事件注册
   ├── Component 事件注册
   ├── Drawer 事件注册
   └── Auth 事件注册
7. initializeLogic()（onCreate 用户逻辑）
8. onActivityResult()（相机/文件选择器/Auth）
9. 生命周期方法（MapView/AdView/SQLite 自动注册）
10. 事件逻辑方法（MoreBlock 等）
11. Adapter 内部类
12. 废弃方法（可选关闭）
13. 类结束 }
```

### 3.3 并行代码生成支持

```java
// Phase 1: 可并行（纯字符串拼接，线程安全）
String formattedCode = generator.generateCode(false);

// Phase 2: 必须串行（CommandBlock 有共享文件 I/O）
String finalCode = ActivityCodeGenerator.applyCommands(formattedCode);
```

### 3.4 Fragment 特殊处理（第 628-645 行）

生成完成后，通过 **全局字符串替换** 将 Activity 代码适配为 Fragment：

```java
code = code.replace("getApplicationContext()", "getContext().getApplicationContext()")
           .replace("getBaseContext()", "getActivity().getBaseContext()")
           .replace("(ClipboardManager) getSystemService", "(ClipboardManager) getContext().getSystemService")
           // ... 共 15 处替换
```

**问题**: 这种后处理方式非常脆弱，无法处理自定义块中的代码。

### 3.5 Import 管理

- `handleAppCompat()` 添加大量通配符 import（`android.app.*`、`android.widget.*` 等）
- `ComponentTypeMapper.getImportsByTypeName()` 按需添加特定 import
- 所有 import 收集到 `ArrayList<String> imports`，去重后写入
- **问题**: 通配符 import 导致生成的代码包含大量不必要的 import

### 3.6 ViewBinding 集成

贯穿整个生成流程：
- 字段声明：`private XxxBinding binding;` 代替独立 View 字段
- onCreate：`binding = XxxBinding.inflate(getLayoutInflater());`
- View 引用：`binding.viewId` 代替 `findViewById(R.id.viewId)`

## 4. EventCodeGenerator 分析

**文件**: `pro/sketchware/core/EventCodeGenerator.java` (458 行)

### 4.1 核心职责

`EventCodeGenerator` 是 **事件分发与监听器代码生成器**，负责将 `EventBean` 按类型分类，并为每个事件生成对应的 Java 监听器代码。

### 4.2 事件分类体系

```
EventBean.eventType:
├── EVENT_TYPE_VIEW (1)        → View 点击/长按/文本变化等
├── EVENT_TYPE_COMPONENT (2)   → Firebase/Camera/FilePicker 回调
├── EVENT_TYPE_ACTIVITY (3)    → Activity 生命周期 (onBackPressed, onResume 等)
└── EVENT_TYPE_DRAWER_VIEW (4) → DrawerLayout 内 View 事件
```

### 4.3 处理流程 (`processEvents()`)

```java
for (EventBean eventBean : events) {
    // 1. 用 BlockInterpreter 将该事件关联的 Block 列表解析为 Java 代码
    String eventLogic = new BlockInterpreter(..., eventLogicBlocks, ...).interpretBlocks();

    // 2. 按事件类型分发
    switch (eventBean.eventType) {
        case VIEW:      → addViewListeners(targetId, eventName, eventLogic);
        case COMPONENT: → addCallbackEvents(...) 或 addViewImports(...)
        case ACTIVITY:  → addActivityEvent(eventName, eventLogic);
        case DRAWER:    → addDrawerEvents(targetId, eventName, eventLogic);
    }
}
```

### 4.4 代码生成输出

| 方法 | 输出位置 | 内容 |
|------|----------|------|
| `generateViewEvents()` | `initialize()` 内 | `view.setOnClickListener(...)` 等 |
| `generateComponentEvents()` | `initialize()` 内 | Firebase listener、RequestNetwork listener 等 |
| `generateDrawerEvents()` | `initialize()` 内 | Drawer 内 View 的事件 |
| `generateAuthEvents()` | `initialize()` 内 | FirebaseAuth 回调 |
| `generateActivityLifecycleEventCode()` | 类级别方法 | `onResume()`, `onDestroy()` 等 |

### 4.5 生命周期事件自动注册

`ActivityCodeGenerator` 在组装完成后自动为特殊 View 添加生命周期事件：

```java
// MapView → onStart/onResume/onPause/onStop/onDestroy
// AdView → onResume/onPause/onDestroy
// SQLiteDatabase → onDestroy
// Gyroscope → onDestroy
```

## 5. ComponentCodeGenerator 分析

**文件**: `pro/sketchware/core/ComponentCodeGenerator.java` (919 行)

### 5.1 核心职责

`ComponentCodeGenerator` 是一个 **多功能静态工具类**，职责涵盖：

1. **build.gradle 生成** — `getBuildGradleString()`
2. **settings.gradle 生成** — `getSettingsGradle()`
3. **字段声明生成** — `getFieldDeclaration()`
4. **组件初始化代码** — `getComponentInitializerCode()`
5. **事件代码模板** — `getEventCode()` (60+ 事件类型)
6. **onActivityResult 代码** — `getOnActivityResultCode()`
7. **Adapter 类名生成** — `getAdapterClassName()`
8. **代码格式化** — `formatCode()`

### 5.2 build.gradle 生成 (`getBuildGradleString()`)

动态构建 `app/build.gradle`：
- **plugins**: `com.android.application`
- **android**: compileSdk, defaultConfig (applicationId, namespace, minSdk, targetSdk, versionCode, versionName)
- **buildFeatures**: viewBinding（可选）
- **dependencies**: 根据项目使用的功能按需添加：
  - AppCompat + Material Design
  - Firebase BOM + Auth/Database/Storage/Messaging
  - Play Services (Ads, Maps, Auth)
  - Glide, Gson, OkHttp
  - CircleImageView, YouTubePlayer, CodeView, Lottie, OTPView, PatternLockView
  - 本地库（从 `local_library` JSON 文件读取）

**库排除机制**: `ExcludeBuiltInLibrariesActivity` 允许用户排除特定内置库。

### 5.3 事件代码模板 (`getEventCode()`)

使用巨型 switch 表达式（~310 行），为 **60+ 事件** 生成方法签名和参数绑定：

| 分类 | 事件示例 |
|------|----------|
| View 基础 | onClick, onLongClick, onTouch |
| 文本输入 | beforeTextChanged, onTextChanged, afterTextChanged |
| 列表交互 | onItemClicked, onItemLongClicked, onItemSelected |
| SeekBar | onProgressChanged, onStartTrackingTouch, onStopTrackingTouch |
| WebView | onPageStarted, onPageFinished |
| 动画 | onAnimationStart, onAnimationEnd, onAnimationCancel |
| Firebase | onChildAdded, onChildChanged, onChildRemoved, onCancelled |
| Auth | onCreateUserComplete, onSignInUserComplete, onResetPasswordEmailSent |
| Storage | onUploadProgress, onDownloadProgress, onUploadSuccess |
| 网络 | onResponse, onErrorResponse |
| 语音 | onSpeechResult, onSpeechError |
| 蓝牙 | onConnected, onDataReceived, onDataSent, onConnectionError |
| 地图 | onMapReady, onMarkerClicked |
| 位置 | onLocationChanged |
| 生命周期 | onBackPressed, onStart, onStop, onResume, onPause, onDestroy |

**扩展点**: `default` 分支调用 `ManageEvent.getExtraEventCode()` 处理自定义事件。

### 5.4 字段声明 (`getFieldDeclaration()`)

```java
// 普通字段
"private Type name;"

// 带初始化器的字段（如 Camera 组件）
"private File _file_cameraName;"

// ViewBinding 模式下 View 字段被跳过（返回空字符串）
```

### 5.5 代码格式化 (`formatCode()`)

对生成的代码进行基本缩进处理，使输出的 Java 文件具有可读性。

## 6. Extra Block 扩展系统

### 6.1 组件概览

| 文件 | 职责 |
|------|------|
| `ExtraBlockInfo.java` | Extra Block 数据模型（name, spec, code, color） |
| `BlockLoader.java` | 加载全局和项目级自定义块 |
| `ExtraBlockFile.java` | 从 JSON 文件读取 Extra Block 原始数据 |
| `PaletteSelector.java` | 管理块的调色板分类 |

### 6.2 数据模型 (`ExtraBlockInfo`)

```java
public class ExtraBlockInfo {
    public transient boolean isMissing;  // 是否为缺失块
    private String name = "";             // 块的 opCode 名称
    private String code = "";             // Java 代码模板（含 %s 占位符）
    private String spec = "";             // 块的显示规格（参数布局）
    private String spec2 = "";            // 备用规格
    private int color = 0;                // 块颜色
    private int paletteColor = 0;         // 调色板颜色
}
```

### 6.3 加载机制 (`BlockLoader`)

**两级查找策略**:

```
1. 全局 Extra Blocks（所有项目共享）
   路径: ExtraBlockFile.getExtraBlockData()
   时机: 静态初始化时加载，缓存到 static ArrayList<ExtraBlockInfo>

2. 项目级 Custom Blocks（单项目私有）
   路径: .sketchware/data/{sc_id}/custom_blocks
   时机: 仅当全局未找到时按需读取（无缓存）
```

### 6.4 代码生成集成

```java
// BlockInterpreter.getBlockCode() 的 default 分支:
default:
    opcode = getCodeExtraBlock(bean, "\"\"");

// getCodeExtraBlock() 内部:
ExtraBlockInfo blockInfo = BlockLoader.getBlockInfo(bean.opCode);  // 全局查找
if (blockInfo.isMissing) {
    blockInfo = BlockLoader.getBlockFromProject(sc_id, bean.opCode);  // 项目级查找
}
String formattedCode = String.format(blockInfo.getCode(), parameters...);
```

### 6.5 Extra Block JSON 格式

```json
{
  "name": "myCustomBlock",
  "spec": "do something with %s and %s",
  "code": "MyHelper.doSomething(%s, %s);",
  "color": "#FF5722",
  "palette": "9"
}
```

## 7. 完整代码生成链路

### 7.1 端到端流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                    ProjectBuilder.build()                        │
│                                                                 │
│  对每个 ProjectFileBean (Activity/Fragment):                     │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ActivityCodeGenerator.generateCode()                     │    │
│  │                                                         │    │
│  │  1. 收集项目数据:                                        │    │
│  │     ├── ProjectDataManager.getViews()     → ViewBean[]  │    │
│  │     ├── ProjectDataManager.getVariables() → Variable[]  │    │
│  │     ├── ProjectDataManager.getLists()      → List[]     │    │
│  │     └── ProjectDataManager.getComponents() → Component[]│    │
│  │                                                         │    │
│  │  2. EventCodeGenerator.processEvents():                 │    │
│  │     ├── 遍历所有 EventBean                               │    │
│  │     ├── 获取每个事件的 BlockBean 列表                      │    │
│  │     └── 对每个事件创建 BlockInterpreter:                   │    │
│  │         ┌─────────────────────────────────┐              │    │
│  │         │ BlockInterpreter.interpretBlocks│              │    │
│  │         │   ├── resolveBlock() [递归]      │              │    │
│  │         │   │   ├── getBlockCode()        │              │    │
│  │         │   │   │   ├── 内置 switch       │              │    │
│  │         │   │   │   └── getCodeExtraBlock │              │    │
│  │         │   │   │       └── BlockLoader   │              │    │
│  │         │   │   └── resolveParam() [递归]  │              │    │
│  │         │   └── 返回 Java 代码字符串       │              │    │
│  │         └─────────────────────────────────┘              │    │
│  │                                                         │    │
│  │  3. 组装 Java 文件:                                      │    │
│  │     ├── package + imports                               │    │
│  │     ├── 类声明 + 字段                                    │    │
│  │     ├── onCreate / onCreateView                         │    │
│  │     ├── initialize() + 事件监听器                        │    │
│  │     ├── initializeLogic()                               │    │
│  │     ├── 生命周期方法                                     │    │
│  │     ├── MoreBlock 方法                                  │    │
│  │     └── Adapter 内部类                                  │    │
│  │                                                         │    │
│  │  4. 后处理:                                              │    │
│  │     ├── Fragment 字符串替换（15 处）                      │    │
│  │     ├── AppCompat 替换                                  │    │
│  │     └── ComponentCodeGenerator.formatCode()             │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  同时生成:                                                       │
│  ├── ComponentCodeGenerator.getBuildGradleString() → build.gradle│
│  ├── ComponentCodeGenerator.getSettingsGradle() → settings.gradle│
│  └── LayoutGenerator.generateLayoutXml() → res/layout/*.xml     │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 关键数据转换

```
BlockBean (JSON)
  ↓ BlockInterpreter
String (Java 代码片段, 如 "view.setText(\"hello\");")
  ↓ EventCodeGenerator
String (带监听器包装, 如 "view.setOnClickListener(new View.OnClickListener(){...})")
  ↓ ActivityCodeGenerator
String (完整 .java 文件, 约 100-2000 行)
  ↓ ComponentCodeGenerator.formatCode()
String (格式化后的最终代码)
```

## 8. 系统限制与问题

### 8.1 ~~巨型 switch 反模式（严重）~~ ✅ 已解决

**位置**: `BlockInterpreter.getBlockCode()` (1100+ 行 switch)、`ComponentCodeGenerator.getEventCode()` (310+ 行 switch)

**解决方案**: 引入 `BlockCodeRegistry`、`EventCodeRegistry`、`ListenerCodeRegistry` 注册表模式，将所有 switch case 迁移为独立的 handler。

~~**影响**: 添加新功能的开发成本高、代码冲突概率大。~~

### 8.2 ~~Fragment 后处理脆弱性（中等）~~ ✅ 已解决

**位置**: `ActivityCodeGenerator.generateCode()`

**解决方案**: 引入 `CodeContext` 类，在代码生成时根据 `isFragment` 直接产出正确的 API 调用（如 `getContext().getApplicationContext()`），消除了 17 处 `String.replace()` 后处理。`BlockCodeRegistry` 的 ~20 个 handler 和 `ComponentCodeGenerator` 的组件初始化/适配器方法均已更新为使用 `CodeContext`。

### 8.3 无 AST / 无类型检查（严重）

**问题**:
- 整个代码生成系统基于字符串拼接，没有中间表示 (IR) 或抽象语法树 (AST)
- 无法在生成时检测类型错误（如把 String 传给期望 int 的参数）
- 无法进行死代码消除、常量折叠等优化
- 生成的代码格式依赖后处理的 `formatCode()`，容易出现缩进不一致

**影响**: 用户只有在编译阶段才能发现代码生成错误，调试体验差。

### 8.4 ~~项目级 Custom Block 无缓存（性能）~~ ✅ 已解决

**位置**: `BlockLoader.getBlockFromProject()`

**解决方案**: 添加 `projectBlockCache` (HashMap) 和 `cachedProjectId` 实现项目级缓存，全局块也改用 `blocksByName` HashMap 实现 O(1) 查找。提供 `invalidateProjectCache()` 方法在编辑自定义块时刷新缓存。

### 8.5 Import 管理粗放（轻微）

**位置**: `ActivityCodeGenerator.handleAppCompat()` 第 879-897 行

**问题**:
- 无条件添加 ~20 个通配符 import（`android.app.*`、`android.widget.*` 等）
- 不论项目是否使用这些包中的类
- 增加了生成代码的体积，影响 IDE 代码提示的准确性

### 8.6 Extra Block 代码模板安全性（中等）

**位置**: `BlockInterpreter.getCodeExtraBlock()` 第 1718-1722 行

**问题**:
- Extra Block 的 `code` 字段直接通过 `String.format()` 执行
- 如果 `code` 模板中的 `%s` 占位符数量与参数数量不匹配，会抛出异常
- 虽然有 try-catch 包裹，但只生成注释 `/* Failed to resolve Custom Block's code */`，用户难以理解错误原因
- 没有对 `code` 模板进行预验证或沙盒化

### 8.7 ComponentCodeGenerator 职责过重（设计）

**问题**:
- 919 行的单一类，承担了至少 8 种不同职责
- build.gradle 生成、事件代码模板、字段声明、代码格式化等功能耦合在一起
- 任何依赖变更（如新增内置库）都需要修改此文件
- 违反单一职责原则 (SRP)

### 8.8 硬编码库版本号（维护）

**位置**: `ComponentCodeGenerator.getBuildGradleString()` 第 88-164 行

**问题**:
- 所有内置库的版本号硬编码在 Java 代码中
- 更新库版本需要修改源代码并重新编译
- 无法让用户自定义或覆盖库版本

```java
// 示例：硬编码的版本号
"implementation 'androidx.appcompat:appcompat:1.7.1'"
"implementation 'com.google.android.material:material:1.12.0'"
"implementation 'com.github.bumptech.glide:glide:4.16.0'"
```

### 8.9 拼写错误导致的潜在 Bug

**位置**: `BlockInterpreter.java` 第 857 行

```java
case "calnedarViewSetMaxDate":  // 拼写错误: calnedar → calendar
```

这个拼写错误已经成为 API 的一部分，修改可能导致依赖此 opcode 的项目出现兼容性问题。

## 9. 改进建议

### 9.1 注册表模式替代巨型 switch（高优先级）

**目标**: 消除 `BlockInterpreter.getBlockCode()` 和 `ComponentCodeGenerator.getEventCode()` 中的巨型 switch。

**方案**: 引入 `BlockCodeRegistry` 和 `EventCodeRegistry`，将每个 opcode/event 的代码生成逻辑注册为独立的 handler。

```java
// 定义接口
public interface BlockCodeHandler {
    String generate(List<String> params, BlockBean bean, BlockInterpreter interpreter);
}

// 注册表
public class BlockCodeRegistry {
    private static final Map<String, BlockCodeHandler> handlers = new HashMap<>();

    static {
        // 内置块
        register("setText", (params, bean, interp) ->
            String.format("%s.setText(%s);", params.get(0), params.get(1)));
        register("setVisible", (params, bean, interp) ->
            String.format("%s.setVisibility(View.%s);", params.get(0), params.get(1)));
        // ... 其他内置块
    }

    public static void register(String opcode, BlockCodeHandler handler) {
        handlers.put(opcode, handler);
    }

    public static String generate(String opcode, List<String> params,
                                   BlockBean bean, BlockInterpreter interpreter) {
        BlockCodeHandler handler = handlers.get(opcode);
        if (handler != null) return handler.generate(params, bean, interpreter);
        return null; // fallback to ExtraBlock
    }
}
```

**优势**:
- 新增 Block 只需注册一个 handler，不修改核心代码
- 每个 handler 可独立测试
- Extra Block 系统可直接注册为 handler
- 支持插件化扩展

### 9.2 项目级 Custom Block 缓存（高优先级）

**方案**: 在 `BlockLoader` 中添加项目级缓存。

```java
private static String cachedProjectId;
private static ArrayList<ExtraBlockInfo> projectBlocks;

public static ExtraBlockInfo getBlockFromProject(String sc_id, String block_name) {
    if (!sc_id.equals(cachedProjectId)) {
        cachedProjectId = sc_id;
        projectBlocks = loadProjectBlocks(sc_id);
    }
    for (ExtraBlockInfo info : projectBlocks) {
        if (block_name.equals(info.getName())) return info;
    }
    // ... missing block handling
}
```

**预期收益**: 对大量使用自定义块的项目，代码生成速度提升数倍。

### 9.3 Fragment 代码生成上下文化（中优先级）

**方案**: 在 `BlockInterpreter` 中引入上下文感知，而非后处理替换。

```java
public class CodeContext {
    final boolean isFragment;
    final boolean isViewBindingEnabled;

    public String getApplicationContext() {
        return isFragment ? "getContext().getApplicationContext()" : "getApplicationContext()";
    }
    public String getSystemService(String cast) {
        return isFragment
            ? "(" + cast + ") getContext().getSystemService"
            : "(" + cast + ") getSystemService";
    }
    // ...
}
```

将 `CodeContext` 注入 `BlockInterpreter`，在代码生成时直接使用正确的 API 调用，而非事后替换。

**优势**: Extra Block 生成的代码也能正确适配 Fragment。

### 9.4 拆分 ComponentCodeGenerator（中优先级）

将 919 行的 `ComponentCodeGenerator` 拆分为独立职责类：

```
ComponentCodeGenerator (919 行)
  → GradleFileGenerator      (build.gradle / settings.gradle 生成)
  → FieldDeclarationGenerator (字段声明)
  → EventTemplateGenerator    (事件代码模板)
  → CodeFormatter             (代码格式化)
  → ComponentInitializer      (组件初始化代码)
```

### 9.5 库版本外部化配置（低优先级）

**方案**: 将硬编码的库版本号移至 JSON 配置文件。

```json
// assets/library_versions.json
{
  "androidx.appcompat:appcompat": "1.7.1",
  "com.google.android.material:material": "1.12.0",
  "com.github.bumptech.glide:glide": "4.16.0"
}
```

**优势**:
- 更新库版本无需修改 Java 源码
- 未来可支持用户自定义覆盖

### 9.6 ~~Extra Block 模板预验证（低优先级）~~ ✅ 已实现

**实现**: `BlockLoader` 中添加 `validateBlock()` 方法，在 `loadCustomBlocks()`（全局）和 `loadProjectBlocks()`（项目级）加载时验证每个块的 `code` 模板占位符数量是否超过可用参数数（spec 参数 + 2 个 subStack）。不匹配时记录警告日志。

---

## 附录：关键类关系图

```
                    ┌──────────────────┐
                    │  ProjectBuilder  │
                    └────────┬─────────┘
                             │ 为每个 Activity/Fragment 调用
                             ▼
                ┌────────────────────────────┐
                │  ActivityCodeGenerator     │ ← 组装完整 .java
                │  (1202 行)                 │
                └────┬──────────┬────────────┘
                     │          │
          ┌──────────▼──┐   ┌──▼─────────────────┐
          │EventCode    │   │ComponentCode        │
          │Generator    │   │Generator            │
          │(458 行)     │   │(919 行)             │
          └──────┬──────┘   └─────────────────────┘
                 │ 对每个事件创建
                 ▼
        ┌─────────────────┐     ┌──────────────┐
        │BlockInterpreter │────▶│  BlockLoader  │
        │(383 行)         │     │  (283 行)     │
        └─────────────────┘     └──────┬───────┘
                                       │
                                ┌──────▼───────┐
                                │ExtraBlockInfo│
                                │(60 行)       │
                                └──────────────┘
```

---

*文档版本: 1.0 | 最后更新: 2026-03-03*
