# Firebase 内置库升级实施计划

## 概述

Sketchware-Pro 内置的 Firebase 库版本为 v19.x（2019-2020 年），已严重过时。
本计划分 4 个阶段逐步升级到与 BOM 33.7.0 兼容的版本。

---

## 一、版本映射

### 需要升级的库

| 库名 | 当前版本 | 目标版本 (BOM 33.7.0) | 变化 |
|------|---------|----------------------|------|
| firebase-common | 19.3.0 | 21.0.0 | 主版本升级 |
| firebase-components | 16.0.0 | ~18.x | 主版本升级 |
| firebase-auth | 19.0.0 | 23.1.0 | 主版本升级×4 |
| firebase-auth-interop | 18.0.0 | ~20.x | 需验证 |
| firebase-database | 19.3.1 | 21.0.0 | 主版本升级 |
| firebase-database-collection | 17.0.1 | ~18.x | 需验证 |
| firebase-storage | 19.0.0 | 21.0.1 | 主版本升级 |
| firebase-messaging | 19.0.0 | 24.1.0 | 主版本升级×5 |

### 需要移除的库（已被 Google 删除）

| 库名 | 当前版本 | 替代方案 |
|------|---------|---------|
| firebase-iid | 19.0.0 | firebase-installations 18.0.0 + FirebaseMessaging.getToken() |
| firebase-iid-interop | 17.0.0 | 无需替代（内部依赖已重构） |
| firebase-measurement-connector | 18.0.0 | 无需替代（功能合并到 firebase-analytics） |

### 需要新增的库

| 库名 | 版本 | 原因 |
|------|------|------|
| firebase-installations | 18.0.0 | 替代 firebase-iid |
| firebase-annotations | ~16.x | 新版 firebase-common 的传递依赖 |
| firebase-datatransport | ~18.x | 新版传递依赖（需验证） |

---

## 二、实施阶段

### 阶段 1：修复版本不匹配（低风险，立即可做）

**目标**：消除生成的 build.gradle 与实际编译库的版本矛盾。

**文件修改：**

1. `ComponentCodeGenerator.java:98`
   ```java
   // 当前（不匹配）：
   content.append("implementation platform('com.google.firebase:firebase-bom:34.1.0')\r\n");
   // 临时修复 — 移除 BOM 声明，改为显式版本号：
   // 或者注释说明实际编译使用的是内置库版本
   ```

**验证**：构建 assembleDebug 通过。

---

### 阶段 2：废弃 API 迁移（中风险，必须在升级前完成）

**目标**：将代码生成中引用的废弃 API 替换为新 API，使生成的用户代码兼容新版 Firebase。

#### 2.1 移除 `firebase-iid` 引用

**文件 1：`ComponentTypeMapper.java:595-601`**
```java
// 当前：
case "FirebaseCloudMessage":
    importList.add("com.google.android.gms.tasks.OnCompleteListener");
    importList.add("com.google.android.gms.tasks.Task");
    importList.add("com.google.firebase.iid.FirebaseInstanceId");      // ❌ 已移除
    importList.add("com.google.firebase.iid.InstanceIdResult");         // ❌ 已移除
    importList.add("com.google.firebase.messaging.FirebaseMessaging");
    return importList;

// 修改为：
case "FirebaseCloudMessage":
    importList.add("com.google.android.gms.tasks.OnCompleteListener");
    importList.add("com.google.android.gms.tasks.Task");
    importList.add("com.google.firebase.messaging.FirebaseMessaging");  // ✅ 保留
    return importList;
```

**文件 2：`ManifestGenerator.java:124-128`**
```java
// 当前：
if (buildConfig.constVarComponent.isFCMUsed) {
    XmlBuilder metadataTag = new XmlBuilder("meta-data");
    metadataTag.addAttribute("android", "name", "com.google.firebase.components:com.google.firebase.iid.Registrar"); // ❌
    metadataTag.addAttribute("android", "value", "com.google.firebase.components.ComponentRegistrar");
    serviceTag.addChildNode(metadataTag);
}

// 修改为：移除整个 if 块（firebase-messaging 新版自动注册）
```

**文件 3：`ManifestGenerator.java:458-461`**
```java
// 当前：
if (buildConfig.constVarComponent.isFCMUsed) {
    writePermission(manifestXml, Manifest.permission.WAKE_LOCK);
    writePermission(manifestXml, "com.google.android.c2dm.permission.RECEIVE"); // ❌ GCM 遗留
}

// 修改为：移除 c2dm 权限行（新版不需要），保留 WAKE_LOCK
```

**文件 4：`EditorManifest.java:23-33`**
```java
// 当前：写入 FirebaseInstanceIdReceiver（已废弃）
// 修改为：移除 firebaseInstanceIdReceiverTag 相关代码（新版自动注册）
```

#### 2.2 更新 FCM 代码生成

需要检查并更新所有生成 `FirebaseInstanceId.getInstance().getInstanceId()` 的代码块：

```java
// 旧 API（v19.x）：
FirebaseInstanceId.getInstance().getInstanceId()
    .addOnCompleteListener(task -> {
        String token = task.getResult().getToken();
    });

// 新 API（v21+）：
FirebaseMessaging.getInstance().getToken()
    .addOnCompleteListener(task -> {
        String token = task.getResult();
    });
```

**注意**：返回类型从 `Task<InstanceIdResult>` 变为 `Task<String>`，需要同步修改
`ComponentCodeGenerator.java` 中 `FirebaseCloudMessage` 的 `onCompleteListener` 代码生成。

**验证**：
1. 构建 assembleDebug 通过
2. 创建一个包含 FCM 组件的测试项目，验证生成的代码编译通过

---

### 阶段 3：资产文件替换（高风险，核心步骤）

**目标**：替换 `libs.zip` 和 `dexs.zip` 中的 Firebase 库文件。

#### 3.1 准备新版 JAR/AAR 文件

从 Maven Central / Google Maven 下载以下 AAR 文件并提取 `classes.jar`：

```
# 升级的库
firebase-common-21.0.0.aar         → classes.jar
firebase-components-18.x.aar       → classes.jar  （需确认精确版本）
firebase-auth-23.1.0.aar           → classes.jar
firebase-auth-interop-20.x.aar     → classes.jar  （需确认精确版本）
firebase-database-21.0.0.aar       → classes.jar
firebase-database-collection-18.x.aar → classes.jar
firebase-storage-21.0.1.aar        → classes.jar
firebase-messaging-24.1.0.aar      → classes.jar

# 新增的库
firebase-installations-18.0.0.aar  → classes.jar
firebase-annotations-16.x.jar     （需确认精确版本）

# 检查其他新增的传递依赖
# 新版 Firebase 可能需要：
#   - firebase-encoders
#   - firebase-datatransport
#   - protobuf-javalite (如果新版依赖)
#   - play-services-tasks 升级
```

#### 3.2 生成 DEX 文件

对每个新/更新的 `classes.jar`，使用 D8 编译为 `.dex`：

```bash
# 示例命令
java -jar d8.jar --output <library-name>.dex classes.jar
```

#### 3.3 替换 ZIP 文件

1. 解压 `app/src/main/assets/libs/libs.zip`
2. 替换/新增对应目录下的 `classes.jar`、`res/`、`AndroidManifest.xml`
3. 重新打包为 `libs.zip`
4. 解压 `app/src/main/assets/libs/dexs.zip`
5. 替换/新增对应的 `.dex` 文件
6. 重新打包为 `dexs.zip`

#### 3.4 构建脚本（推荐创建）

建议编写一个 Gradle task 或 shell 脚本自动化此过程：

```
scripts/
  update-builtin-libs.sh    # 从 Maven 下载 → 提取 JAR → D8 编译 → 打包 ZIP
```

**验证**：
1. 构建 assembleDebug 通过
2. 安装到设备，创建使用 Firebase Auth/Database/Storage 的项目
3. 编译用户项目，验证无 ClassNotFoundException 或 NoSuchMethodError

---

### 阶段 4：更新代码中的库声明（中风险）

**目标**：更新 `BuiltInLibraries.java` 中的版本常量和依赖树。

#### 4.1 更新版本常量

**文件：`BuiltInLibraries.java:93-103`**
```java
// 修改前：
public static String FIREBASE_AUTH = "firebase-auth-19.0.0";
public static String FIREBASE_AUTH_INTEROP = "firebase-auth-interop-18.0.0";
public static String FIREBASE_COMMON = "firebase-common-19.3.0";
public static String FIREBASE_COMPONENTS = "firebase-components-16.0.0";
public static String FIREBASE_DATABASE = "firebase-database-19.3.1";
public static String FIREBASE_DATABASE_COLLECTION = "firebase-database-collection-17.0.1";
public static String FIREBASE_IID = "firebase-iid-19.0.0";                         // 移除
public static String FIREBASE_IID_INTEROP = "firebase-iid-interop-17.0.0";         // 移除
public static String FIREBASE_MEASUREMENT_CONNECTOR = "firebase-measurement-connector-18.0.0"; // 移除
public static String FIREBASE_MESSAGING = "firebase-messaging-19.0.0";
public static String FIREBASE_STORAGE = "firebase-storage-19.0.0";

// 修改后：
public static String FIREBASE_AUTH = "firebase-auth-23.1.0";
public static String FIREBASE_AUTH_INTEROP = "firebase-auth-interop-20.x.0";     // 需确认
public static String FIREBASE_COMMON = "firebase-common-21.0.0";
public static String FIREBASE_COMPONENTS = "firebase-components-18.x.0";          // 需确认
public static String FIREBASE_DATABASE = "firebase-database-21.0.0";
public static String FIREBASE_DATABASE_COLLECTION = "firebase-database-collection-18.x.0"; // 需确认
public static String FIREBASE_INSTALLATIONS = "firebase-installations-18.0.0";    // 新增
public static String FIREBASE_MESSAGING = "firebase-messaging-24.1.0";
public static String FIREBASE_STORAGE = "firebase-storage-21.0.1";
```

#### 4.2 更新依赖树

`KNOWN_BUILT_IN_LIBRARIES` 数组中的依赖关系需要根据新版 POM 文件更新：

```java
// 移除：
new BuiltInLibrary(FIREBASE_IID, ...),
new BuiltInLibrary(FIREBASE_IID_INTEROP, ...),
new BuiltInLibrary(FIREBASE_MEASUREMENT_CONNECTOR, ...),

// 新增：
new BuiltInLibrary(FIREBASE_INSTALLATIONS, List.of(...)),

// 更新所有 Firebase 库的依赖列表
// 需要从 Maven POM 文件中提取精确的传递依赖
```

#### 4.3 更新 ProjectBuilder.java

```java
// ProjectBuilder.java:768-779
// 如果需要新增 firebase-installations 作为 FCM 的依赖：
if (projectFilePaths.buildConfig.constVarComponent.isFCMUsed) {
    builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_INSTALLATIONS);
}
```

#### 4.4 更新 ExtLibSelected.java

```java
// mod/agus/jcoderz/editor/library/ExtLibSelected.java
// 更新 FCM 依赖链
if (component.isFCMUsed) {
    kp.addLibrary(BuiltInLibraries.FIREBASE_MESSAGING);
    kp.addLibrary(BuiltInLibraries.FIREBASE_INSTALLATIONS); // 新增
}
```

#### 4.5 更新 ComponentCodeGenerator.java 的 build.gradle 生成

```java
// ComponentCodeGenerator.java:98
// 更新 BOM 版本号为与内置库匹配的版本
content.append("implementation platform('com.google.firebase:firebase-bom:33.7.0')\r\n");
```

**验证**：
1. 构建 assembleDebug 通过
2. 完整的端到端测试（创建项目 → 添加 Firebase 组件 → 编译 → 运行）

---

## 三、测试计划

### 单元测试
- [ ] 验证 `BuiltInLibraries` 依赖解析无循环依赖
- [ ] 验证所有 Firebase 库的 `classes.jar` 存在且可读

### 集成测试
- [ ] 创建包含 Firebase Auth 的项目 → 编译成功
- [ ] 创建包含 Firebase Database 的项目 → 编译成功
- [ ] 创建包含 Firebase Storage 的项目 → 编译成功
- [ ] 创建包含 FCM 的项目 → 编译成功
- [ ] 创建包含 Firebase Google Login 的项目 → 编译成功
- [ ] 创建同时使用所有 Firebase 功能的项目 → 编译成功

### 回归测试
- [ ] 不使用 Firebase 的项目不受影响
- [ ] 生成的 AndroidManifest.xml 正确
- [ ] 生成的 build.gradle 版本号正确
- [ ] 生成的 Java 代码无废弃 API 引用

---

## 四、风险与回退策略

### 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 新版 Firebase 引入不兼容的传递依赖 | 高 | 高 | 逐步升级，每步验证 |
| 用户已有项目使用旧 API 的代码块 | 中 | 中 | 保持生成代码向后兼容 |
| 新版库文件体积增大，导致 APK 膨胀 | 中 | 低 | 监控 APK 大小变化 |
| Play Services 版本冲突 | 中 | 高 | 同步升级 play-services-* |

### 回退策略

每个阶段完成后创建 git tag：
```
firebase-upgrade/phase-1-bom-fix
firebase-upgrade/phase-2-api-migration
firebase-upgrade/phase-3-assets
firebase-upgrade/phase-4-declarations
```

如果任何阶段失败，可以回退到上一个 tag。

---

## 五、时间估计

| 阶段 | 工作量 | 预计时间 |
|------|--------|---------|
| 阶段 1：修复 BOM 不匹配 | 低 | 10 分钟 |
| 阶段 2：废弃 API 迁移 | 中 | 1-2 小时 |
| 阶段 3：资产文件替换 | 高 | 3-5 小时（含依赖分析） |
| 阶段 4：代码声明更新 | 中 | 1-2 小时 |
| 测试验证 | 高 | 2-3 小时 |
| **总计** | | **7-12 小时** |

---

## 六、文件变更清单

| 文件 | 阶段 | 变更类型 |
|------|------|---------|
| `ComponentCodeGenerator.java` | 1, 4 | 修改 BOM 版本号 |
| `ComponentTypeMapper.java` | 2 | 移除废弃 import |
| `ManifestGenerator.java` | 2 | 移除 firebase-iid Registrar + c2dm 权限 |
| `EditorManifest.java` | 2 | 移除 FirebaseInstanceIdReceiver |
| `app/src/main/assets/libs/libs.zip` | 3 | 替换 Firebase JAR 文件 |
| `app/src/main/assets/libs/dexs.zip` | 3 | 替换 Firebase DEX 文件 |
| `BuiltInLibraries.java` | 4 | 更新版本常量 + 依赖树 |
| `ProjectBuilder.java` | 4 | 可能需要添加 firebase-installations |
| `ExtLibSelected.java` | 4 | 更新 FCM 依赖 |
