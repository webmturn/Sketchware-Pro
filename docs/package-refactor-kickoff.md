# Package Refactor Kickoff Pack

This document is the **kickoff pack** for the multi-session package restructuring of `pro.sketchware.core` (currently 124 flat files / ~1.5 MB).

It is split into:

1. **Context snapshot** — what the next model session needs to know up-front
2. **Phase 1 prompt** — planning session (Opus 4.7 High, 1M context)
3. **Phase 2 prompt template** — execution sessions (Sonnet 4.5 Medium)
4. **Phase 3 prompt** — review pass (Sonnet, fresh session)
5. **Acceptance checklist** — what each batch must pass before commit
6. **Branch / rollback / completion** — operational rules and exit criteria
7. **Cost estimate** — per-phase token budget

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

### Verified baseline (measured 2026-05-09 on `main` @ commit `566de958e`)

These numbers are the empirical baseline. Phase 1 / Phase 2 sessions MUST treat them as ground truth and not re-derive them.

| Item | Value | Implication |
|---|---|---|
| Files inside `pro.sketchware.core/` | **124** | Total files to relocate |
| External Java files importing core | **260** | Files outside `core/` whose imports must be rewritten |
| Total `import pro.sketchware.core.X` lines (external) | **764** | Total import lines to update |
| External package distribution | `com.besome.sketch`=134, `mod.*`=59, `pro.sketchware.*` (non-core)=50, `dev.aldi.*`=17 | Update surface is spread across 4 root packages |
| References from `AndroidManifest.xml` | **0** | Manifest is safe; no Activity/Service/Receiver classes live in `core/` |
| References from `app/src/main/res/**` (XML) | **0** | Layouts/preferences do not reference core classes |
| References from `*.gradle` / `proguard-rules.pro` | **0** | Build config and shrink rules are safe |
| `Class.forName("pro.sketchware.core...")` / quoted FQN string refs | **0** | No hidden reflective resolution by package name |
| Reflective calls inside `core/` | **2** (`NinePatchDecoder` → `Bitmap.mNinePatchChunk` system field; `ProjectBuilder` → `dx.command.dexer.Main$Arguments`) | Both target classes outside `core/` — package move is safe |
| `EncryptedFileUtil` on-disk format | Section-based **plain text** (not Java serialization) | Package renames do not affect saved project data compatibility |
| Test source code (JUnit / instrumented) | **0** | No automated test suite — verification is `assembleDebug` + manual smoke install |

**Conclusions baked into this plan (do not re-question)**:

- Pure package moves + import rewrites can achieve a fully behavior-preserving refactor.
- No Manifest / resource / proguard / reflection / serialization tail.
- Verification is exclusively: compile clean → assemble clean → install + open app + smoke a sample project.

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
>    - 每个子包列出它包含的全部 .java 文件名（**必须是当前 124 个文件的完整划分，不漏不重**）
>    - 每个子包写一句话职责定义
>    - **硬约束**：拆分完成后，`pro.sketchware.core` 自身（不含子包）必须为空。不允许以"杂项/通用"为由保留扁平类。
>
> B. 子包依赖规则与依赖图
>    - 强制要求：子包之间的依赖关系必须是 **DAG**（有向无环图），禁止循环依赖。
>    - 列出推荐的依赖方向（顶层例：`ui → fragments → codegen → project → build → async, util`），任何子包只能向"下游"依赖。
>    - 用 grep 统计每个子包对其它子包的实际引用强度（class 引用次数），用 ASCII 表格列出 (from → to, ref_count)。
>    - 如发现 grep 结果暗示循环依赖（A→B 和 B→A 同时存在），**必须** 在方案中提出消解策略：将一方涉及的少量类抽到更下游的子包，或合并两个子包。绝不允许在最终方案中残留循环依赖。
>
> C. 拆分批次顺序（必须使用以下结构化格式，Phase 2 模型按此机械读取）
>    - 每批 ≤ 15 文件
>    - 必须保证：每批完成后，`:app:compileDebugJavaWithJavac` 单独通过
>    - 优先迁移叶子子包（被依赖少的），最后迁移枢纽（如 core.codegen 中的 ProjectFilePaths）
>    - 每批严格使用以下 Markdown + YAML-like 块格式：
>
>      ```
>      ### 第 N 批：<sub_package_short_name>（叶子/中游/枢纽）
>      target_package: pro.sketchware.core.<name>
>      files:
>        - <ClassName1>.java
>        - <ClassName2>.java
>        ...
>      depends_on_already_moved:
>        - pro.sketchware.core.<earlier_subpackage>
>      external_import_sites_estimate: <int>   # 该批被外部 .java 文件引用的数量级估算
>      commit_message: "refactor(core): split <responsibility> into core.<name> sub-package"
>      ```
>
> D. 风险点列表
>    - 哪些文件被外部（com.besome.sketch / mod.* / dev.* / kellinwood.* / pro.sketchware 其它子包）引用最多 — 用 grep 统计 top 10
>    - 不需要重新统计反射 / 序列化 / Manifest / 资源 XML —— Verified baseline 已确认这些路径全部为 0，直接引用 Verified baseline 表格的结论即可
>    - 列出本次方案中确实跨子包高耦合（例如 ProjectFilePaths 同时引用 codegen 内部多个类）的文件，标注其所属子包及风险
>
> E. 验收 checklist 模板
>    - 引用 docs/package-refactor-kickoff.md 第 5 节中已经给出的 checklist 模板，不要重复发明，只列出与具体批次相关的额外回归点
>
> 强制规则：
> - 不要直接改代码或移动文件。本会话只输出方案文档。
> - 不要建议改动行为、修改可见性、删除"看起来死"的代码。
> - 不要把 com.besome.sketch / mod.* / dev.* / kellinwood.* 纳入此次重构范围。
> - 中间发现真正必要的重命名（极少数情况），列在文档"待人工决策"小节，不要擅自纳入计划。
> - 输出完成后，用一句话总结：基于 Verified baseline（260 外部文件 / 764 imports），按此方案预估 import 改动总量级 与 总批次数。
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
> 1. docs/package-refactor-kickoff.md（约束 + 验收 checklist + Verified baseline）
> 2. docs/package-refactor-plan.md → 跳到"第 N 批"小节，按其中的 YAML 块严格执行
>
> 工作分支：
> - 本次重构在 feature 分支 `refactor/core-package-split` 上进行（不是 main）
> - 如果当前分支不是 `refactor/core-package-split`：先 `git checkout -b refactor/core-package-split`（仅当首次执行）或 `git checkout refactor/core-package-split`
>
> 规模预期（来自 Verified baseline，符合此规模数量级即正常，不要中途"觉得搜错了"）：
> - 整个重构外部引用方共 260 个 Java 文件、764 处 import 行
> - 单批触及的引用方文件通常在数十至上百量级
> - 单批 grep 命中数远超 100 是正常的，不是 bug
>
> 执行步骤：
> 1. 用 `git status` 确认工作树干净（如不干净，停止并报告）；用 `git rev-parse --abbrev-ref HEAD` 确认在 `refactor/core-package-split` 分支
> 2. 按计划列出的文件清单：
>    a. 创建目标子包目录（如 `app/src/main/java/pro/sketchware/core/codegen`）
>    b. 用 `git mv` 将每个文件移动到新位置（保留 git 历史）
>    c. 修改每个被移动文件的 `package` 声明
>    d. 用 grep_search 全仓搜索 `import pro.sketchware.core.<旧扁平类名>;`（精确字面量），逐文件改成新子包路径
>    e. 同样搜索 **不带 import** 的 fully-qualified 引用 `pro\.sketchware\.core\.<ClassName>` （正则），如有则修复
>    f. 同样搜索 star import `import pro\.sketchware\.core\.\*;` （即使 0 命中也要记录"已检查"）
> 3. 运行 `.\gradlew.bat :app:compileDebugJavaWithJavac`
>    - 如果编译失败：分析错误，修复 import，重试。最多 3 次。
>    - 如果 3 次仍失败：停止，把错误贴出来求助，不要硬改。
> 4. 编译通过后，运行 `git diff --check` 确认无空白错误
> 5. **行为不变性验证**（重要）：对本批每个被移动的文件，运行：
>    ```
>    git show HEAD:<old-path> > /tmp/before.java
>    git diff --no-index --ignore-blank-lines /tmp/before.java <new-path>
>    ```
>    diff 必须**只包含** `package` 声明这一行的变化（以及该文件内部如有 fully-qualified 同包引用导致的等价 import 调整）。如果出现 package + import 之外的代码内容差异，停止并报告。
> 6. `git add` + `git commit`，commit message 严格使用计划中的草稿
> 7. **不要 push**（用户会统一审视后 push）
>
> 强制规则：
> - 只移动计划中列出的文件，多一个少一个都不行
> - 不修改文件内容，除了：(a) package 声明，(b) 必要的 import 调整（含外部引用方 import 重写）
> - 不重命名类
> - 不修改可见性
> - 不删除任何代码（即使看起来是 dead code）
> - 不动 `com.besome.sketch.*` / `mod.*` / `dev.*` / `kellinwood.*` 的内容（这些目录里的 `.java` 文件如有 import 行需更新，那是允许的，但仅限 import 行）
>
> 完成后报告：
> - 移动的文件数（应等于计划中列出的数量）
> - 修改的引用方文件数（外部 .java 文件数）
> - 修改的 import 行总数
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
> 审视范围：从某 commit X 到 HEAD（先用 `git log --oneline refactor/core-package-split` 查看 hash 范围，确认在 feature 分支上）。
>
> 任务：
> 1. `git diff --stat X..HEAD` 看总改动规模
> 2. 抽样检查 5 个代表性已移动文件，对每个文件运行：
>    ```
>    git show X:<old-path> > /tmp/before.java
>    git diff --no-index --ignore-blank-lines /tmp/before.java <new-path>
>    ```
>    diff 必须只在 `package` 声明 / 必要的 import 调整 这两类行上有变化
> 3. 用 grep 全仓搜索本批已迁移类名的旧路径残留：对每个已迁移类 `Foo`，搜索字面量 `import pro.sketchware.core.Foo;`，命中数应当为 0（即旧扁平 import 已被全部替换）
> 4. 用 grep 搜索 `import pro\.sketchware\.core\.\*;` star import 残留，命中数应当为 0
> 5. 跑 `.\gradlew.bat :app:assembleDebug` 验证完整产物
> 6. 用 docs/package-refactor-kickoff.md 第 5 节 checklist 逐项确认
>
> 输出：
> - 通过 / 不通过
> - 如不通过，列出具体问题文件 + 建议修复（描述即可）
> - 不要直接改代码（让原执行模型修）
> ```

---

## 5. Acceptance checklist（每批 commit 前必过）

```
[ ] git status 在执行前是 clean 的
[ ] 当前分支为 refactor/core-package-split（不是 main）
[ ] 本批移动的文件数 = 计划文档列出的数量（精确匹配）
[ ] 每个被移动的文件，第一行 package 声明已更新
[ ] 行为不变性：对本批每个被移动的文件，运行
      git show HEAD~1:<old-path> > <tmp>
      git diff --no-index --ignore-blank-lines <tmp> <new-path>
   diff 必须只包含 package 声明这一行（以及必要的 import 行）的变化，无其它代码内容改动
[ ] grep_search 全仓搜索 `import pro.sketchware.core.<旧扁平类名>;` 字面量，命中数 = 0
[ ] grep_search 全仓搜索 `import pro\.sketchware\.core\.\*;` star import 残留，命中数 = 0
[ ] .\gradlew.bat :app:compileDebugJavaWithJavac 通过
[ ] git diff --check 无空白错误
[ ] commit message 使用 conventional commits 格式且与计划文档草稿一致
[ ] 除「import 行」外没有改动 com.besome.sketch / mod.* / dev.* / kellinwood.* 的内容
```

每 3 批附加一次：

```
[ ] .\gradlew.bat :app:assembleDebug 通过（完整产物 smoke 验证）
[ ] adb install -r app\build\outputs\apk\debug\app-debug.apk 后能打开应用主屏幕（不闪退）
```

全部完成后附加一次（**完成定义**，详见第 6 节）：

```
[ ] pro.sketchware.core 包内已无任何 .java 文件（只剩子目录）
[ ] 选一个简单工程 (e.g. project 743) 完整跑一遍：打开 → 编辑一个 block → 保存 → Run → 装机运行
[ ] git log --stat 上看总改动只涉及 pro.sketchware.core/** 文件移动 + 各处 import 调整
[ ] 写 RELEASE_NOTES.md 条目（refactor 段落）
[ ] feature 分支合回 main（fast-forward 或单 squash commit，由用户决定）
```

---

## 6. Branch / rollback / completion 操作规则

### 6.1 分支策略

- 整个重构在 feature 分支 `refactor/core-package-split` 上完成。
- 每批一个独立 commit；不要在分支上做无关改动（避免合并冲突放大）。
- 重构期间，**任何其它工作（feature / bugfix）继续提交到 `main`**，不要进入此 feature 分支。
- 重构期间用户应避免在 main 上修改 `pro.sketchware.core/**`（否则后续 merge / rebase 工作量极大）。

### 6.2 回滚策略

- 单批回归（编译失败、装机闪退）：`git revert <bad-batch-commit>`，然后让执行模型修复后重新提交。
- 多批连环回归（罕见）：`git reset --hard <last-good-commit>` + 强推此 feature 分支（不会影响 main）。
- **不要**对已合回 main 的批次做 force-push 回滚。如果发现回归，应在 main 上前向修复或 revert。

### 6.3 IDE 与并发工作注意事项

- 执行期间不要在 Android Studio / IntelliJ 中保存 `core/` 内的文件（IDE 的 "optimize imports" 可能加回旧 import）。
- 执行期间不要让多个 AI 会话同时并行工作（容易在同一批内争抢 grep 命中）。
- 执行期间用户尽量不在 Sketchware-Pro 应用本身上做编辑（无关——这是 IDE 工作流，但避免误触 commit 状态）。

### 6.4 "完全完成" 退出条件

以下条件全部成立时，方可宣布重构完毕：

- `pro.sketchware.core/` 目录下**没有任何直接的 .java 文件**（只剩子包目录）
- `git grep -E "^import pro\.sketchware\.core\.[A-Z][A-Za-z0-9_]+;\$" -- "app/src/main/java"` 命中数为 **0**（所有外部 import 都已下沉到子包）
- `:app:assembleDebug` 与 `:app:assembleRelease` 双双通过
- 在真机上：装 APK → 打开应用 → 进入一个示例工程 → 编辑 block → 保存 → Run → 生成的子工程能装机运行
- 写入 `RELEASE_NOTES.md` 的 `refactor` 段落
- feature 分支合并回 main 并打 git tag（如下一个 beta 版本）

---

## 7. 模型成本预估（粗略）

| 阶段 | 模型 | 会话数 | 预估 token |
|---|---|---|---|
| Phase 1 规划 | Opus 4.7 High | 1 | ~300K（读全部 124 文件 + 输出方案）|
| Phase 2 执行 | Sonnet 4.5 Medium | 5–8（每批一个） | ~80K × N |
| Phase 3 审视 | Sonnet 4.5 Medium | 2–3 | ~50K × N |

> 用 Phase 1 的全景理解换 Phase 2/3 的"窄上下文 + 廉价模型"，是这个流程的核心 ROI 杠杆。
