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
| firebase-components | 16.0.0 | 18.0.0 | 主版本升级 |
| firebase-auth | 19.0.0 | 23.1.0 | 主版本升级×4 |
| firebase-auth-interop | 18.0.0 | 20.0.0 | 主版本升级 |
| firebase-database | 19.3.1 | 21.0.0 | 主版本升级 |
| firebase-database-collection | 17.0.1 | 18.0.1 | 主版本升级 |
| firebase-storage | 19.0.0 | 21.0.1 | 主版本升级 |
| firebase-messaging | 19.0.0 | 24.1.0 | 主版本升级×5 |

### 需要移除的库（已被 Google 删除）

| 库名 | 当前版本 | 替代方案 |
|------|---------|---------|
| firebase-iid | 19.0.0 | firebase-installations 18.0.0 + FirebaseMessaging.getToken() |
| firebase-iid-interop | 17.0.0 | 保留 17.1.0（firebase-messaging 24.1.0 仍依赖） |
| firebase-measurement-connector | 18.0.0 | 升级到 19.0.0（firebase-messaging 24.1.0 仍依赖） |

### 需要新增的库

| 库名 | 版本 | 原因 |
|------|------|------|
| firebase-installations | 17.2.0 | 替代 firebase-iid（firebase-messaging 24.1.0 依赖） |
| firebase-installations-interop | 17.1.0 | firebase-installations 17.2.0 的编译依赖 |
| firebase-annotations | 16.2.0 | firebase-components 18.0.0 的编译依赖 |
| firebase-common-ktx | 21.0.0 | firebase-auth/database/storage/messaging 的编译依赖 |
| firebase-datatransport | 18.2.0 | firebase-messaging 24.1.0 的编译依赖 |
| firebase-encoders | 17.0.0 | firebase-messaging 24.1.0 的编译依赖 |
| firebase-encoders-json | 18.0.0 | firebase-messaging 24.1.0 的编译依赖 |
| firebase-encoders-proto | 16.0.0 | firebase-messaging 24.1.0 的编译依赖 |
| firebase-appcheck-interop | 17.1.0 | firebase-database 21.0.0 + firebase-storage 21.0.1 的编译依赖 |
| firebase-appcheck | 17.1.0 | firebase-storage 21.0.1 的编译依赖 |
| transport-api | 3.1.0 | firebase-datatransport 的传递依赖 |
| transport-runtime | 3.1.8 | firebase-datatransport 的传递依赖 |
| transport-backend-cct | 3.1.8 | firebase-datatransport 的传递依赖 |
| play-services-cloud-messaging | 17.2.0 | firebase-messaging 24.1.0 的运行时依赖 |
| kotlinx-coroutines-play-services | 1.6.4 | firebase-common 21.0.0 的编译依赖 |

### Play Services 版本升级（同步要求）

| 库名 | 当前版本 | 最低要求版本 | 原因 |
|------|---------|------------|------|
| play-services-basement | 18.0.0 | 18.3.0 | firebase-common 21.0.0 运行时依赖 |
| play-services-tasks | 18.0.1 | 18.1.0 | firebase-common 21.0.0 运行时依赖 |
| play-services-base | 18.0.0 | 18.1.0 | firebase-database/storage 运行时依赖 |

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

从 Google Maven (https://maven.google.com/) 下载以下 AAR/JAR 文件：

```
# ═══ 升级的库（8 个）═══
firebase-common-21.0.0.aar              → 提取 classes.jar + res/ + AndroidManifest.xml
firebase-components-18.0.0.aar          → 提取 classes.jar
firebase-auth-23.1.0.aar                → 提取 classes.jar + res/
firebase-auth-interop-20.0.0.aar        → 提取 classes.jar
firebase-database-21.0.0.aar            → 提取 classes.jar
firebase-database-collection-18.0.1.aar → 提取 classes.jar
firebase-storage-21.0.1.aar             → 提取 classes.jar
firebase-messaging-24.1.0.aar           → 提取 classes.jar + res/ + AndroidManifest.xml

# ═══ 升级的库（保留但更新版本）═══
firebase-iid-interop-17.1.0.aar         → 提取 classes.jar
firebase-measurement-connector-19.0.0.aar → 提取 classes.jar

# ═══ 新增的库（15 个）═══
firebase-installations-17.2.0.aar       → 提取 classes.jar
firebase-installations-interop-17.1.0.aar → 提取 classes.jar
firebase-annotations-16.2.0.jar         → 直接使用（纯 JAR）
firebase-common-ktx-21.0.0.aar          → 提取 classes.jar
firebase-datatransport-18.2.0.aar       → 提取 classes.jar
firebase-encoders-17.0.0.jar            → 直接使用（纯 JAR）
firebase-encoders-json-18.0.0.aar       → 提取 classes.jar
firebase-encoders-proto-16.0.0.aar      → 提取 classes.jar
firebase-appcheck-interop-17.1.0.aar    → 提取 classes.jar
firebase-appcheck-17.1.0.aar            → 提取 classes.jar
transport-api-3.1.0.aar                 → 提取 classes.jar
transport-runtime-3.1.8.aar             → 提取 classes.jar
transport-backend-cct-3.1.8.aar         → 提取 classes.jar
play-services-cloud-messaging-17.2.0.aar → 提取 classes.jar
kotlinx-coroutines-play-services-1.6.4.jar → 直接使用

# ═══ 同步升级的 Play Services（3 个）═══
play-services-basement-18.3.0.aar       → 替换原 18.0.0
play-services-tasks-18.1.0.aar          → 替换原 18.0.1
play-services-base-18.1.0.aar           → 替换原 18.0.0

# ═══ 移除的库（1 个）═══
firebase-iid-19.0.0                     → 删除（被 firebase-installations 替代）
```

#### 3.1.1 下载脚本

建议在 `scripts/download-firebase-libs.sh` 中自动化：

```bash
BASE_URL="https://maven.google.com/com/google/firebase"

# 示例：下载 firebase-common-21.0.0.aar
curl -O "$BASE_URL/firebase-common/21.0.0/firebase-common-21.0.0.aar"

# 提取 classes.jar
unzip -o firebase-common-21.0.0.aar classes.jar -d firebase-common-21.0.0/
# 提取 res/ 和 AndroidManifest.xml（如果存在）
unzip -o firebase-common-21.0.0.aar 'res/*' AndroidManifest.xml -d firebase-common-21.0.0/ 2>/dev/null || true
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
public static String FIREBASE_IID = "firebase-iid-19.0.0";
public static String FIREBASE_IID_INTEROP = "firebase-iid-interop-17.0.0";
public static String FIREBASE_MEASUREMENT_CONNECTOR = "firebase-measurement-connector-18.0.0";
public static String FIREBASE_MESSAGING = "firebase-messaging-19.0.0";
public static String FIREBASE_STORAGE = "firebase-storage-19.0.0";

// 修改后：
public static String FIREBASE_ANNOTATIONS = "firebase-annotations-16.2.0";           // 新增
public static String FIREBASE_APPCHECK = "firebase-appcheck-17.1.0";                 // 新增
public static String FIREBASE_APPCHECK_INTEROP = "firebase-appcheck-interop-17.1.0"; // 新增
public static String FIREBASE_AUTH = "firebase-auth-23.1.0";
public static String FIREBASE_AUTH_INTEROP = "firebase-auth-interop-20.0.0";
public static String FIREBASE_COMMON = "firebase-common-21.0.0";
public static String FIREBASE_COMMON_KTX = "firebase-common-ktx-21.0.0";             // 新增
public static String FIREBASE_COMPONENTS = "firebase-components-18.0.0";
public static String FIREBASE_DATABASE = "firebase-database-21.0.0";
public static String FIREBASE_DATABASE_COLLECTION = "firebase-database-collection-18.0.1";
public static String FIREBASE_DATATRANSPORT = "firebase-datatransport-18.2.0";       // 新增
public static String FIREBASE_ENCODERS = "firebase-encoders-17.0.0";                 // 新增
public static String FIREBASE_ENCODERS_JSON = "firebase-encoders-json-18.0.0";       // 新增
public static String FIREBASE_ENCODERS_PROTO = "firebase-encoders-proto-16.0.0";     // 新增
public static String FIREBASE_IID_INTEROP = "firebase-iid-interop-17.1.0";           // 保留，升级
public static String FIREBASE_INSTALLATIONS = "firebase-installations-17.2.0";       // 新增（替代 firebase-iid）
public static String FIREBASE_INSTALLATIONS_INTEROP = "firebase-installations-interop-17.1.0"; // 新增
public static String FIREBASE_MEASUREMENT_CONNECTOR = "firebase-measurement-connector-19.0.0"; // 升级
public static String FIREBASE_MESSAGING = "firebase-messaging-24.1.0";
public static String FIREBASE_STORAGE = "firebase-storage-21.0.1";
// 移除：FIREBASE_IID（已被 Google 完全移除）
```

#### 4.2 更新依赖树

`KNOWN_BUILT_IN_LIBRARIES` 数组中的依赖关系需要根据 Maven POM 精确更新：

```java
// ═══ 移除 ═══
new BuiltInLibrary(FIREBASE_IID, ...),        // 完全移除

// ═══ 新增 ═══
new BuiltInLibrary(FIREBASE_ANNOTATIONS, List.of()),

new BuiltInLibrary(FIREBASE_APPCHECK, List.of(
    FIREBASE_APPCHECK_INTEROP, FIREBASE_COMMON, FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS,
    FIREBASE_ANNOTATIONS, PLAY_SERVICES_BASE, PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS)),

new BuiltInLibrary(FIREBASE_APPCHECK_INTEROP, List.of()),

new BuiltInLibrary(FIREBASE_COMMON_KTX, List.of(FIREBASE_COMMON, JETBRAINS_KOTLIN_STDLIB)),

new BuiltInLibrary(FIREBASE_DATATRANSPORT, List.of(
    FIREBASE_COMMON, FIREBASE_COMPONENTS, TRANSPORT_API, TRANSPORT_RUNTIME, TRANSPORT_BACKEND_CCT)),

new BuiltInLibrary(FIREBASE_ENCODERS, List.of()),

new BuiltInLibrary(FIREBASE_ENCODERS_JSON, List.of(FIREBASE_ENCODERS)),

new BuiltInLibrary(FIREBASE_ENCODERS_PROTO, List.of(FIREBASE_ENCODERS)),

new BuiltInLibrary(FIREBASE_INSTALLATIONS, List.of(
    FIREBASE_ANNOTATIONS, FIREBASE_COMMON, FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS,
    FIREBASE_INSTALLATIONS_INTEROP, PLAY_SERVICES_TASKS, JETBRAINS_KOTLIN_STDLIB)),

new BuiltInLibrary(FIREBASE_INSTALLATIONS_INTEROP, List.of()),

new BuiltInLibrary(PLAY_SERVICES_CLOUD_MESSAGING, List.of(
    PLAY_SERVICES_BASE, PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS)),

new BuiltInLibrary(TRANSPORT_API, List.of()),
new BuiltInLibrary(TRANSPORT_RUNTIME, List.of(TRANSPORT_API)),
new BuiltInLibrary(TRANSPORT_BACKEND_CCT, List.of(TRANSPORT_API, TRANSPORT_RUNTIME)),

new BuiltInLibrary(KOTLINX_COROUTINES_PLAY_SERVICES, List.of(
    JETBRAINS_KOTLINX_COROUTINES_CORE_JVM, PLAY_SERVICES_TASKS, JETBRAINS_KOTLIN_STDLIB)),

// ═══ 更新现有依赖树 ═══

// firebase-components 18.0.0
new BuiltInLibrary(FIREBASE_COMPONENTS, List.of(
    FIREBASE_ANNOTATIONS, ANDROIDX_ANNOTATION_JVM, ERROR_PRONE_ANNOTATIONS)),

// firebase-common 21.0.0
new BuiltInLibrary(FIREBASE_COMMON, List.of(
    FIREBASE_COMPONENTS, FIREBASE_ANNOTATIONS, KOTLINX_COROUTINES_PLAY_SERVICES,
    PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS,
    ANDROIDX_CONCURRENT_FUTURES, JETBRAINS_KOTLIN_STDLIB)),

// firebase-auth-interop 20.0.0
new BuiltInLibrary(FIREBASE_AUTH_INTEROP, List.of(
    FIREBASE_ANNOTATIONS, FIREBASE_COMMON,
    PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS)),

// firebase-auth 23.1.0
new BuiltInLibrary(FIREBASE_AUTH, List.of(
    FIREBASE_ANNOTATIONS, FIREBASE_APPCHECK_INTEROP, FIREBASE_AUTH_INTEROP,
    FIREBASE_COMMON, FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS,
    ANDROIDX_BROWSER, ANDROIDX_COLLECTION_JVM, ANDROIDX_FRAGMENT,
    ANDROIDX_LOCALBROADCASTMANAGER,
    PLAY_SERVICES_AUTH_API_PHONE, PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS,
    JETBRAINS_KOTLIN_STDLIB)),
    // 注意：还依赖 androidx.credentials, com.google.android.play:integrity,
    //       com.google.android.recaptcha:recaptcha — 这些需评估是否纳入内置库

// firebase-database 21.0.0
new BuiltInLibrary(FIREBASE_DATABASE, List.of(
    FIREBASE_APPCHECK_INTEROP, FIREBASE_AUTH_INTEROP, FIREBASE_COMMON,
    FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS, FIREBASE_DATABASE_COLLECTION,
    PLAY_SERVICES_BASE, PLAY_SERVICES_BASEMENT, PLAY_SERVICES_TASKS,
    ANDROIDX_ANNOTATION_JVM, JETBRAINS_KOTLIN_STDLIB)),

// firebase-database-collection 18.0.1
new BuiltInLibrary(FIREBASE_DATABASE_COLLECTION, List.of(PLAY_SERVICES_BASE)),

// firebase-storage 21.0.1
new BuiltInLibrary(FIREBASE_STORAGE, List.of(
    FIREBASE_ANNOTATIONS, FIREBASE_APPCHECK, FIREBASE_APPCHECK_INTEROP,
    FIREBASE_AUTH_INTEROP, FIREBASE_COMMON, FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS,
    PLAY_SERVICES_BASE, PLAY_SERVICES_TASKS,
    ANDROIDX_ANNOTATION_JVM, JETBRAINS_KOTLIN_STDLIB)),

// firebase-messaging 24.1.0
new BuiltInLibrary(FIREBASE_MESSAGING, List.of(
    FIREBASE_COMMON, FIREBASE_COMMON_KTX, FIREBASE_COMPONENTS,
    FIREBASE_DATATRANSPORT, FIREBASE_ENCODERS, FIREBASE_ENCODERS_JSON,
    FIREBASE_ENCODERS_PROTO, FIREBASE_IID_INTEROP, FIREBASE_INSTALLATIONS,
    FIREBASE_INSTALLATIONS_INTEROP, FIREBASE_MEASUREMENT_CONNECTOR,
    PLAY_SERVICES_BASE, PLAY_SERVICES_BASEMENT, PLAY_SERVICES_CLOUD_MESSAGING,
    PLAY_SERVICES_STATS, PLAY_SERVICES_TASKS,
    ERROR_PRONE_ANNOTATIONS, JETBRAINS_KOTLIN_STDLIB,
    TRANSPORT_API, TRANSPORT_RUNTIME, TRANSPORT_BACKEND_CCT),
    "com.google.firebase.messaging"),

// firebase-iid-interop 17.1.0（保留，更新版本）
new BuiltInLibrary(FIREBASE_IID_INTEROP, List.of(
    PLAY_SERVICES_BASE, PLAY_SERVICES_BASEMENT)),

// firebase-measurement-connector 19.0.0（保留，更新版本）
new BuiltInLibrary(FIREBASE_MEASUREMENT_CONNECTOR, List.of(PLAY_SERVICES_BASEMENT)),
```

#### 4.2.1 firebase-auth 额外依赖风险分析

⚠️ 通过对多个 firebase-auth 版本的 Maven POM 对比研究：

| firebase-auth 版本 | play:integrity | recaptcha | safetynet | credentials | 编译依赖数 |
|-------------------|---------------|-----------|-----------|-------------|-----------|
| 21.0.1 | ❌ | ❌ | ✅ 17.0.0 | ❌ | 12 |
| 21.3.0 | ✅ 1.0.1 | ✅ 18.0.1 | ✅ 17.0.0 | ❌ | 13 |
| 22.3.1 | ✅ 1.2.0 | ✅ 18.4.0 | ❌ | ❌ | 16 |
| 23.1.0 (BOM 33.7.0) | ✅ 1.3.0 | ✅ 18.5.1 | ❌ | ✅ 1.2.0-rc01 | 18 |

**关键发现**：
- `play:integrity` 和 `recaptcha` 从 **21.3.0 起** 就已引入，**无法通过降级避免**
- `androidx.credentials` 仅在 **23.x** 中引入
- firebase-auth 21.0.1 使用 `play-services-safetynet`（已弃用）替代 integrity/recaptcha
- firebase-auth 21.0.1 依赖 firebase-common **20.0.0**，与目标 21.0.0 可能不兼容

**必须新增的 firebase-auth 额外依赖（所有 21.3.0+ 版本）：**

| 依赖 | 版本 (23.1.0) | 说明 |
|------|-------------|------|
| `com.google.android.play:integrity` | 1.3.0 | Play Integrity API，App Check 使用 |
| `com.google.android.recaptcha:recaptcha` | 18.5.1 | reCAPTCHA Enterprise |
| `play-services-auth-api-phone` | 17.4.0 | 电话号码认证（已存在但需验证版本） |
| `androidx.browser` | 1.4.0 | Custom Tabs 支持（已存在） |
| `androidx.credentials:credentials` | 1.2.0-rc01 | **仅 23.x**，Credential Manager |
| `androidx.credentials:credentials-play-services-auth` | 1.2.0-rc01 | **仅 23.x** |

**推荐方案**：使用 **firebase-auth 23.1.0**（与 BOM 33.7.0 一致）
- 理由：integrity/recaptcha 在 21.3.0+ 都需要，降级不能减少依赖
- 代价：需额外捆绑 ~6 个新依赖（integrity, recaptcha, credentials ×2, auth-api-phone, browser）
- 这些库的传递依赖还需进一步分析

#### 4.3 更新 ProjectBuilder.java

```java
// ProjectBuilder.java:768-779
// 无需修改 — firebase-installations 会通过 BuiltInLibrary 依赖树自动引入
// 但如需显式添加（安全起见）：
if (projectFilePaths.buildConfig.constVarComponent.isFCMUsed) {
    builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_INSTALLATIONS);
}
```

#### 4.4 更新 ExtLibSelected.java

```java
// mod/agus/jcoderz/editor/library/ExtLibSelected.java
// FCM 依赖链无需手动修改 — firebase-messaging 的依赖树已包含 firebase-installations
// 但如需显式添加（安全起见）：
if (component.isFCMUsed) {
    kp.addLibrary(BuiltInLibraries.FIREBASE_MESSAGING);
    // firebase-installations 已通过依赖树自动包含
}
```

#### 4.5 更新 ComponentCodeGenerator.java 的 build.gradle 生成

```java
// ComponentCodeGenerator.java:98
// 更新 BOM 版本号为与内置库匹配的版本
content.append("implementation platform('com.google.firebase:firebase-bom:33.7.0')\r\n");
```

#### 4.6 更新 play-services 版本常量

```java
// BuiltInLibraries.java
public static String PLAY_SERVICES_BASE = "play-services-base-18.1.0";           // 原 18.0.0
public static String PLAY_SERVICES_BASEMENT = "play-services-basement-18.3.0";   // 原 18.0.0
public static String PLAY_SERVICES_TASKS = "play-services-tasks-18.1.0";         // 原 18.0.1

// 新增：
public static String PLAY_SERVICES_CLOUD_MESSAGING = "play-services-cloud-messaging-17.2.0";
public static String TRANSPORT_API = "transport-api-3.1.0";
public static String TRANSPORT_RUNTIME = "transport-runtime-3.1.8";
public static String TRANSPORT_BACKEND_CCT = "transport-backend-cct-3.1.8";
public static String KOTLINX_COROUTINES_PLAY_SERVICES = "kotlinx-coroutines-play-services-1.6.4";
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

## 五、库数量统计

| 类别 | 数量 | 说明 |
|------|------|------|
| 升级的 Firebase 库 | 8 | auth (23.1.0), common (21.0.0), components (18.0.0), database (21.0.0), db-collection (18.0.1), storage (21.0.1), messaging (24.1.0), auth-interop (20.0.0) |
| 升级的 Play Services | 3 | base (18.1.0), basement (18.3.0), tasks (18.1.0) |
| 保留但版本更新 | 2 | iid-interop (17.1.0), measurement-connector (19.0.0) |
| 新增的 Firebase 库 | 10 | annotations (16.2.0), appcheck (17.1.0), appcheck-interop (17.1.0), common-ktx (21.0.0), datatransport (18.2.0), encoders (17.0.0), encoders-json (18.0.0), encoders-proto (16.0.0), installations (17.2.0), installations-interop (17.1.0) |
| 新增的其他库 | 5 | transport-api (3.1.0), transport-runtime (3.1.8), transport-backend-cct (3.1.8), cloud-messaging (17.2.0), coroutines-play-services (1.6.4) |
| 移除的库 | 1 | firebase-iid |
| **总影响** | **29** | libs.zip 和 dexs.zip 中需要修改/新增/删除 29 个库 |

## 六、时间估计

| 阶段 | 工作量 | 预计时间 |
|------|--------|--------|
| 阶段 1：修复 BOM 不匹配 | 低 | 10 分钟 |
| 阶段 2：废弃 API 迁移 | 中 | 1-2 小时 |
| 阶段 3：资产文件替换（29 个库） | 高 | 4-6 小时（含下载、提取、打包） |
| 阶段 4：代码声明更新 | 高 | 2-3 小时（含依赖树验证） |
| 测试验证 | 高 | 2-3 小时 |
| **总计** | | **9-15 小时** |

---

## 七、文件变更清单

| 文件 | 阶段 | 变更类型 |
|------|------|--------|
| `ComponentCodeGenerator.java` | 1, 4 | 修改 BOM 版本号 (34.1.0 → 33.7.0) |
| `ComponentTypeMapper.java` | 2 | 移除 FirebaseInstanceId/InstanceIdResult import |
| `ManifestGenerator.java` | 2 | 移除 firebase-iid Registrar + c2dm 权限 |
| `EditorManifest.java` | 2 | 移除 FirebaseInstanceIdReceiver |
| `app/src/main/assets/libs/libs.zip` | 3 | 替换/新增/删除 29 个库的 JAR + res 文件 |
| `app/src/main/assets/libs/dexs.zip` | 3 | 替换/新增/删除 29 个库的 DEX 文件 |
| `BuiltInLibraries.java` | 4 | 更新 20+ 版本常量 + 完整依赖树重建 |
| `ProjectBuilder.java` | 4 | 可能需要添加 firebase-installations 显式引入 |
| `ExtLibSelected.java` | 4 | 验证 FCM 依赖链 |
