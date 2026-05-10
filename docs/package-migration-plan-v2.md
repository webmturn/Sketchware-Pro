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
| `com/`（其他） | 1 | 残留 |
| `kellinwood/` | 44 | **第三方库**（Ken Ellinwood zipsigner/zipio/logging），来源外部 |

### 1.2 双重 util — 命名风格分裂

| 包 | 文件数 | 命名特征 | 代表文件 |
|----|-------|---------|---------|
| `pro.sketchware.util` | 36（顶层）+ 9（子包） | 混合命名（`Helper`、`UI`、`Network`、`FileUtil`、`SketchwareUtil` 等） | `Helper.java`、`SketchwareUtil.java`、`FileUtil.java` |
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
pro.sketchware.core.fragments/                15 files  通用编辑器/管理 fragment
pro.sketchware.activities.main.fragments/     12 files  主页 fragment 树（projects、projects_store/{adapters,api,classes}）
pro.sketchware.fragments.settings/            15 files  设置子 fragment（appearance 1、block/selector{,/details} 6、events{,/creator,/details} 4、language 4）
```

三个位置的 fragment 性质差异大，但定位重叠，需要统一规则。

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

实测 **34 处**布局 XML 直接以 FQN 引用 `com.besome.sketch.*` 类（v1 Phase 8 已确立的 XML 同步模式）：

| FQN 类 | XML 引用次数 | 涉及阶段 |
|--------|------------|---------|
| `com.besome.sketch.lib.ui.{EasyDeleteEditText,CustomScrollView,CustomHorizontalScrollView,CustomViewPager,CircleImageView}` | 14+ | Phase 12 |
| `com.besome.sketch.editor.view.{ViewPane,ViewProperty,ViewEditor,ViewDummy,ViewEvents,ViewLogicEditor}` | 8 | Phase 14b |
| `com.besome.sketch.editor.view.palette.{PaletteWidget,PaletteFavorite}` | 2 | Phase 14b |
| `com.besome.sketch.editor.logic.{PaletteBlock,LogicTopMenu,PaletteSelector}` | 3 | Phase 14a |
| `com.besome.sketch.editor.{LogicEditorDrawer}` | 1 | Phase 14a |
| `com.besome.sketch.editor.event.CollapsibleEventLayout` | 1 | Phase 14a |
| `com.besome.sketch.editor.component.CollapsibleComponentLayout` | 1 | Phase 14b |
| `com.besome.sketch.editor.manage.ViewBlockCollectionEditor` | 1 | Phase 14c |
| `com.besome.sketch.design.DesignDrawer` | 1 | Phase 15 |
| `com.besome.sketch.MainDrawer` | 1 | Phase 15 |

**所有 Phase 必须在源代码 FQN 替换的同时同步更新 res/**/*.xml**，并在残留扫描中明确包含 XML 文件。

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

**注意**：`com/`、`kellinwood/` 顶层包将完全消失（kellinwood 内容下沉到 `pro.sketchware.third_party.kellinwood`）。

### 2.2 不再需要的包

迁移完成后应删除（顶层目录）：

- `com/besome/sketch/`
- `com/`（如无其他子包）
- `kellinwood/`（移到 `pro.sketchware.third_party.kellinwood`，保留 logging/security/zipio 内部子树原样）

---

## 三、关键设计决策

### 3.1 Fragment 三处合并的策略

**决策**：三处 fragment 全部归到 `pro.sketchware.core.fragments`，按 fragment 用途分子目录：

| 旧位置 | 新位置 | 原因 |
|--------|--------|------|
| `pro.sketchware.core.fragments.*` | 保留 | 已是 core fragments 的家 |
| `pro.sketchware.activities.main.fragments.*` | `pro.sketchware.core.fragments.main.*` | 主页 fragment 也是 fragment |
| `pro.sketchware.fragments.settings.*` | `pro.sketchware.core.fragments.settings.*` | 设置 fragment 也是 fragment |

迁移后顶层 `pro.sketchware.fragments` 整个删除。

### 3.2 双重 util 合并的策略

**决策**：保留单一 `pro.sketchware.util` 顶层，按"领域"细分子包，删除 `pro.sketchware.core.util`：

| 子包 | 文件来源 |
|------|---------|
| `util/` 顶层 | 现 `pro.sketchware.util` 顶层（保留通用工具如 Helper、SketchwareUtil） |
| `util/format/`（新建） | `core.util.{DateTimeUtil,FormatUtil,ReflectiveToString}` |
| `util/io/`（新建） | `core.util.{EncryptedFileUtil,UriPathResolver,ZipUtil,SharedPrefsHelper}` |
| `util/graphics/`（已存在 `graphics/`，合并） | `core.util.{BitmapUtil,NinePatchDecoder,AnimationUtil}` → 进 `pro.sketchware.graphics` |
| `util/`（合入顶层） | `core.util.{ViewUtil,UIHelper,SketchToast,DeviceUtil,ThrottleTimer,GsonMapHelper,MapValueHelper,HashMapTypeToken}` |

合并时若发现同名/同功能（例如 `pro.sketchware.util.UI` vs `pro.sketchware.core.util.UIHelper`），需要单独 review，可能保留较新/更完善的一份。

⚠️ **扁平化警告**：合入后 `pro.sketchware.util/` 顶层将达到 **44 文件**（原 36 + 来自 core.util 顶层的 8）。这接近功能分类的极限。Phase 10 完成后建议**单独评估**是否再细分（例如 `util/cache/`、`util/string/`），**或**接受平铺现状。本计划不强制此细分。

### 3.3 com.besome.sketch.lib 拆分的策略

| 旧位置 | 新位置 |
|--------|--------|
| `com.besome.sketch.lib.base.{BaseAppCompatActivity,BaseDialogActivity,BaseBottomSheetDialogActivity,BasePermissionAppCompatActivity}` | `pro.sketchware.activities.base/` |
| `com.besome.sketch.lib.base.{BaseWidget,CollapsibleLayout,CollapsibleViewHolder}` | `pro.sketchware.widgets/`（视图层基类） |
| `com.besome.sketch.lib.ui.*` 全 12 文件 | `pro.sketchware.widgets/` |

### 3.4 kellinwood 处理策略

**决策**：移动到 `pro.sketchware.third_party.kellinwood`，**保留作者名**作为 vendor 标识，**保留内部 logging/security/zipsigner/zipio 子结构原样**。

理由：
- 这是**第三方代码**（Ken Ellinwood 编写的 zipsigner/zipio/logging 三个独立模块）
- 当前位置 `kellinwood/` 顶层污染 java/ 根
- 取名 `third_party.kellinwood` 而非 `third_party.zipsigner`：zipsigner 只是其中一个子模块
- 改名后可被 `.gitattributes` 标记为 vendored，避免 PR diff 干扰

**特别注意（v1 未涉及，v2 新发现）**：

1. **字符串字面量反射**：`kellinwood.security.zipsigner.ZipSigner` 含 `Class.forName("kellinwood.security.zipsigner.optional.SignatureBlockGenerator")`。Phase 11 必须**同步替换字符串字面量**，不可只换 `import`。
2. **拼写错误保留**：`kellinwood/logging/DeaultLoggerFactory.java`（应为 `DefaultLoggerFactory`）系上游 vendor 原样拼写。Phase 11 **保留原拼写**（属 vendored 代码），并在 commit message 标注以避免 reviewer 误判为新引入错误。

---

## 四、迁移阶段（顺序敏感）

### 阶段编号沿用 v1（v1 用了 1–9），v2 从 10 开始

#### Phase 10 — pro.sketchware 内部整理预备（小型）

| Task | 文件数 | 备注 |
|------|-------|------|
| 10.1 fragment 三处合并到 `core.fragments/{main,settings}` | 12 + 15 = 27 | 同时删除 `pro.sketchware.fragments/` 顶层；注意 `main.fragments` 下含 adapters/api/classes 支撑代码（须随迁） |
| 10.2 `core.util` 非图形部分 → `util/{format,io}` 与顶层合入 | 15 | format 3 + io 4 + 顶层合入 8；检查同名冲突（`util.UI` vs `core.util.UIHelper` 等） |
| 10.3 `core.util` 中图形相关迁到 `graphics/` | 3 | BitmapUtil/NinePatchDecoder/AnimationUtil |

**预估**：~45 文件、3 commit、1–2 小时

#### Phase 11 — kellinwood 重命名为 third_party.kellinwood

| Task | 文件数 |
|------|-------|
| 11.1 `kellinwood/**` → `pro.sketchware.third_party.kellinwood/**`（保留 logging/security/zipio 子树） | 44 |
| 11.2 更新 35 处 `import kellinwood.` | — |
| 11.3 更新 1 处 `Class.forName("kellinwood.…")` 字符串字面量 | — |
| 11.4 保留 `DeaultLoggerFactory` 拼写错误（vendor 原样），在 commit message 中说明 | — |

**预估**：~44 文件、1 commit、30 分钟

#### Phase 12 — com.besome.sketch lib 拆分（含 Activity 基类）

| Task | 文件数 |
|------|-------|
| 12.1 `com.besome.sketch.lib.base.Base*Activity` 4 个 → `pro.sketchware.activities.base/` | 4 |
| 12.2 `com.besome.sketch.lib.base.{BaseWidget,CollapsibleLayout,CollapsibleViewHolder}` → `widgets/` | 3 |
| 12.3 `com.besome.sketch.lib.ui/*` 12 个 → `widgets/` | 12 |

**预估**：~19 文件 + **实测 85 处 import** + **14+ 处 res/**/*.xml widget 标签**、1–2 commit、1 小时

⚠️ **风险**：实测 **79 个文件**继承 4 个 `Base*Activity` 之一；`com.besome.sketch.lib.ui.*` 在 14+ 处 XML 中作 widget 标签（`<com.besome.sketch.lib.ui.EasyDeleteEditText>` 等），必须同步更新。

#### Phase 13 — com.besome.sketch.beans 整体迁移

| Task | 文件数 |
|------|-------|
| 13.1 `com.besome.sketch.beans/*` 27 个 → `pro.sketchware.beans/` | 27 |
| 13.2 与现有 `pro.sketchware.beans/` 5 个文件合并（检查命名冲突） | — |

**预估**：~27 文件 + **实测 392 处 import**、1 commit、1 小时

⚠️ **风险**：bean 类是数据流核心，使用最广泛。392 import 行远超第二名 editor 的 283，是 com.besome.sketch 中最被引用的子包。Phase 13 前先做 `pro.sketchware.beans` 5 文件与 `com.besome.sketch.beans` 27 文件的同名扫描。

#### Phase 14 — com.besome.sketch.editor 主体迁移（最大）

按 v1 风格分多个子 commit：

| 子阶段 | 范围 | 文件数 | Manifest Activity | XML 标签 |
|--------|------|-------|-------------------|---------|
| 14a | `com.besome.sketch.editor` 顶层 + `event/`、`logic/`、`makeblock/` | 3 + 2 + 5 + 3 = 13 | 4（editor 2、event 1、makeblock 1） | 5（LogicEditorDrawer + 3 logic + CollapsibleEventLayout） |
| 14b | `editor/component/`、`property/`、`view/`（含 palette、item） | 3 + 16 + 14 + 52 + 45 = 130 | 0 | 11（6 view + 2 palette + CollapsibleComponentLayout + 其余） |
| 14c | `editor/manage/`（含 font/image/sound/view 子包，**不含** library） | 6 + 6 + 4 + 4 + 4 = 24 | 21 | 1（ViewBlockCollectionEditor） |
| 14d | `editor/manage/library/*`（含 admob/compat/firebase/googlemap/material3） | 4 + 7 + 1 + 2 + 1 + 3 = 18 | 8 | 0 |

**预估**：~185 文件、4 commit、3–4 小时
**关键约束**：14a→14b→14c→14d 必须**逐 commit** 通过 `:app:compileDebugJavaWithJavac`；不允许跨子阶段中间态合并。理由：palette/item 大量引用 view 顶层，editor 顶层 `LogicEditorActivity` 引用 view 子树；任意中间态停留都会出现“已搬走的 X 引用尚在原位的 Y”。

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
**完成本阶段后**：删除 `com/` 顶层目录

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
Phase 14 ─ editor 主体（最大，4 个子 commit）
        │
        ▼
Phase 15 ─ 收尾子包（删除 com/ 顶层）
```

### 总规模与时间预估

| 项 | 文件数 | commit | 时间 |
|----|-------|--------|------|
| Phase 10 | ~45 | 3 | 1–2 h |
| Phase 11 | ~44 | 1 | 30 m |
| Phase 12 | ~19 | 1–2 | 1 h |
| Phase 13 | ~27 | 1 | 1 h |
| Phase 14 | ~185 | 4 | 3–4 h |
| Phase 15 | ~22 | 2–3 | 1–2 h |
| **合计** | **~342** | **12–14** | **8–12 h** |

---

## 六、风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| Phase 12 Base*Activity 牵动几乎所有 Activity import | 实测 79 文件继承 + 85 import 行 | 使用 v1 实证有效的“longest-prefix-first 全局替换 + 自动残留扫描”流程；同步更新 14+ 处 XML widget 标签 |
| Phase 13 beans 类型在 res/xml 也被引用（XML inflate） | 运行时 InflateException | Phase 13 与所有 Phase 残留扫描必须**包含 res/**/*.xml**（v1 仅 Phase 8 单独处理 XML，v2 起每 Phase 必检） |
| **res/**/*.xml 中 34 处 FQN 视图标签**（**新增**） | 运行时 InflateException | §1.8 已表格化 → 每 Phase 任务包含对应 XML 更新；扫描脚本默认覆盖 res/ |
| **Phase 14 子阶段必须各自通过编译**（**新增**） | 跨子阶段中间态合并将带入崩溃 | 14a→14b→14c→14d 每个子 commit 单独运行 `:app:compileDebugJavaWithJavac` 通过后才进入下一子阶段 |
| Phase 14 子阶段间 Manifest 同步 | 中间态 ANR | 每个子 commit 自带对应 Activity 声明（14a:4、14c:21、14d:8） |
| **Phase 15.9 `tools/` 同名冲突**（**新增**） | 同名类合并失败 | Phase 15.9 前先 diff `pro.sketchware.tools/` 5 文件 vs `com.besome.sketch.tools/` 3 文件类名 |
| **Phase 11 字符串字面量反射**（**新增**） | 反射失败 → 运行时 ClassNotFoundException | 全局替换覆盖 `Class.forName("kellinwood…")` 字符串字面量（1 处） |
| Phase 14 palette 52 + item 45 文件已在 v1 通过非迁移方式接受了来自 mod/dev 的新文件 | 该处可能已含部分 pro.sketchware 引用 | Phase 14b 前先做依赖扫描确定起点状态 |
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

6. **空目录及时清理**：每个 Phase commit 前清空被腾出的源目录。完成 Phase 15 后顶层 `com/`、`kellinwood/` 必须不存在

---

## 八、立即执行 / 暂缓的判定

**强烈建议立即执行**：
- Phase 10（内部整理）—— 影响小、收益高，统一 util 命名风格

**建议谨慎执行**：
- Phase 11（kellinwood 重命名）—— 单纯的目录改名，对外不构成 API 破坏

**需要预留较大维护窗口再执行**：
- Phase 12（lib 拆分）—— 由于 Base*Activity 牵动面广，建议安排独立 PR 周期
- Phase 13（beans）—— 600+ import 改动，可能在 IDE 中引发持久的索引重建
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
