# Sketchware-Pro 作为 Android 端 App 开发工具的复核审查报告

## 审查目标

本报告用于复核 Sketchware-Pro 作为 Android 端 App 开发工具时存在的架构、兼容性、安全性、数据可靠性和维护性风险。

本次审查特别强调：

- **避免误判**：只将当前代码能够支撑的内容列为已确认问题。
- **避免不可落地建议**：区分短期可修、中期优化和长期重构。
- **区分历史问题与当前问题**：对已经修复或当前代码不再支持的判断进行修正。
- **保持审查性质**：本报告只做问题识别和改进建议，不包含代码修改。

## 复核范围

重点复核了以下模块和实现点：

- `ProjectDataManager` 的项目数据管理生命周期。
- `DesignActivity` 的保存、退出、恢复未保存数据流程。
- `ResourceManager` 的资源临时备份与恢复流程。
- `EncryptedFileUtil` 的文件写入原子性。
- `ManifestGenerator` 的权限、Manifest 属性和组件声明生成。
- `PermissionManager` 的运行时权限代码生成。
- `GradleFileGenerator` 与 `BuiltInLibraries` 的依赖版本一致性。
- `ProjectBuilder` 的构建工具 asset 释放与更新判断。

## 前次判断中需要修正的内容

### 1. 不能再判断资源临时目录仍是全局目录

当前 `ResourceManager` 已经使用带 `projectId` 的项目级临时目录：

- `SketchwarePaths.getTempImagesPath(projectId)`
- `SketchwarePaths.getTempSoundsPath(projectId)`
- `SketchwarePaths.getTempFontsPath(projectId)`

资源恢复前也会检查临时目录是否存在，缺失时直接跳过，不再先删除真实资源目录。

因此，当前更准确的判断是：

- **历史上存在过资源 temp 跨项目污染风险。**
- **当前代码已有项目级隔离和缺失 temp 保护。**
- **除非测试复现，否则不能继续将其列为当前已确认 bug。**

### 2. 不能再判断项目数据单文件写入没有原子性

`EncryptedFileUtil.writeBytes()` 和 `writeText()` 当前已经使用：

- `.tmp` 临时文件写入。
- `FileDescriptor.sync()` 刷盘。
- `renameTo()` 提交。
- 失败时删除临时文件。

因此，单个数据文件已经具备基本原子写保护。

但仍然存在更准确的问题：

- **单文件保存具备原子性。**
- **项目级多文件保存不具备事务性。**

也就是说，如果 `file`、`logic`、`view`、`resource`、`library` 中部分保存成功、部分失败，仍可能形成项目状态不一致。

### 3. 不能再判断 Firebase 内置库仍是 v19 时代

当前 `BuiltInLibraries` 中 Firebase 相关库已经是较新的版本，例如：

- `firebase-auth-23.1.0`
- `firebase-database-21.0.0`
- `firebase-messaging-24.1.0`
- `firebase-storage-21.0.1`

`ManifestGenerator` 中也已经移除了旧的 c2dm permission 写入逻辑。

因此，不能继续使用“Firebase 仍停留在 v19 / firebase-iid 已移除但仍被依赖”作为当前事实。

当前更准确的问题是：

- **手机端内置构建依赖版本、导出 Gradle 依赖版本、主 App 自身依赖版本不完全一致。**
- 这是维护性和行为一致性风险，而不是已经确认的 Firebase v19 兼容性 bug。

### 4. 不能直接判断 `ProjectDataManager.clearAll()` 必然导致正常退出丢数据

`DesignActivity.finish()` 会调用：

```java
ProjectDataManager.clearAll();
```

`clearAll()` 本身确实是不保存直接释放 manager 引用。但从主要退出路径看：

- 保存退出会先执行 `SaveChangesProjectCloser`。
- 放弃退出会先执行 `DiscardChangesProjectCloser`。
- Activity 状态保存时会通过 `UnsavedChangesSaver` 写 backup。

因此，不能简单判断正常退出一定丢数据。

更准确的判断是：

- **`clearAll()` 是危险 API，需要审计调用路径。**
- **主要正常退出路径已有保存或放弃处理。**
- **仍需验证异常路径是否存在未保存状态被直接清空。**

## 已确认问题

### 1. 生成项目权限模型落后于 Android 12/13+

`PermissionManager` 当前主要生成以下旧权限模型：

- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `CAMERA`
- `RECORD_AUDIO`
- `ACCESS_FINE_LOCATION`

`ProjectFilePaths` 中蓝牙组件仍主要添加：

- `BLUETOOTH`
- `BLUETOOTH_ADMIN`

但 Android 12+ 蓝牙权限模型需要区分：

- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `BLUETOOTH_ADVERTISE`

Android 13+ 媒体权限模型也需要区分：

- `READ_MEDIA_IMAGES`
- `READ_MEDIA_VIDEO`
- `READ_MEDIA_AUDIO`

当前生成层没有看到统一、完整的 targetSdk 条件化权限策略。

风险：

- 生成项目在 Android 12+ 使用蓝牙组件时可能权限不足。
- 生成项目在 Android 13+ 访问媒体文件时可能功能异常。
- 用户难以理解旧权限已经被系统弱化或忽略。

需要注意：通知权限不能归入“完全未处理”。当前代码已经有：

- Manifest 中写入 `POST_NOTIFICATIONS`。
- Notification 组件初始化时请求 `POST_NOTIFICATIONS`。

### 2. `requestLegacyExternalStorage` 生成条件过宽

`ManifestGenerator` 当前逻辑是：

```java
boolean addRequestLegacyExternalStorage = targetSdkVersion >= 28;
```

随后在 `application` 标签中写入：

```java
android:requestLegacyExternalStorage="true"
```

问题：

- `requestLegacyExternalStorage` 主要用于 Android 10 / targetSdk 29 的 scoped storage 过渡期。
- 对 targetSdk 30+ 基本不再提供旧外部存储兼容效果。
- 当前 `targetSdkVersion >= 28` 的条件会在更高 targetSdk 下继续写入该属性。

风险：

- 不一定导致构建失败。
- 但会给用户造成“外部存储兼容已处理”的误导。
- 对 Android 11+ 真实文件访问问题没有实质帮助。

### 3. 生成 Manifest 默认安全策略偏宽松

`ManifestGenerator` 默认写入：

```java
android:allowBackup="true"
android:usesCleartextTraffic="true"
```

风险：

- `allowBackup=true` 可能导致生成 App 的私有数据进入系统备份范围。
- `usesCleartextTraffic=true` 默认允许 HTTP 明文流量，不符合现代 Android 安全默认值。

需要谨慎处理：

- 对学习型工具来说，默认允许明文流量可能降低入门门槛。
- 对正式 App 开发工具来说，安全基线偏低。
- 不建议直接破坏旧项目行为，应通过项目设置和新项目模板逐步迁移。

### 4. 内置构建依赖与导出 Gradle 依赖版本不统一

当前 `BuiltInLibraries` 中部分版本为：

- `material-1.13.0`
- `glide-5.0.4`
- `okhttp-android-5.1.0`
- `firebase-messaging-24.1.0`

但 `GradleFileGenerator` 导出依赖中仍存在：

- `firebase-bom:33.7.0`
- `material:1.12.0`
- `glide:4.16.0`
- `okhttp:4.12.0`
- `play-services-auth:19.0.0`

风险：

- 手机端构建成功，导出到 Android Studio 后行为不同。
- 组件生成代码在两种构建模式下依赖 API 不一致。
- 依赖升级需要在多个地方同步，维护成本高。
- 用户遇到问题时难以判断是手机端构建问题还是导出工程问题。

### 5. 构建工具 asset 更新判断只比较文件大小

`ProjectBuilder.hasFileChanged()` 当前通过 asset 文件大小和目标文件大小比较判断是否重新释放：

```java
long lengthOfFileInAssets = fileUtil.getAssetFileSize(...);
long length = compareToFile.exists() ? compareToFile.length() : 0;
if (lengthOfFileInAssets == length) {
    return false;
}
```

该方法用于：

- AAPT2。
- `libs.zip`。
- `dexs.zip`。
- `android.jar.zip`。
- `testkey`。
- `core-lambda-stubs.jar`。

风险：

- 如果 asset 内容变更但文件大小相同，旧文件不会重新释放。
- 对构建工具链来说，可能导致难以定位的旧工具残留问题。
- 该问题发生概率不一定高，但一旦发生排查成本较大。

### 6. 项目级保存缺少统一事务

虽然单文件写入已经具备原子性，但项目保存涉及多个 manager：

- `ProjectFileManager`
- `ProjectDataStore`
- `ResourceManager`
- `LibraryManager`

风险：

- 某些数据文件保存成功，另一些保存失败。
- 成功保存的 manager 可能删除 backup，失败的 manager 保留 backup。
- 下次打开项目时可能面对部分新数据、部分旧数据、部分 backup 的组合。
- 对开发工具来说，这会影响项目可靠性和用户信任。

准确表述：

- **不是单文件写坏问题。**
- **是跨文件项目级一致性问题。**

## 需验证风险

### 1. `finish()` 调用 `clearAll()` 的异常路径风险

需要重点验证以下场景：

- 编辑过程中撤销存储权限，`onResume()` 直接 `finish()`。
- `sc_id` 为空或异常时直接 `finish()`。
- `onSaveInstanceState()` 触发异步 backup 后进程快速死亡。
- 构建过程中退出设计器。
- 子编辑器未 flush 当前内存态时 Activity 结束。

建议验证方式：

- 临时记录 `clearAll()` 调用栈。
- 记录调用时各 manager 是否存在 backup。
- 构造异常退出和权限撤销场景。
- 验证下次打开是否能恢复最新编辑状态。

结论：

- **这是需要测试确认的边界风险。**
- **不能直接定性为当前已确认数据丢失 bug。**

### 2. 备份恢复是否缺少资源消耗限制

当前备份解压已有 Zip Slip 防护，不能再判断其完全没有安全保护。

但仍需验证是否限制：

- 单个 entry 最大大小。
- 总解压大小。
- entry 数量。
- 重复 entry。
- 畸形 JSON。
- 恢复中断后的部分写入状态。

风险：

- 超大备份可能造成 OOM 或长时间卡顿。
- 恶意构造备份可能造成恢复流程异常。
- 如果恢复不是先到临时目录再提交，可能留下部分恢复状态。

### 3. 本地库冲突治理仍需更多真实项目验证

当前本地库下载路径、FUSE fallback、Manifest merge 等已有修复和增强，不应笼统判断为不可用。

但仍需验证：

- duplicate class。
- resource name conflict。
- manifest provider authority conflict。
- minSdk / targetSdk 要求冲突。
- native ABI 兼容。
- transitive dependency tree 解析。
- 手机端构建和导出 Gradle 构建的一致性。

结论：

- **本地库系统是高复杂度区域。**
- **当前不能直接定性为 bug。**
- **需要测试矩阵验证真实兼容性。**

## 不应作为当前问题继续报告的内容

以下判断在当前代码下不应继续作为已确认问题：

- **Firebase 仍停留在 v19 时代。**
- **资源 temp 仍然是全局目录，会跨项目污染。**
- **项目数据完全没有原子保存。**
- **`clearAll()` 在正常退出路径中必然导致数据丢失。**
- **通知权限完全没有处理。**

这些内容要么已经被当前代码修复，要么需要测试验证，不能作为当前事实直接报告。

## 可现实落地的整改建议

### 短期建议

#### 1. 将构建工具 asset 校验从文件大小改为 hash

目标：

- 避免内容变更但大小相同导致旧工具不更新。

建议：

- 对 AAPT2、`libs.zip`、`dexs.zip`、`android.jar.zip` 计算 SHA-256。
- 保存上次释放的 hash。
- hash 不一致时重新释放。

可行性：

- 改动集中。
- 风险较低。
- 对用户项目数据无侵入。

#### 2. 修正 `requestLegacyExternalStorage` 写入条件

建议：

- 仅在确实有效的 targetSdk 范围内写入。
- 对 targetSdk 30+ 不再写入该属性。
- 在 UI 或编译提示中解释 Android 11+ 存储限制。

可行性：

- 改动小。
- 行为变化可控。
- 需要注意旧项目兼容提示。

#### 3. 补齐 Android 12+ 蓝牙权限生成

建议：

- targetSdk 31+ 时为连接类功能添加 `BLUETOOTH_CONNECT`。
- 如果涉及扫描，再添加 `BLUETOOTH_SCAN`。
- 同步 runtime permission 生成逻辑。

可行性：

- 可先只覆盖 `BluetoothConnect` 组件。
- 不需要一次性重写整个权限系统。

#### 4. 补齐 Android 13+ 媒体权限生成

建议：

- 图片、音频、视频访问按场景生成 `READ_MEDIA_*`。
- 文件选择优先考虑 SAF，不要一律申请外部存储权限。
- 保留低 targetSdk / 旧 Android 的兼容分支。

可行性：

- 需要梳理 FilePicker、资源导入、生成项目文件访问场景。
- 建议先从生成项目组件侧做，不要混入 Sketchware 主 App 自身存储权限。

#### 5. 统一导出 Gradle 依赖版本表

建议：

- 让 `GradleFileGenerator` 复用 `BuiltInLibraries` 的版本信息。
- 避免依赖版本散落硬编码。
- 对导出 Android Studio 工程和手机端构建建立版本对照测试。

可行性：

- 中小规模改动。
- 需要测试导出项目编译结果。

### 中期建议

#### 1. 增加项目保存 checkpoint

建议：

- 每次保存前创建保存事务 ID。
- 所有 manager 保存成功后写入 `save_manifest`。
- 下次打开项目时检查 manifest 是否完整。
- 不完整时提示恢复 backup 或回滚。

可行性：

- 比完整数据库化或 Git 化简单。
- 能显著提高项目一致性保障。

#### 2. 增强构建错误分类

建议：

- 区分 AAPT2 错误、Java 编译错误、duplicate class、缺失依赖、Manifest 冲突。
- 给出面向非专业用户的解释。
- 对常见错误给出修复建议。

可行性：

- 不需要重写构建链。
- 用户体验收益高。

#### 3. 建立权限策略层

建议：

- 新建集中式权限策略模块。
- 按 targetSdk、组件类型、API level 输出 Manifest 权限和 runtime 权限代码。
- 先覆盖 Storage/FilePicker、Bluetooth、Notification、Location。

可行性：

- 可渐进迁移。
- 不建议一次性替换所有权限生成逻辑。

#### 4. 增强备份恢复输入限制

建议：

- 限制总解压大小。
- 限制 entry 数量。
- 限制单文件大小。
- 恢复先写入临时目录，校验成功后再提交。

可行性：

- 对安全性和稳定性收益明显。
- 需要保持旧备份格式兼容。

### 长期重构建议

#### 1. 项目级事务保存系统

目标：

- 解决多个 manager 之间的保存一致性问题。

难点：

- 涉及 `ProjectDataStore`、`ProjectFileManager`、`ResourceManager`、`LibraryManager` 生命周期。
- 需要设计 commit / rollback / recovery 协议。

不建议短期承诺一次性完成。

#### 2. 多项目并发 session 架构

当前 `ProjectDataManager` 是单项目全局缓存模型。

如果未来要支持更复杂的多窗口、多项目并发、后台构建，建议演进为：

- `ProjectSession`
- `ProjectRepository`
- 明确生命周期 owner
- 明确保存和释放边界

这是大范围架构调整，不应作为短期修复。

#### 3. 手机端构建和 Android Studio 导出的一致性治理

目标：

- 手机端构建依赖和导出 Gradle 依赖尽量一致。
- 组件生成代码在两种构建模式下行为一致。

难点：

- 手机端构建使用预置 jar/dex。
- Android Studio 使用 Maven 依赖。
- 两者天然存在差异。

建议逐步缩小差异，而不是承诺完全等价。

## 建议验证清单

### 数据可靠性验证

- 编辑项目后直接按 Home，等待系统回收，再重新打开。
- 编辑项目后撤销存储权限，再回到设计器。
- 保存过程中模拟部分 manager 保存失败。
- 保存过程中杀进程。
- 放弃修改后确认资源、逻辑、文件、库是否全部回到旧状态。

### 权限兼容性验证

- targetSdk 28、29、30、31、33、35 分别生成项目。
- 测试蓝牙连接组件在 Android 12+ 的运行情况。
- 测试文件/媒体访问在 Android 13+ 的运行情况。
- 测试通知组件在 Android 13+ 是否能正常弹通知。

### 构建链验证

- 修改同大小的 `libs.zip` 或测试 asset，验证是否会重新释放。
- 测试 AAPT2、ECJ、D8/R8、签名环节错误是否能准确归类。
- 对比手机端构建 APK 与导出 Android Studio 工程构建 APK 的依赖差异。

### 备份恢复验证

- 正常备份恢复。
- 大文件备份恢复。
- 大量 entry 备份恢复。
- 畸形 JSON 恢复。
- 重复路径 entry 恢复。
- 恢复中断后再次打开项目。

### 本地库验证

- duplicate class 场景。
- Manifest provider authority 冲突场景。
- resource name 冲突场景。
- transitive dependency 场景。
- native so / ABI 场景。
- 手机端构建和导出 Gradle 构建对照。

## 总体结论

Sketchware-Pro 当前已经修复或改善了一些历史高风险点，例如：

- 资源临时目录已项目隔离。
- 缺失资源 temp 时恢复会跳过，不再直接删除真实资源目录。
- 单文件写入已经具备 tmp + rename 的原子写保护。
- Firebase 内置库不再是早期 v19 时代。
- 通知权限已有 Manifest 和运行时请求处理。

但作为 Android 端 App 开发工具，仍然存在几类真实风险：

- **权限模型需要跟进 Android 12/13+。**
- **存储兼容提示和 Manifest 生成逻辑需要更精确。**
- **默认安全策略偏宽松。**
- **内置构建依赖与导出 Gradle 依赖版本不统一。**
- **构建工具 asset 更新判断过弱。**
- **项目保存缺少跨文件事务一致性。**

短期最现实的改进方向是：

1. 修复构建 asset size-only 校验。
2. 修正 `requestLegacyExternalStorage` 生成条件。
3. 补齐 Android 12+ 蓝牙权限。
4. 补齐 Android 13+ 媒体权限。
5. 统一导出 Gradle 依赖版本来源。

中长期应逐步推进：

1. 项目保存 checkpoint。
2. 权限策略层。
3. 备份恢复限制。
4. 构建错误分类。
5. 项目级事务保存架构。

本报告的核心原则是：**只把当前代码能支撑的内容列为已确认问题，对需要测试验证的内容保留风险判断，避免将历史问题或推测问题误报为当前缺陷。**
