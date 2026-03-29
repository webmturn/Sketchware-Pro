# 本地库依赖顺序方案
## 问题
`dependency-tree.json` 已经用于子依赖展示、共享删除保护和启用/禁用防护；但 `local_library` JSON 中库的顺序仍会直接影响 classpath 和 DEX 合并顺序，当前尚未实现构建期拓扑排序或版本冲突检测，因此仍可能出现：
- 依赖库排在依赖方之后 → 类解析顺序错误
- 多库版本冲突 → 先出现的版本生效，用户难以感知
---
## 方案一：拓扑排序（推荐，改动小）
### 思路
利用已有的 `dependency-tree.json` 构建依赖图，对 `projectUsedLibs` 做拓扑排序，使**依赖项排在依赖方之前**。
### 依赖关系来源
- **根库**：有 `dependency-tree.json`，其中 `builtIn: false` 的条目为本地子依赖
- **子依赖**：无 dependency-tree，视为叶子节点
- **独立库**：无 dependency-tree，视为叶子节点
### 实现步骤

1. **在 `LocalLibrariesUtil` 或 `ManageLocalLibrary` 中新增**：

   ```java
   /**
    * Returns projectUsedLibs sorted by dependency order: dependencies before dependents.
    * Uses dependency-tree.json from root libraries to build the graph.
    */
   public static ArrayList<HashMap<String, Object>> sortByDependencyOrder(
           ArrayList<HashMap<String, Object>> projectUsedLibs,
           List<LocalLibrary> allLocalLibraries) {
       // 1. Build name -> LocalLibrary map from allLocalLibraries
       // 2. Build graph: for each root, add edges (root -> subDep) for each subDep
       // 3. Topological sort: dependencies first
       // 4. Return new list in sorted order (preserve entries not in graph)
   }
   ```

2. **调用点**：
   - `ManageLocalLibrary` 构造函数：对 `list` 调用 `sortByDependencyOrder` 后再使用（或提供 `getOrderedList()`）
   - 或 `ProjectBuilder.getClasspath()` / `getDexLocalLibrary()` 等：在构建 classpath/DEX 列表时使用排序后的顺序

3. **拓扑排序算法**：Kahn 或 DFS 逆序，O(V+E)。

### 优点

- 不修改 `local_library` 文件，用户看到的顺序可保持不变
- 仅影响构建时的 classpath/DEX 顺序
- 复用现有 `dependency-tree.json`，无需新数据

### 缺点

- 不解决版本冲突（如 A 需要 okhttp 4.x、B 需要 okhttp 5.x）
- 无 dependency-tree 的库（手动添加、旧格式）无法参与排序，保持原顺序

---

## 方案二：构建时排序 + 缓存

### 思路

在 `ProjectBuilder` 或 `ManageLocalLibrary` 首次构建时计算排序结果，缓存到 `{project}/.build/local_lib_order.json`，后续构建直接使用，减少重复计算。

### 实现

- 缓存 key：`projectUsedLibs` 的 name 列表的 hash
- 缓存 value：排序后的 name 列表
- 当 `local_library` 变更时，hash 变化，重新计算

### 优点

- 避免每次构建都做拓扑排序
- 适合大型项目、多库场景

---

## 方案三：写入时自动排序（侵入性较强）

### 思路

在 `toggleLibrary`、`rewriteLocalLibFile` 等写入 `local_library` 的路径，写入前对 `projectUsedLibs` 做拓扑排序，使持久化数据本身有序。

### 实现

- `ManageLocalLibraryActivity.rewriteLocalLibFile` 调用前，先 `sortByDependencyOrder`
- 或 `LocalLibrariesUtil` 提供 `rewriteLocalLibFileSorted(scId, projectUsedLibs, allLibraries)`

### 优点

- 存储即有序，所有读取路径自动正确

### 缺点

- 会改变用户看到的库顺序
- 需在 UI 层拿到 `allLocalLibraries`，可能增加参数传递

---

## 方案四：版本冲突检测（长期）

### 思路

在 dependency-tree 中解析 `groupId:artifactId:version`，检测同一 `groupId:artifactId` 存在多个 version 时提示用户。

### 实现

- 遍历所有 root 的 dependency-tree，收集 `(groupId, artifactId) -> Set<version>`
- 若某 artifact 存在多版本，在库管理页显示警告，或构建前弹窗

### 优点

- 用户可主动选择保留哪一版本，或排除冲突库

### 缺点

- 需要解析 Maven 坐标，实现成本较高
- 无法自动解决，仅能提示

---

## 推荐实施顺序

| 阶段 | 方案 | 工作量 | 收益 |
|------|------|--------|------|
| 1 | 方案一：拓扑排序 | 中 | 解决大部分顺序问题 |
| 2 | 方案二：缓存 | 小 | 提升构建性能 |
| 3 | 方案四：冲突检测 | 中 | 提升可观测性 |
| 4 | 方案三：写入时排序 | 小 | 统一数据源，可选 |

---

## 实现要点（方案一）

### 依赖图构建

```
for each lib in projectUsedLibs:
  name = lib.get("name")
  localLib = find LocalLibrary by name in allLocalLibraries
  if localLib != null && localLib.isRootLibrary():
    for subDep in localLib.getSubDependencyNames():
      if subDep in projectUsedLibs:
        add edge: subDep -> name  (subDep must come before name)
```

### 拓扑排序

- 使用 Kahn 算法：计算入度，队列处理入度为 0 的节点，输出顺序即为「依赖在前」。
- 图中不存在的库（无 dependency-tree 的独立库）放在排序结果末尾，保持相对顺序。

### 集成位置

- **选项 A**：`ManageLocalLibrary` 构造函数中，对 `list` 做一次排序（会改变 `list` 顺序，影响所有调用方）
- **选项 B**：新增 `getOrderedLibraryList()`，内部对 `list` 排序后返回，`getJarLocalLibrary`、`getDexLocalLibrary` 等改为使用该方法
- **选项 C**：在 `ProjectBuilder.getClasspath()` 等调用处，对 `localLibraryManager.list` 做排序后再拼接

推荐 **选项 B**：不改变持久化数据，仅影响构建时的迭代顺序，行为清晰。
