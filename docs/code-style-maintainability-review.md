# Sketchware-Pro 代码风格与可维护性审查记录

## 审查范围

- 以 `app/src/main/java` 为主
- 重点排除第三方/外部引入目录对主结论的干扰：`dx/`、`kellinwood/`、`multidex/`
- 重点关注：命名、重复代码、异常处理、路径管理、线程使用、日志一致性、废弃 API、数据结构建模

## 第一轮结论

### 1. 重复工具方法：dpToPx 三重定义

同一功能存在多个入口：

- `pro.sketchware.core.ViewUtil.dpToPx(Context, float)`
- `pro.sketchware.utility.SketchwareUtil.dpToPx(float)`
- `pro.sketchware.utility.SketchwareUtil.getDip(int)`（已废弃但仍存在调用）

问题：

- 返回类型不一致（`float` / `int`）
- 调用方式不一致（显式 `Context` / 全局 `Context`）
- 维护者难以判断权威实现

### 2. `.sketchware` 路径硬编码分散

虽然 `SketchwarePaths.java` 已经集中定义了大量路径，但仍有大量代码直接使用：

- `FileUtil.getExternalStorageDir() + "/.sketchware/..."`
- `FileUtil.getExternalStorageDir().concat("/.sketchware/...")`

这会导致：

- 路径变更时需要全局手工排查
- 业务类直接耦合存储布局
- Scoped Storage 迁移更困难

### 3. `HashMap<String, Object>` 滥用

项目中大量逻辑依赖弱类型 `HashMap<String, Object>` 传递结构化数据。

热点文件包括：

- `ComponentsHandler.java`
- `EventsHandler.java`
- `AndroidManifestInjection.java`
- `BlocksManagerDetailsActivity.java`
- `EventsManagerFragment.java`
- `ProjectListManager.java`

主要问题：

- 字段名由字符串硬编码维护
- 类型转换和 `instanceof` 检查重复出现
- 编译期无法发现字段变更
- 同一 key 语义在多个文件中容易漂移

### 4. 常量命名/拼写质量问题

已发现典型示例：

- `EventsHandler.CUSTOM_LISTENERE_FILE_PATH`

问题：

- 存在明显拼写错误
- 已被其他文件引用，说明错误会扩散

### 5. 注释掉的调试代码与历史残留代码

典型文件：

- `CommandBlock.java`
- `BackupFactory.java`
- `ProjectFile.java`

问题：

- 保留大量注释掉的 `writeLog(...)`
- 保留整块旧实现
- 保留贡献者讨论式注释/历史反编译代码
- 增加阅读噪声，降低权威实现可识别性

### 6. 宽泛异常捕获与吞异常

项目中存在大量：

- `catch (Exception e)`
- `catch (...) {}`
- `catch (...) { return fallback; }`

主要风险：

- 掩盖真实失败原因
- 让错误表现成“正常回退”
- 调试成本高

### 7. `new Thread()` 裸线程

多处代码直接 `new Thread(...).start()`，没有统一线程池或任务调度抽象。

典型文件：

- `DesignActivity.java`
- `LogicEditorActivity.java`
- `SketchwareUtil.java`
- `CustomBlocksDialog.java`

问题：

- 生命周期管理弱
- 线程命名和异常处理缺失
- 容易散落成不可控的后台逻辑

### 8. 废弃 API 使用

重点包括：

- `AsyncTask`
- `getExternalStorageDirectory()`
- `ConnectivityManager.getActiveNetworkInfo()` / `NetworkInfo`

### 9. 硬编码颜色值

存在较多 `Color.parseColor(...)` 直接写死颜色值的情况。

问题：

- 与主题系统耦合差
- 不利于深色模式和统一配色收敛

### 10. 安全敏感信息硬编码

典型位置：

- `SketchwarePaths.getEncryptionKey()`
- `SketchwarePaths.getPublicKey()`

问题：

- 安全意义不清晰
- 维护者不易区分“协议常量”与“可替换密钥”

### 11. `printStackTrace()` / `System.out` / `System.exit` 使用

虽然并不一定全部构成立即 bug，但整体风格不一致，且生成模板代码中仍存在 `printStackTrace()`。

### 12. TODO / FIXME 热点集中

热点区域集中在：

- `CommandBlock.java`
- `ManageCollectionActivity.java`
- `BlocksManagerCreatorActivity.java`
- `ResourceManager.java`
- Kotlin 编译链相关文件

这说明高风险核心区域仍有较多未完成治理事项。

### 13. 日志体系不统一

项目内同时存在：

- `android.util.Log`
- `LogUtil`
- 注释掉的 `writeLog(...)`
- 文件写日志方式

问题：

- 日志输出渠道不统一
- 级别控制和格式难以集中管理

### 14. 包结构碎片化

项目同时跨多个来源/贡献者包：

- `com.besome.sketch`
- `pro.sketchware`
- `mod.hilal.saif`
- `mod.hey.studios`
- `dev.aldi.sayuti`
- 其他 `mod.*`

问题：

- 功能边界按历史来源分裂，而不是按职责组织
- 同一功能经常横跨多个包实现
- 查找“权威实现”成本高

## 第二轮补充结论

### 1. `CommandBlock.java` 属于 P0 级维护性风险文件

该文件存在如下问题：

- 不可读缩写方法名：`WTF`、`CB`、`aCs`、`aC`、`rCCs`
- 单字母或弱语义变量：`a`、`b`、`n`、`v`、`kk`、`aa`、`hm`
- 大量手工字符串拼接循环代替统一工具方法
- 多处异常吞没后直接返回兜底结果
- 保留整块旧实现注释
- 直接写 `.sketchware/temp/log.txt`
- 路径硬编码

结论：

- 不适合继续小修小补
- 应作为专项可维护性治理对象优先处理

### 2. `ComponentsHandler.java` 存在大面积重复模式

`id()`、`typeName()`、`name()`、`icon()`、`description2()`、`docs()` 等方法共享相同的数据查找和容错逻辑。

问题：

- 复制粘贴式实现
- 变更时容易遗漏某一分支
- 错误提示逻辑重复

### 3. `EventsHandler.java` 存在协议散落问题

该类不只是一个业务处理器，还承担了：

- 文件路径定义
- JSON 读取
- 数据结构协议解释
- UI 报错
- 事件注册拼装

这导致单类职责过重。

### 4. `NetworkInfo` 仍在使用

重点位置：

- `SketchwareUtil.isConnected()`
- `DeviceUtil` 中相关网络判断
- `ComponentTemplates` 生成模板代码中对应网络判断

### 5. `ProjectFile.java` 保留无效历史注释

存在整块旧反编译/讨论代码注释，降低可读性。

## `FieldBlockView.java` 现状记录

当前文件 `app/src/main/java/pro/sketchware/core/FieldBlockView.java` 处于未提交状态。

已观察到的未提交修改方向：

- 为长文本字段增加预览截断逻辑
- 对 `inputOnly` / `inputCode` / `import` 这类输入做 UI 限宽
- `setArgValue()` 增加空值安全处理
- `createValueTextView()` 增加 `TextUtils.TruncateAt.END`

风格评价：

- 这批改动方向正确，命名和封装明显优于大量旧代码
- 但文件本身仍保留遗留问题，例如：
  - 多个 `public` 可变字段
  - `initSs()` 这类不可读方法名
  - 旧式条件判断风格

## 优先级建议

### P0

- `CommandBlock.java` 可维护性治理
- 高风险吞异常点修复

### P1

- 路径硬编码统一到 `SketchwarePaths`
- `EventsHandler` 路径和配置访问收口
- 日志调用统一到 `LogUtil`

### P2

- 将高热点 `HashMap<String, Object>` 区域替换为强类型模型
- 逐步收敛遗留 UI 类的字段可见性和方法命名

### P3

- 包结构长期治理
- 历史注释代码清理

## 本记录用途

- 作为后续代码清理与重构的依据
- 作为 P0 / P1 修复顺序的备忘
- 避免重复进行同一轮人工审查
