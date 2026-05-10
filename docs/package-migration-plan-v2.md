# 包结构迁移规划 v2 — pro.sketchware 内部整理 + com.besome.sketch 整体迁移

**状态**：规划阶段（不立即执行）
**先决条件**：v1 计划（`docs/package-migration-plan.md`）9 个 Phase 已全部完成（167 文件），最新提交 `f3b54f93c`
**适用范围**：本文档涵盖 v1 计划第八节列出的剩余两项后续工作 —— pro.sketchware 内部整理 + com.besome.sketch → pro.sketchware 整体迁移

---

## 一、调研结果速览

### 1.1 当前 java/ 顶层

| 顶层包 | 文件总数 | 性质 |
|--------|---------|------|
| `pro/` | 383 | 目标包，已合并 v1 的 167 文件 |
| `com/besome/sketch/` | 253 | 旧主包，**待整体迁移** |
| `com/bumptech/glide/signature/` | 1 | **Glide 反射桥接** —— 用户补丁的 package-private 类，需放在 `com.bumptech.glide.signature` 包下才能访问内部成员，**不能简单合入 pro.sketchware** |
| `kellinwood/` | 44 | **第三方库**（Ken Ellinwood zipsigner/zipio/logging），来源外部 |

### 1.2 双重 util — 命名风格分裂

| 包 | 文件数 | 命名特征 | 代表文件 |
|----|-------|---------|---------|
| `pro.sketchware.util` | 35（顶层）+ 9（子包） | 混合命名（`Helper`、`UI`、`Network`、`FileUtil`、`SketchwareUtil` 等） | `Helper.java`、`SketchwareUtil.java`、`FileUtil.java` |
| `pro.sketchware.core.util` | 18 | 一致后缀 `XxxUtil`（Kotlin 风格） | `BitmapUtil.java`、`DateTimeUtil.java`、`ZipUtil.java` |

**重叠风险点**：

- `pro.sketchware.util.UI` ↔ `pro.sketchware.core.util.UIHelper` —— 名称暗示功能可能重叠
- `pro.sketchware.util.theme.ThemeUtils` ↔ `pro.sketchware.core.util.*`（无直接同名但主题工具分散）
- `pro.sketchware.core.util.GsonMapHelper` / `MapValueHelper` / `HashMapTypeToken` ↔ `pro.sketchware.util.GsonUtils` —— Gson 工具分散两处

### 1.3 双重 lib — 关注点完全不同

| 包 | 文件数 | 内容 |
|----|-------|------|
| `pro.sketchware.lib` | 12 | 编辑器 UI（`code_editor/` 6、`highlighter/` 2、`base/` 3 视图基类、`iconcreator/` 1） |
| `com.besome.sketch.lib` | 19 | Activity 基类（`base/` 7：`BaseAppCompatActivity` 等）+ 通用 widget（`ui/` 12：`CircleImageView`、`LoadingDialog` 等） |

**结论**：两个 `lib/` 关注点不同，**不应直接合并**，应当：

- `com.besome.sketch.lib.base/*` → `pro.sketchware.activities.base/`（Activity 基类）
- `com.besome.sketch.lib.ui/*` → `pro.sketchware.widgets/`（与现有 widgets 合并）

### 1.4 fragment 分散三处

```
pro.sketchware.core.fragments/                15 files  通用编辑器/管理 fragment（与 activity 解耦）
pro.sketchware.activities.main.fragments/     12 files  主页 fragment 树（projects、projects_store/{adapters,api,classes}）—— 与 MainActivity 强耦合
pro.sketchware.fragments.settings/            15 files  设置子 fragment（appearance 1、block/selector{,/details} 6、events{,/creator,/details} 4、language 4）—— 与 SettingsActivity 强耦合
```

三处性质差异大：core 一支属于通用层，main / settings 两支与 activity 强耦合。处理策略见 §3.1（main 保留原位、settings 改入 `activities.settings.fragments/`、`pro.sketchware.fragments/` 顶层删除）。

### 1.5 com.besome.sketch 子包分布（253 文件）

| 子包 | 文件 | 备注 |
|------|------|------|
| `com.besome.sketch`（loose） | 2 | `Config.java`、`MainDrawer.java` |
| `adapters/` | 2 | |
| `beans/` | 27 | 数据 bean，含 `ViewBean`(19KB)、`ComponentBean`(23KB)、`ProjectFileBean`(10KB) |
| `common/` | 1 | |
| `ctrls/` | 2 | |
| `design/` | 2 | `DesignActivity` 等核心编辑器入口 |
| `editor/` 与全部子包 | 185 | 编辑器主体合计；含 `view/palette/` 52、`view/item/` 45、`property/` 16、`view/` 顶层 14、`logic/` 5、`event/` 2、`makeblock/` 3、`component/` 3、`editor/` 顶层 3；下辖 `editor/manage/` 整支 42（见下） |
| ↳ 其中 `editor/manage/` | 42 | `manage/` 顶层 6 + font/image/sound/view 各 4–6 + library/ 整支 18（admob 7、firebase 2、material3 3、googlemap 1、compat 1、library 顶层 4） |
| `export/` | 1 | |
| `help/` | 4 | |
| `lib/` | 19 | 已分析，见 1.3 |
| `projects/` | 5 | |
| `tools/` | 3 | |

### 1.6 现有跨包依赖体量

```
import com.besome.sketch.*  共 817 处
import pro.sketchware.*     共 2603 处
import kellinwood.*         共 35 处
```

→ com.besome.sketch 整体迁移会触及 **800+ 个 import 行**，分散在两个根包中的几乎每个 Java/Kotlin 文件。

**子包级 import 实测（用于 Phase 风险预估）**：

| 子包前缀 | import 行数 | 备注 |
|---------|-----------|------|
| `com.besome.sketch.beans.` | 392 | 影响最广 → Phase 13 |
| `com.besome.sketch.editor.` | 283 | 编辑器整体（含子包递归） |
| `com.besome.sketch.editor.view.` | 188 | 含 palette 70 + item 47 |
| `com.besome.sketch.lib.base.` | 85 | 79 个文件继承 4 个 Base*Activity |
| `com.besome.sketch.editor.view.palette.` | 70 | |
| `com.besome.sketch.editor.manage.` | 56 | |
| `com.besome.sketch.editor.view.item.` | 47 | |
| `com.besome.sketch.lib.ui.` | 33 | 12 个 widget 类 |
| `com.besome.sketch.editor.logic.` | 11 | |
| `com.besome.sketch.editor.property.` | 6 | |
| `kellinwood.` | 35 | |

### 1.7 AndroidManifest 残留

仍有 **44 个** `com.besome.sketch.*` Activity 声明，按子包分布：

| 子包 | Activity 数 |
|------|-------------|
| `com.besome.sketch.editor` | 2 |
| `com.besome.sketch.editor.event` | 1 |
| `com.besome.sketch.editor.makeblock` | 1 |
| `com.besome.sketch.editor.manage`（含 sound/view/font/image 等） | 21 |
| `com.besome.sketch.editor.manage.library`（含 admob/firebase/compat/googlemap/material3） | 8 |
| `com.besome.sketch.{common,design,export,help,projects,tools}` | 11 |

每一类都需要 Manifest 同步更新。Phase 内任务必须自带对应 Activity 声明的 FQN 替换。

### 1.8 res/**/*.xml 视图标签依赖（v1 漏报项）

实测 **41 行匹配 / 21 个 layout 文件**（包含开标签 + 闭合标签；自闭合 widget 计 1 行，双标签 widget 计 2 行）直接以 FQN 引用 `com.besome.sketch.*` 类（v1 Phase 8 已确立的 XML 同步模式）：

| FQN 类 | XML 行匹配 | 涉及阶段 |
|--------|-----------|---------|
| `com.besome.sketch.lib.ui.{EasyDeleteEditText,CustomScrollView,CustomHorizontalScrollView,CustomViewPager,CircleImageView}`（含闭合标签） | 21 | Phase 12 |
| `com.besome.sketch.editor.view.{ViewPane,ViewProperty,ViewEditor,ViewDummy,ViewEvents,ViewLogicEditor}` | 8 | Phase 14b |
| `com.besome.sketch.editor.view.palette.{PaletteWidget,PaletteFavorite}` | 2 | Phase 14b |
| `com.besome.sketch.editor.logic.{PaletteBlock,LogicTopMenu,PaletteSelector}` | 3 | Phase 14a |
| `com.besome.sketch.editor.{LogicEditorDrawer}` | 1 | Phase 14a |
| `com.besome.sketch.editor.event.CollapsibleEventLayout` | 1 | Phase 14a |
| `com.besome.sketch.editor.component.CollapsibleComponentLayout` | 1 | Phase 14b |
| `com.besome.sketch.editor.manage.ViewBlockCollectionEditor` | 1 | Phase 14c |
| `com.besome.sketch.design.DesignDrawer` | 1 | Phase 15 |
| `com.besome.sketch.MainDrawer` | 1 | Phase 15 |
| **合计** | **41** | |

**统计口径**：使用 `Select-String -Pattern '<com\.besome\.sketch\.'` 跨 `app/src/main/res/layout/*.xml` 计行匹配数。每个 widget 实例若在 XML 中以 `<X .../>`（自闭合）出现计 1 行；若以 `<X>...</X>` 出现，开标签 + 闭合标签共计 2 行。**lib.ui 这一类含较多双标签实例，故行数（21）>=widget 实例数**。

**所有 Phase 必须在源代码 FQN 替换的同时同步更新 res/**/*.xml**，并在残留扫描中明确包含 XML 文件（同时匹配开标签和闭合标签）。

---

## 二、目标架构

### 2.1 顶层目标

```
app/src/main/java/
├── pro/sketchware/         ★ 唯一一级业务包
│   ├── SketchApplication.java         (已存在；根级 Application 类，对应 <application android:name=".SketchApplication">)
│   ├── activities/
│   │   ├── about/
│   │   ├── appcompat/
│   │   ├── base/                         ◀ 新建：来自 com.besome.sketch.lib.base
│   │   ├── design/                       ◀ 新建：来自 com.besome.sketch.design
│   │   ├── editor/                       (已存在 + 来自 com.besome.sketch.editor)
│   │   │   ├── block/
│   │   │   ├── code/
│   │   │   ├── command/
│   │   │   ├── component/                (已存在 + com.besome.sketch.editor.component)
│   │   │   ├── event/                    ◀ 新建：来自 com.besome.sketch.editor.event
│   │   │   ├── logic/                    ◀ 新建：来自 com.besome.sketch.editor.logic
│   │   │   ├── makeblock/                (已存在 + com.besome.sketch.editor.makeblock)
│   │   │   ├── manage/                   ◀ 新建：来自 com.besome.sketch.editor.manage（含子包）
│   │   │   │   ├── font/, image/, sound/, view/
│   │   │   │   └── library/{admob,compat,firebase,googlemap,material3}/
│   │   │   ├── manifest/
│   │   │   ├── property/                 ◀ 新建：来自 com.besome.sketch.editor.property
│   │   │   └── view/                     (已存在 + com.besome.sketch.editor.view 含 palette/item)
│   │   ├── export/                       ◀ 新建：来自 com.besome.sketch.export
│   │   ├── help/                         ◀ 新建：来自 com.besome.sketch.help
│   │   ├── importicon/
│   │   ├── main/
│   │   ├── preview/
│   │   ├── projects/                     ◀ 新建：来自 com.besome.sketch.projects
│   │   ├── resourceseditor/
│   │   ├── settings/
│   │   └── tools/                        (已存在 5 + com.besome.sketch.tools 3)
│   ├── adapters/                         ◀ 新建：来自 com.besome.sketch.adapters
│   ├── beans/                            (已存在 5 + com.besome.sketch.beans 27 = 32)
│   ├── blocks/
│   ├── common/                           ◀ 新建：来自 com.besome.sketch.common
│   ├── control/
│   ├── core/
│   │   ├── async/
│   │   ├── build/
│   │   ├── callback/
│   │   ├── codegen/
│   │   ├── ctrls/                        ◀ 新建：来自 com.besome.sketch.ctrls
│   │   ├── exception/
│   │   ├── fragments/                    (合并所有 fragment 入此处，见 §3.1)
│   │   ├── project/
│   │   ├── ui/
│   │   └── validation/
│   ├── dialogs/
│   ├── firebase/
│   ├── graphics/
│   ├── lib/                              (UI 元素：code_editor/highlighter/iconcreator/base 视图)
│   ├── library/
│   ├── menu/
│   ├── project/
│   ├── tools/
│   ├── util/                             (统一所有 util，见 §3.2)
│   │   ├── apk/
│   │   ├── format/                       ◀ 新建：DateTimeUtil/FormatUtil 等
│   │   ├── library/
│   │   ├── relativelayout/
│   │   ├── theme/
│   │   └── xml/
│   └── widgets/                          (已存在 3 + com.besome.sketch.lib.ui 12 = 15)
│
└── third_party/                          ★ 重命名 kellinwood → 明确标记第三方
    └── kellinwood/                       (保留作者名作 vendor 标识；下设 logging/、security/zipsigner/、zipio/)
```

**注意**：`com/besome/`、`kellinwood/` 顶层包将完全消失（kellinwood 内容下沉到 `pro.sketchware.third_party.kellinwood`）。`com/bumptech/glide/signature/` 因 Glide 反射桥接的 package-private 约束**必须保留在原位**。

### 2.2 不再需要的包

迁移完成后应删除（顶层目录）：

- `com/besome/sketch/`
- `com/besome/`（如无其他子包）
- `kellinwood/`（移到 `pro.sketchware.third_party.kellinwood`，保留 logging/security/zipio 内部子树原样）

**保留**：

- `com/bumptech/glide/signature/`（1 文件）—— Glide 反射桥接，必须留在 `com.bumptech.glide.signature` 包下才能访问 Glide 内部 package-private 成员。**Phase 15 退出条件须改为"`com/besome/` 不存在"，不要求"`com/` 不存在"**。

---

## 三、关键设计决策

### 3.1 Fragment 三处分散的处理策略

**边界声明**：`pro.sketchware.core.fragments/` 仅承载**与具体 activity 解耦的通用编辑器/管理 fragment**（不依赖某个 activity 的 view binding 或回调契约）。带有 activity 上下文耦合或网络/数据支撑代码的 fragment 应跟随其 activity 入 `pro.sketchware.activities.<activity>.fragments/`。

**决策**：

| 旧位置 | 新位置 | 原因 |
|--------|--------|------|
| `pro.sketchware.core.fragments.*`（15 文件） | 保留 | 已是 core fragments 的家 |
| `pro.sketchware.activities.main.fragments.*`（12 文件，含 `projects_store/{adapters,api,classes}` 支撑代码） | **保留原位** | 这些 fragment 与 `MainActivity` 强耦合，且其 `projects_store/` 子目录是 fragment 私有的网络/数据层，移入 core 会让 core 同时承载 UI 和数据访问，违反 core 包"无 activity 依赖"的隐含语义 |
| `pro.sketchware.fragments.settings.*`（15 文件） | `pro.sketchware.activities.settings.fragments.*` | 设置 fragment 与 `SettingsActivity` 强耦合，应与 activity 同级而非进 core |

迁移后顶层 `pro.sketchware.fragments/` 整个删除。`pro.sketchware.activities.main.fragments/` 与 `pro.sketchware.activities.settings.fragments/` 保留为各 activity 的私有 fragment 容器。

### 3.2 双重 util 合并的策略

**决策**：保留单一 `pro.sketchware.util` 顶层，按"领域"细分子包，删除 `pro.sketchware.core.util`：

| 子包 | 文件来源 |
|------|---------|
| `util/` 顶层 | 现 `pro.sketchware.util` 顶层（保留通用工具如 Helper、SketchwareUtil） |
| `util/format/`（新建） | `core.util.{DateTimeUtil,FormatUtil,ReflectiveToString}` |
| `util/io/`（新建） | `core.util.{EncryptedFileUtil,UriPathResolver,ZipUtil,SharedPrefsHelper}` |
| `pro.sketchware.graphics/`（**顶层包**，已存在） | `core.util.{BitmapUtil,NinePatchDecoder,AnimationUtil}` → 合入此处（**不进 `util/graphics/`**） |
| `util/`（合入顶层） | `core.util.{ViewUtil,UIHelper,SketchToast,DeviceUtil,ThrottleTimer,GsonMapHelper,MapValueHelper,HashMapTypeToken}` |

合并时若发现同名/同功能（例如 `pro.sketchware.util.UI` vs `pro.sketchware.core.util.UIHelper`），需要单独 review，可能保留较新/更完善的一份。

⚠️ **扁平化警告**：合入后 `pro.sketchware.util/` 顶层将达到 **43 文件**（原 35 + 来自 `core.util` 顶层的 8）。这接近功能分类的极限。Phase 10 完成后建议**单独评估**是否再细分（例如 `util/cache/`、`util/string/`），**或**接受平铺现状。本计划不强制此细分。

### 3.3 com.besome.sketch.lib 拆分的策略

| 旧位置 | 新位置 |
|--------|--------|
| `com.besome.sketch.lib.base.{BaseAppCompatActivity,BaseDialogActivity,BaseBottomSheetDialogActivity,BasePermissionAppCompatActivity}` | `pro.sketchware.activities.base/` |
| `com.besome.sketch.lib.base.{BaseWidget,CollapsibleLayout,CollapsibleViewHolder}` | `pro.sketchware.widgets.base/`（**新建子包**，专放 widget 抽象基类） |
| `com.besome.sketch.lib.ui.*` 全 12 文件 | `pro.sketchware.widgets/`（具体 widget 实现） |

**子包分层理由**：`widgets/` 顶层放具体 widget（`CircleImageView`、`LoadingDialog`、`EasyDeleteEditText` 等可直接 `<x>` 标签引用的视图类）；`widgets.base/` 放抽象基类（`BaseWidget`、`CollapsibleLayout` 等用于继承而非直接实例化）。避免抽象与具体在同一包混杂。

### 3.4 kellinwood 处理策略

**决策**：移动到 `pro.sketchware.third_party.kellinwood`，**保留作者名**作为 vendor 标识，**保留内部 logging/security/zipsigner/zipio 子结构原样**。

理由：
- 这是**第三方代码**（Ken Ellinwood 编写的 zipsigner/zipio/logging 三个独立模块）
- 当前位置 `kellinwood/` 顶层污染 java/ 根
- 取名 `third_party.kellinwood` 而非 `third_party.zipsigner`：zipsigner 只是其中一个子模块
- 改名后可被 `.gitattributes` 标记为 vendored，避免 PR diff 干扰

**特别注意（v1 未涉及，v2 新发现）**：

1. **字符串字面量反射**：`@app/src/main/java/kellinwood/security/zipsigner/ZipSigner.java:533` 含 `Class.forName("kellinwood.security.zipsigner.optional.SignatureBlockGenerator")`。Phase 11 必须**同步替换字符串字面量**，不可只换 `import`。实测仅此 1 处反射调用（用 `rg 'Class\.forName.*kellinwood'` 全仓验证）。
2. **拼写错误保留**：`@app/src/main/java/kellinwood/logging/DeaultLoggerFactory.java`（应为 `DefaultLoggerFactory`）系上游 vendor 原样拼写。Phase 11 **保留原拼写**（属 vendored 代码），并在 commit message 标注以避免 reviewer 误判为新引入错误。

---

## 四、迁移阶段（顺序敏感）

### 阶段编号沿用 v1（v1 用了 1–9），v2 从 10 开始

#### Phase 10 — pro.sketchware 内部整理预备（小型）

| Task | 文件数 | 备注 |
|------|-------|------|
| 10.1 `pro.sketchware.fragments.settings/` 15 → `pro.sketchware.activities.settings.fragments/` | 15 | 与 `SettingsActivity` 同级；同时删除 `pro.sketchware.fragments/` 顶层。`activities.main.fragments/` 12 文件**保留原位**（见 §3.1） |
| 10.2 `core.util` 非图形部分 → `util/{format,io}` 与顶层合入 | 15 | format 3 + io 4 + 顶层合入 8；检查同名冲突（`util.UI` vs `core.util.UIHelper` 等） |
| 10.3 `core.util` 中图形相关迁到 `pro.sketchware.graphics/`（顶层包） | 3 | BitmapUtil/NinePatchDecoder/AnimationUtil |

**预估**：~33 文件、3 commit、1–2 小时

#### Phase 11 — kellinwood 重命名为 third_party.kellinwood

| Task | 文件数 |
|------|-------|
| 11.1 `kellinwood/**` → `pro.sketchware.third_party.kellinwood/**`（保留 logging/security/zipio 子树） | 44 |
| 11.2 更新 `import kellinwood.` —— 范围**两类全覆盖**：(a) 非 vendor 文件中的 import；(b) vendor 文件之间的内部互引 import | — |
| 11.3 更新 1 处 `Class.forName("kellinwood.…")` 字符串字面量（`@app/src/main/java/kellinwood/security/zipsigner/ZipSigner.java:533`） | — |
| 11.4 保留 `DeaultLoggerFactory` 拼写错误（vendor 原样），在 commit message 中说明 | — |

**预估**：~44 文件、1 commit、30 分钟

⚠️ **范围澄清**：`§1.6` 给出的"35 处 `import kellinwood.`"是非 vendor 文件中的 import 计数。vendor 内部还有 vendor → vendor 的 import（例如 `ZipSigner.java` 引用 `kellinwood.logging.LoggerInterface` 等）。Phase 11 必须**同时**重写这两类 import，否则 vendor 子模块之间会因 package 不一致而编译失败。验证脚本须用 `rg 'kellinwood\.' app/src/main/java/` 全量归零（含字符串字面量 + javadoc）。

#### Phase 12 — com.besome.sketch lib 拆分（含 Activity 基类）

| Task | 文件数 |
|------|-------|
| 12.1 `com.besome.sketch.lib.base.Base*Activity` 4 个 → `pro.sketchware.activities.base/` | 4 |
| 12.2 `com.besome.sketch.lib.base.{BaseWidget,CollapsibleLayout,CollapsibleViewHolder}` → `pro.sketchware.widgets.base/` | 3 |
| 12.3 `com.besome.sketch.lib.ui/*` 12 个 → `pro.sketchware.widgets/` | 12 |

**预估**：~19 文件 + **实测 85 处 import** + **21 行 res/**/*.xml widget 标签**（含闭合标签）、1–2 commit、1 小时

⚠️ **风险**：实测 **79 个文件**继承 4 个 `Base*Activity` 之一；`com.besome.sketch.lib.ui.*` 在 21 行 XML 中作 widget 标签（`<com.besome.sketch.lib.ui.EasyDeleteEditText>` 等，含开标签 + 闭合标签），必须同步更新（参见 §1.8）。

#### Phase 13 — com.besome.sketch.beans 整体迁移

| Task | 文件数 |
|------|-------|
| 13.1 **前置同名扫描**：`git ls-files app/src/main/java/pro/sketchware/beans` + `git ls-files app/src/main/java/com/besome/sketch/beans`，diff 类名集合 | — |
| 13.2 若发现同名：**重命名旧 `pro.sketchware.beans/` 一方**（依据 v1 实证规则"使用最广的保留名称"——27 文件方一定是被引用最广的），然后再执行 13.3 | — |
| 13.3 `com.besome.sketch.beans/*` 27 个 → `pro.sketchware.beans/` | 27 |
| 13.4 验证：`pro.sketchware.beans/` 最终文件数 = 27 + 5 - 重命名抵消数 | — |

**预估**：~27 文件 + **实测 392 处 import**、1 commit（如有重命名则 2 commit：先重命名，再迁移）、1 小时

⚠️ **风险**：bean 类是数据流核心，使用最广泛。392 import 行远超第二名 editor 的 283，是 com.besome.sketch 中最被引用的子包。**冲突处理顺序非常关键**：必须先重命名旧 `pro.sketchware.beans/` 中冲突类（影响 5 文件 × N 个引用），再迁入 27 个新 bean，**反过来会让 27 个迁入文件覆盖旧的同名类，运行时崩溃**。

#### Phase 14 — com.besome.sketch.editor 主体迁移（最大）

按 v1 风格分多个子 commit。**14b 因 import 改动量大（约 305 处 import + 130 文件迁移 + 11 行 XML），单 commit diff 易超 800 行影响 review，故拆为 14b₁ + 14b₂**：

| 子阶段 | 范围 | 文件数 | Manifest Activity | XML 标签（行匹配） |
|--------|------|-------|-------------------|---------|
| 14a | `com.besome.sketch.editor` 顶层 + `event/`、`logic/`、`makeblock/` | 3 + 2 + 5 + 3 = 13 | 4（editor 2、event 1、makeblock 1） | 5（LogicEditorDrawer + 3 logic + CollapsibleEventLayout） |
| 14b₁ | `editor/component/`、`property/`（小且独立） | 3 + 16 = 19 | 0 | 1（CollapsibleComponentLayout） |
| 14b₂ | `editor/view/`（含 palette、item） | 14 + 52 + 45 = 111 | 0 | 10（6 view + 2 palette + 其余） |
| 14c | `editor/manage/`（含 font/image/sound/view 子包，**不含** library） | 6 + 6 + 4 + 4 + 4 = 24 | 21 | 1（ViewBlockCollectionEditor） |
| 14d | `editor/manage/library/*`（含 admob/compat/firebase/googlemap/material3） | 4 + 7 + 1 + 2 + 1 + 3 = 18 | 8 | 0 |

**预估**：~185 文件、5 commit、3–4 小时
**关键约束**：14a→14b₁→14b₂→14c→14d 必须**逐 commit** 通过 `:app:compileDebugJavaWithJavac`；不允许跨子阶段中间态合并。理由：palette/item 大量引用 view 顶层，editor 顶层 `LogicEditorActivity` 引用 view 子树；任意中间态停留都会出现"已搬走的 X 引用尚在原位的 Y"。

#### Phase 15 — com.besome.sketch 收尾（其余子包）

| Task | 文件数 | XML / 同名检查 |
|------|-------|----------------|
| 15.1 `com.besome.sketch` 根 `Config.java`、`MainDrawer.java` → `pro.sketchware`（待定具体位置） | 2 | XML 引用 `MainDrawer` 1 处 |
| 15.2 `com.besome.sketch.adapters/*` 2 → `pro.sketchware.adapters/` 或合入 ui 用例所在的 adapters/ | 2 | |
| 15.3 `com.besome.sketch.common/*` 1 → `pro.sketchware.common/` 或并入 util | 1 | |
| 15.4 `com.besome.sketch.ctrls/*` 2 → `pro.sketchware.core.ctrls/` | 2 | |
| 15.5 `com.besome.sketch.design/*` 2 → `pro.sketchware.activities.design/` | 2 | XML 引用 `DesignDrawer` 1 处 |
| 15.6 `com.besome.sketch.export/*` 1 → `pro.sketchware.activities.export/` | 1 | |
| 15.7 `com.besome.sketch.help/*` 4 → `pro.sketchware.activities.help/` | 4 | |
| 15.8 `com.besome.sketch.projects/*` 5 → `pro.sketchware.activities.projects/` | 5 | |
| 15.9 `com.besome.sketch.tools/*` 3 → `pro.sketchware.tools/` 或 `activities.tools/` | 3 | ⚠️ `pro.sketchware.tools/` 已存 5 文件，必须**先做同名扫描** |

**预估**：~22 文件、2–3 commit、1–2 小时
**Manifest 影响**：11 处（common/design/export/help/projects/tools）
**完成本阶段后**：删除 `com/besome/` 顶层目录（**保留 `com/bumptech/glide/signature/`**，见 §2.2）

---

## 五、阶段顺序与依赖关系

```
[已完成 Phase 1–9]                  v1 完成后状态：mod.*/dev.* Manifest 项 18→0；com.besome.sketch.* 仍存 44（v2 待处理）
        │
        ▼
Phase 10 ─ 内部整理（无外部依赖）
        │
        ▼
Phase 11 ─ kellinwood 重命名（独立第三方）
        │
        ▼
Phase 12 ─ lib 拆分（先于 13/14，因为很多类继承 Base*Activity）
        │
        ▼
Phase 13 ─ beans（先于 14，因为 editor 大量引用 bean）
        │
        ▼
Phase 14 ─ editor 主体（最大，5 个子 commit：14a/14b₁/14b₂/14c/14d）
        │
        ▼
Phase 15 ─ 收尾子包（删除 com/besome/ 顶层；保留 com/bumptech/glide/signature/）
```

### 总规模与时间预估

| 项 | 文件数 | commit | 时间 |
|----|-------|--------|------|
| Phase 10 | ~33 | 3 | 1–2 h |
| Phase 11 | ~44 | 1 | 30 m |
| Phase 12 | ~19 | 1–2 | 1 h |
| Phase 13 | ~27 | 1–2 | 1 h |
| Phase 14 | ~185 | 5 | 3–4 h |
| Phase 15 | ~22 | 2–3 | 1–2 h |
| **合计** | **~330** | **13–16** | **8–12 h** |

**计数口径**：合计 330 = 净迁移文件 ~286（kellinwood 44 + com.besome.sketch 净迁移约 242，因为 `com/bumptech/glide/signature/` 1 文件保留原位且 `pro.sketchware.activities.main.fragments/` 12 文件不迁移）+ pro 内部重组 ~44（10.2/10.3 共 18 + Phase 13 同名重命名可能产生若干重命名 commit + Phase 12 widgets 拆分内部调整）。同一文件可能在不同 Phase 中各计 1 次（例如 Phase 13 重命名后 Phase 14 仍引用该 bean）。**Phase 14 的 5 commit** 含 14b₁ + 14b₂ 拆分。

---

## 六、风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| Phase 12 Base*Activity 牵动几乎所有 Activity import | 实测 79 文件继承 + 85 import 行 | 使用 v1 实证有效的"longest-prefix-first 全局替换 + 自动残留扫描"流程；同步更新 21 行 XML widget 标签（含闭合标签） |
| Phase 13 beans 类型在 res/xml 也被引用（XML inflate） | 运行时 InflateException | Phase 13 与所有 Phase 残留扫描必须**包含 res/**/*.xml**（v1 仅 Phase 8 单独处理 XML，v2 起每 Phase 必检） |
| **res/**/*.xml 中 41 行 FQN 视图标签**（**新增**，跨 21 个 layout 文件） | 运行时 InflateException | §1.8 已表格化 → 每 Phase 任务包含对应 XML 更新；扫描脚本默认覆盖 res/，同时匹配开标签 `<com.besome.sketch.` 与闭合标签 `</com.besome.sketch.` |
| **Phase 14 子阶段必须各自通过编译**（**新增**） | 跨子阶段中间态合并将带入崩溃 | 14a→14b₁→14b₂→14c→14d 每个子 commit 单独运行 `:app:compileDebugJavaWithJavac` 通过后才进入下一子阶段 |
| Phase 14 子阶段间 Manifest 同步 | 中间态 ANR | 每个子 commit 自带对应 Activity 声明（14a:4、14c:21、14d:8）；14b₁/14b₂ 不涉及 Manifest |
| **Phase 15.9 `tools/` 同名冲突**（**新增**） | 同名类合并失败 | Phase 15.9 前先 diff `pro.sketchware.tools/` 5 文件 vs `com.besome.sketch.tools/` 3 文件类名 |
| **Phase 11 字符串字面量反射**（**新增**） | 反射失败 → 运行时 ClassNotFoundException | 全局替换覆盖 `Class.forName("kellinwood…")` 字符串字面量（1 处） |
| Phase 14 palette 52 + item 45 文件已在 v1 通过非迁移方式接受了来自 mod/dev 的新文件 | 该处可能已含部分 pro.sketchware 引用 | Phase 14b₂ 前先做依赖扫描确定起点状态 |
| 跨 Phase 时间窗 build 必须始终绿 | 任意中间态被合并将带入崩溃 | 每个 commit 必须 `:app:compileDebugJavaWithJavac` 通过 |
| 同名类冲突（双重 util 合并、双重 beans 合并） | 编译失败 | Phase 10、13 开始前先做同名扫描 |
| Kotlin 文件 package 声明与 Java 不同 | 漏改 .kt 包名 | v1 已实证：`.kt` 用正则匹配换行后的 package（不带 `;`） |
| 残留 `import com.besome.sketch.` 进入未来代码 | 重新孳生旧路径 | 完成后加 detekt/checkstyle 规则禁止旧包 import |

---

## 七、执行原则（继承 v1 实证经验）

1. **写文件必须 UTF-8 无 BOM**：`[System.IO.File]::ReadAllText/WriteAllText` + `New System.Text.UTF8Encoding($false)`，**禁止用** PowerShell 的 `Set-Content` 默认编码（cp936/cp1252 会破坏 javadoc 中的非 ASCII 字符）

2. **替换列表 longest-prefix-first 排序**：当两个待替换 FQN 中一个是另一个的**前缀**且**目标包不同**时（例如 `mod.X.LocalLibrary` → `pro.sketchware.library.LocalLibrary` vs `mod.X.LocalLibraryImportPackageIndex` → `pro.sketchware.library.OTHER.LocalLibraryImportPackageIndex`），短前缀优先会先把长名截断。.NET `String.Replace` 是子串匹配，无法区分类边界，必须长优先。同目标包时无此问题。

3. **每个 Phase 自带验证三件套**：
   - 全局残留扫描（旧 FQN 在 java/kt/xml 中归零）
   - `:app:compileDebugJavaWithJavac` 通过
   - `git status` 干净后再写 commit message

4. **同包隐式访问陷阱**：当 A 移走、B 留下且 B 引用 A 时，需要主动给 B 加 `import`。v1 中典型案例：`AppSettings ↔ BlocksManager`、`BackupFactory ↔ ConfigActivity`、`BuiltInLibraries ↔ BuildProgressReceiver`

5. **commit message 格式**：`refactor(<scope>): <verb> <subject>`，正文说明文件来源与目标包，列出 Manifest/XML/javadoc 副作用

6. **空目录及时清理**：每个 Phase commit 前清空被腾出的源目录。完成 Phase 15 后 `com/besome/`、`kellinwood/` 必须不存在；`com/bumptech/glide/signature/` 保留（见 §2.2）

---

## 八、立即执行 / 暂缓的判定

**强烈建议立即执行**：
- Phase 10（内部整理）—— 影响小、收益高，统一 util 命名风格

**建议谨慎执行**：
- Phase 11（kellinwood 重命名）—— 单纯的目录改名，对外不构成 API 破坏

**需要预留较大维护窗口再执行**：
- Phase 12（lib 拆分）—— 由于 Base*Activity 牵动面广，建议安排独立 PR 周期
- Phase 13（beans）—— 392 处 import 改动 + 27 文件搬移，可能在 IDE 中引发持久的索引重建
- Phase 14（editor 主体）—— 文件最多、子 commit 最多，建议在团队代码冻结期或独立 feature freeze 周内完成

**永久评估项**：
- Phase 15.1 `Config.java` / `MainDrawer.java` 的具体目标位置 —— 需要 review 这两个文件用途后定（可能在 `pro.sketchware/` 根，也可能在 `tools/` 或 `activities/main/`）

---

## 九、与现有文档的关系

| 文档 | 关系 |
|------|------|
| `package-migration-policy.md` | 通用策略，本文档继承其原则 |
| `package-target-architecture.md` | 总体目标蓝图，本文档为其制定增量步骤 |
| `package-migration-plan.md` (v1) | Phase 1–9 已执行完成，本文档为 Phase 10–15 |
| `package-refactor-kickoff.md` / `package-refactor-plan.md` | 原始重构启动文档，已部分被 v1/v2 取代 |

完成本计划后，`com/`、`kellinwood/` 顶层目录消失，所有业务代码统一至 `pro.sketchware/`，目标架构落地。
