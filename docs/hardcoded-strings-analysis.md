# 硬编码字符串分析 - 国际化指南

本文档详细记录了 Sketchware Pro 代码库中需要提取到 `strings.xml` 以支持国际化的硬编码英文字符串。

## 最新状态 (2026-02-25)

### 国际化架构

项目使用自定义语言覆盖机制（`LanguageOverrideManager` + `Helper.getResString()`），而非 Android 原生 `values-xx/` 多语言目录。

| 指标 | 数量 | 说明 |
|------|------|------|
| **strings.xml 已有条目** | **~3370+** | 包含本次新增的 16 条 |
| **使用 Helper.getResString()** | **~1246 处 / 143 文件** | ✅ 支持语言覆盖 |
| **使用 context.getString()** | **~355 处 / 82 文件** | ⚠️ 绕过覆盖机制 |
| **翻译资源目录 (values-xx/)** | **0 个** | ❌ 无内置翻译 |

### 已完成的提取工作

| 批次 | 文件 | 提取数 | 新增字符串资源 |
|------|------|--------|----------------|
| 批次1 | 大量文件（历史提交） | ~800+ | 绝大部分对话框/按钮/菜单/Toast |
| 批次2 (本次) | BackupRestoreManager | 2 | `backup_restore_local_libs_single/multi` |
| 批次2 (本次) | CustomBlocksDialog | 3 | `custom_blocks_used_count/missing_one/missing_multi` |
| 批次2 (本次) | ExcludeBuiltInLibrariesActivity | 5 | `library_default_description/excluded_count` + 菜单 |
| 批次2 (本次) | DesignActivity | 1 | `design_error_install_failed` |
| 批次2 (本次) | 5个资源编辑器 | 10 | `resource_type_*` (10个类型名) |

## 原始统计概览（初始扫描时）

| 类别 | 数量 |
|------|------|
| **Java代码硬编码** | **~853处** |
| **XML布局硬编码 (text+hint)** | **~324处** |
| **Preference XML** | **~18处** |
| **Menu XML** | **~64处** |
| **硬编码总计** | **~1259处** |
| **文档章节** | **92** |
| **已审查Java文件** | **325+** |
| **已审查XML文件** | **90个** |
| **扫描维度** | **19个** |

> **注意**: 上述原始统计中的大部分高优先级项目已在历史提交中完成国际化。
> 当前实际剩余的用户可见硬编码字符串主要集中在技术术语和内部标识符，不需要翻译。

---

## 一、UI组件与对话框

### 1.1 对话框标题 (Dialog Titles) — ✅ 全部已完成

| 文件路径 | 硬编码字符串 | 状态 |
|----------|--------------|------|
| `LogReaderActivity.java` | `"Filter by package name"` | ✅ `R.string.logcat_title_filter_package` |
| `ExcludeBuiltInLibrariesActivity.java` | `"Exclude built-in libraries"` | ✅ `R.string.library_title_exclude` |
| `ExcludeBuiltInLibrariesActivity.java` | `"Select built-in libraries"` | ✅ `R.string.library_title_select` |
| `BackupRestoreManager.java` | `"Backup Options"` | ✅ `R.string.backup_title_options` |
| `BackupRestoreManager.java` | `"Warning"` | ✅ `R.string.common_word_warning` |
| `BackupRestoreManager.java` | `"Please wait"` | ✅ `R.string.common_word_please_wait` |
| `MoreblockImporterDialog.java` | `"Select a more block"` | ✅ `R.string.logic_more_block_title_select` |
| `CustomBlocksDialog.java` | `"Import Custom blocks to"` | ✅ `R.string.blocks_title_import_to` |
| `CustomBlocksDialog.java` | `"Create a new palette"` | ✅ `R.string.blocks_title_create_palette` |

### 1.2 对话框消息 (Dialog Messages) — ✅ 全部已完成

| 文件路径 | 硬编码字符串 | 状态 |
|----------|--------------|------|
| `LogReaderActivity.java` | `"For multiple package names..."` | ✅ `R.string.logcat_msg_filter_hint` |
| `ExcludeBuiltInLibrariesActivity.java` | `"Reset excluded built-in libraries?..."` | ✅ `R.string.library_msg_reset_confirm` |
| `BackupRestoreManager.java` | `"Creating backup..."` | ✅ `R.string.backup_msg_creating` |
| `BackupRestoreManager.java` | `"Restoring..."` | ✅ `R.string.backup_msg_restoring` |
| `BackupRestoreManager.java` | Local libraries restore message (单/多) | ✅ `R.string.backup_restore_local_libs_single/multi` (本次) |

### 1.3 按钮标签 (Button Labels) — ✅ 全部已完成

| 文件路径 | 硬编码字符串 | 状态 |
|----------|--------------|------|
| `BackupRestoreManager.java` | `"Copy"` | ✅ `R.string.backup_button_copy_libs` |
| `CustomBlocksDialog.java` | `"Create new palette"` | ✅ `R.string.blocks_button_create_palette` |

---

## 二、菜单与导航

### 2.1 额外菜单项

**文件**: `pro/sketchware/menu/DefaultExtraMenuBean.java`

| 行号范围 | 硬编码内容 | 说明 |
|----------|-----------|------|
| 全文件 | 18处菜单项名称 | 扩展菜单功能名称 |

**文件**: `pro/sketchware/menu/ExtraMenuBean.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 549 | `"Custom Activities"` | `menu_custom_activities` |

### 2.2 块调色板

**文件**: `dev/aldi/sayuti/block/ExtraPaletteBlock.java`

| 数量 | 内容 | 说明 |
|------|------|------|
| 15处 | 块类别名称 | 逻辑编辑器调色板分类名 |

---

## 三、编辑器与代码功能

### 3.1 源代码编辑器

**文件**: `mod/hey/studios/code/SrcCodeEditor.java`

| 数量 | 内容类型 | 优先级 |
|------|----------|--------|
| 14处 | 编辑器UI文本 | 高 |

**文件**: `mod/hey/studios/lib/code_editor/CodeEditorLayout.java`

| 数量 | 内容类型 | 优先级 |
|------|----------|--------|
| 12处 | 编辑器布局文本 | 高 |

### 3.2 块管理器

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerDetailsActivity.java`

| 数量 | 内容类型 | 示例 |
|------|----------|------|
| 15处 | 块管理UI | 块属性、操作按钮等 |

**文件**: `mod/hilal/saif/activities/tools/BlocksManager.java`

| 数量 | 内容类型 |
|------|----------|
| 6处 | 块列表管理 |

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerCreatorActivity.java`

| 数量 | 内容类型 |
|------|----------|
| 5处 | 块创建界面 |

### 3.3 块处理器

**文件**: `mod/hilal/saif/blocks/BlocksHandler.java`

| 数量 | 内容类型 |
|------|----------|
| 13处 | 块操作相关 |

---

## 四、项目管理

### 4.1 备份与恢复

**文件**: `mod/hey/studios/project/backup/BackupRestoreManager.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 136 | `"Select backups to restore"` | `backup_select_to_restore` |
| 68 | `"Backup Options"` | `backup_options_title` |
| 148 | `"Warning"` | `common_word_warning` |
| 151 | `"Copy"` | `common_word_copy` |
| 191 | `"Creating backup..."` | `backup_creating` |
| 193 | `"Please wait"` | `common_word_please_wait` |
| 247 | `"Restoring..."` | `backup_restoring` |

### 4.2 主题管理

**文件**: `com/besome/sketch/projects/ThemeManager.java`

| 数量 | 内容类型 | 说明 |
|------|----------|------|
| 13处 | 主题名称/描述 | 应用主题选项 |

---

## 五、库管理

### 5.1 库设置

**文件**: `mod/jbk/editor/manage/library/ExcludeBuiltInLibrariesActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 188 | `"Exclude built-in libraries"` | `library_exclude_title` |
| 281 | `"Reset excluded built-in libraries?..."` | `library_reset_confirm` |
| 297 | `"Select built-in libraries"` | `library_select_title` |

**文件**: `mod/jbk/editor/manage/library/LibrarySettingsImporter.java`

| 状态 | 说明 |
|------|------|
| ✅ 已国际化 | 使用 `R.string.design_library_*` |

### 5.2 本地库管理

**文件**: `mod/hey/studios/activity/managers/nativelib/ManageNativelibsActivity.java`

| 数量 | 内容类型 |
|------|----------|
| 6处 | 本地库管理UI |

---

## 六、Android Manifest 注入

### 6.1 Manifest 编辑器

**文件**: `mod/hilal/saif/activities/android_manifest/AndroidManifestInjection.java`

| 数量 | 内容类型 |
|------|----------|
| 6处 | Manifest编辑UI |

**文件**: `mod/hilal/saif/activities/android_manifest/AndroidManifestInjectionDetails.java`

| 数量 | 内容类型 |
|------|----------|
| 4处 | 详情页面UI |

---

## 七、设置与配置

### 7.1 应用设置

**文件**: `mod/hilal/saif/activities/tools/AppSettings.java`

| 数量 | 内容类型 | 示例 |
|------|----------|------|
| 5处 | 设置项文本 | APK签名相关 |

### 7.2 事件管理器

**文件**: `pro/sketchware/fragments/settings/events/EventsManagerFragment.java`

| 状态 | 说明 |
|------|------|
| ✅ 大部分已国际化 | 使用 R.string |

**文件**: `pro/sketchware/fragments/settings/events/creator/EventsManagerCreatorFragment.java`

| 状态 | 说明 |
|------|------|
| ⚠️ 少量硬编码 | 需检查 |

---

## 八、组件系统

### 8.1 组件处理

**文件**: `mod/hilal/saif/components/ComponentsHandler.java`

| 数量 | 内容类型 |
|------|----------|
| 6处 | 组件名称 |

**文件**: `mod/hilal/saif/components/ComponentExtraCode.java`

| 数量 | 内容类型 |
|------|----------|
| 1处 | 组件代码 |

### 8.2 事件处理

**文件**: `mod/hilal/saif/events/EventsHandler.java`

| 数量 | 内容类型 |
|------|----------|
| 6处 | 事件名称 |

---

## 九、资源管理

### 9.1 资源管理器

**文件**: `mod/agus/jcoderz/editor/manage/resource/ManageResourceActivity.java`

| 数量 | 内容类型 |
|------|----------|
| 5处 | 资源管理UI |

### 9.2 资产管理器

**文件**: `mod/hey/studios/activity/managers/assets/ManageAssetsActivity.java`

| 数量 | 内容类型 |
|------|----------|
| 4处 | 资产管理UI |

### 9.3 Java文件管理

**文件**: `mod/hey/studios/activity/managers/java/ManageJavaActivity.java`

| 数量 | 内容类型 |
|------|----------|
| 3处 | Java文件管理UI |

---

## 十、第三方代码（排除）

以下目录包含第三方代码，**不应修改**用于国际化：

| 目录 | 说明 | 原因 |
|------|------|------|
| `mod/agus/jcoderz/dx/` | Android dx/dex 工具 | 来自AOSP |
| `mod/agus/jcoderz/dex/` | Dex 操作库 | 第三方库 |
| `kellinwood/` | ZIP 签名库 | 第三方库 |

---

## 十一、良好实践示例

以下文件展示了正确的国际化实践：

| 文件 | 使用模式 | 示例 |
|------|----------|------|
| `LibrarySettingsImporter.java` | `R.string.design_library_*` | `R.string.design_library_title_select_project` |
| `MoreblockImporter.java` | `R.string.logic_more_block_*` | `R.string.logic_more_block_title_add_variable_resource` |
| `WidgetsCreatorManager.java` | `Helper.getResString()` | `Helper.getResString(R.string.common_word_save)` |
| `CustomBlocksDialog.java` | `Helper.getResString()` | `Helper.getResString(R.string.used_custom_blocks)` |

---

## 十二、优先级建议

### 高优先级（用户直接可见）
1. 对话框标题和消息
2. 菜单项名称
3. 按钮标签
4. 错误消息和提示

### 中优先级
1. 块/组件名称
2. 主题名称
3. 编辑器UI文本

### 低优先级
1. 日志消息
2. 调试字符串
3. 内部错误消息

---

## 十三、实现指南

### 步骤 1: 添加字符串到 `res/values/strings.xml`

```xml
<!-- 对话框标题 -->
<string name="dialog_title_filter_package">Filter by package name</string>
<string name="dialog_title_backup_options">Backup Options</string>
<string name="dialog_title_please_wait">Please wait</string>

<!-- 对话框消息 -->
<string name="dialog_msg_package_filter_hint">For multiple package names, separate them with a comma (,).</string>
<string name="dialog_msg_creating_backup">Creating backup...</string>

<!-- 按钮 -->
<string name="button_copy">Copy</string>
<string name="button_create_palette">Create new palette</string>
```

### 步骤 2: 替换 Java 代码中的硬编码字符串

```java
// 修改前
.setTitle("Filter by package name")

// 修改后 - 方式1（Activity/Fragment中）
.setTitle(R.string.dialog_title_filter_package)

// 修改后 - 方式2（使用Helper）
.setTitle(Helper.getResString(R.string.dialog_title_filter_package))

// 修改后 - 方式3（需要Context）
.setTitle(context.getString(R.string.dialog_title_filter_package))
```

### 步骤 3: 创建翻译文件

```
res/values/strings.xml        (英文 - 默认)
res/values-zh/strings.xml     (中文)
res/values-zh-rTW/strings.xml (繁体中文)
res/values-es/strings.xml     (西班牙语)
res/values-pt/strings.xml     (葡萄牙语)
res/values-ru/strings.xml     (俄语)
res/values-ar/strings.xml     (阿拉伯语)
res/values-ja/strings.xml     (日语)
res/values-ko/strings.xml     (韩语)
```

---

## 十四、工作量估算

| 任务 | 预估时间 | 说明 |
|------|----------|------|
| 提取~500个字符串到strings.xml | 2-3天 | 逐文件处理 |
| 测试UI布局（防止截断） | 1-2天 | 不同语言长度不同 |
| 设置翻译平台（Crowdin/Weblate） | 1天 | 推荐使用 |
| 社区翻译（每种语言） | 持续进行 | 需要母语者 |

---

## 十五、命名规范建议

### 字符串键名格式

```
[模块]_[类型]_[描述]
```

### 常用前缀

| 前缀 | 用途 | 示例 |
|------|------|------|
| `dialog_title_` | 对话框标题 | `dialog_title_backup_options` |
| `dialog_msg_` | 对话框消息 | `dialog_msg_creating_backup` |
| `button_` | 按钮文本 | `button_copy` |
| `menu_` | 菜单项 | `menu_custom_activities` |
| `hint_` | 输入提示 | `hint_enter_name` |
| `error_` | 错误消息 | `error_file_not_found` |
| `toast_` | Toast消息 | `toast_save_success` |
| `label_` | 标签文本 | `label_project_name` |

---

---

## 十六、com.besome.sketch 包详细分析

### 16.1 导出功能 (export/)

**文件**: `com/besome/sketch/export/ExportProjectActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 267 | `"Important note"` | `export_dialog_title_important` |
| 268 | `"The generated .aab file must be signed..."` | `export_msg_aab_sign_required` |
| 271 | `"Understood"` | `button_understood` |
| 334 | `"Important note"` | `export_dialog_title_important` |
| 335-338 | `"To sign an APK, you need a keystore..."` | `export_msg_apk_sign_info` |
| 763 | `"Finished exporting AAB"` | `export_title_aab_complete` |
| 764 | `"You can find the generated, signed AAB file at:..."` | `export_msg_aab_location` |

### 16.2 编辑器视图 (editor/view/)

**文件**: `com/besome/sketch/editor/view/ViewEvents.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 140 | `"Confirm Delete"` | `dialog_title_confirm_delete` |
| 141 | `"Click on Confirm to delete the selected Event."` | `dialog_msg_delete_event_confirm` |

### 16.3 错误收集 (tools/)

**文件**: `com/besome/sketch/tools/CollectErrorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 39-41 | `"An error occurred while running Sketchware Pro..."` | `error_msg_crash_report` |
| 42 | `"Copy"` | `button_copy` |

---

## 十七、mod 包详细分析

### 17.1 Logcat 阅读器

**文件**: `mod/khaled/logcat/LogReaderActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 130 | `"Filter by package name"` | `logcat_title_filter_package` |
| 131 | `"For multiple package names, separate them with a comma (,)."` | `logcat_msg_filter_hint` |

### 17.2 自定义块

**文件**: `mod/hey/studios/project/custom_blocks/CustomBlocksDialog.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 140 | `"Import Custom blocks to"` | `blocks_title_import_to` |
| 142 | `"Create new palette"` | `blocks_button_create_palette` |
| 187 | `"Create a new palette"` | `blocks_title_create_palette` |

### 17.3 备份恢复

**文件**: `mod/hey/studios/project/backup/BackupRestoreManager.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 249 | `"Please wait"` | `common_word_please_wait` |

### 17.4 库排除

**文件**: `mod/jbk/editor/manage/library/ExcludeBuiltInLibrariesActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 281 | `"Reset excluded built-in libraries? This action cannot be undone."` | `library_msg_reset_confirm` |

---

## 十八、pro.sketchware 包详细分析

### 18.1 组件帮助器

**文件**: `pro/sketchware/tools/ComponentHelper.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 22 | `"Component."` | `component_prefix` |

### 18.2 额外菜单

**文件**: `pro/sketchware/menu/ExtraMenuBean.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 十九、完整文件清单

### 需要国际化的文件（按优先级排序）

#### 高优先级（用户直接可见） — ✅ 全部已完成

| 序号 | 文件路径 | 原硬编码数 | 状态 |
|------|----------|----------|------|
| 1 | `export/ExportProjectActivity.java` | 7 | ✅ 已国际化 |
| 2 | `tools/CollectErrorActivity.java` | 2 | ✅ 已国际化 |
| 3 | `editor/view/ViewEvents.java` | 2 | ✅ 已国际化 |
| 4 | `mod/khaled/logcat/LogReaderActivity.java` | 2 | ✅ 已国际化 |
| 5 | `mod/hey/studios/project/backup/BackupRestoreManager.java` | 8 | ✅ 已国际化（本次完成最后2处） |
| 6 | `mod/jbk/editor/manage/library/ExcludeBuiltInLibrariesActivity.java` | 3 | ✅ 已国际化（本次完成） |
| 7 | `mod/hey/studios/project/custom_blocks/CustomBlocksDialog.java` | 3 | ✅ 已国际化（本次完成） |

#### 中优先级 — ✅ 大部分已完成

| 序号 | 文件路径 | 原硬编码数 | 状态 |
|------|----------|----------|------|
| 8 | `pro/sketchware/menu/DefaultExtraMenuBean.java` | 18 | ✅ 已国际化 (41处getResString) |
| 9 | `dev/aldi/sayuti/block/ExtraPaletteBlock.java` | 15 | ✅ 已国际化 (75处getResString) |
| 10 | `mod/hilal/saif/activities/tools/BlocksManagerDetailsActivity.java` | 15 | ✅ 已国际化 (20处R.string) |
| 11 | `mod/hey/studios/code/SrcCodeEditor.java` | 14 | ✅ 已国际化 (9处getResString) |
| 12 | `com/besome/sketch/projects/ThemeManager.java` | 13 | ⚠️ 内部主题标识符 |
| 13 | `mod/hilal/saif/blocks/BlocksHandler.java` | 13 | ⚠️ 块操作码/技术标识符 |
| 14 | `mod/hey/studios/lib/code_editor/CodeEditorLayout.java` | 12 | ⚠️ 编辑器内部标识 |

#### 低优先级（内部/调试） — 不需要翻译

| 序号 | 文件路径 | 硬编码数 | 状态 |
|------|----------|----------|------|
| 15 | `pro/sketchware/core/BlockColorMapper.java` | 9 | ⏭️ 块操作码映射，技术标识符 |
| 16 | `mod/hilal/saif/activities/tools/BlocksManager.java` | 6 | ✅ 已国际化 |
| 17 | `mod/hilal/saif/components/ComponentsHandler.java` | 6 | ⏭️ 组件技术名称 |
| 18 | `mod/hilal/saif/events/EventsHandler.java` | 6 | ⏭️ 事件技术名称 |

---

## 二十、strings.xml 模板

以下是建议添加到 `res/values/strings.xml` 的字符串：

```xml
<!-- ========== 通用词汇 ========== -->
<string name="common_word_warning">Warning</string>
<string name="common_word_please_wait">Please wait</string>
<string name="button_copy">Copy</string>
<string name="button_understood">Understood</string>

<!-- ========== 导出功能 ========== -->
<string name="export_dialog_title_important">Important note</string>
<string name="export_msg_aab_sign_required">The generated .aab file must be signed.\nCopy your keystore to /Internal storage/sketchware/keystore/release_key.jks and enter the alias\' password.</string>
<string name="export_msg_apk_sign_info">To sign an APK, you need a keystore. Use your already created one, and copy it to /Internal storage/sketchware/keystore/release_key.jks and enter the alias\'s password.</string>
<string name="export_title_aab_complete">Finished exporting AAB</string>
<string name="export_msg_aab_location">You can find the generated, signed AAB file at:\n/Internal storage/sketchware/signed_aab/%s</string>

<!-- ========== 备份恢复 ========== -->
<string name="backup_title_options">Backup Options</string>
<string name="backup_title_select_restore">Select backups to restore</string>
<string name="backup_msg_creating">Creating backup…</string>
<string name="backup_msg_restoring">Restoring…</string>

<!-- ========== 库管理 ========== -->
<string name="library_title_exclude">Exclude built-in libraries</string>
<string name="library_title_select">Select built-in libraries</string>
<string name="library_msg_reset_confirm">Reset excluded built-in libraries? This action cannot be undone.</string>

<!-- ========== 自定义块 ========== -->
<string name="blocks_title_import_to">Import Custom blocks to</string>
<string name="blocks_title_create_palette">Create a new palette</string>
<string name="blocks_button_create_palette">Create new palette</string>

<!-- ========== Logcat ========== -->
<string name="logcat_title_filter_package">Filter by package name</string>
<string name="logcat_msg_filter_hint">For multiple package names, separate them with a comma (,).</string>

<!-- ========== 编辑器 ========== -->
<string name="dialog_title_confirm_delete">Confirm Delete</string>
<string name="dialog_msg_delete_event_confirm">Click on Confirm to delete the selected Event.</string>

<!-- ========== 错误报告 ========== -->
<string name="error_msg_crash_report">An error occurred while running Sketchware Pro. Do you want to report this error log so that we can fix it? No personal information will be included.</string>

<!-- ========== 菜单 ========== -->
<string name="menu_custom_activities">Custom Activities</string>
```

---

---

## 二十一、a.a.a 包详细分析

### 22.1 Firebase预览视图

**文件**: `a/a/a/FirebasePreviewView.java`

| 状态 | 说明 |
|------|------|
| ✅ 已国际化 | 使用 `Helper.getResString(R.string.*)` |

### 22.2 权限Fragment

**文件**: `a/a/a/PermissionFragment.java`

| 状态 | 说明 |
|------|------|
| ✅ 已国际化 | 使用 `R.string.common_message_permission_*` |

### 22.3 事件列表Fragment

**文件**: `a/a/a/EventListFragment.java`

| 状态 | 说明 |
|------|------|
| ✅ 已国际化 | 使用 `R.string.logic_more_block_*` |

### 22.4 代码生成器（内部字符串）

以下文件包含代码生成用的字符串，**不需要翻译**：

| 文件 | 说明 |
|------|------|
| `BlockInterpreter.java` | 生成Java代码的模板 |
| `ComponentCodeGenerator.java` | 组件代码生成模板 |
| `BlockColorMapper.java` | 块操作码映射 |

---

## 二十二、已完成国际化的文件（参考）

以下文件展示了良好的国际化实践，可作为参考：

### 23.1 完全国际化的文件

| 文件路径 | 使用的资源前缀 |
|----------|---------------|
| `a/a/a/FirebasePreviewView.java` | `R.string.common_word_*`, `R.string.design_library_*` |
| `a/a/a/PermissionFragment.java` | `R.string.common_message_permission_*` |
| `a/a/a/EventListFragment.java` | `R.string.logic_more_block_*` |
| `mod/jbk/editor/manage/library/LibrarySettingsImporter.java` | `R.string.design_library_*` |
| `mod/jbk/editor/manage/MoreblockImporter.java` | `R.string.logic_more_block_*` |

### 23.2 国际化模式示例

```java
// 模式1: 直接使用R.string
dialog.setTitle(R.string.common_word_warning);
dialog.setMessage(R.string.myprojects_settings_message_package_rename);

// 模式2: 使用Helper工具类
dialog.setTitle(Helper.getResString(R.string.common_word_warning));

// 模式3: 使用Context.getString()
dialog.setTitle(context.getString(R.string.common_word_warning));
```

---

## 二十三、不需要翻译的字符串类型

以下类型的字符串**不需要**提取到strings.xml：

| 类型 | 示例 | 原因 |
|------|------|------|
| 代码模板 | `"setTitle(%s);"` | Java代码生成 |
| 技术标识符 | `"hideKeyboard"`, `"doToast"` | 内部块标识 |
| 文件路径 | `"/sketchware/keystore/"` | 系统路径 |
| 日志标签 | `"TAG"`, `"LogReaderActivity"` | 调试用途 |
| 正则表达式 | `"[a-z]+"` | 技术匹配 |

---

## 二十四、翻译平台推荐

### 推荐使用 Crowdin 或 Weblate

| 平台 | 优点 | 适用场景 |
|------|------|----------|
| **Crowdin** | 免费开源项目、社区翻译、CI集成 | 推荐 |
| **Weblate** | 自托管选项、Git集成 | 备选 |
| **POEditor** | 简单易用 | 小型项目 |

### 设置步骤

1. 在平台创建项目
2. 上传 `res/values/strings.xml`
3. 邀请社区翻译者
4. 设置自动同步到GitHub

---

## 二十五、检查清单

### 国际化工作检查清单

- [ ] 提取所有对话框标题到strings.xml
- [ ] 提取所有对话框消息到strings.xml
- [ ] 提取所有按钮文本到strings.xml
- [ ] 提取所有菜单项名称到strings.xml
- [ ] 提取所有Toast消息到strings.xml
- [ ] 提取所有错误消息到strings.xml
- [ ] 创建翻译文件夹结构
- [ ] 设置翻译平台
- [ ] 测试不同语言的UI布局
- [ ] 处理从右到左(RTL)语言支持

---

---

## 二十六、pro.sketchware.activities 包详细分析

### 27.1 资源编辑器

**文件**: `pro/sketchware/activities/resourceseditor/ResourcesEditorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 206 | `"Open code editor"` | `resources_button_open_code_editor` |

> **注意**: 大部分已使用 `Helper.getResString(R.string.*)` ✅

### 27.2 主题编辑器

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/ThemesEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 125 | `"Create new theme"` | `theme_title_create` |
| 126 | `"Create"` | `button_create` |
| 162 | `"Edit theme"` | `theme_title_edit` |
| 163 | `"Edit"` | `button_edit` |
| 185 | `"Warning"` | `common_word_warning` |
| 186 | `"Are you sure you want to delete...?"` | `theme_msg_delete_confirm` |
| 222 | `"Warning"` | `common_word_warning` |
| 223 | `"Are you sure you want to delete...?"` | `attr_msg_delete_confirm` |
| 260 | `"Edit attribute"` / `"Create new attribute"` | `attr_title_edit` / `attr_title_create` |

### 27.3 样式编辑器

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/StylesEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 132 | `"Create new style"` | `style_title_create` |
| 133 | `"Create"` | `button_create` |
| 174 | `"Edit style"` | `style_title_edit` |
| 175 | `"Edit"` | `button_edit` |
| 196 | `"Warning"` | `common_word_warning` |
| 197 | `"Are you sure you want to delete...?"` | `style_msg_delete_confirm` |
| 234 | `"Warning"` | `common_word_warning` |
| 272 | `"Edit attribute"` / `"Create new attribute"` | `attr_title_edit` / `attr_title_create` |

### 27.4 字符串编辑器

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/StringsEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 130 | `"Create new string"` | `string_title_create` |

---

## 二十七、重复出现的硬编码字符串

以下字符串在多处重复出现，应统一使用同一资源：

| 字符串 | 出现次数 | 建议键名 |
|--------|----------|----------|
| `"Warning"` | 15+ | `common_word_warning` |
| `"Please wait"` | 5+ | `common_word_please_wait` |
| `"Copy"` | 3+ | `button_copy` |
| `"Create"` | 5+ | `button_create` |
| `"Edit"` | 5+ | `button_edit` |
| `"Cancel"` | 10+ | `button_cancel` |
| `"Are you sure you want to delete...?"` | 8+ | `dialog_msg_delete_confirm` |

---

## 二十八、实施路线图

### 阶段一：高优先级（1-2天）
1. 提取对话框标题（~15处）
2. 提取对话框消息（~10处）
3. 提取按钮文本（~10处）

### 阶段二：中优先级（2-3天）
1. 资源编辑器UI（~20处）
2. 块管理界面（~35处）
3. 菜单系统（~20处）

### 阶段三：低优先级（1-2天）
1. 组件/事件相关（~12处）
2. 内部提示（~10处）
3. 清理和验证

### 阶段四：翻译设置（1天）
1. 设置Crowdin/Weblate
2. 创建翻译文件夹结构
3. 邀请社区翻译者

---

---

## 二十九、com.besome.sketch.design 包详细分析

### 32.1 设计活动

**文件**: `com/besome/sketch/design/DesignActivity.java`

| 行号 | 硬编码字符串 | 建议键名 | 状态 |
|------|--------------|----------|------|
| 716 | - | - | ✅ 已使用R.string |
| 752 | - | - | ✅ 已使用R.string |
| 763 | - | - | ✅ 已使用R.string |
| 837 | - | - | ✅ 已使用R.string |
| 1231 | `"Missing directory detected"` | `build_error_missing_directory_title` | ❌ 需处理 |
| 1232-1234 | `"A directory important for building is missing..."` | `build_error_missing_directory_msg` | ❌ 需处理 |
| 1242 | `"Missing file detected"` | `build_error_missing_file_title` | ❌ 需处理 |
| 1243-1245 | `"A file needed for building is missing..."` | `build_error_missing_file_msg` | ❌ 需处理 |

### 32.2 构建对话框

**文件**: `com/besome/sketch/design/BuildingDialog.java`

| 状态 | 说明 |
|------|------|
| ✅ 已国际化 | 使用 `Helper.getResString(R.string.*)` |

---

## 三十、构建错误消息专项

### 构建相关硬编码字符串

以下字符串出现在构建/编译流程中，对用户体验重要：

| 文件 | 字符串 | 建议键名 |
|------|--------|----------|
| `DesignActivity.java` | `"Missing directory detected"` | `build_error_missing_directory_title` |
| `DesignActivity.java` | `"Missing file detected"` | `build_error_missing_file_title` |
| `DesignActivity.java` | `"A directory important for building is missing..."` | `build_error_missing_directory_msg` |
| `DesignActivity.java` | `"A file needed for building is missing..."` | `build_error_missing_file_msg` |

### strings.xml 补充

```xml
<!-- ========== 构建错误 ========== -->
<string name="build_error_missing_directory_title">Missing directory detected</string>
<string name="build_error_missing_directory_msg">A directory important for building is missing. Sketchware Pro can try creating %s if you\'d like to.</string>
<string name="build_error_missing_file_title">Missing file detected</string>
<string name="build_error_missing_file_msg">A file needed for building is missing. Put the correct file back to %s and try building again.</string>
```

---

## 三十一、mod.hilal.saif 包详细分析（完成度38%）

### 35.1 图标选择对话框

**文件**: `mod/hilal/saif/activities/tools/IconSelectorDialog.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 56 | `"Select an icon"` | `dialog_title_select_icon` |
| 58 | `"Cancel"` | `button_cancel` |

### 35.2 配置活动

**文件**: `mod/hilal/saif/activities/tools/ConfigActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 171 | `"App Settings"` | `settings_title` |
| 225 | `"Backup directory"` | `settings_backup_directory_title` |
| 226 | `"Directory inside /Internal storage/..."` | `settings_backup_directory_msg` |
| 270 | `"Backup filename format"` | `settings_backup_filename_title` |
| 271-274 | `"This defines how SWB backup files get named..."` | `settings_backup_filename_msg` |

### 35.3 块管理详情

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerDetailsActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 122 | `"Select a JSON file"` | `file_picker_select_json` |
| 223 | `"Recycle Bin"` | `blocks_recycle_bin` |
| 229 | `"Manage Block"` | `blocks_manage_title` |
| 366 | `"Delete block?"` | `blocks_delete_title` |
| 367 | `"Are you sure you want to delete this block?"` | `blocks_delete_msg` |
| 368 | `"Recycle bin"` | `button_recycle_bin` |
| 370 | `"Delete permanently"` | `button_delete_permanently` |
| 427 | `"Restore to"` | `blocks_restore_title` |
| 429 | `"Restore"` | `button_restore` |
| 439 | `"Move to"` | `blocks_move_title` |
| 441 | `"Move"` | `button_move` |
| 465 | `"Import blocks"` | `blocks_import_title` |

### 35.4 块管理创建

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerCreatorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 138 | `"Block type"` | `blocks_type_title` |
| 342 | `"Add a new block"` | `blocks_add_title` |
| 347 | `"Insert block"` | `blocks_insert_title` |
| 349 | `"Edit block"` | `blocks_edit_title` |

### 35.5 块管理

**文件**: `mod/hilal/saif/activities/tools/BlocksManager.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 209 | `"Block configuration"` | `blocks_config_title` |

### 35.6 strings.xml 补充

```xml
<!-- ========== mod.hilal.saif 块管理 ========== -->
<string name="dialog_title_select_icon">Select an icon</string>
<string name="settings_title">App Settings</string>
<string name="settings_backup_directory_title">Backup directory</string>
<string name="settings_backup_directory_msg">Directory inside /Internal storage/, e.g. .sketchware/backups</string>
<string name="settings_backup_filename_title">Backup filename format</string>
<string name="settings_backup_filename_msg">This defines how SWB backup files get named.\nAvailable variables:\n - $projectName - Project name\n - $versionCode - App version code\n - $versionName - App version name\n - $pkgName - Package name</string>

<string name="file_picker_select_json">Select a JSON file</string>
<string name="blocks_recycle_bin">Recycle Bin</string>
<string name="blocks_manage_title">Manage Block</string>
<string name="blocks_delete_title">Delete block?</string>
<string name="blocks_delete_msg">Are you sure you want to delete this block?</string>
<string name="button_recycle_bin">Recycle bin</string>
<string name="button_delete_permanently">Delete permanently</string>
<string name="blocks_restore_title">Restore to</string>
<string name="button_restore">Restore</string>
<string name="blocks_move_title">Move to</string>
<string name="button_move">Move</string>
<string name="blocks_import_title">Import blocks</string>
<string name="blocks_type_title">Block type</string>
<string name="blocks_add_title">Add a new block</string>
<string name="blocks_insert_title">Insert block</string>
<string name="blocks_edit_title">Edit block</string>
<string name="blocks_config_title">Block configuration</string>
```

---

---

## 三十二、dev.aldi 包详细分析（完成度33%）

### 37.1 本地库管理

**文件**: `dev/aldi/sayuti/editor/manage/ManageLocalLibraryActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 426 | `"Warning"` | `common_word_warning` |
| 427 | `"This library \"...\" already used in your project, removing it may break your project\rDo you want to continue removing it?"` | `library_remove_warning_msg` |

### 37.2 库下载器

**文件**: `dev/aldi/sayuti/editor/manage/LibraryDownloaderDialogFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 162 | `"Confirm Download"` | `library_download_confirm_title` |
| 164 | `"Download"` | `button_download` |
| 165 | `"Cancel"` | `button_cancel` |

### 37.3 strings.xml 补充

```xml
<!-- ========== 库管理 ========== -->
<string name="library_remove_warning_msg">This library \"%s\" already used in your project, removing it may break your project. Do you want to continue removing it?</string>
<string name="library_download_confirm_title">Confirm Download</string>
<string name="button_download">Download</string>
```

---

## 三十三、补充：遗漏的硬编码字符串

### 40.1 源代码查看器

**文件**: `com/besome/sketch/common/SrcViewerActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 154 | `"Select font size"` | `dialog_title_select_font_size` |
| 157 | `"Apply"` | `button_apply` |

### 40.2 strings.xml 补充

```xml
<string name="dialog_title_select_font_size">Select font size</string>
```

---

## 三十四、tools包补充审查

### 42.1 新密钥库活动

**文件**: `com/besome/sketch/tools/NewKeyStoreActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 150 | `"Export path: "` | `keystore_export_path_prefix` |

### 42.2 编译日志活动

**文件**: `com/besome/sketch/tools/CompileLogActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 54 | `"Last compile log"` | `compile_log_title_last` |
| 56 | `"Compile log"` | `compile_log_title` |
| 194 | `"Select font size"` | `dialog_title_select_font_size` |
| 196 | `"Save"` | `button_save` |

### 42.3 错误收集活动

**文件**: `com/besome/sketch/tools/CollectErrorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 39-41 | `"An error occurred while running Sketchware Pro..."` | `error_crash_report_msg` |

### 42.4 strings.xml 补充

```xml
<!-- ========== 工具 ========== -->
<string name="keystore_export_path_prefix">Export path: </string>
<string name="compile_log_title_last">Last compile log</string>
<string name="compile_log_title">Compile log</string>
<string name="error_crash_report_msg">An error occurred while running Sketchware Pro. Do you want to report this error log so that we can fix it? No personal information will be included.</string>
```

---

## 三十五、editor/manage包补充

### 44.1 添加视图活动

**文件**: `com/besome/sketch/editor/manage/view/AddViewActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 196 | `"Create new"` | `view_title_create_new` |
| 204 | `"Edit "` | `view_title_edit_prefix` |

### 44.2 Material3库活动

**文件**: `com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 41 | `"AppCompat is disabled!"` | `library_appcompat_disabled_title` |
| 42 | `"Please enable AppCompat first to use this feature"` | `library_appcompat_disabled_msg` |
| 43 | `"OK"` | `button_ok` |

### 44.3 Firebase管理活动

**文件**: `com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 279 | `"Select your google-services.json"` | `firebase_select_json_title` |

### 44.4 GoogleMap管理活动

**文件**: `com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 124 | `"GoogleMap Settings"` | `googlemap_settings_title` |

### 44.5 strings.xml 补充

```xml
<!-- ========== 视图管理 ========== -->
<string name="view_title_create_new">Create new</string>
<string name="view_title_edit_prefix">Edit %s</string>

<!-- ========== 库管理 ========== -->
<string name="library_appcompat_disabled_title">AppCompat is disabled!</string>
<string name="library_appcompat_disabled_msg">Please enable AppCompat first to use this feature</string>
<string name="firebase_select_json_title">Select your google-services.json</string>
<string name="googlemap_settings_title">GoogleMap Settings</string>
```

---

## 三十六、property包补充

### 48.1 属性输入项

**文件**: `com/besome/sketch/editor/property/PropertyInputItem.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 895 | `"Delete"` | `dialog_title_delete` |
| 896 | `"Are you sure you want to delete...?"` | `dialog_msg_delete_attr` |
| 934 | `"Add new attribute"` | `dialog_title_add_attribute` |

### 48.2 属性项

**文件**: `com/besome/sketch/editor/property/PropertyAttributesItem.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 191 | `"Choose an attributes"` | `dialog_title_choose_attr` |
| 197 | `"Choose an id"` | `dialog_title_choose_id` |
| 287 | `"Choose an id"` | `dialog_title_choose_id` |
| 300 | `"Delete"` | `dialog_title_delete` |
| 302 | `"Yes"` | `button_yes` |
| 336 | `"Delete"` | `dialog_title_delete` |
| 338 | `"Yes"` | `button_yes` |

### 48.3 strings.xml 补充

```xml
<!-- ========== 属性编辑器 ========== -->
<string name="dialog_title_delete">Delete</string>
<string name="dialog_msg_delete_attr">Are you sure you want to delete %s?</string>
<string name="dialog_title_add_attribute">Add new attribute</string>
<string name="dialog_title_choose_attr">Choose an attributes</string>
<string name="dialog_title_choose_id">Choose an id</string>
<string name="button_yes">Yes</string>
```

---

## 三十七、projects包补充

### 50.1 项目设置活动

**文件**: `com/besome/sketch/projects/MyProjectSettingActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 166 | `"Project Settings"` | `project_settings_title` |

### 50.2 strings.xml 补充

```xml
<string name="project_settings_title">Project Settings</string>
```

---

## 三十八、pro.sketchware.fragments包补充

### 52.1 事件管理器Fragment

**文件**: `pro/sketchware/fragments/settings/events/EventsManagerFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 151 | `"New Listener"` / `"Edit Listener"` | `events_new_listener` / `events_edit_listener` |
| 153 | `"Save"` | `button_save` |
| 190 | `"Select a .txt file"` | `file_picker_select_txt` |
| 329 | `"Delete listener"` | `events_delete_listener_title` |
| 330 | `"Are you sure you want to delete this item?"` | `events_delete_confirm_msg` |
| 331 | `"Yes"` | `button_yes` |

### 52.2 事件详情Fragment

**文件**: `pro/sketchware/fragments/settings/events/details/EventsManagerDetailsFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 67 | `"Event Details"` | `events_details_title` |
| 211 | `"Delete this event?"` | `events_delete_event_msg` |
| 212 | `"Delete"` | `button_delete` |
| 213 | `"Edit"` | `button_edit` |

### 52.3 事件创建器Fragment

**文件**: `pro/sketchware/fragments/settings/events/creator/EventsManagerCreatorFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 213 | `"Event Properties"` | `events_properties_title` |
| 216 | `"New Activity Event"` | `events_new_activity_title` |
| 218 | `"New Event"` | `events_new_title` |

### 52.4 块选择器管理Fragment

**文件**: `pro/sketchware/fragments/settings/block/selector/BlockSelectorManagerFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 148 | `"New selector"` / `"Edit selector"` | `selector_new_title` / `selector_edit_title` |
| 150 | `"Create"` / `"Save"` | `button_create` / `button_save` |
| 182 | `"Actions"` | `dialog_title_actions` |
| 210 | `"Attention"` | `dialog_title_attention` |
| 212 | `"Yes"` | `button_yes` |
| 213 | `"Cancel"` | `button_cancel` |
| 235 | `"Select .json selector file"` | `file_picker_select_json_selector` |

### 52.5 块选择器详情Fragment

**文件**: `pro/sketchware/fragments/settings/block/selector/details/BlockSelectorDetailsFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 131 | `"New Selector Item"` | `selector_new_item_title` |
| 132 | `"Create"` | `button_create` |
| 154 | `"Actions"` | `dialog_title_actions` |
| 183 | `"Attention"` | `dialog_title_attention` |
| 185 | `"Yes"` | `button_yes` |
| 186 | `"Cancel"` | `button_cancel` |

### 52.6 strings.xml 补充

```xml
<!-- ========== 事件管理器 ========== -->
<string name="events_new_listener">New Listener</string>
<string name="events_edit_listener">Edit Listener</string>
<string name="events_delete_listener_title">Delete listener</string>
<string name="events_delete_confirm_msg">Are you sure you want to delete this item?</string>
<string name="events_details_title">Event Details</string>
<string name="events_delete_event_msg">Delete this event?</string>
<string name="events_properties_title">Event Properties</string>
<string name="events_new_activity_title">New Activity Event</string>
<string name="events_new_title">New Event</string>

<!-- ========== 块选择器 ========== -->
<string name="selector_new_title">New selector</string>
<string name="selector_edit_title">Edit selector</string>
<string name="selector_new_item_title">New Selector Item</string>
<string name="file_picker_select_txt">Select a .txt file</string>
<string name="file_picker_select_json_selector">Select .json selector file</string>

<!-- ========== 通用对话框 ========== -->
<string name="dialog_title_actions">Actions</string>
<string name="dialog_title_attention">Attention</string>
```

---

## 三十九、resourceseditor补充（数组/颜色编辑器）

### 54.1 数组编辑器

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/ArraysEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 131 | `"Create new array"` | `array_title_create` |
| 136 | `"Select Array Type"` | `array_select_type_title` |
| 198 | `"Edit array"` | `array_title_edit` |
| 199 | `"Edit"` | `button_edit` |
| 220 | `"Warning"` | `common_word_warning` |
| 304 | `"Edit item"` / `"Create new item"` | `item_title_edit` / `item_title_create` |

### 54.2 颜色编辑器

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/ColorsEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 159 | `"Edit color"` | `color_title_edit` |
| 162 | `"Create new color"` | `color_title_create` |

### 54.3 strings.xml 补充

```xml
<!-- ========== 数组编辑器 ========== -->
<string name="array_title_create">Create new array</string>
<string name="array_title_edit">Edit array</string>
<string name="array_select_type_title">Select Array Type</string>
<string name="item_title_create">Create new item</string>
<string name="item_title_edit">Edit item</string>

<!-- ========== 颜色编辑器 ========== -->
<string name="color_title_create">Create new color</string>
<string name="color_title_edit">Edit color</string>
```

---

## 四十、mod.hey.studios包深度扫描

### 56.1 Proguard管理

**文件**: `mod/hey/studios/project/proguard/ManageProguardActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 59 | `"Select Local libraries"` | `proguard_select_libraries_title` |
| 123 | `"Code Shrinking Manager"` | `proguard_manager_title` |

### 56.2 本地库管理

**文件**: `mod/hey/studios/activity/managers/nativelib/ManageNativelibsActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 126 | `"Native Library Manager"` | `nativelib_manager_title` |
| 173 | `"Create a new folder"` | `folder_create_title` |
| 174 | `"Enter the name of the new folder"` | `folder_create_msg` |
| 175 | `"Cancel"` | `button_cancel` |
| 176 | `"Create"` | `button_create` |
| 237 | `"Import Native Libraries"` | `nativelib_import_title` |
| 264 | `"Rename"` | `dialog_title_rename` |
| 266 | `"Cancel"` | `button_cancel` |
| 267 | `"Rename"` | `button_rename` |

### 56.3 Java/Kotlin管理

**文件**: `mod/hey/studios/activity/managers/java/ManageJavaActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 137 | `"Java/Kotlin Manager"` | `java_manager_title` |
| 185 | `"Create new"` | `dialog_title_create_new` |
| 186 | `"File extension will be added automatically..."` | `java_create_msg` |
| 187 | `"Cancel"` | `button_cancel` |
| 188 | `"Create"` | `button_create` |

### 56.4 代码编辑器

**文件**: `mod/hey/studios/code/SrcCodeEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 256 | `"Select Theme"` | `editor_select_theme_title` |
| 270 | `"Select Language"` | `editor_select_language_title` |

**文件**: `mod/hey/studios/lib/code_editor/CodeEditorLayout.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 167 | `"Select font size"` | `dialog_title_select_font_size` |

### 56.5 strings.xml 补充

```xml
<!-- ========== Proguard管理 ========== -->
<string name="proguard_select_libraries_title">Select Local libraries</string>
<string name="proguard_manager_title">Code Shrinking Manager</string>

<!-- ========== 本地库管理 ========== -->
<string name="nativelib_manager_title">Native Library Manager</string>
<string name="nativelib_import_title">Import Native Libraries</string>
<string name="folder_create_title">Create a new folder</string>
<string name="folder_create_msg">Enter the name of the new folder</string>
<string name="dialog_title_rename">Rename</string>
<string name="button_rename">Rename</string>

<!-- ========== Java/Kotlin管理 ========== -->
<string name="java_manager_title">Java/Kotlin Manager</string>
<string name="java_create_msg">File extension will be added automatically based on the file type you select</string>
<string name="dialog_title_create_new">Create new</string>

<!-- ========== 代码编辑器 ========== -->
<string name="editor_select_theme_title">Select Theme</string>
<string name="editor_select_language_title">Select Language</string>
```

---

## 四十一、小部件创建器

**文件**: `pro/sketchware/widgets/WidgetsCreatorManager.java`

| 行号 | 硬编码字符串 | 建议键名 | 状态 |
|------|--------------|----------|------|
| 169 | - | - | ✅ R.string |
| 281 | `"Select .json widgets files"` | `widgets_select_json_title` | ❌ 需处理 |
| 343 | - | - | ✅ R.string |
| 399 | - | - | ✅ R.string |
| 478 | `"Actions"` | `dialog_title_actions` | ❌ 需处理 |
| 508 | - | - | ✅ R.string |

### strings.xml 补充

```xml
<string name="widgets_select_json_title">Select .json widgets files</string>
```

---

## 四十二、mod.agus.jcoderz.editor包

### 62.1 资源管理器

**文件**: `mod/agus/jcoderz/editor/manage/resource/ManageResourceActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 151 | `"Resource Manager"` | `resource_manager_title` |
| 196 | `"Create a new folder"` / `"Create a new file"` | `resource_create_folder_title` / `resource_create_file_title` |
| 197 | `"Enter a name for the new..."` | `resource_create_msg` |
| 198 | `"Cancel"` | `button_cancel` |
| 199 | `"Create"` | `button_create` |
| 255 | `"Select resource files"` | `resource_select_files_title` |
| 285 | `"Rename"` | `dialog_title_rename` |
| 318 | `"Delete ...?"` | `resource_delete_title` |
| 319-320 | `"Are you sure you want to delete...?"` | `resource_delete_msg` |

### 62.2 权限管理器

**文件**: `mod/agus/jcoderz/editor/manage/permission/ManagePermissionActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 140 | `"Reset permissions"` | `permission_reset_title` |
| 141 | `"Are you sure you want to reset all permissions?"` | `permission_reset_msg` |
| 142 | `"Reset"` | `button_reset` |

### 62.3 strings.xml 补充

```xml
<!-- ========== 资源管理器 ========== -->
<string name="resource_manager_title">Resource Manager</string>
<string name="resource_create_folder_title">Create a new folder</string>
<string name="resource_create_file_title">Create a new file</string>
<string name="resource_create_msg">Enter a name for the new %s</string>
<string name="resource_select_files_title">Select resource files</string>
<string name="resource_delete_msg">Are you sure you want to delete this %s? This action cannot be undone.</string>

<!-- ========== 权限管理器 ========== -->
<string name="permission_reset_title">Reset permissions</string>
<string name="permission_reset_msg">Are you sure you want to reset all permissions?</string>
```

---

## 四十三、utility包

**文件**: `pro/sketchware/utility/SketchwareUtil.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 161 | `"Couldn't get "` | `error_couldnt_get_prefix` |
| 162 | `"Failed to parse..."` | `error_parse_json_msg` |
| 163 | `"Rename"` | `button_rename` |
| 177 | `"Okay"` | `button_okay` |

### strings.xml 补充

```xml
<string name="error_couldnt_get_prefix">Couldn\'t get %s</string>
<string name="error_parse_json_msg">Failed to parse %s from file %s. Fix by renaming old file to %s.bak? If not, no %s will be used.</string>
<string name="button_okay">Okay</string>
```

---

## 四十四、MainActivity补充

**文件**: `pro/sketchware/activities/main/activities/MainActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 237 | `"Warning"` | `common_word_warning` |
| 239 | `"Copy"` | `button_copy` |
| 240 | `"Don't copy"` | `button_dont_copy` |
| 367 | `"Major changes in v7.0.0"` | `changelog_v7_title` |
| 368-370 | 更新说明文本 | `changelog_v7_msg` |
| 432 | `"Android 11 storage access"` | `permission_storage_android11_title` |
| 433 | 权限说明文本 | `permission_storage_android11_msg` |

### strings.xml 补充

```xml
<!-- ========== 主活动 ========== -->
<string name="button_dont_copy">Don\'t copy</string>
<string name="changelog_v7_title">Major changes in v7.0.0</string>
<string name="permission_storage_android11_title">Android 11 storage access</string>
<string name="permission_storage_android11_msg">Starting with Android 11, Sketchware Pro needs a new permission to avoid taking ages to build projects. Don\'t worry, we can\'t do more to storage than with current granted permissions.</string>
```

---

## 四十五、组件管理活动

### 72.1 ManageCustomComponentActivity

**文件**: `pro/sketchware/activities/editor/component/ManageCustomComponentActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 179 | `"Select .json selector file"` | `file_picker_select_json_component` |

### 72.2 AddCustomComponentActivity

**文件**: `pro/sketchware/activities/editor/component/AddCustomComponentActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 216 | `"Select json file"` | `file_picker_select_json` |

### strings.xml 补充

```xml
<string name="file_picker_select_json_component">Select .json selector file</string>
```

---

## 四十六、BlocksManager系列

### 74.1 BlocksManager

**文件**: `mod/hilal/saif/activities/tools/BlocksManager.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 349 | `"Recycle bin"` | `blocks_recycle_bin_title` |
| 350-351 | `"Are you sure you want to empty..."` | `blocks_empty_bin_msg` |
| 352 | `"Empty"` | `button_empty` |
| 438 | `"Create a new palette"` / `"Edit palette"` | `blocks_palette_create` / `blocks_palette_edit` |
| 598 | `"Remove all blocks related to this palette?"` | `blocks_remove_palette_msg` |
| 599 | `"Remove permanently"` | `button_remove_permanently` |

### 74.2 BlocksManagerDetailsActivity

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerDetailsActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### 74.3 BlocksManagerCreatorActivity

**文件**: `mod/hilal/saif/activities/tools/BlocksManagerCreatorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 四十七、ConfigActivity

**文件**: `mod/hilal/saif/activities/tools/ConfigActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 271-273 | 格式说明文本 | `config_backup_format_msg` |

---

## 四十八、LogicClickListener

**文件**: `pro/sketchware/control/logic/LogicClickListener.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 95 | `"Add a new custom variable"` | `logic_add_custom_var_title` |
| 223 | `"Add a new custom List"` | `logic_add_custom_list_title` |

---

## 四十九、ManageXMLCommandActivity

**文件**: `pro/sketchware/activities/editor/command/ManageXMLCommandActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 99 | `"XML Command Manager"` | `xml_command_manager_title` |
| 151 | `"Are you sure you want to delete this item?"` | `common_delete_confirm_msg` |
| 189 | `"Select an XML"` | `xml_select_title` |
| 301 | `"Confirmation"` | `dialog_title_confirmation` |
| 302-303 | XML Command启用确认文本 | `xml_command_enable_msg` |
| 334 | `"Dismiss"` | `button_dismiss` |

---

## 五十、ImportIconActivity

**文件**: `pro/sketchware/activities/importicon/ImportIconActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 283 | `"Filter icons"` | `icon_filter_title` |
| 284 | `"Cancel"` | `button_cancel` |
| 285 | `"Apply"` | `button_apply` |
| 382 | `"Save"` | `button_save` |

---

## 五十一、AndroidManifestInjectionDetails

**文件**: `mod/hilal/saif/activities/android_manifest/AndroidManifestInjectionDetails.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 105 | `"Application Permissions"` | `manifest_app_permissions_title` |
| 132 | `"Edit Value"` | `manifest_edit_value_title` |
| 149 | `"Add new permission"` / `"Add new attribute"` | `manifest_add_permission` / `manifest_add_attribute` |
| 242 | `"Delete this attribute?"` | `manifest_delete_attr_title` |
| 243 | `"This action cannot be undone."` | `common_action_cannot_undo` |

---

## 五十二、IconSelectorDialog

**文件**: `mod/hilal/saif/activities/tools/IconSelectorDialog.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 五十三、ViewEvents

**文件**: `com/besome/sketch/editor/view/ViewEvents.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 五十四、资源编辑器Activity补充

**文件**: `pro/sketchware/activities/resourceseditor/ResourcesEditorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 401 | `"Save completed"` | `resource_save_completed` |

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/StringsEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 146 | `"Create"` | `button_create` |
| 151 | `"Please fill in all fields"` | `error_fill_all_fields` |

---

## 五十五、AndroidManifestInjection补充

**文件**: `mod/hilal/saif/activities/android_manifest/AndroidManifestInjection.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 318 | `"AndroidManifest Manager"` | `manifest_manager_title` |

---

## 五十六、Toast错误消息扫描

### 89.1 EventsHandler（高频重复模式）

**文件**: `mod/hilal/saif/events/EventsHandler.java`

此文件包含大量重复模式的错误消息，约30+处，主要为：
- `"Found invalid name data type in Custom Event #%d"` → `error_invalid_name_data_type`
- `"Found invalid var data type in Custom Event #%d"` → `error_invalid_var_data_type`
- `"Found invalid (null) Custom Event at position %d"` → `error_null_custom_event`
- `"Found invalid listener data type in Custom Event #%d"` → `error_invalid_listener_data_type`
- `"Found invalid icon data type in Custom Event #%d"` → `error_invalid_icon_data_type`
- `"Found invalid description data type in Custom Event #%d"` → `error_invalid_description_data_type`
- `"Found invalid code data type in Custom Event #%d"` → `error_invalid_code_data_type`
- `"Found invalid parameters data type in Custom Event #%d"` → `error_invalid_parameters_data_type`

**建议**: 使用统一模板 `error_invalid_custom_event` + 参数化

### 89.2 WidgetsCreatorManager

**文件**: `pro/sketchware/widgets/WidgetsCreatorManager.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 117 | `"Error loading widgets: "` | `error_loading_widgets` |
| 126 | `"Missing required keys for widget: "` | `error_missing_widget_keys` |
| 133 | `"Invalid widget type: "` | `error_invalid_widget_type` |
| 253 | `"Failed: "` | `error_failed_prefix` |
| 453 | `"Failed to add widget: "` | `error_add_widget_failed` |

### 89.3 EventsManagerFragment

**文件**: `pro/sketchware/fragments/settings/events/EventsManagerFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 171 | `"Invalid name!"` | `error_invalid_name` |
| 197 | `"The selected file is empty!"` | `error_file_empty` |
| 206 | `"Invalid file"` | `error_invalid_file` |

### 89.4 BlockSelectorManagerFragment

**文件**: `pro/sketchware/fragments/settings/block/selector/BlockSelectorManagerFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 276 | `"Make sure you select a file that contains selector item(s)."` | `error_invalid_selector_file` |
| 300 | `"An error occurred while trying to get the selector"` | `error_get_selector_failed` |

### 89.5 LogReaderActivity

**文件**: `mod/khaled/logcat/LogReaderActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 157 | `"Nothing to Export"` | `error_nothing_to_export` |
| 195 | `"Something went wrong!"` | `error_something_went_wrong` |

### 89.6 其他Toast消息

**文件**: `pro/sketchware/activities/resourceseditor/ResourcesEditorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 550 | `"Invalid variant input"` | `error_invalid_variant` |

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/ColorsEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 213 | `"Please fill in all fields"` | `error_fill_all_fields` |
| 219 | `"Please enter a valid HEX color"` | `error_invalid_hex_color` |

**文件**: `pro/sketchware/activities/resourceseditor/components/fragments/ArraysEditor.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 150 | `"Array name Input is Empty"` | `error_array_name_empty` |
| 155 | `"Array type not selected"` | `error_array_type_not_selected` |

**文件**: `mod/jbk/export/GetKeyStoreCredentialsDialog.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 79 | `"Keystore not found"` | `error_keystore_not_found` |

**文件**: `pro/sketchware/activities/main/fragments/projects_store/ProjectPreviewActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 73 | `"Size: "` | `project_size_prefix` |
| 74 | `"Released: "` | `project_released_prefix` |
| 76 | `"Downloading projects is unavailable right now!"` | `error_download_unavailable` |

### 89.7 strings.xml 补充

```xml
<!-- ========== Toast错误消息 ========== -->
<string name="error_invalid_custom_event">Found invalid %s data type in Custom Event #%d</string>
<string name="error_null_custom_event">Found invalid (null) Custom Event at position %d</string>
<string name="error_loading_widgets">Error loading widgets: %s</string>
<string name="error_missing_widget_keys">Missing required keys for widget: %s</string>
<string name="error_invalid_widget_type">Invalid widget type: %s</string>
<string name="error_add_widget_failed">Failed to add widget: %s</string>
<string name="error_invalid_name">Invalid name!</string>
<string name="error_file_empty">The selected file is empty!</string>
<string name="error_invalid_file">Invalid file</string>
<string name="error_invalid_selector_file">Make sure you select a file that contains selector item(s).</string>
<string name="error_get_selector_failed">An error occurred while trying to get the selector</string>
<string name="error_nothing_to_export">Nothing to Export</string>
<string name="error_something_went_wrong">Something went wrong!</string>
<string name="error_invalid_variant">Invalid variant input</string>
<string name="error_invalid_hex_color">Please enter a valid HEX color</string>
<string name="error_array_name_empty">Array name Input is Empty</string>
<string name="error_array_type_not_selected">Array type not selected</string>
<string name="error_keystore_not_found">Keystore not found</string>
<string name="error_download_unavailable">Downloading projects is unavailable right now!</string>
<string name="project_size_prefix">Size: %s</string>
<string name="project_released_prefix">Released: %s</string>
```

---

## 五十七、Toast成功/提示消息扫描

### 91.1 WidgetsCreatorManager

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 294 | `"Exported in "` | `toast_exported_in` |
| 336 | `"Imported!"` | `toast_imported` |

### 91.2 EventsManagerFragment

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 227 | `"Successfully imported events"` | `toast_events_imported` |
| 245-246 | `"Successfully exported event to:..."` | `toast_event_exported` |

### 91.3 EventsManagerCreatorFragment

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 157 | `"Some required fields are empty!"` | `error_required_fields_empty` |
| 192 | `"Saved"` | `toast_saved` |

### 91.4 BlockSelectorManagerFragment

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 144 | `"You cannot change the name of this selector"` | `error_cannot_change_selector_name` |
| 155 | `"Please type the selector's name"` | `error_selector_name_required` |
| 159 | `"Please type the selector's title"` | `error_selector_title_required` |
| 166 | `"An item with this name already exists"` | `error_item_already_exists` |
| 263 | `"Exported in "` | `toast_exported_in` |

### 91.5 BlockSelectorDetailsFragment

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 193 | `"Saved!"` | `toast_saved` |

### 91.6 BuildSettingsBottomSheet

**文件**: `pro/sketchware/dialogs/BuildSettingsBottomSheet.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 62 | `"Don't forget to enable D8..."` | `toast_enable_d8_reminder` |
| 160 | `"Note that this option may cause issues..."` | `toast_no_http_legacy_warning` |

### 91.7 ManageAppCompatActivity

**文件**: `pro/sketchware/activities/appcompat/ManageAppCompatActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 284 | `"Added"` | `toast_added` |
| 290 | `"Saved"` | `toast_saved` |

### 91.8 ViewCodeEditorActivity

**文件**: `pro/sketchware/activities/editor/view/ViewCodeEditorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 238 | `"Saved"` | `toast_saved` |
| 240 | `"No changes to save"` | `toast_no_changes` |

### 91.9 StringsAdapter

**文件**: `pro/sketchware/activities/resourceseditor/components/adapters/StringsAdapter.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 107 | `"Please fill in all fields"` | `error_fill_all_fields` |

### 91.10 LogReaderActivity补充

**文件**: `mod/khaled/logcat/LogReaderActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 134 | `"Apply"` | `button_apply` |
| 193 | `"Logcat exported successfully: "` | `toast_logcat_exported` |

### 91.11 strings.xml 补充

```xml
<!-- ========== Toast成功消息 ========== -->
<string name="toast_exported_in">Exported in %s</string>
<string name="toast_imported">Imported!</string>
<string name="toast_events_imported">Successfully imported events</string>
<string name="toast_event_exported">Successfully exported event to:\n/Internal storage/.sketchware/data/system/export/events</string>
<string name="toast_saved">Saved</string>
<string name="toast_added">Added</string>
<string name="toast_no_changes">No changes to save</string>
<string name="toast_logcat_exported">Logcat exported successfully: %s</string>

<!-- ========== Toast提示消息 ========== -->
<string name="toast_enable_d8_reminder">Don\'t forget to enable D8 to be able to compile Java 8+ code</string>
<string name="toast_no_http_legacy_warning">Note that this option may cause issues if RequestNetwork component is used</string>
<string name="error_required_fields_empty">Some required fields are empty!</string>
<string name="error_cannot_change_selector_name">You cannot change the name of this selector</string>
<string name="error_selector_name_required">Please type the selector\'s name</string>
<string name="error_selector_title_required">Please type the selector\'s title</string>
<string name="error_item_already_exists">An item with this name already exists</string>

<!-- ========== Logcat ========== -->
<string name="logcat_filter_title">Filter by package name</string>
<string name="logcat_filter_msg">For multiple package names, separate them with a comma (,).</string>
```

---

## 五十八、ManageAppCompatActivity补充

**文件**: `pro/sketchware/activities/appcompat/ManageAppCompatActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 101 | `"Edit"` | `button_edit` |
| 102 | `"Delete"` | `button_delete` |
| 160 | `"No options are found."` | `appcompat_no_options_title` |
| 160 | `"No AppCompat options are currently available..."` | `appcompat_no_options_msg` |
| 174 | `"AppCompat is disabled."` | `appcompat_disabled_title` |
| 174 | `"Please enable AppCompat..."` | `appcompat_disabled_msg` |
| 177 | `"Not available."` | `appcompat_not_available_title` |
| 177 | `"You're not currently in the Activity layout."` | `appcompat_not_available_msg` |
| 203 | `"Are you sure you want to reset appcompat attributes..."` | `appcompat_reset_confirm_msg` |
| 255 | `"Add new attribute"` / `"Edit attribute"` | `appcompat_add_attr` / `appcompat_edit_attr` |

### strings.xml 补充

```xml
<!-- ========== AppCompat管理 ========== -->
<string name="appcompat_no_options_title">No options are found.</string>
<string name="appcompat_no_options_msg">No AppCompat options are currently available in this activity.</string>
<string name="appcompat_disabled_title">AppCompat is disabled.</string>
<string name="appcompat_disabled_msg">Please enable AppCompat in the Library Manager to use it.</string>
<string name="appcompat_not_available_title">Not available.</string>
<string name="appcompat_not_available_msg">You\'re not currently in the Activity layout.</string>
<string name="appcompat_reset_confirm_msg">Are you sure you want to reset appcompat attributes for %s?</string>
<string name="appcompat_add_attr">Add new attribute</string>
<string name="appcompat_edit_attr">Edit attribute</string>
```

---

## 五十九、ViewCodeEditorActivity补充

**文件**: `pro/sketchware/activities/editor/view/ViewCodeEditorActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 104 | `"XML Editor"` | `xml_editor_title` |
| 119 | `"Use AppCompat Manager to modify attributes..."` | `xml_editor_appcompat_note` |
| 136 | `"Undo"` | `menu_undo` |
| 139 | `"Redo"` | `menu_redo` |
| 142 | `"Save"` | `menu_save` |
| 147 | `"Edit AppCompat"` | `menu_edit_appcompat` |
| 149 | `"Reload color schemes"` | `menu_reload_colors` |
| 150 | `"Layout Preview"` | `menu_layout_preview` |
| 226-227 | `"Circular dependency found in..."` | `error_circular_dependency` |

### strings.xml 补充

```xml
<!-- ========== XML编辑器 ========== -->
<string name="xml_editor_title">XML Editor</string>
<string name="xml_editor_appcompat_note">Use AppCompat Manager to modify attributes for CoordinatorLayout, Toolbar, and other appcompat layout/widget.</string>
<string name="menu_undo">Undo</string>
<string name="menu_redo">Redo</string>
<string name="menu_save">Save</string>
<string name="menu_edit_appcompat">Edit AppCompat</string>
<string name="menu_reload_colors">Reload color schemes</string>
<string name="menu_layout_preview">Layout Preview</string>
<string name="error_circular_dependency">Circular dependency found in \"%s\"\nPlease resolve the issue before saving</string>
```

---

## 六十、ProjectsFragment补充

**文件**: `pro/sketchware/activities/main/fragments/projects/ProjectsFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 280 | `"Sort options"` | `projects_sort_title` |
| 301 | `"Save"` | `button_save` |

### strings.xml 补充

```xml
<string name="projects_sort_title">Sort options</string>
```

---

## 六十一、ExtraMenuBean补充

**文件**: `pro/sketchware/menu/ExtraMenuBean.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 606 | `"Deprecated"` | `menu_deprecated_title` |
| 607 | `"This Block Menu was initially used to parse resource values..."` | `menu_deprecated_msg` |

### strings.xml 补充

```xml
<string name="menu_deprecated_title">Deprecated</string>
<string name="menu_deprecated_msg">This Block Menu was initially used to parse resource values, but was too I/O heavy and has been removed due to that. Please use the Code Editor instead.</string>
```

---

## 六十二、DesignActivity深度扫描

**文件**: `com/besome/sketch/design/DesignActivity.java`

### 菜单项硬编码

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 492 | `"Build Settings"` | `menu_build_settings` |
| 497 | `"Clean temporary files"` | `menu_clean_temp_files` |
| 509 | `"Show last compile error"` | `menu_show_last_error` |
| 513 | `"Show source code"` | `menu_show_source_code` |
| 517 | `"Install last built APK"` | `menu_install_last_apk` |
| 523 | `"Show Apk signatures"` | `menu_show_apk_signatures` |
| 528 | `"Direct XML editor"` | `menu_direct_xml_editor` |

### Toast消息

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 372 | `"Package installed successfully!"` | `toast_package_installed` |
| 378 | `"Couldn't launch project..."` | `error_cannot_launch_project` |
| 388 | `"No root access granted..."` | `error_no_root_access` |
| 502 | `"Done cleaning temporary files!"` | `toast_clean_temp_done` |
| 520 | `"APK doesn't exist anymore"` | `error_apk_not_exist` |
| 822 | `"Failed to generate source."` | `error_generate_source_failed` |
| 887 | `"Failed to generate code."` | `error_generate_code_failed` |

### 对话框

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 1232-1234 | `"A directory important for building is missing..."` | `error_missing_dir_msg` |
| 1235 | `"Create"` | `button_create` |
| 1238 | `"Failed to create directory / directories!"` | `error_create_dir_failed` |
| 1243-1245 | `"A file needed for building is missing..."` | `error_missing_file_msg` |

---

## 六十三、ExportProjectActivity深度扫描

**文件**: `com/besome/sketch/export/ExportProjectActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 281 | `"Sign outputted AAB"` | `export_sign_aab_title` |
| 281 | `"Fill in the keystore details to sign the AAB."` | `export_sign_aab_msg` |
| 335-340 | APK签名说明文本 | `export_apk_sign_msg` |
| 343 | `"Understood"` | `button_understood` |
| 354 | `"Sign an APK"` | `export_sign_apk_title` |
| 355-356 | `"Fill in the keystore details..."` | `export_sign_apk_msg` |
| 764-765 | `"You can find the generated, signed AAB file at:..."` | `export_aab_finished_msg` |

---

## 六十四、PropertyInputItem/PropertyAttributesItem补充

**文件**: `com/besome/sketch/editor/property/PropertyInputItem.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 896 | `"Are you sure you want to delete..."` | `property_delete_attr_msg` |

**文件**: `com/besome/sketch/editor/property/PropertyAttributesItem.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 六十五、其他com.besome.sketch补充

### CompileLogActivity

**文件**: `com/besome/sketch/tools/CompileLogActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### MyProjectSettingActivity

**文件**: `com/besome/sketch/projects/MyProjectSettingActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### AddViewActivity

**文件**: `com/besome/sketch/editor/manage/view/AddViewActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### Material3LibraryActivity

**文件**: `com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### ManageFirebaseActivity

**文件**: `com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### ManageGoogleMapActivity

**文件**: `com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### SrcViewerActivity

**文件**: `com/besome/sketch/common/SrcViewerActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 六十六、ProjectsAdapter补充

**文件**: `com/besome/sketch/adapters/ProjectsAdapter.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 321 | `"Unpin project"` | `project_unpin` |
| 324 | `"Pin project"` | `project_pin` |

---

## 六十七、ManageLocalLibraryActivity

**文件**: `dev/aldi/sayuti/editor/manage/ManageLocalLibraryActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 179 | `"Deleted successfully"` | `toast_deleted_successfully` |
| 427 | `"This library ... already used in your project..."` | `library_remove_warning_msg` |

---

## 六十八、LibraryDownloaderDialogFragment

**文件**: `dev/aldi/sayuti/editor/manage/LibraryDownloaderDialogFragment.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 164 | `"Cancel"` | `button_cancel` |
| 311 | `"Library downloaded successfully"` | `toast_library_downloaded` |

---

## 六十九、ExcludeBuiltInLibrariesActivity补充

**文件**: `mod/jbk/editor/manage/library/ExcludeBuiltInLibrariesActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

---

## 七十、About页面深度扫描

### 114.1 AboutResponseModel

**文件**: `pro/sketchware/activities/about/models/AboutResponseModel.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 57 | `"Core Team Members"` | `about_core_team_title` |
| 57 | `"Contributors"` | `about_contributors_title` |
| 101 | `"Deleted Account"` | `about_deleted_account` |
| 148 | `"Today"` | `about_date_today` |
| 150 | `"Yesterday"` | `about_date_yesterday` |
| 152 | `"This week"` | `about_date_this_week` |
| 154 | `"Last week"` | `about_date_last_week` |
| 156 | `"Last month"` | `about_date_last_month` |

### 114.2 TeamAdapter

**文件**: `pro/sketchware/activities/about/adapters/TeamAdapter.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 82 | `"Active"` | `about_status_active` |
| 86 | `"Inactive"` | `about_status_inactive` |

### 114.3 CommitAdapter

**文件**: `pro/sketchware/activities/about/adapters/CommitAdapter.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 76 | `"Core Team"` | `about_badge_core_team` |
| 80 | `"Contributor"` | `about_badge_contributor` |
| 88 | `"New Update"` | `about_update_new` |
| 93 | `"Old Version"` | `about_update_old` |
| 98 | `"Current Version"` | `about_update_current` |

### 114.4 ChangeLogAdapter

**文件**: `pro/sketchware/activities/about/adapters/ChangeLogAdapter.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 55-56 | `"We've messed something up..."` | `about_changelog_error` |
| 72 | `"Beta"` / `"Official"` | `about_variant_beta` / `about_variant_official` |

---

## 七十一、VersionDialog

**文件**: `pro/sketchware/control/VersionDialog.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 28 | `"Advanced Version Control"` | `version_control_title` |
| 47 | `"Invalid Version Code"` | `error_invalid_version_code` |
| 53 | `"Invalid Version Name"` | `error_invalid_version_name` |

---

## 七十二、LayoutPreviewActivity补充

**文件**: `pro/sketchware/activities/preview/LayoutPreviewActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 33 | `"Layout Preview"` | `layout_preview_title` |
| 63 | `"content is null"` | `error_content_null` |

---

## 七十三、XML布局文件硬编码扫描

### 重大发现

XML布局文件中共发现 **217处** `android:text="..."` 硬编码字符串，分布在 **66个布局文件** 中。

### 高优先级布局文件

#### bottom_sheet_project_options.xml（7处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 17 | `"Project settings"` | `project_options_settings` |
| 67 | `"Pin project"` | `project_pin` |
| 95 | `"Backup project"` | `project_backup` |
| 124 | `"Export/Sign"` | `project_export_sign` |
| 152 | `"Change project settings"` | `project_change_settings` |
| 180 | `"Project configuration"` | `project_configuration` |
| 209 | `"Delete project"` | `project_delete` |

#### sort_project_dialog.xml（4处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 19 | `"Sort by Project Name"` | `sort_by_name` |
| 26 | `"Sort by ID"` | `sort_by_id` |
| 40 | `"Ascending (A-Z)"` | `sort_ascending` |
| 47 | `"Descending (Z-A)"` | `sort_descending` |

#### 高频布局文件统计

| 布局文件 | 硬编码数 |
|----------|----------|
| `activity_icon_creator.xml` | 24 |
| `manage_screen_activity_add_temp.xml` | 16 |
| `make_block_layout.xml` | 12 |
| `dialog_create_new_file_layout.xml` | 8 |
| `bottom_sheet_project_options.xml` | 7 |
| `dialog_filter_icons_layout.xml` | 7 |
| `property_popup_selector_gravity.xml` | 7 |
| `activity_blocks_manager_creator.xml` | 6 |
| `dialog_project_settings.xml` | 6 |
| `manage_library_material3.xml` | 6 |
| `project_config_layout.xml` | 6 |
| `property_popup_input_indent.xml` | 6 |
| `manage_font_add.xml` | 5 |
| `manage_library_exclude_builtin_libraries.xml` | 5 |
| 其他52个文件 | ~96 |

### 修复方式

XML布局中的硬编码应替换为 `@string/` 引用：

```xml
<!-- 修复前 -->
<TextView android:text="Project settings" />

<!-- 修复后 -->
<TextView android:text="@string/project_options_settings" />
```

### strings.xml 补充

```xml
<!-- ========== 项目选项底部弹窗 ========== -->
<string name="project_options_settings">Project settings</string>
<string name="project_backup">Backup project</string>
<string name="project_export_sign">Export/Sign</string>
<string name="project_change_settings">Change project settings</string>
<string name="project_configuration">Project configuration</string>
<string name="project_delete">Delete project</string>

<!-- ========== 排序对话框 ========== -->
<string name="sort_by_name">Sort by Project Name</string>
<string name="sort_by_id">Sort by ID</string>
<string name="sort_ascending">Ascending (A-Z)</string>
<string name="sort_descending">Descending (Z-A)</string>
```

---

## 七十四、XML布局 android:hint 硬编码扫描

### 重大发现

XML布局文件中共发现 **107处** `android:hint="..."` 硬编码字符串，分布在 **42个布局文件** 中。

### 高频布局文件统计

| 布局文件 | 硬编码数 |
|----------|----------|
| `fragment_events_manager_creator.xml` | 9 |
| `activity_blocks_manager_creator.xml` | 8 |
| `manage_xml_command_add.xml` | 7 |
| `make_block_layout.xml` | 5 |
| `property_popup_input_indent.xml` | 5 |
| `widgets_creator_dialog.xml` | 5 |
| `add_custom_variable.xml` | 4 |
| `dialog_keystore_credentials.xml` | 4 |
| `arrays_editor_add.xml` | 3 |
| `color_editor_add.xml` | 3 |
| `custom_dialog_attribute.xml` | 3 |
| `dialog_add_new_listener.xml` | 3 |
| `dialog_advanced_version_control.xml` | 3 |
| `dialog_project_settings.xml` | 3 |
| `menu_activity.xml` | 3 |
| `style_editor_add.xml` | 3 |
| `view_string_editor_add.xml` | 3 |
| 其他25个文件 | ~35 |

### 修复方式

```xml
<!-- 修复前 -->
<EditText android:hint="Enter name" />

<!-- 修复后 -->
<EditText android:hint="@string/hint_enter_name" />
```

---

## 七十五、Preference XML硬编码扫描

### preferences_config_activity.xml（18处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 6 | `"May slow down loading blocks in Logic Editor."` | `pref_builtin_blocks_summary` |
| 7 | `"Built-in blocks"` | `pref_builtin_blocks_title` |
| 12 | `"All variable blocks will be visible..."` | `pref_show_all_var_blocks_summary` |
| 13 | `"Show all variable blocks"` | `pref_show_all_var_blocks_title` |
| 18 | `"Every single available block will be shown..."` | `pref_show_all_blocks_summary` |
| 19 | `"Show all blocks of palettes"` | `pref_show_all_blocks_title` |
| 24 | `"The default directory is /Internal storage/.sketchware/backups/."` | `pref_backup_dir_summary` |
| 25 | `"Backup directory"` | `pref_backup_dir_title` |
| 30 | `"Automatically installs project APKs..."` | `pref_root_install_summary` |
| 31 | `"Install projects with root access"` | `pref_root_install_title` |
| 36 | `"Opens projects automatically after auto-installation..."` | `pref_root_launch_summary` |
| 37 | `"Launch projects after installing"` | `pref_root_launch_title` |
| 42 | `"Enables custom version code and name..."` | `pref_version_control_summary` |
| 43 | `"Use new Version Control"` | `pref_version_control_title` |
| 48 | `"Enables syntax highlighting..."` | `pref_asd_highlighter_summary` |
| 49 | `"Enable block text input highlighting"` | `pref_asd_highlighter_title` |
| 54 | 备份文件名格式说明 | `pref_backup_filename_summary` |
| 55 | `"Backup filename format"` | `pref_backup_filename_title` |

### strings.xml 补充

```xml
<!-- ========== 设置偏好 ========== -->
<string name="pref_builtin_blocks_title">Built-in blocks</string>
<string name="pref_builtin_blocks_summary">May slow down loading blocks in Logic Editor.</string>
<string name="pref_show_all_var_blocks_title">Show all variable blocks</string>
<string name="pref_show_all_var_blocks_summary">All variable blocks will be visible, even if you don\'t have variables for them.</string>
<string name="pref_show_all_blocks_title">Show all blocks of palettes</string>
<string name="pref_show_all_blocks_summary">Every single available block will be shown. Will slow down opening palettes!</string>
<string name="pref_backup_dir_title">Backup directory</string>
<string name="pref_backup_dir_summary">The default directory is /Internal storage/.sketchware/backups/.</string>
<string name="pref_root_install_title">Install projects with root access</string>
<string name="pref_root_install_summary">Automatically installs project APKs after building using root access.</string>
<string name="pref_root_launch_title">Launch projects after installing</string>
<string name="pref_root_launch_summary">Opens projects automatically after auto-installation using root.</string>
<string name="pref_version_control_title">Use new Version Control</string>
<string name="pref_version_control_summary">Enables custom version code and name for projects.</string>
<string name="pref_asd_highlighter_title">Enable block text input highlighting</string>
<string name="pref_asd_highlighter_summary">Enables syntax highlighting while editing blocks\' text parameters.</string>
<string name="pref_backup_filename_title">Backup filename format</string>
<string name="pref_backup_filename_summary">Default is \"$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd\'T\'HHmmss)\"</string>
```

---

## 七十六、Menu XML硬编码扫描

### 重大发现

Menu XML文件中共发现 **64处** `android:title="..."` 硬编码字符串，分布在 **22个菜单文件** 中。

### 高频菜单文件统计

| 菜单文件 | 硬编码数 |
|----------|----------|
| `main_drawer_menu.xml` | 11 |
| `menu_code_editor_hs.xml` | 8 |
| `popup_menu_double.xml` | 5 |
| `logic_menu.xml` | 4 |
| `menu_logcat.xml` | 4 |
| `manage_font_menu.xml` | 3 |
| `manage_sound_menu.xml` | 3 |
| `var_type_selector_menu.xml` | 3 |
| 其他14个文件 | ~23 |

### main_drawer_menu.xml 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 6 | `"About the team"` | `menu_about_team` |
| 11 | `"Changelog"` | `menu_changelog` |
| 16 | `"App information"` | `menu_app_info` |
| 21 | `"Create keystore"` | `menu_create_keystore` |
| 26 | `"Settings"` | `menu_settings` |
| 27 | `"Apps"` | `menu_apps` |
| 32 | `"SwAssist"` | `menu_swassist` |
| 36 | `"Related links"` | `menu_related_links` |
| 41 | `"Discord"` | `menu_discord` |
| 46 | `"Telegram"` | `menu_telegram` |
| 51 | `"GitHub"` | `menu_github` |

### menu_code_editor_hs.xml 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 7 | `"Undo"` | `menu_undo` |
| 13 | `"Redo"` | `menu_redo` |
| 19 | `"Paste"` | `menu_paste` |
| 25 | `"Word wrap"` | `menu_word_wrap` |
| 30 | `"Pretty print"` | `menu_pretty_print` |
| 35 | `"Find & Replace"` | `menu_find_replace` |
| 41 | `"Auto complete"` | `menu_auto_complete` |
| 47 | `"Auto complete symbol pair"` | `menu_auto_complete_symbol` |

### strings.xml 补充

```xml
<!-- ========== 抽屉菜单 ========== -->
<string name="menu_about_team">About the team</string>
<string name="menu_changelog">Changelog</string>
<string name="menu_app_info">App information</string>
<string name="menu_create_keystore">Create keystore</string>
<string name="menu_settings">Settings</string>
<string name="menu_apps">Apps</string>
<string name="menu_swassist">SwAssist</string>
<string name="menu_related_links">Related links</string>
<string name="menu_discord">Discord</string>
<string name="menu_telegram">Telegram</string>
<string name="menu_github">GitHub</string>

<!-- ========== 代码编辑器菜单 ========== -->
<string name="menu_word_wrap">Word wrap</string>
<string name="menu_pretty_print">Pretty print</string>
<string name="menu_find_replace">Find &amp; Replace</string>
<string name="menu_auto_complete">Auto complete</string>
<string name="menu_auto_complete_symbol">Auto complete symbol pair</string>

<!-- ========== 逻辑编辑器菜单 ========== -->
<string name="menu_show_source">Show Source Code</string>
<string name="menu_blocks_collection">Blocks collection</string>
```

---

## 七十七、Menu XML补充扫描

### popup_menu_double.xml（5处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 4 | `"Add to AndroidManifest"` | `menu_add_to_manifest` |
| 7 | `"Edit"` | `menu_edit` |
| 8 | `"Edit with..."` | `menu_edit_with` |
| 11 | `"Rename"` | `menu_rename` |
| 14 | `"Delete"` | `menu_delete` |

### menu_logcat.xml（4处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 8 | `"Auto scroll"` | `menu_auto_scroll` |
| 14 | `"Filter by package name"` | `menu_filter_package` |
| 20 | `"Export Logcat"` | `menu_export_logcat` |
| 26 | `"Clear all logs"` | `menu_clear_logs` |

### manage_collection_menu.xml（2处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 14 | `"remove"` | `menu_remove` |

### strings.xml 补充

```xml
<!-- ========== 弹出菜单 ========== -->
<string name="menu_add_to_manifest">Add to AndroidManifest</string>
<string name="menu_edit_with">Edit with…</string>
<string name="menu_rename">Rename</string>
<string name="menu_remove">remove</string>

<!-- ========== Logcat菜单 ========== -->
<string name="menu_auto_scroll">Auto scroll</string>
<string name="menu_filter_package">Filter by package name</string>
<string name="menu_export_logcat">Export Logcat</string>
<string name="menu_clear_logs">Clear all logs</string>
```

---

## 七十八、Java代码 setError 硬编码扫描

### 重大发现

Java代码中共发现 **51处** `setError("...")` 硬编码字符串，分布在 **16个文件** 中。

### 高频文件统计

| 文件 | 硬编码数 |
|------|----------|
| `BlocksManagerCreatorActivity.java` | 10 |
| `LibraryDownloaderDialogFragment.java` | 7 |
| `VariableTypeValidator.java` | 5 |
| `LogicClickListener.java` | 4 |
| `VariableModifierValidator.java` | 4 |
| `PropertyInputItem.java` | 3 |
| `CustomBlocksDialog.java` | 3 |
| `GetKeyStoreCredentialsDialog.java` | 3 |
| 其他8个文件 | ~12 |

### BlocksManagerCreatorActivity 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 101 | `"Block name already in use"` | `error_block_name_exists` |
| 181 | `"Invalid hex color"` | `error_invalid_hex_color` |
| 361 | `"Invalid name block data"` | `error_invalid_name_data` |
| 374 | `"Invalid type block data"` | `error_invalid_type_data` |
| 383 | `"Invalid typeName block data"` | `error_invalid_typename_data` |
| 392 | `"Invalid spec block data"` | `error_invalid_spec_data` |
| 401 | `"Invalid spec2 block data"` | `error_invalid_spec2_data` |
| 411 | `"Invalid imports block data"` | `error_invalid_imports_data` |
| 421 | `"Invalid color block data"` | `error_invalid_color_data` |

### LibraryDownloaderDialogFragment 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 136 | `"Please enter a dependency"` | `error_enter_dependency` |
| 143 | `"Invalid dependency format"` | `error_invalid_dependency_format` |
| 223 | `"Version not available"` | `error_version_not_available` |
| 232 | `"Dependencies not found"` | `error_dependencies_not_found` |
| 241 | `"Invalid scope: "` | `error_invalid_scope` |
| 250 | `"Invalid packaging"` | `error_invalid_packaging` |
| 300 | `"Dexing failed: "` | `error_dexing_failed` |

### strings.xml 补充

```xml
<!-- ========== 输入验证错误 ========== -->
<string name="error_block_name_exists">Block name already in use</string>
<string name="error_invalid_hex_color">Invalid hex color</string>
<string name="error_invalid_name_data">Invalid name block data</string>
<string name="error_invalid_type_data">Invalid type block data</string>
<string name="error_invalid_typename_data">Invalid typeName block data</string>
<string name="error_invalid_spec_data">Invalid spec block data</string>
<string name="error_invalid_spec2_data">Invalid spec2 block data</string>
<string name="error_invalid_imports_data">Invalid imports block data</string>
<string name="error_invalid_color_data">Invalid color block data</string>

<!-- ========== 库下载错误 ========== -->
<string name="error_enter_dependency">Please enter a dependency</string>
<string name="error_invalid_dependency_format">Invalid dependency format</string>
<string name="error_version_not_available">Version not available</string>
<string name="error_dependencies_not_found">Dependencies not found</string>
<string name="error_invalid_scope">Invalid scope: %s</string>
<string name="error_invalid_packaging">Invalid packaging</string>
<string name="error_dexing_failed">Dexing failed: %s</string>
```

---

## 七十九、Java代码 setHint 硬编码扫描

### 发现

Java代码中共发现 **7处** `setHint("...")` 硬编码字符串，分布在 **5个文件** 中。

| 文件 | 硬编码数 |
|------|----------|
| `AndroidManifestInjectionDetails.java` | 2 |
| `BlockSelectorManagerFragment.java` | 2 |
| `PropertyInputItem.java` | 1 |
| `ManageNativelibsActivity.java` | 1 |
| `BlockSelectorDetailsFragment.java` | 1 |

---

## 八十、对话框按钮硬编码扫描

### 重大发现

Java代码中共发现 **107处** 对话框按钮硬编码字符串：

| 方法 | 硬编码数 | 文件数 |
|------|----------|--------|
| `setPositiveButton("...")` | 55 | 35 |
| `setNegativeButton("...")` | 39 | 23 |
| `setNeutralButton("...")` | 13 | 11 |
| **总计** | **107** | - |

### 高频文件统计

| 文件 | 硬编码数 |
|------|----------|
| `PropertyAttributesItem.java` | 7 |
| `BlocksManagerDetailsActivity.java` | 6 |
| `ManageResourceActivity.java` | 4 |
| `ManageAssetsActivity.java` | 4 |
| `ManageJavaActivity.java` | 4 |
| `ManageNativelibsActivity.java` | 4 |
| `BackupRestoreManager.java` | 3 |
| `ImportIconActivity.java` | 4 |
| `MainActivity.java` | 5 |
| 其他26个文件 | ~66 |

### 常见按钮文本

| 硬编码字符串 | 出现次数 | 建议键名 |
|--------------|----------|----------|
| `"Cancel"` | ~25 | `button_cancel` |
| `"OK"` / `"Ok"` | ~20 | `button_ok` |
| `"Delete"` | ~12 | `button_delete` |
| `"Save"` | ~10 | `button_save` |
| `"Yes"` | ~8 | `button_yes` |
| `"No"` | ~6 | `button_no` |
| `"Rename"` | ~5 | `button_rename` |
| `"Create"` | ~5 | `button_create` |
| `"Copy"` | ~4 | `button_copy` |
| `"Edit"` | ~4 | `button_edit` |

### strings.xml 补充

```xml
<!-- ========== 通用按钮文本 ========== -->
<string name="button_cancel">Cancel</string>
<string name="button_ok">OK</string>
<string name="button_delete">Delete</string>
<string name="button_save">Save</string>
<string name="button_yes">Yes</string>
<string name="button_no">No</string>
<string name="button_rename">Rename</string>
<string name="button_create">Create</string>
<string name="button_copy">Copy</string>
<string name="button_edit">Edit</string>
<string name="button_close">Close</string>
<string name="button_continue">Continue</string>
<string name="button_retry">Retry</string>
<string name="button_skip">Skip</string>
```

---

## 八十一、通知硬编码扫描

### DesignActivity 通知

**文件**: `com/besome/sketch/design/DesignActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 1322 | `"Building project"` | `notification_building_project` |
| 1323 | `"Starting build..."` | `notification_starting_build` |
| 1339 | `"Building project"` | `notification_building_project` |

### strings.xml 补充

```xml
<!-- ========== 通知 ========== -->
<string name="notification_building_project">Building project</string>
<string name="notification_starting_build">Starting build…</string>
```

---

## 八十二、Snackbar硬编码扫描

### ConfigActivity（2处）

**文件**: `mod/hilal/saif/activities/tools/ConfigActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 253 | `"Couldn't acquire root access"` | `snackbar_root_access_failed` |
| 285 | `"Reset to default complete."` | `snackbar_reset_complete` |

### DesignActivity（1处）

**文件**: `com/besome/sketch/design/DesignActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| - | Snackbar消息 | `snackbar_*` |

---

## 八十三、TabLayout硬编码扫描

### ItemTabLayout（3处）

**文件**: `com/besome/sketch/editor/view/item/ItemTabLayout.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 36 | `"Tab 1"` | `tab_default_1` |
| 37 | `"Tab 2"` | `tab_default_2` |
| 38 | `"Tab 3"` | `tab_default_3` |

### strings.xml 补充

```xml
<!-- ========== Snackbar消息 ========== -->
<string name="snackbar_root_access_failed">Couldn\'t acquire root access</string>
<string name="snackbar_reset_complete">Reset to default complete.</string>

<!-- ========== TabLayout默认标签 ========== -->
<string name="tab_default_1">Tab 1</string>
<string name="tab_default_2">Tab 2</string>
<string name="tab_default_3">Tab 3</string>
```

---

## 八十四、对话框选项数组硬编码扫描

### 重大发现

Java代码中共发现 **19处** `new String[]{...}` 对话框选项硬编码，分布在 **15个文件** 中。

### 高频文件统计

| 文件 | 硬编码数 |
|------|----------|
| `AppSettings.java` | 3 |
| `SketchwareConstants.java` | 2 |
| `EventsManagerFragment.java` | 2 |
| 其他12个文件 | 12 |

### AppSettings 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 151 | `new String[]{"Delete"}` | `dialog_option_delete` |
| 169 | `new String[]{"Edit", "Delete"}` | `dialog_options_edit_delete` |

### EventsManagerFragment 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 319 | `new String[]{"Edit", "Export", "Delete"}` | `dialog_options_edit_export_delete` |

### strings.xml 补充

```xml
<!-- ========== 对话框选项数组 ========== -->
<string-array name="dialog_options_edit_delete">
    <item>Edit</item>
    <item>Delete</item>
</string-array>
<string-array name="dialog_options_edit_export_delete">
    <item>Edit</item>
    <item>Export</item>
    <item>Delete</item>
</string-array>
```

---

## 八十五、文件选择器标题硬编码扫描

### 发现

Java代码中共发现 **15处** `options.setTitle("...")` 文件选择器标题硬编码，分布在 **14个文件** 中。

### 示例

| 文件 | 行号 | 硬编码字符串 | 建议键名 |
|------|------|--------------|----------|
| `ExtraMenuBean.java` | 779 | `"Select an Asset"` | `file_picker_select_asset` |
| `ExtraMenuBean.java` | 783 | `"Select a Native library"` | `file_picker_select_native_lib` |
| `EventsManagerFragment.java` | 190 | `"Select a .txt file"` | `file_picker_select_txt` |
| `ManageAssetsActivity.java` | - | 文件选择标题 | `file_picker_*` |
| `ManageJavaActivity.java` | - | 文件选择标题 | `file_picker_*` |
| `BackupRestoreManager.java` | - | 文件选择标题 | `file_picker_*` |

### strings.xml 补充

```xml
<!-- ========== 文件选择器标题 ========== -->
<string name="file_picker_select_asset">Select an Asset</string>
<string name="file_picker_select_native_lib">Select a Native library</string>
<string name="file_picker_select_txt">Select a .txt file</string>
<string name="file_picker_select_file">Select a file</string>
<string name="file_picker_select_folder">Select a folder</string>
```

---

## 八十六、工具栏副标题硬编码扫描

### NewKeyStoreActivity（1处）

**文件**: `com/besome/sketch/tools/NewKeyStoreActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|

### strings.xml 补充

```xml
<!-- ========== 工具栏 ========== -->
<string name="toolbar_subtitle_export_path">Export path: %s</string>
```

---

## 八十七、搜索提示硬编码扫描

### ImportIconActivity（1处）

**文件**: `pro/sketchware/activities/importicon/ImportIconActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 186 | `"Search"` | `search_hint` |

### strings.xml 补充

```xml
<!-- ========== 搜索 ========== -->
<string name="search_hint">Search</string>
```

---

## 八十八、PopupMenu动态菜单项硬编码扫描

### 重大发现

Java代码中共发现 **14处** `popupMenu.add("...")` 动态菜单项硬编码，分布在 **4个文件** 中。

### 高频文件统计

| 文件 | 硬编码数 |
|------|----------|
| `ManageJavaActivity.java` | 8 |
| `ManageAssetsActivity.java` | 3 |
| `ManageAppCompatActivity.java` | 2 |
| `ManageResourceActivity.java` | 1 |

### ManageJavaActivity 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 479 | `"Remove Activity from manifest"` | `popup_remove_activity_manifest` |
| 481 | `"Add as Activity to manifest"` | `popup_add_activity_manifest` |
| 485 | `"Remove Service from manifest"` | `popup_remove_service_manifest` |
| 487 | `"Add as Service to manifest"` | `popup_add_service_manifest` |
| 490 | `"Edit"` | `popup_edit` |
| 491 | `"Edit with..."` | `popup_edit_with` |
| 494 | `"Rename"` | `popup_rename` |
| 495 | `"Delete"` | `popup_delete` |

### strings.xml 补充

```xml
<!-- ========== PopupMenu动态菜单项 ========== -->
<string name="popup_remove_activity_manifest">Remove Activity from manifest</string>
<string name="popup_add_activity_manifest">Add as Activity to manifest</string>
<string name="popup_remove_service_manifest">Remove Service from manifest</string>
<string name="popup_add_service_manifest">Add as Service to manifest</string>
<string name="popup_edit">Edit</string>
<string name="popup_edit_with">Edit with…</string>
<string name="popup_rename">Rename</string>
<string name="popup_delete">Delete</string>
```

---

## 八十九、动态菜单项硬编码扫描（补充）

### 重大发现

Java代码中共发现 **66处** `menu.add("...")` 动态菜单项硬编码，分布在 **17个文件** 中。

### 高频文件统计

| 文件 | 硬编码数 |
|------|----------|
| `SrcCodeEditor.java` | 11 |
| `BlocksManagerDetailsActivity.java` | 10 |
| `ManageJavaActivity.java` | 8 |
| `DesignActivity.java` | 7 |
| `CodeEditorLayout.java` | 6 |
| `ViewCodeEditorActivity.java` | 6 |
| `ManageXMLCommandActivity.java` | 4 |
| `ItemBottomNavigationView.java` | 3 |
| 其他9个文件 | 11 |

### SrcCodeEditor 示例

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 394 | `"Undo"` | `menu_undo` |
| 395 | `"Redo"` | `menu_redo` |
| 396 | `"Save"` | `menu_save` |
| 398 | `"Layout Preview"` | `menu_layout_preview` |
| 400 | `"Find & Replace"` | `menu_find_replace` |
| 401 | `"Word wrap"` | `menu_word_wrap` |
| 402 | `"Pretty print"` | `menu_pretty_print` |
| 403 | `"Select language"` | `menu_select_language` |
| 404 | `"Select theme"` | `menu_select_theme` |
| 405 | `"Auto complete"` | `menu_auto_complete` |
| 406 | `"Auto complete symbol pair"` | `menu_auto_complete_symbol` |

### strings.xml 补充

```xml
<!-- ========== 代码编辑器菜单 ========== -->
<string name="menu_undo">Undo</string>
<string name="menu_redo">Redo</string>
<string name="menu_save">Save</string>
<string name="menu_layout_preview">Layout Preview</string>
<string name="menu_find_replace">Find &amp; Replace</string>
<string name="menu_word_wrap">Word wrap</string>
<string name="menu_pretty_print">Pretty print</string>
<string name="menu_select_language">Select language</string>
<string name="menu_select_theme">Select theme</string>
<string name="menu_auto_complete">Auto complete</string>
<string name="menu_auto_complete_symbol">Auto complete symbol pair</string>
```

---

## 九十、输入辅助文本硬编码扫描

### LogicClickListener（1处）

**文件**: `pro/sketchware/control/logic/LogicClickListener.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 99 | `"Enter modifier e.g. private, public, public static, or empty (package private)."` | `helper_text_modifier` |

### strings.xml 补充

```xml
<!-- ========== 输入辅助文本 ========== -->
<string name="helper_text_modifier">Enter modifier e.g. private, public, public static, or empty (package private).</string>
```

---

## 九十一、AppSettings.java 硬编码补充扫描

### 分类标题硬编码（2处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 96 | `"Managers"` | `settings_category_managers` |
| 107 | `"General"` | `settings_category_general` |

### 设置项标题和描述硬编码（12处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 99 | `"Block manager"` | `settings_block_manager` |
| 99 | `"Manage your own blocks to use in Logic Editor"` | `settings_block_manager_desc` |
| 100 | `"Block selector menu manager"` | `settings_block_selector_manager` |
| 100 | `"Manage your own block selector menus"` | `settings_block_selector_manager_desc` |
| 101 | `"Component manager"` | `settings_component_manager` |
| 101 | `"Manage your own components"` | `settings_component_manager_desc` |
| 102 | `"Event manager"` | `settings_event_manager` |
| 102 | `"Manage your own events"` | `settings_event_manager_desc` |
| 103 | `"Local library manager"` | `settings_local_library_manager` |
| 103 | `"Manage and download local libraries"` | `settings_local_library_manager_desc` |
| 110 | `"App settings"` | `settings_app_settings` |
| 110 | `"Change general app settings"` | `settings_app_settings_desc` |
| 112 | `"Open working directory"` | `settings_open_working_dir` |
| 112 | `"Open Sketchware Pro's directory and edit files in it"` | `settings_open_working_dir_desc` |
| 113 | `"Sign an APK file with testkey"` | `settings_sign_apk` |
| 113 | `"Sign an already existing APK file with testkey and signature schemes up to V4"` | `settings_sign_apk_desc` |
| 114 | `"Auto-save and vibrations"` | `settings_system_settings_desc` |

### 文件选择器/对话框硬编码（18处）

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 141 | `"Select an entry to modify"` | `file_picker_select_entry` |
| 150 | `"Select an action"` | `dialog_select_action` |
| 151 | `"Delete"` | `action_delete` |
| 153 | `"Delete folder?"` / `"Delete file?"` | `dialog_delete_folder` / `dialog_delete_file` |
| 154 | `"Are you sure you want to delete this folder/file permanently? This cannot be undone."` | `dialog_delete_confirm_message` |
| 169 | `"Edit"` | `action_edit` |
| 179 | `"Delete file?"` | `dialog_delete_file` |
| 180 | `"Are you sure you want to delete this file permanently? This cannot be undone."` | `dialog_delete_file_message` |
| 199 | `"Sign APK with testkey"` | `dialog_sign_apk_title` |
| 219 | `"Continue"` | `action_continue` |
| 221 | `"Please select an APK file to sign"` | `toast_select_apk_file` |
| 233 | `"File exists"` | `dialog_file_exists` |
| 234 | `"An APK named ... already exists at /sketchware/signed_apk/. Overwrite it?"` | `dialog_file_exists_message` |
| 237 | `"Overwrite"` | `action_overwrite` |
| 266 | `"Signing APK..."` | `progress_signing_apk` |
| 288 | `"Successfully saved signed APK to: ..."` | `toast_apk_signed_success` |
| 292 | `"An error occurred. Check the log for more details."` | `error_check_log` |
| 297 | `"Signing failed: ..."` | `error_signing_failed` |

### strings.xml 补充

```xml
<!-- ========== AppSettings 设置页面 ========== -->
<!-- 分类标题 -->
<string name="settings_category_managers">Managers</string>
<string name="settings_category_general">General</string>

<!-- 设置项 -->
<string name="settings_block_manager">Block manager</string>
<string name="settings_block_manager_desc">Manage your own blocks to use in Logic Editor</string>
<string name="settings_block_selector_manager">Block selector menu manager</string>
<string name="settings_block_selector_manager_desc">Manage your own block selector menus</string>
<string name="settings_component_manager">Component manager</string>
<string name="settings_component_manager_desc">Manage your own components</string>
<string name="settings_event_manager">Event manager</string>
<string name="settings_event_manager_desc">Manage your own events</string>
<string name="settings_local_library_manager">Local library manager</string>
<string name="settings_local_library_manager_desc">Manage and download local libraries</string>
<string name="settings_app_settings">App settings</string>
<string name="settings_app_settings_desc">Change general app settings</string>
<string name="settings_open_working_dir">Open working directory</string>
<string name="settings_open_working_dir_desc">Open Sketchware Pro\'s directory and edit files in it</string>
<string name="settings_sign_apk">Sign an APK file with testkey</string>
<string name="settings_sign_apk_desc">Sign an already existing APK file with testkey and signature schemes up to V4</string>
<string name="settings_system_settings_desc">Auto-save and vibrations</string>

<!-- 文件选择器/对话框 -->
<string name="file_picker_select_entry">Select an entry to modify</string>
<string name="dialog_select_action">Select an action</string>
<string name="action_edit">Edit</string>
<string name="action_delete">Delete</string>
<string name="action_continue">Continue</string>
<string name="action_overwrite">Overwrite</string>
<string name="dialog_delete_folder">Delete folder?</string>
<string name="dialog_delete_file">Delete file?</string>
<string name="dialog_delete_confirm_message">Are you sure you want to delete this %s permanently? This cannot be undone.</string>
<string name="dialog_sign_apk_title">Sign APK with testkey</string>
<string name="toast_select_apk_file">Please select an APK file to sign</string>
<string name="dialog_file_exists">File exists</string>
<string name="dialog_file_exists_message">An APK named %s already exists at /sketchware/signed_apk/. Overwrite it?</string>
<string name="progress_signing_apk">Signing APK…</string>
<string name="toast_apk_signed_success">Successfully saved signed APK to: %s</string>
<string name="error_check_log">An error occurred. Check the log for more details.</string>
<string name="error_signing_failed">Signing failed: %s</string>
```

---

## 九十二、最终全面统计

### 总览

| 类别 | 数量 |
|------|------|
| **Java代码硬编码** | **~530处** |
| **Java setError** | **~51处** |
| **Java setHint/setHelperText** | **~8处** |
| **Java 对话框按钮** | **~107处** |
| **Java 对话框选项数组** | **~19处** |
| **Java 文件选择器标题** | **~15处** |
| **Java 动态菜单项 (menu.add)** | **~80处** |
| **Java 设置页面硬编码** | **~32处** |
| **Java 搜索提示/通知/Snackbar等** | **~11处** |
| **XML布局 android:text** | **~217处** |
| **XML布局 android:hint** | **~107处** |
| **Preference XML** | **~18处** |
| **Menu XML** | **~64处** |
| **硬编码总计** | **~1259处** |
| **文档章节** | **92** |
| **已审查Java文件** | **325+** |
| **已审查XML文件** | **90个** |

### 分类统计汇总

| 来源 | 硬编码数 | 占比 |
|------|----------|------|
| Java代码 (toast/dialog标题等) | ~530 | 42.1% |
| Java 对话框按钮 | ~107 | 8.5% |
| Java 动态菜单项 | ~80 | 6.4% |
| Java setError | ~51 | 4.1% |
| Java 设置页面 | ~32 | 2.5% |
| Java 其他 (hint/数组/选择器等) | ~53 | 4.2% |
| XML布局 (text+hint) | ~324 | 25.7% |
| Preference XML | ~18 | 1.4% |
| Menu XML | ~64 | 5.1% |
| **总计** | **~1259** | **100%** |

### 扫描覆盖的完整维度列表（19个）

| 维度 | 方法/属性 | 状态 |
|------|-----------|------|
| 对话框标题 | `setTitle()` | ✅ |
| 对话框消息 | `setMessage()` | ✅ |
| 对话框按钮 | `setPositiveButton()`, `setNegativeButton()`, `setNeutralButton()` | ✅ |
| 对话框选项 | `setItems()`, `setSingleChoiceItems()` | ✅ |
| Toast消息 | `toast()`, `toastError()` | ✅ |
| 文本设置 | `setText()` | ✅ |
| 输入错误 | `setError()` | ✅ |
| 输入提示 | `setHint()` | ✅ |
| 输入辅助文本 | `setHelperText()` | ✅ |
| 搜索提示 | `setQueryHint()` | ✅ |
| 通知 | `setContentTitle()`, `setContentText()` | ✅ |
| Snackbar | `Snackbar.make()` | ✅ |
| 工具栏副标题 | `setSubtitle()` | ✅ |
| 文件选择器 | `options.setTitle()` | ✅ |
| 动态菜单项 | `menu.add()`, `popupMenu.add()` | ✅ |
| XML布局文本 | `android:text` | ✅ |
| XML布局提示 | `android:hint` | ✅ |
| 偏好设置 | `android:title`, `android:summary`, `app:title`, `app:summary` | ✅ |
| 菜单项 | `android:title` | ✅ |

---

## 九十三、通知相关硬编码补充

### FirebaseMessagingServiceImpl（1处）

**文件**: `pro/sketchware/firebase/FirebaseMessagingServiceImpl.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 55 | `"Sketchware Pro Notifications"` | `notification_channel_name` |

### DesignActivity 通知操作按钮（2处）

**文件**: `com/besome/sketch/design/DesignActivity.java`

| 行号 | 硬编码字符串 | 建议键名 |
|------|--------------|----------|
| 1326 | `"Cancel build"` | `notification_action_cancel_build` |
| 1343 | `"Cancel Build"` | `notification_action_cancel_build` |

> **注意**: 同一字符串大小写不一致（`build` vs `Build`），国际化时应统一。

### strings.xml 补充

```xml
<!-- ========== 通知相关 ========== -->
<string name="notification_channel_name">Sketchware Pro Notifications</string>
<string name="notification_action_cancel_build">Cancel build</string>
```

---

*文档创建时间: 2026-02-15*
*最后更新: 2026-02-16*
*版本: 8.1*
*全面扫描完成：Java~854 + XML布局~324 + 偏好~18 + 菜单~64 = 总计~1260处硬编码*
*已审查：330+ Java文件 + 90 XML文件*
*扫描维度：20个（完整覆盖所有常见UI文本API）*
*文档已清理：从153章节精简至93章节*
