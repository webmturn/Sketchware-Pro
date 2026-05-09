# Package Refactor Kickoff Pack

This document is the **kickoff pack** for the multi-session package restructuring of `pro.sketchware.core` (currently 124 flat files / ~1.5 MB).

It is split into:

1. **Context snapshot** — what the next model session needs to know up-front
2. **Phase 1 prompt** — planning session (Opus 4.7 High, 1M context)
3. **Phase 2 prompt template** — execution sessions (Sonnet 4.5 Medium)
4. **Phase 3 prompt** — review pass (Sonnet, fresh session)
5. **Acceptance checklist** — what each batch must pass before commit

Use these prompts verbatim. They are designed so a fresh model session does not need to re-derive context from scratch.

---

## 1. Context snapshot

- **Repo root**: `c:\Users\Administrator\IdeaProjects\Sketchware-Pro`
- **Single Gradle module**: `app/`
- **Branch**: `main`
- **Java toolchain**: `JAVA_HOME=C:\Users\Administrator\.jdks\corretto-23.0.2`
- **Build verification**: `.\gradlew.bat :app:compileDebugJavaWithJavac` (~30s)
- **Full smoke build**: `.\gradlew.bat :app:assembleDebug` (~1m30s)
- **Latest tag**: `v7.0.0-beta-09` (versionCode 160)
- **User language**: 中文 (Chinese)
- **Commit style**: `type(scope): summary` (Conventional Commits)

### Target package

```
app/src/main/java/pro/sketchware/core/   ← 124 .java files, ~1.5 MB total
```

It currently mixes the following responsibilities at the same flat depth:

- Project data store / managers (`ProjectDataStore`, `ProjectFileManager`, `ResourceManager`, `LibraryManager`, `ProjectListManager`, `ProjectDataManager`, `SketchwarePaths`, `EncryptedFileUtil`, `ProjectDataParser`)
- Codegen (`ActivityCodeGenerator`, `ComponentCodeGenerator`, `ComponentTypeMapper`, `ComponentTemplates`, `LayoutGenerator`, `ManifestGenerator`, `BlockInterpreter`, `BlockSpecRegistry`, `BlockCodeRegistry`, `BlockCodeHandler`, `EventCodeRegistry`, `EventCodeHandler`, `EventCodeGenerator`, `ListenerCodeRegistry`, `BlockColorMapper`, `BlockConstants`, `EventRegistry`, `LocalLibraryManifestMerger`, `GradleFileGenerator`, `XmlLayoutParser`, `ProjectFilePaths`, `IncrementalBuildCache`, `CodeContext`, `CodeFormatter`, `ClassInfo`, `BuildConfig`, `BuiltInLibrary`, `CompileQuizManager`, `SketchwareConstants`)
- Build/sign (`ProjectBuilder`, `KeyStoreManager`, `KeyStoreOutputStream`)
- Validators (`*Validator.java` ~10 files extending `BaseValidator`)
- Block UI (`BaseBlockView`, `BlockView`, `DefinitionBlockView`, `FieldBlockView`, `ViewHistoryManager`, `BlockHistoryManager`, `RecentHistoryManager`)
- Generic UI fragments (`ImageListFragment`, `SoundListFragment`, `ImageCollectionFragment`, `EventListFragment`, `ComponentListFragment`, `ViewFilesFragment`, `ViewEditorFragment`, `SoundImportFragment`, `BaseFragment`, `PermissionFragment`)
- Collection managers (`*CollectionManager.java`)
- View files adapter / listeners (`ViewFiles*.java`, `WidgetPaletteIcon`, `PresetLayoutFactory`)
- Utilities (`AnimationUtil`, `BitmapUtil`, `DateTimeUtil`, `DeviceUtil`, `FormatUtil`, `MapValueHelper`, `GsonMapHelper`, `HashMapTypeToken`, `NinePatchDecoder`, `ReflectiveToString`, `SharedPrefsHelper`, `SketchToast`, `ThrottleTimer`, `UIHelper`, `UriPathResolver`, `ViewUtil`, `ZipUtil`)
- Async infra (`BackgroundTasks`, `TaskHost`)
- Callback/exception types (`SimpleCallback`, `BuildCallback`, `IntCallback`, `EventSelectedCallback`, `FileSelectedCallback`, `PropertyChangedCallback`, `ViewBeanCallback`, `ViewEditorCallback`, `BlockSizeListener`, `ViewEnableRunnable`, `SimpleException`, `SketchwareException`, `CompileException`)
- Firebase preview UI (`FirebasePreviewView`, `FirebaseSettingsView`, `FirebaseStorageView`)
- User experience (`UserExperienceLevel`, `LibraryConfigView`, `LibrarySettingsView`)

### Why this refactor matters

- Flat 124 files makes "find by responsibility" expensive.
- Cross-cutting changes (e.g. add a new opcode, add a new component type) currently require remembering which 5–7 files in this flat list to touch.
- Future god-class splits (LogicEditorActivity 147KB, ProjectFilePaths 82KB, ProjectBuilder 70KB, ProjectDataStore 70KB) need a clean home for extracted helpers.

### What is OUT of scope

- **No behavior changes**. Pure package moves + import updates.
- **No file content changes** beyond the `package` declaration line and any internal references.
- **Do NOT touch**:
  - `com.besome.sketch.*` (legacy editor surface, separate later refactor)
  - `mod.*`, `dev.aldi.*`, `kellinwood.*` (third-party / vendored)
  - The `pro.sketchware` Activities, Fragments outside `core/`
- **Do NOT** rename classes (only package).
- **Do NOT** change visibility modifiers.
- **Do NOT** delete dead code in this pass — that is a separate audit.

### Key existing memories worth respecting

- `pro.sketchware.core` is the post-deobfuscation home of the old `a.a.a` package. Naming map is in `docs/refactoring-naming-map.md`.
- `BackgroundTasks` + `TaskHost` is the unified async pattern; do not replace.
- `BlockSpecRegistry`/`BlockCodeRegistry`/`EventCodeRegistry` already use Registry pattern — keep them grouped together.
- `SketchwarePaths` is the central path source — do not split its constants.
- `ProjectDataManager` is a Facade over the other Managers — must be visible to UI layers.

---

## 2. Phase 1 prompt — Planning session (Opus 4.7 High)

Paste this as the **first message** of a new Opus 4.7 High session. Goal: produce a written plan, **no code edits**.

> ```
> 你将主导 Sketchware-Pro 项目 pro.sketchware.core 包的拆分重构方案制定。
>
> 背景与约束已经写在 docs/package-refactor-kickoff.md，请先完整阅读这个文件，然后再阅读以下三类输入：
>
> 1. pro.sketchware.core 目录下的全部 124 个 .java 文件（用 list_dir + read_file 拿到文件名列表，再有选择地读取关键文件）
> 2. docs/refactoring-naming-map.md（去混淆历史背景）
> 3. docs/code-generation-system-analysis.md（codegen 子系统当前架构）
>
> 你的任务：产出一份新文档 docs/package-refactor-plan.md，内容必须包含：
>
> A. 目标子包划分
>    - 推荐切成 5–9 个子包（不要更多，避免碎片化）
>    - 每个子包给一个明确的命名（如 core.codegen, core.project, core.build, core.validation, core.async, core.ui, core.util, core.fragments）
>    - 每个子包列出它包含的全部 .java 文件名（必须是当前 124 个文件的完整划分，不漏不重）
>    - 每个子包写一句话职责定义
>
> B. 文件依赖图（粗粒度）
>    - 用 grep 统计每个子包对其它子包的引用强度（class 引用次数）
>    - 用 ASCII 表格列出 (from -> to, ref_count)
>    - 标注循环依赖（如果有）
>
> C. 拆分批次顺序
>    - 每批 ≤ 15 文件
>    - 必须保证：每批完成后，:app:compileDebugJavaWithJavac 单独通过
>    - 优先迁移叶子包（被依赖少的），最后迁移枢纽（如 core.codegen 中的 ProjectFilePaths）
>    - 每批给一个 commit message 草稿（type(scope): summary 格式）
>
> D. 风险点列表
>    - 哪些文件被外部（com.besome.sketch / mod.* / dev.* / kellinwood.* / pro.sketchware 其它子包）引用最多 — 用 grep 统计
>    - 哪些文件用反射访问类名（必须保留全限定名手工修复）
>    - 哪些文件参与序列化 / Gson / Intent extra（包名变化可能影响持久化数据兼容性）
>
> E. 验收 checklist 模板
>    - 每批 commit 前必须验证的命令清单
>    - 每批 commit 后建议的回归点
>
> 强制规则：
> - 不要直接改代码或移动文件。本会话只输出方案文档。
> - 不要建议改动行为、修改可见性、删除"看起来死"的代码。
> - 不要把 com.besome.sketch / mod.* / dev.* / kellinwood.* 纳入此次重构范围。
> - 中间发现真正必要的重命名（极少数情况），列在文档"待人工决策"小节，不要擅自纳入计划。
> - 输出完成后，用一句话总结"如果按此计划执行，预计需要多少批 / 预计总执行 token 量级"。
>
> 完成后请仅输出 docs/package-refactor-plan.md 的写入结果，不要其他多余说明。
> ```

**预期产出**：`docs/package-refactor-plan.md`，~800–1500 行，可直接交给 Phase 2 的执行模型。

---

## 3. Phase 2 prompt template — Execution session (Sonnet 4.5 Medium)

每完成一批就开 **新的 Sonnet 会话**（避免长会话 drift）。模板：

> ```
> 你将执行 Sketchware-Pro 包重构计划的第 N 批。
>
> 必读：
> 1. docs/package-refactor-kickoff.md（约束 + 验收 checklist）
> 2. docs/package-refactor-plan.md → 跳到"第 N 批"小节，按其中的文件清单严格执行
>
> 执行步骤：
> 1. 用 git status 确认工作树干净（如不干净，停止并报告）
> 2. 按计划列出的文件清单：
>    a. 创建目标子包目录（如 app/src/main/java/pro/sketchware/core/codegen）
>    b. 用 git mv 将每个文件移动到新位置（保留 git 历史）
>    c. 修改每个被移动文件的 package 声明
>    d. 用 grep_search 找出所有引用这些类的文件（包内+包外），更新它们的 import 语句
> 3. 运行 .\gradlew.bat :app:compileDebugJavaWithJavac
>    - 如果编译失败：分析错误，修复 import，重试。最多 3 次。
>    - 如果 3 次仍失败：停止，把错误贴出来求助，不要硬改。
> 4. 编译通过后，运行 git diff --check 确认无空白错误
> 5. git add + commit，commit message 严格使用计划中的草稿
> 6. 不要 push（用户会统一 push）
>
> 强制规则：
> - 只移动计划中列出的文件，多一个少一个都不行
> - 不修改文件内容，除了：(a) package 声明，(b) 必要的 import 调整
> - 不重命名类
> - 不修改可见性
> - 不删除任何代码
> - 不动 com.besome.sketch / mod.* / dev.* / kellinwood.*
>
> 完成后报告：
> - 移动的文件数
> - 修改的引用方文件数
> - 编译耗时
> - commit hash
> ```

> **变量替换**：把 `第 N 批` 替换为实际批次号（如"第 1 批"），其它保持不变。

---

## 4. Phase 3 prompt — Review pass (Sonnet, fresh session)

每 3 批 / 全部完成后跑一次独立审视：

> ```
> 你是独立审视者。Sketchware-Pro 刚完成 pro.sketchware.core 包拆分的第 1–3 批（或全部）。
>
> 审视范围：从 commit X 到 HEAD（用 git log --oneline 查看具体 hash 范围）。
>
> 任务：
> 1. 用 git diff --stat X..HEAD 看总改动规模
> 2. 抽样检查 5 个代表性文件，确认：
>    - package 声明已更新
>    - 没有意外的代码内容改动（除 package + import 之外）
>    - 没有删除代码
> 3. 用 grep 全仓搜索是否还有 `pro.sketchware.core.<旧扁平类名>` 这种残留引用（应当为 0）
> 4. 跑 .\gradlew.bat :app:assembleDebug 验证完整产物
> 5. 用 docs/package-refactor-plan.md 中的 checklist 逐项确认
>
> 输出：
> - 通过/不通过
> - 如不通过，列出具体问题文件 + 建议修复
> - 不要直接改代码（让原执行模型修）
> ```

---

## 5. Acceptance checklist（每批 commit 前必过）

```
[ ] git status 在执行前是 clean 的
[ ] 本批移动的文件数 = 计划文档列出的数量（精确匹配）
[ ] 每个被移动的文件，第一行 package 声明已更新
[ ] 没有任何文件出现「除 package + import 以外」的代码内容改动
   验证: git diff <commit-before> -- <files> | grep -v "^[-+]package " | grep -v "^[-+]import " | grep "^[-+]" | grep -v "^[-+]\{3\}"
[ ] grep_search 全仓搜索旧 import 路径，结果为空
[ ] .\gradlew.bat :app:compileDebugJavaWithJavac 通过
[ ] git diff --check 无空白错误
[ ] commit message 使用 conventional commits 格式且与计划文档草稿一致
[ ] 没有改动 com.besome.sketch / mod.* / dev.* / kellinwood.*
```

每 3 批附加一次：

```
[ ] .\gradlew.bat :app:assembleDebug 通过（完整产物 smoke 验证）
[ ] adb install -r app\build\outputs\apk\debug\app-debug.apk 后能打开应用主屏幕
```

全部完成后附加一次：

```
[ ] 选一个简单工程 (e.g. project 743) 完整跑一遍：打开 → 编辑一个 block → 保存 → Run → 装机运行
[ ] git log --stat 上看总改动只涉及 pro.sketchware.core/** + 各处 import 调整
[ ] 写 RELEASE_NOTES.md 条目（refactor 段落）
```

---

## 6. 模型成本预估（粗略）

| 阶段 | 模型 | 会话数 | 预估 token |
|---|---|---|---|
| Phase 1 规划 | Opus 4.7 High | 1 | ~300K（读全部 124 文件 + 输出方案）|
| Phase 2 执行 | Sonnet 4.5 Medium | 5–8（每批一个） | ~80K × N |
| Phase 3 审视 | Sonnet 4.5 Medium | 2–3 | ~50K × N |

> 用 Phase 1 的全景理解换 Phase 2/3 的"窄上下文 + 廉价模型"，是这个流程的核心 ROI 杠杆。
