# 本地库（Local Library）问题审查

> 审查日期：2026-03-13

---

## 一、已修复问题

### 1. ManageLocalLibrary 构造函数

**问题**：`new FilePathUtil().getPathLocalLibrary(projectId)` 错误实例化工具类，`FilePathUtil` 方法均为静态。

**修复**：改为 `FilePathUtil.getPathLocalLibrary(projectId)`。

### 2. getNativeLibs() 潜在 NPE

**问题**：`localLibraryDexFile.getParentFile()` 可能返回 null（如根路径），直接传入 `new File(parent, "jni")` 存在 NPE 风险。

**修复**：增加 null 检查，`if (parentDir == null) continue;`。

---

## 二、架构与路径

### 路径一致性

| 用途 | 路径来源 | 路径 |
|------|----------|------|
| 项目本地库配置 | `FilePathUtil.getPathLocalLibrary(sc_id)` | `{ext}/.sketchware/data/{sc_id}/local_library` |
| 本地库配置（LocalLibrariesUtil） | `FileUtil.getExternalStorageDir()` | 同上 |
| 本地库目录（主） | `FilePathUtil.getLocalLibsDir()` | `{ext}/.sketchware/libs/local_libs` |
| 本地库目录（备用） | `FilePathUtil.getLocalLibsFallbackDir()` | `{app_ext}/local_libs` |

`getExternalStorageDir()` 与 `Environment.getExternalStorageDirectory().getAbsolutePath()` 等价，路径一致。

### 双路径策略

- **主路径**：共享存储 `.sketchware/libs/local_libs`，卸载应用后仍保留。
- **备用路径**：应用专属存储 `getExternalFilesDir(null)/local_libs`，用于规避 Android 11+ FUSE/MediaProvider 限制。

---

## 三、已有功能（已实现）

| 功能 | 实现位置 |
|------|----------|
| 子依赖过滤展示 | `LocalLibrariesUtil.getAllLocalLibraries()` — 仅展示根库和独立库 |
| 共享子依赖删除 | `deleteSelectedLocalLibraries()` — 多根库共享的子依赖保留并提示 |
| 孤立库检测与清理 | `getOrphanLibraries()` + ManageLocalLibraryActivity 清理入口 |
| 多 DEX 支持 | `ManageLocalLibrary.getExtraDexes()` — 收集 classes2.dex 等 |
| 双路径回退 | `FilePathUtil` 主路径 + fallback 路径，兼容 FUSE 限制 |

## 四、已知限制（未实现）

### 依赖顺序与冲突解析

`local_library` JSON 中库的顺序会影响 classpath 和 DEX 合并顺序。当前无 Maven 式依赖解析，依赖顺序由用户配置决定，可能出现版本冲突需手动调整。

---

## 五、相关文件

| 文件 | 职责 |
|------|------|
| `LocalLibrariesUtil` | 库列表、删除、创建 library map、子依赖映射 |
| `LocalLibrary` | 库元数据、dependency-tree.json 解析 |
| `ManageLocalLibrary` | 项目级库配置、classpath/DEX/res 路径 |
| `LocalLibraryImportPackageIndex` | 从 JAR 扫描并缓存 import 包列表 |
| `ManageLocalLibraryActivity` | 库管理 UI、启用/禁用、级联、孤立库清理 |
| `LibraryDownloaderDialogFragment` | 从 Maven 下载库 |

---

## 六、与 GitHub Issue #1971 的关系

Issue #1971 涉及库加载、DEX 合并性能、依赖解析等。本地库相关点：

- **DEX 合并**：本地库 DEX 通过 `DexMerger` 合并，大量库可能影响性能。
- **依赖解析**：本地库通过 Dependency Resolver 下载，依赖 `dependency-tree.json`。
- **classpath**：`ManageLocalLibrary.getJarLocalLibrary()` 生成 JAR classpath，格式为 `:path1:path2`，与既有 classpath 拼接使用。
