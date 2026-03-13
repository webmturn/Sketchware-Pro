# 本次会话修改审查

> 审查日期：2026-03-13

---

## 一、LogicEditorActivity.java

### 1. 逻辑编辑器块加载优化（防卡顿）

| 改动 | 说明 |
|------|------|
| `BLOCK_LOAD_CHUNK_SIZE = 80` | 每批加载 80 个块 |
| `loadEventBlocks(Runnable onComplete)` | 支持完成回调 |
| `scheduleChunkedBlockAdd()` | 分块添加，`Choreographer.postFrameCallback` 分帧 |
| `connectBlocksAndLayout()` | 连接后 `postFrameCallback` 执行 layoutChain + updatePaneSize |
| `loadBlockCollections()` | 延后到 onComplete，再 `postFrameCallback` 一帧 |

**验证**：空块、非空块、onComplete 为 null 的分支均有处理。

### 2. 反编译残留重命名

| 原名称 | 新名称 | 位置 |
|--------|--------|------|
| `ss` | `fieldBlock` | setFieldValue, pickImage, showColorPicker, showStringInput, showFontPicker, showIntentDataInput, showViewSelector, showSoundPicker, showTypefaceSelector 参数及方法体 |
| `var ProjectDataStore` | `var projectDataStore` | showViewSelector、showNumberOrStringInput 内 onBindCustomView 分支 |

**保留**：`instanceof FieldBlockView ss` 中的 `ss` 为 Java 模式变量，非残留。

---

## 二、ViewEditor.java

| 原名称 | 新名称 |
|--------|--------|
| `syVar` | `previousSelected`（updateSelection） |
| `syVar` | `firstItemView`（addViews） |
| `syVar` | `itemView`（setSelectedItem 参数） |
| `aVar` | `layoutType`（addWidgetLayout 参数） |
| `bVar` | `widgetType`（addWidget 参数） |

---

## 三、ExtraPaletteBlock.java

| 原名称 | 新名称 |
|--------|--------|
| `var ProjectDataStore` | `var projectDataStore` | 2 处（onBindCustomView、blockCustomViews） |

---

## 四、ManageLocalLibrary.java

| 改动 | 说明 |
|------|------|
| `new FilePathUtil().getPathLocalLibrary()` | → `FilePathUtil.getPathLocalLibrary()`（静态调用） |
| `getNativeLibs()` | 增加 `getParentFile() == null` 检查，避免 NPE |

---

## 五、文档

| 文件 | 改动 |
|------|------|
| README.md | Source Code Map：`a.a.a.*` → `pro.sketchware.core.*` |
| block-programming-bottleneck-analysis.md | 6.1 补充块级搜索已有 |
| local-library-issues.md | 新增：已修复项、已有功能、已知限制 |
| local-library-dependency-order-solution.md | 新增：依赖顺序方案（拓扑排序等） |

---

## 六、潜在风险点

1. **Choreographer 回调**：`postFrameCallback` 在 Activity 销毁后仍可能执行，已通过 `getActivity() != null` 检查。
2. **LoadEventBlocksTask**：`onComplete` 中再次 `postFrameCallback` 调用 `loadBlockCollections()`，若 Activity 已 finish，`getActivity()` 为 null 会安全跳过。
3. **pattern variable `ss`**：与参数 `fieldBlock` 作用域不同，无命名冲突。

---

## 七、未修改文件（来自其他会话）

以下文件有 git 变更但非本次会话修改：ShowWidgetCollectionActivity、AddViewActivity、PropertyIndentItem、ViewPropertyItems、ProjectsStoreFragment、fragment_projects_store.xml、colors、strings 等。请结合对应提交或会话单独审查。
