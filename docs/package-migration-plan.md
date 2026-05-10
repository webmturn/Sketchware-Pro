# Sketchware-Pro 包结构迁移方案

> 状态：待审批  
> 日期：2026-05-10  
> 范围：消除 `mod.*` / `dev.*` 贡献者命名空间，统一到 `pro.sketchware` / `com.besome.sketch`

---

## 一、目标

将 167 个分散在 10 个贡献者命名空间（`mod.agus`、`mod.hey`、`mod.hilal`、`mod.jbk`、`mod.pranav`、`mod.tyron`、`mod.bobur`、`mod.khaled`、`mod.alucard`、`mod.remaker`、`dev.aldi`）的文件，按**功能域**归入 `pro.sketchware.*` 或 `com.besome.sketch.*` 现有体系。

**不在本次范围内：**
- `com.besome.sketch` → `pro.sketchware` 的整体迁移（独立后续阶段）
- `kellinwood.*` 第三方 ZipSigner 库（44 文件，保持不动）
- `com.bumptech.glide.signature`（1 文件，保持不动）
- `pro.sketchware` 内部子包整理（双重 util/lib 合并等，独立后续阶段）

---

## 二、现状数据

### 2.1 待迁移文件分布

| 来源包 | Java | Kotlin | 合计 | 外部引用热度 |
|--------|------|--------|------|-------------|
| mod.agus.jcoderz | 38 | 0 | 38 | 每文件 1 处 |
| mod.hey.studios | 33 | 3 | 36 | 每文件 1 处 |
| mod.hilal.saif | 20 | 0 | 20 | 每文件 1 处 |
| mod.jbk | 20 | 0 | 20 | 每文件 1 处 |
| mod.pranav | 0 | 4 | 4 | 每文件 1 处 |
| mod.tyron | 1 | 1 | 2 | 每文件 1 处 |
| mod.bobur | 2 | 0 | 2 | 每文件 1 处 |
| mod.khaled | 1 | 0 | 1 | 每文件 1 处 |
| mod.alucard | 1 | 0 | 1 | 每文件 1 处 |
| mod.remaker | 1 | 0 | 1 | 每文件 1 处 |
| dev.aldi.sayuti | 42 | 0 | 42 | 每文件 1 处 |
| **合计** | **159** | **8** | **167** | |

### 2.2 额外需同步更新的引用

| 类型 | 数量 | 说明 |
|------|------|------|
| AndroidManifest.xml Activity 声明 | 18 | 必须与 Java 包名同步修改 |
| XML 布局 FQN 自定义 View | 2 | `code_editor_layout.xml`、`code_editor_layout_nowrap.xml` 引用 `mod.hey.studios.lib.code_editor.CodeEditorEditText` |
| 代码内 FQN 字符串引用 | ~15 | `new mod.agus...()` 构造、`@link`/`@see` javadoc、字符串常量 |
| 反射 `Class.forName()` | 0 | 无 |

---

## 三、分 Phase 迁移计划

### 总览

| Phase | 功能域 | 文件数 | 主要来源 | 目标包 |
|-------|--------|--------|---------|--------|
| 1 | 视图 palette + item | 50 | mod.agus + dev.aldi | com.besome.sketch.editor.view.{palette,item} |
| 2 | 构建编译器 | 13 | mod.jbk + mod.pranav + mod.hey | pro.sketchware.core.build.{compiler,} |
| 3 | 代码生成扩展 | 16 | mod.hilal + dev.aldi + mod.agus + mod.hey | pro.sketchware.core.codegen |
| 4 | 库管理 | 16 | mod.jbk + dev.aldi + mod.agus + mod.pranav | pro.sketchware.library |
| 5 | 项目设置与备份 | 13 | mod.hey + mod.tyron | pro.sketchware.project |
| 6 | 编辑器 Activity 与对话框 | 27 | mod.hilal + mod.hey + mod.jbk + mod.agus + mod.khaled | pro.sketchware.activities.editor.* / dialogs |
| 7 | 工具类与数据 | 20 | mod.jbk + mod.hey + mod.hilal + mod.bobur + mod.alucard + mod.remaker + mod.agus | pro.sketchware.util.* / beans / graphics / widgets |
| 8 | 代码编辑器组件 | 6 | mod.hey + mod.jbk | pro.sketchware.lib.code_editor |
| 9 | MultiDex | 6 | mod.agus | pro.sketchware.core.build.multidex |
| **合计** | | **167** | | |

---

### Phase 1：视图 palette + item（50 文件）

**原理**：3 个包的 Icon*/Item* 类功能完全同质，合并到 `com.besome.sketch.editor.view` 已有目录。

**commit**: `refactor(editor): merge view palette and item classes from mod/dev`

#### 1a — Palette Icons（30 文件 → `com.besome.sketch.editor.view.palette`）

| # | 源文件 | 来源包 |
|---|--------|--------|
| 1 | IconAnalogClock.java | mod.agus.jcoderz.editor.view.palette |
| 2 | IconAutoCompleteTextView.java | mod.agus.jcoderz.editor.view.palette |
| 3 | IconDatePicker.java | mod.agus.jcoderz.editor.view.palette |
| 4 | IconDigitalClock.java | mod.agus.jcoderz.editor.view.palette |
| 5 | IconGridView.java | mod.agus.jcoderz.editor.view.palette |
| 6 | IconMultiAutoCompleteTextView.java | mod.agus.jcoderz.editor.view.palette |
| 7 | IconRadioButton.java | mod.agus.jcoderz.editor.view.palette |
| 8 | IconRatingBar.java | mod.agus.jcoderz.editor.view.palette |
| 9 | IconSearchView.java | mod.agus.jcoderz.editor.view.palette |
| 10 | IconTimePicker.java | mod.agus.jcoderz.editor.view.palette |
| 11 | IconVideoView.java | mod.agus.jcoderz.editor.view.palette |
| 12 | IconBadgeView.java | dev.aldi.sayuti.editor.view.palette |
| 13 | IconBottomNavigationView.java | dev.aldi.sayuti.editor.view.palette |
| 14 | IconCardView.java | dev.aldi.sayuti.editor.view.palette |
| 15 | IconCircleImageView.java | dev.aldi.sayuti.editor.view.palette |
| 16 | IconCodeView.java | dev.aldi.sayuti.editor.view.palette |
| 17 | IconCollapsingToolbar.java | dev.aldi.sayuti.editor.view.palette |
| 18 | IconGoogleSignInButton.java | dev.aldi.sayuti.editor.view.palette |
| 19 | IconLottieAnimation.java | dev.aldi.sayuti.editor.view.palette |
| 20 | IconMaterialButton.java | dev.aldi.sayuti.editor.view.palette |
| 21 | IconOTPView.java | dev.aldi.sayuti.editor.view.palette |
| 22 | IconPatternLockView.java | dev.aldi.sayuti.editor.view.palette |
| 23 | IconRadioGroup.java | dev.aldi.sayuti.editor.view.palette |
| 24 | IconRecyclerView.java | dev.aldi.sayuti.editor.view.palette |
| 25 | IconSwipeRefreshLayout.java | dev.aldi.sayuti.editor.view.palette |
| 26 | IconTabLayout.java | dev.aldi.sayuti.editor.view.palette |
| 27 | IconTextInputLayout.java | dev.aldi.sayuti.editor.view.palette |
| 28 | IconViewPager.java | dev.aldi.sayuti.editor.view.palette |
| 29 | IconWaveSideBar.java | dev.aldi.sayuti.editor.view.palette |
| 30 | IconYoutubePlayer.java | dev.aldi.sayuti.editor.view.palette |

#### 1b — Item Views（20 文件 → `com.besome.sketch.editor.view.item`）

| # | 源文件 | 来源包 |
|---|--------|--------|
| 1 | ItemAnalogClock.java | mod.agus.jcoderz.editor.view.item |
| 2 | ItemAutoCompleteTextView.java | mod.agus.jcoderz.editor.view.item |
| 3 | ItemDatePicker.java | mod.agus.jcoderz.editor.view.item |
| 4 | ItemDigitalClock.java | mod.agus.jcoderz.editor.view.item |
| 5 | ItemGridView.java | mod.agus.jcoderz.editor.view.item |
| 6 | ItemMultiAutoCompleteTextView.java | mod.agus.jcoderz.editor.view.item |
| 7 | ItemRadioButton.java | mod.agus.jcoderz.editor.view.item |
| 8 | ItemRatingBar.java | mod.agus.jcoderz.editor.view.item |
| 9 | ItemTimePicker.java | mod.agus.jcoderz.editor.view.item |
| 10 | ItemVideoView.java | mod.agus.jcoderz.editor.view.item |
| 11 | ItemBadgeView.java | dev.aldi.sayuti.editor.view.item |
| 12 | ItemCircleImageView.java | dev.aldi.sayuti.editor.view.item |
| 13 | ItemCodeView.java | dev.aldi.sayuti.editor.view.item |
| 14 | ItemLottieAnimation.java | dev.aldi.sayuti.editor.view.item |
| 15 | ItemMaterialButton.java | dev.aldi.sayuti.editor.view.item |
| 16 | ItemOTPView.java | dev.aldi.sayuti.editor.view.item |
| 17 | ItemPatternLockView.java | dev.aldi.sayuti.editor.view.item |
| 18 | ItemViewPager.java | dev.aldi.sayuti.editor.view.item |
| 19 | ItemWaveSideBar.java | dev.aldi.sayuti.editor.view.item |
| 20 | ItemYoutubePlayer.java | dev.aldi.sayuti.editor.view.item |

**影响范围**：每个文件仅被 PaletteWidget.java / ViewEditor.java 等 1 处 import，总计约 50 个 import 行变更。无 Manifest/布局/反射影响。

---

### Phase 2：构建编译器（13 文件）

**commit**: `refactor(build): consolidate compiler classes into core.build`

#### 2a — 新建 `pro.sketchware.core.build.compiler`（10 文件）

| # | 源文件 | 来源包 |
|---|--------|--------|
| 1 | ResourceCompiler.java | mod.jbk.build.compiler.resource |
| 2 | DexCompiler.java | mod.jbk.build.compiler.dex |
| 3 | AppBundleCompiler.java | mod.jbk.build.compiler.bundle |
| 4 | R8Compiler.kt | mod.pranav.build |
| 5 | JarTask.kt | mod.pranav.build |
| 6 | KotlinCompiler.kt | mod.hey.studios.compiler.kotlin |
| 7 | KotlinCompilerBridge.java | mod.hey.studios.compiler.kotlin |
| 8 | KotlinCompilerUtil.java | mod.hey.studios.compiler.kotlin |
| 9 | Diagnostic.kt | mod.hey.studios.compiler.kotlin |
| 10 | DiagnosticCollector.kt | mod.hey.studios.compiler.kotlin |

#### 2b — 归入已有 `pro.sketchware.core.build`（3 文件）

| # | 源文件 | 来源包 |
|---|--------|--------|
| 11 | BuildProgressReceiver.java | mod.jbk.build |
| 12 | ViewBindingBuilder.kt | mod.pranav.viewbinding |
| 13 | BuildSettings.java | mod.hey.studios.build |

**影响范围**：13 个 import 变更。无 Manifest/布局影响。

---

### Phase 3：代码生成扩展（16 文件）

**原理**：Block/Event/Component 的扩展定义和代码注入逻辑，与 `pro.sketchware.core.codegen` 现有 23 个文件同属一个功能域。

**commit**: `refactor(codegen): merge custom block/event/component handlers`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | BlocksHandler.java (126KB) | mod.hilal.saif.blocks | pro.sketchware.core.codegen |
| 2 | CommandBlock.java | mod.hilal.saif.blocks | pro.sketchware.core.codegen |
| 3 | BlockTypeUtils.java | mod.hilal.saif.blocks | pro.sketchware.core.codegen |
| 4 | EventsHandler.java | mod.hilal.saif.events | pro.sketchware.core.codegen |
| 5 | LogicHandler.java | mod.hilal.saif.events | pro.sketchware.core.codegen |
| 6 | ComponentsHandler.java | mod.hilal.saif.components | pro.sketchware.core.codegen |
| 7 | ComponentExtraCode.java | mod.hilal.saif.components | pro.sketchware.core.codegen |
| 8 | AndroidManifestInjector.java | mod.hilal.saif.android_manifest | pro.sketchware.core.codegen |
| 9 | ExtraPaletteBlock.java (108KB) | dev.aldi.sayuti.block | pro.sketchware.core.codegen |
| 10 | ExtraBlockFile.java | dev.aldi.sayuti.block | pro.sketchware.core.codegen |
| 11 | AppCompatInjection.java | dev.aldi.sayuti.editor.injection | pro.sketchware.core.codegen |
| 12 | ManifestInjection.java | dev.aldi.sayuti.editor.injection | pro.sketchware.core.codegen |
| 13 | EditorManifest.java | mod.agus.jcoderz.editor.manifest | pro.sketchware.core.codegen |
| 14 | ExtraBlockInfo.java | mod.hey.studios.editor.manage.block | pro.sketchware.core.codegen |
| 15 | BlockLoader.java | mod.hey.studios.editor.manage.block.v2 | pro.sketchware.core.codegen |
| 16 | ConstVarComponent.java | mod.agus.jcoderz.handle.component | pro.sketchware.core.codegen |

**影响范围**：16 个 import 变更 + ~8 处 FQN javadoc `@link` 更新。无 Manifest/布局影响。

---

### Phase 4：库管理（16 文件）

**原理**：内置库定义、本地库管理、依赖下载全部统一到新建的 `pro.sketchware.library` 包。

**commit**: `refactor(library): consolidate library management classes`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | BuiltInLibraries.java (48KB) | mod.jbk.build | pro.sketchware.library |
| 2 | ExcludeBuiltInLibrariesActivity.java | mod.jbk.editor.manage.library | pro.sketchware.library |
| 3 | ExcludeBuiltInLibrariesLibraryItemView.java | mod.jbk.editor.manage.library | pro.sketchware.library |
| 4 | LibrarySettingsImporter.java | mod.jbk.editor.manage.library | pro.sketchware.library |
| 5 | ManageLocalLibrary.java | mod.agus.jcoderz.editor.manage.library.locallibrary | pro.sketchware.library |
| 6 | ExtLibSelected.java | mod.agus.jcoderz.editor.library | pro.sketchware.library |
| 7 | ManageLocalLibraryActivity.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 8 | LocalLibrariesUtil.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 9 | LocalLibrary.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 10 | LocalLibrariesComparator.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 11 | LocalLibraryImportPackageIndex.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 12 | LibraryDownloaderDialogFragment.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 13 | DependencyDownloadItem.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 14 | DependencyDownloadAdapter.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 15 | SubDependenciesActivity.java | dev.aldi.sayuti.editor.manage | pro.sketchware.library |
| 16 | DependencyResolver.kt | mod.pranav.dependency.resolver | pro.sketchware.library |

**影响范围**：16 个 import 变更。**3 个 Manifest Activity 声明需更新**（ManageLocalLibraryActivity、SubDependenciesActivity、ExcludeBuiltInLibrariesActivity）。

---

### Phase 5：项目设置与备份（13 文件）

**commit**: `refactor(project): consolidate project settings, proguard, stringfog, backup`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | ProjectSettings.java | mod.hey.studios.project | pro.sketchware.project |
| 2 | ProjectSettingsDialog.java | mod.hey.studios.project | pro.sketchware.project |
| 3 | ProjectTracker.java | mod.hey.studios.project | pro.sketchware.project |
| 4 | ProguardHandler.java | mod.hey.studios.project.proguard | pro.sketchware.project |
| 5 | ManageProguardActivity.java | mod.hey.studios.project.proguard | pro.sketchware.project |
| 6 | StringfogHandler.java | mod.hey.studios.project.stringfog | pro.sketchware.project |
| 7 | ManageStringFogFragment.java | mod.hey.studios.project.stringfog | pro.sketchware.project |
| 8 | CustomBlocksDialog.java | mod.hey.studios.project.custom_blocks | pro.sketchware.project |
| 9 | CustomBlocksManager.java | mod.hey.studios.project.custom_blocks | pro.sketchware.project |
| 10 | BackupFactory.java | mod.hey.studios.project.backup | pro.sketchware.project |
| 11 | BackupRestoreManager.java | mod.hey.studios.project.backup | pro.sketchware.project |
| 12 | SingleCopyTask.kt | mod.tyron.backup | pro.sketchware.project |
| 13 | CallBackTask.java | mod.tyron.backup | pro.sketchware.project |

**影响范围**：13 个 import 变更。**1 个 Manifest Activity 声明需更新**（ManageProguardActivity）。BackupFactory.java 有 2 处 FQN 字符串引用 `mod.hilal.saif.activities.tools.ConfigActivity`，需在 Phase 6 后一并更新（或此处先用 import 替代 FQN）。

---

### Phase 6：编辑器 Activity 与对话框（27 文件）

**分 3 个 commit 执行。**

#### 6a — Block/Makeblock 编辑器（9 文件）

**commit**: `refactor(editor): migrate block and makeblock management screens`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | BlocksManager.java | mod.hilal.saif.activities.tools | pro.sketchware.activities.editor.block |
| 2 | BlocksManagerDetailsActivity.java | mod.hilal.saif.activities.tools | pro.sketchware.activities.editor.block |
| 3 | BlocksManagerCreatorActivity.java | mod.hilal.saif.activities.tools | pro.sketchware.activities.editor.block |
| 4 | PaletteSelector.java | mod.agus.jcoderz.editor.manage.block.palette | pro.sketchware.activities.editor.block |
| 5 | MoreblockImporter.java | mod.jbk.editor.manage | pro.sketchware.activities.editor.makeblock |
| 6 | MoreblockValidator.java | mod.hey.studios.moreblock | pro.sketchware.activities.editor.makeblock |
| 7 | ReturnMoreblockManager.java | mod.hey.studios.moreblock | pro.sketchware.activities.editor.makeblock |
| 8 | MoreblockImporterDialog.java | mod.hey.studios.moreblock.importer | pro.sketchware.activities.editor.makeblock |
| 9 | BlockMenu.java | mod.agus.jcoderz.editor.manage.block.makeblock | pro.sketchware.activities.editor.makeblock |

**Manifest 变更**：3 个 Activity（BlocksManager、BlocksManagerCreatorActivity、BlocksManagerDetailsActivity）。

#### 6b — Manifest/Permission/Resource/Misc 编辑器（11 文件）

**commit**: `refactor(editor): migrate manifest, permission, resource, and misc editor screens`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | AndroidManifestInjection.java | mod.hilal.saif.activities.android_manifest | pro.sketchware.activities.editor.manifest |
| 2 | AndroidManifestInjectionDetails.java | mod.hilal.saif.activities.android_manifest | pro.sketchware.activities.editor.manifest |
| 3 | ManagePermissionActivity.java | mod.agus.jcoderz.editor.manage.permission | pro.sketchware.activities.editor |
| 4 | ListPermission.java | mod.agus.jcoderz.editor.manage.permission | pro.sketchware.activities.editor |
| 5 | ManageResourceActivity.java | mod.agus.jcoderz.editor.manage.resource | pro.sketchware.activities.editor |
| 6 | ManageAssetsActivity.java | mod.hey.studios.activity.managers.assets | pro.sketchware.activities.editor |
| 7 | ManageNativelibsActivity.java | mod.hey.studios.activity.managers.nativelib | pro.sketchware.activities.editor |
| 8 | ManageJavaActivity.java | mod.hey.studios.activity.managers.java | pro.sketchware.activities.editor |
| 9 | ManageEvent.java | mod.agus.jcoderz.editor.event | pro.sketchware.activities.editor |
| 10 | SrcCodeEditor.java | mod.hey.studios.code | pro.sketchware.activities.editor.code |
| 11 | LogReaderActivity.java | mod.khaled.logcat | pro.sketchware.activities.editor |

**Manifest 变更**：8 个 Activity（AndroidManifestInjection、AndroidManifestInjectionDetails、ManagePermissionActivity、ManageResourceActivity、ManageAssetsActivity、ManageNativelibsActivity、ManageJavaActivity、SrcCodeEditor）+ 1 个（LogReaderActivity）= 9 个。

#### 6c — 设置与对话框（7 文件）

**commit**: `refactor(editor): migrate settings and dialog classes`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | ConfigActivity.java | mod.hilal.saif.activities.tools | pro.sketchware.activities.settings |
| 2 | AppSettings.java | mod.hilal.saif.activities.tools | pro.sketchware.activities.settings |
| 3 | AsdDialog.java | mod.hilal.saif.asd | pro.sketchware.dialogs |
| 4 | AsdHandlerCodeEditor.java | mod.hilal.saif.asd | pro.sketchware.dialogs |
| 5 | DialogButtonGradientDrawable.java | mod.hilal.saif.asd | pro.sketchware.dialogs |
| 6 | IconSelectorDialog.java | mod.hilal.saif.activities.tools | pro.sketchware.dialogs |
| 7 | GetKeyStoreCredentialsDialog.java | mod.jbk.export | pro.sketchware.dialogs |

**Manifest 变更**：2 个 Activity（ConfigActivity、AppSettings）。

**额外清理**：BackupFactory.java 中的 2 处 `mod.hilal.saif.activities.tools.ConfigActivity` FQN 引用需改为 import 引用。`FileResConfig.java:49` 的 `"mod.agus.jcoderz"` 字符串常量需更新。

---

### Phase 7：工具类与数据（20 文件）

**commit**: `refactor(util): consolidate utility classes and data models`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | Helper.java | mod.hey.studios.util | pro.sketchware.util |
| 2 | CompileLogHelper.java | mod.hey.studios.util | pro.sketchware.util |
| 3 | ProjectFile.java | mod.hey.studios.util | pro.sketchware.util |
| 4 | SystemLogPrinter.java | mod.hey.studios.util | pro.sketchware.util |
| 5 | JarCheck.java | mod.hey.studios.lib | pro.sketchware.util |
| 6 | IdGenerator.java | mod.hey.studios.editor.view | pro.sketchware.util |
| 7 | LogUtil.java | mod.jbk.util | pro.sketchware.util |
| 8 | BlockUtil.java | mod.jbk.util | pro.sketchware.util |
| 9 | AudioMetadata.java | mod.jbk.util | pro.sketchware.util |
| 10 | TestkeySignBridge.java | mod.jbk.util | pro.sketchware.util |
| 11 | SoundPlayingAdapter.java | mod.jbk.util | pro.sketchware.util |
| 12 | OldResourceIdMapper.java (84KB) | mod.jbk.util | pro.sketchware.util |
| 13 | PCP.java | mod.hilal.saif.lib | pro.sketchware.util |
| 14 | CompileErrorSaver.java | mod.jbk.diagnostic | pro.sketchware.core.build |
| 15 | MissingFileException.java | mod.jbk.diagnostic | pro.sketchware.core.exception |
| 16 | ApkSigner.java | mod.alucard.tn.apksigner | pro.sketchware.util.apk |
| 17 | VectorDrawableLoader.java | mod.bobur | pro.sketchware.graphics |
| 18 | VectorDrawableParser.java | mod.bobur | pro.sketchware.graphics |
| 19 | ViewBeans.java | mod.agus.jcoderz.beans | pro.sketchware.beans |
| 20 | CustomAttributeView.java | mod.remaker.view | pro.sketchware.widgets |

**影响范围**：20 个 import 变更。无 Manifest 影响。`pro.sketchware.graphics` 为新建包。

---

### Phase 8：代码编辑器组件（6 文件）

**commit**: `refactor(editor): consolidate code editor UI components`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | CodeEditorEditText.java | mod.hey.studios.lib.code_editor | pro.sketchware.lib.code_editor |
| 2 | CodeEditorLayout.java | mod.hey.studios.lib.code_editor | pro.sketchware.lib.code_editor |
| 3 | ColorScheme.java | mod.hey.studios.lib.code_editor | pro.sketchware.lib.code_editor |
| 4 | ColorTheme.java | mod.hey.studios.lib.code_editor | pro.sketchware.lib.code_editor |
| 5 | CodeEditorColorSchemes.java | mod.jbk.code | pro.sketchware.lib.code_editor |
| 6 | CodeEditorLanguages.java | mod.jbk.code | pro.sketchware.lib.code_editor |

**额外清理**：2 个 XML 布局文件需更新 FQN：
- `res/layout/code_editor_layout.xml` — `mod.hey.studios.lib.code_editor.CodeEditorEditText` → `pro.sketchware.lib.code_editor.CodeEditorEditText`
- `res/layout/code_editor_layout_nowrap.xml` — 同上

---

### Phase 9：MultiDex（6 文件）

**commit**: `refactor(build): move multidex classes to core.build.multidex`

| # | 源文件 | 来源包 | 目标包 |
|---|--------|--------|--------|
| 1 | MainDexListBuilder.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |
| 2 | ClassReferenceListBuilder.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |
| 3 | Path.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |
| 4 | ClassPathElement.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |
| 5 | FolderPathElement.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |
| 6 | ArchivePathElement.java | mod.agus.jcoderz.multidex | pro.sketchware.core.build.multidex |

**影响范围**：6 个 import 变更 + ~5 处代码内 FQN 引用（`new mod.agus.jcoderz.multidex.FolderPathElement()` 等）。

---

## 四、执行规则

### 4.1 每个 Phase / commit 的门禁检查

```powershell
# 1. 编译通过
.\gradlew.bat :app:compileDebugJavaWithJavac

# 2. 旧包无残留（以 Phase N 涉及的源包为目标）
# 例如 Phase 1 后：
Get-ChildItem "app\src\main\java\mod\agus\jcoderz\editor\view" -Recurse -Filter "*.java"
# 应返回空

# 3. import 无残留
Select-String -Path "app\src\main\java" -Pattern "import mod\.agus\.jcoderz\.editor\.view\." -Include "*.java" -Recurse
# 应返回空

# 4. Manifest 无旧引用
Select-String -Path "app\src\main\AndroidManifest.xml" -Pattern "mod\.|dev\.aldi"
# Phase 全部完成后应返回空

# 5. git diff --check
git diff --check HEAD
```

### 4.2 迁移单个文件的标准操作

1. **修改 `package` 声明**为目标包
2. **移动文件**到目标目录
3. **全局搜索替换** `import <旧FQCN>` → `import <新FQCN>`
4. **搜索代码内 FQN 引用**（`new <旧FQCN>()`、javadoc `@link`、字符串常量）并更新
5. **如果是 Activity**：更新 `AndroidManifest.xml` 中的 `android:name`
6. **如果是自定义 View**：更新 XML 布局中的 FQN 标签

### 4.3 commit 格式

```
refactor(<scope>): <summary>

Migrate N files from mod.*/dev.* to pro.sketchware.*:
- <source_pkg_1> → <target_pkg_1> (N files)
- <source_pkg_2> → <target_pkg_2> (N files)
```

---

## 五、迁移后空目录清理

每个 Phase 完成后，对应的 `mod.*` / `dev.*` 源目录应变为空。在**所有 9 个 Phase 全部完成且主分支合并后**，执行一次性清理：

```
chore: remove empty mod/ and dev/ directories
```

预期最终状态：
- `mod/` 目录完全删除
- `dev/` 目录完全删除
- 所有 167 个文件位于 `pro.sketchware.*` 或 `com.besome.sketch.*` 下

---

## 六、迁移后的包结构预览

```
pro.sketchware/                       (1 file: SketchApplication)
├── activities/                       (现有 + Phase 6 新增)
│   ├── about/
│   ├── appcompat/
│   ├── editor/
│   │   ├── block/                   ★ Phase 6a 新建
│   │   ├── code/                    ★ Phase 6b（SrcCodeEditor）
│   │   ├── command/
│   │   ├── component/
│   │   ├── makeblock/               ★ Phase 6a 新建
│   │   ├── manifest/                ★ Phase 6b 新建
│   │   ├── view/
│   │   └── (ManagePermission, ManageResource, ManageAssets, ManageNativelibs,
│   │        ManageJava, ManageEvent, LogReader, ListPermission)
│   ├── iconcreator/
│   ├── importicon/
│   ├── main/
│   ├── preview/
│   ├── resourceseditor/
│   └── settings/                    (现有 + Phase 6c: ConfigActivity, AppSettings)
├── beans/                           (现有 4 + Phase 7: ViewBeans)
├── blocks/                          (现有，保留)
├── control/                         (现有，保留)
├── core/
│   ├── async/
│   ├── build/
│   │   ├── compiler/                ★ Phase 2a 新建（10 文件）
│   │   ├── multidex/                ★ Phase 9 新建（6 文件）
│   │   ├── (现有 7 + Phase 2b: BuildProgressReceiver, ViewBindingBuilder, BuildSettings)
│   │   └── (Phase 7: CompileErrorSaver)
│   ├── callback/
│   ├── codegen/                     (现有 23 + Phase 3: 16 文件 = 39)
│   ├── exception/                   (现有 3 + Phase 7: MissingFileException)
│   ├── fragments/
│   ├── project/
│   ├── ui/
│   ├── util/
│   └── validation/
├── dialogs/                         (现有 3 + Phase 6c: 5 文件)
├── firebase/
├── fragments/
├── graphics/                        ★ Phase 7 新建（VectorDrawable 2 文件）
├── lib/
│   ├── base/
│   ├── code_editor/                 ★ Phase 8 新建（6 文件）
│   ├── highlighter/
│   └── iconcreator/
├── library/                         ★ Phase 4 新建（16 文件）
├── menu/
├── project/                         ★ Phase 5 新建（13 文件）
├── tools/
├── util/                            (现有 22 + Phase 7: 13 文件)
│   ├── apk/                         (现有 2 + Phase 7: ApkSigner)
│   ├── library/
│   ├── relativelayout/
│   ├── theme/
│   └── xml/
└── widgets/                         (现有 2 + Phase 7: CustomAttributeView)

com.besome.sketch/                   (不变，仅 Phase 1 增加文件)
└── editor/
    └── view/
        ├── palette/                 (现有 22 + Phase 1a: 30 = 52)
        └── item/                    (现有 25 + Phase 1b: 20 = 45)
```

---

## 七、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Manifest Activity 声明遗漏 | 运行时 ActivityNotFoundException | 每个 Phase 有 Manifest Activity 变更时，用脚本自动验证 |
| XML 布局 FQN 遗漏 | 运行时 InflateException | Phase 8 后全局扫描 `res/layout/*.xml` 中的 `mod.`/`dev.` 引用 |
| FQN 字符串常量遗漏 | 运行时功能异常 | `FileResConfig.java:49` 需手动确认字符串用途再更新 |
| Kotlin 文件 `package` 声明 | 编译失败 | 7 个 .kt 文件需同步修改 package 声明 |
| BackupFactory FQN 引用 ConfigActivity | Phase 5 先于 Phase 6，中间态 FQN 仍指向旧包 | Phase 5 先将 FQN 改为 import（不影响位置），Phase 6 迁移时 import 自然更新 |
| 0 字节占位文件 | 无编译影响 | `com.besome.sketch.beans` 的 2 个 0 字节文件保持不动，待后续 chore commit 清理 |

---

## 八、后续阶段（不在本次范围）

完成上述 9 个 Phase 后，可启动：

1. **`pro.sketchware` 内部整理** — 合并双重 util/（`pro.sketchware.util` + `core.util`）、双重 lib/（`pro.sketchware.lib` + `com.besome.sketch.lib`）、整理 `tools/control/menu/widgets`
2. **`com.besome.sketch` → `pro.sketchware` 整体迁移** — 203 文件，按子包分批（beans → lib → editor → design → 其余）
3. **0 字节占位文件清理** — `chore: remove empty placeholder files`
4. **Unicode mojibake 修复** — `chore(docs): normalize unicode punctuation in javadoc`
