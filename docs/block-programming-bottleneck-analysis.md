# Sketchware-Pro 块编程瓶颈分析

基于对以下核心文件的深度审查：
- 核心引擎：BlockInterpreter、BlockCodeRegistry、BlockSpecRegistry
- 代码生成：ActivityCodeGenerator、ComponentCodeGenerator、ComponentTypeMapper
- 函数系统：ReturnMoreblockManager、MoreBlockBuilderView、VariableItemView
- 事件系统：EventCodeRegistry、ManageEvent、EventsHandler
- 扩展系统：BlocksHandler（内置扩展块）、ExtraBlocks、ExtraPaletteBlock
- 块收藏：BlockCollectionManager、MoreBlockCollectionManager

---

## 已有功能清单（避免误判为缺失）

在分析瓶颈之前，先明确项目已有的扩展功能（主要在 BlocksHandler.java 中通过
`showBuiltIn()` 开关提供，需在设置中启用 "built-in-blocks"）：

### 已有控制流扩展
| 块 | 来源 | 生成代码 |
|----|------|----------|
| `whileLoop` | BlocksHandler:2136 | `while(%s) { ... }` |
| `tryCatch` | BlocksHandler:2146 | `try { ... } catch (Exception e) { ... }` |
| `getExceptionMessage` | BlocksHandler:2156 | `e.getMessage()` |
| `switchStr` / `switchNum` | BlocksHandler:2167/2177 | `switch(%s) { ... }` / `switch((int)%s) { ... }` |
| `caseStr` / `caseNum` | BlocksHandler:2187/2197 | `case %s: { ... break; }` |
| `caseStrAnd` / `caseNumAnd` | ExtraBlocks:36-37 | `case %s:` (fall-through) |
| `defaultSwitch` | BlocksHandler:2207 | `default: { ... break; }` |
| `continue` | ExtraBlocks:38 | `continue;` |
| `repeatKnownNum` | BlocksHandler:2126 | `for(int %s=0; %s<(int)(%s); %s++) { ... }` |
| `RepeatKnownNumDescending` | BlocksHandler:2514 | `for(int %s=((int)%s-1); %s>-1; %s--) { ... }` |
| `forLoopIncrease` | BlocksHandler:553 | `for(%s=%s; %s; %s++) { ... }` |
| `ternaryString` / `ternaryNumber` | BlocksHandler:535/544 | `%s ? %s : %s` |
| `instanceOfOperator` | ExtraBlocks:41 | `%s instanceof %s` |
| `isEmpty` | ExtraBlocks:39 | `%s.isEmpty()` |

### 已有 return 块（MoreBlock 提前返回）
| 块 | 生成代码 |
|----|----------|
| `returnString` | `return (%s);` |
| `returnNumber` | `return (%s);` |
| `returnBoolean` | `return (%b);` |
| `returnMap` | `return %m.varMap;` |
| `returnListStr` | `return %m.listStr;` |
| `returnListMap` | `return %m.listMap;` |
| `returnView` | `return %m.view;` |

### 已有事件扩展
| 事件 | 来源 |
|------|------|
| `onCreateOptionsMenu` | ManageEvent（菜单创建） |
| `onOptionsItemSelected` | ManageEvent（菜单项点击） |
| `onCreateContextMenu` | ManageEvent（上下文菜单） |
| `onContextItemSelected` | ManageEvent（上下文菜单项点击） |
| `onSaveInstanceState` / `onRestoreInstanceState` | ManageEvent（状态保存/恢复） |
| `viewOnClick` | BlockCodeRegistry:324 + ExtraPaletteBlock:310（C 形块，内联 OnClickListener） |
| `viewOnTouch` | BlocksHandler:1687（C 形块，内联 OnTouchListener） |
| `viewOnLongClick` | BlocksHandler:1670（C 形块，内联 OnLongClickListener） |
| `onSwipeRefreshLayout` | BlocksHandler:1645（C 形块，内联刷新监听） |
| `checkboxOnChecked` | BlocksHandler:1704（C 形块，内联选中监听） |
| `showSnackbar` | BlocksHandler:1230（C 形块，内联 Snackbar onClick） |
| 自定义事件 | EventsHandler — 用户可通过 JSON 定义自定义事件/监听器 |

### 已有 Map 类型化存取块
| 块 | 来源 | 生成代码 |
|----|------|----------|
| `hashmapGetNumber` | BlocksHandler:2373 | `(double)%s.get(%s)` |
| `hashmapGetBoolean` | BlocksHandler:2400 | `(boolean)%s.get(%s)` |
| `hashmapGetMap` | BlocksHandler:2418 | `(HashMap<String,Object>)%s.get(%s)` |
| `hashmapListstr` | BlocksHandler:2436 | `(ArrayList<String>)%s.get(%s)` |
| `hashmapGetListmap` | BlocksHandler:2455 | `(ArrayList<HashMap<String,Object>>)%s.get(%s)` |
| `hashmapPutNumber` | BlocksHandler:2382 | `%s.put(%s, (int)(%s));` |
| `hashmapPutNumber2` | BlocksHandler:2391 | `%s.put(%s, (double)(%s));` |
| `hashmapPutBoolean` | BlocksHandler:2409 | `%s.put(%s, %s);` |
| `hashmapPutMap` | BlocksHandler:2427 | `%s.put(%s, %s);` |
| `hashmapPutListstr` | BlocksHandler:2446 | `%s.put(%s, %s);` |
| `hashmapPutListmap` | BlocksHandler:2465 | `%s.put(%s, %s);` |

### 已有其他扩展
| 功能 | 说明 |
|------|------|
| `addCustomVariable` 块 | 允许声明任意 Java 类型的字段（ExtraBlocks:66, ActivityCodeGenerator:142） |
| `addInitializer` 块 | 允许在 initialize() 中添加任意代码 |
| `customImport` / `customImport2` 块 | 允许添加任意 import 语句 |
| 自定义变量（类型 5） | `addCustomVariable` 块 + `isCustomVarUsed()` 自动检测已知类型并提供配套块 |
| 自定义变量（类型 6） | UI 对话框添加，支持修饰符/类型/名称/初始化值，自动映射块形状 |
| 自定义列表 | UI 对话框添加，CustomVariableUtil 解析自定义列表类型 |
| MoreBlock 参数类型 | VariableItemView 支持 boolean/number/string/Map/ListInt/ListStr/ListMap/View 及组件类型 |
| Block Collection | 块收藏功能，可保存/加载块组合（BlockCollectionManager） |
| MoreBlock Collection | MoreBlock 收藏功能，可保存/加载自定义函数（MoreBlockCollectionManager） |
| Undo / Redo | BlockHistoryManager 支持块操作撤销/重做（LogicEditorActivity:270/466，工具栏按钮） |
| 查看源代码 | 菜单项 menu_logic_showsource，可预览生成的 Java 源码 |
| 块复制 | 拖拽块到 Copy 区域可复制块树（LogicTopMenu.isCopyActive） |
| `startService` / `stopService` | BlocksHandler:490/499 |
| `sendBroadcast` | BlocksHandler:508 |
| 类型化 addSourceDirectly | `asdBoolean`（返回 boolean）、`asdNumber`（返回 double）、`asdString`（返回 String） |

---

## 一、类型系统瓶颈（根本性限制）

### 1.1 变量类型系统的双层结构

**核心变量类型**（ComponentTypeMapper.getVariableTypeName，类型 0-3）：

| 类型ID | Java 类型 | 说明 |
|--------|-----------|------|
| 0 | boolean | 布尔 |
| 1 | double | 数字（全部用 double） |
| 2 | String | 字符串 |
| 3 | Map (`HashMap<String, Object>`) | 键值对 |

**自定义变量**（类型 5）通过 `addCustomVariable` 块扩展：
- 可声明任意 Java 类型字段（如 `File myFile`、`int[] arr`）
- **已知类型自动获得配套块** — `ExtraBlocks.isCustomVarUsed()` 检测变量类型名，
  若匹配已知类型则显示对应块。例如：
  - `EditText myEt` → 获得 setText/getText/setHint 等 EditText 块
  - `Timer myTimer` → 获得 timerAfter/timerEvery/timerCancel 块
  - `Dialog myDlg` → 获得 dialogSetTitle/dialogShow 等块
  - `File myFile` → 获得专属 fileCanRead/fileGetName/fileGetPath 等块（ExtraBlocks.fileBlocks）
  - `ProgressDialog`、`CompoundButton`、`ImageView` 等同理
- **未知类型无块操作** — 非内置类型（如 `MyCustomClass obj`）只能通过 addSourceDirectly 操作
- **无自动补全** — 不知道可用的方法/属性

**自定义变量**（类型 6）通过 UI 对话框添加（LogicClickListener.addCustomVariable）：
- 支持修饰符（private/public/static/final 等）、类型、名称、初始化值
- 使用 `CustomVariableUtil` 解析，自动映射块形状：
  - boolean/Boolean → 布尔块（b）
  - String → 字符串块（s）
  - double/int/float/long 等数值类型 → 数字块（d）
  - 其他类型 → 通用块（v）

**真正的瓶颈：**
- **int / long 缺失** — 所有数字统一为 double，导致：
  - 20+ 个块内部需要 `(int)(...)` 强转（repeat、getAt、indexOf 等）
  - 位运算完全不可用（double 不支持 `&`, `|`, `^`, `<<`, `>>`）
  - 大整数精度丢失（double 仅有 53 位有效位）
- **未知自定义类型无块操作** — 非内置类型声明后只能用 addSourceDirectly，失去块编程优势

### 1.2 列表类型受限

3 种核心列表（ComponentTypeMapper.getListInternalName）：

| 类型ID | Java 类型 |
|--------|-----------|
| 1 | `ArrayList<Double>` (ListInt) |
| 2 | `ArrayList<String>` (ListString) |
| 3 | `ArrayList<HashMap<String, Object>>` (ListMap) |

**自定义列表** — 通过 "Add custom list" 对话框（LogicClickListener.addCustomList），
可声明任意元素类型的列表。ExtraPaletteBlock.list() 使用 CustomVariableUtil 解析
自定义列表并显示为列表块。

**缺失：**
- **Set / Queue / Stack** — 无其他集合类型
- **嵌套列表** — `ArrayList<ArrayList<...>>` 无法用块表达
- **数组** — 无原生数组支持
- **自定义列表无专用操作块** — 仅有 getter 块，无 add/remove/getAt 等列表操作块

### 1.3 Map 类型系统

**核心块** `mapGet` 返回 `.toString()`（BlockCodeRegistry），但 **BlocksHandler 已补充类型化取值块**：
- `hashmapGetNumber` — `(double)%s.get(%s)`
- `hashmapGetBoolean` — `(boolean)%s.get(%s)`
- `hashmapGetMap` — `(HashMap<String,Object>)%s.get(%s)`
- `hashmapListstr` / `hashmapGetListmap` — 取出列表

**剩余限制：**
- 类型化取值块使用强制转换，**类型不匹配时运行时 ClassCastException**
- 核心 `mapGet` 仍返回 String，初学者容易误用
- 无类型安全的 Map 结构（值始终为 Object）

---

## 二、控制流瓶颈

### 2.1 已有控制流的局限性

虽然 BlocksHandler 提供了 while、try-catch、switch-case、continue 等扩展块，
但这些块有一些设计上的限制：

**try-catch 限制：**
- **异常变量固定为 `e`** — 嵌套 try-catch 会导致变量名冲突
- **只能捕获 Exception** — 无法指定具体异常类型（如 IOException）
- **无 finally 块** — 无法保证资源清理
- **无多 catch** — 无法分别处理不同类型的异常

**switch-case 限制：**
- **case 块各自独立** — 与标准 switch 语义不同，每个 case 是独立的 C 形块而非 switch 的子结构
- **需要用户手动嵌套** — switch 块的 subStack 里放 case 块，但没有结构化约束

### 2.2 仍然缺失的控制流

| 控制流 | 现状 | 影响 |
|--------|------|------|
| **for-each (列表)** | 无专用块，需 repeat + getAt | 遍历列表代码冗余 |
| **do-while** | 完全不可用 | 需要至少执行一次的循环场景 |
| **else-if 链** | 只能嵌套 ifElse，每层增加缩进 | 多条件判断可读性差 |
| **带标签的 break/continue** | 不可用 | 嵌套循环控制 |

### 2.3 BlockBean 子栈限制

BlockBean 仅有 `subStack1` 和 `subStack2` 两个子栈：
- `if` 使用 subStack1
- `ifElse` 使用 subStack1（if体）+ subStack2（else体）
- `tryCatch` 使用 subStack1（try体）+ subStack2（catch体）
- **无 subStack3** → 无法实现 if/else-if/else 三段式、try/catch/finally 三段式

---

## 三、函数系统瓶颈（MoreBlock）

### 3.1 参数类型

MoreBlockBuilderView + VariableItemView 已支持的参数类型：
- `b` — boolean
- `d` — number (double)
- `s` — string
- `m.varMap` — Map
- `m.listInt` / `m.listStr` / `m.listMap` — 三种列表
- `m.view` / `m.textview` / `m.imageview` 等 — View 类型
- `m.intent` / `m.firebase` 等 — 组件类型

**剩余限制：**
- **可变参数 / 默认值** — 不支持
- **自定义类型参数** — 不支持（如传递自定义变量类型 5 的对象）

### 3.2 返回类型

ReturnMoreblockManager 支持的返回类型已经比较完整：
void、String、double、boolean、Map、List String、List Map、View

**缺失：**
- **List Number** — 无法返回数字列表
- **自定义返回类型** — 不支持

### 3.3 函数作用域限制

- **无全局 MoreBlock** — 绑定到单个 Activity，跨 Activity 不可复用
  - 缓解方案：MoreBlock Collection 可以保存/加载，但需要手动导入到每个 Activity
- **无重载** — 同名不同参数的 MoreBlock 不可能
- **无异步函数** — MoreBlock 始终同步执行

### 3.4 无类/对象系统

- 所有代码生成在单个 Activity 类内
- 无法定义自定义类、接口、抽象类（`addCustomVariable` 可以声明字段，但无法定义新类）
- 无法实现继承、多态
- 数据建模只能用 `HashMap<String, Object>`（无类型安全）

---

## 四、事件系统瓶颈

### 4.1 事件绑定模型

EventCodeRegistry（~40 个核心事件）+ ManageEvent（~30 个扩展事件）+
EventsHandler（自定义事件 JSON）构成了事件系统。

**优势：** 支持自定义事件/监听器（通过 EventsHandler JSON 文件定义）

**限制：**
- **动态绑定不可能** — 事件在 initialize() 中一次性绑定，无法运行时注册/注销
- **同一 View 同类型事件唯一** — 同一个 View 只能有一个 onClick 处理
- **内联事件块与标准事件重复** — viewOnClick、viewOnTouch、checkboxOnChecked 等通过 C 形块在任意位置绑定，但会覆盖之前的监听器

### 4.2 回调变量受限

事件回调暴露的变量是固定的：
- onClick 只暴露 `_view`
- onTextChanged 只暴露 `_charSeq`
- viewOnTouch（BlocksHandler）— 虽然有 `MotionEvent _motionEvent` 参数，但**无配套块**读取 getX()/getY()/getAction()

### 4.3 仍然缺少的 Android 事件

| 事件 | 现状 | 影响 |
|------|------|------|
| **onKeyDown / onKeyUp** | 无 | 按键处理（返回键已有 onBackPressed） |
| **onConfigurationChanged** | 无 | 屏幕旋转/语言切换 |
| **onNewIntent** | 无 | singleTop Activity 的 Intent 处理 |
| **BroadcastReceiver 完整回调** | 仅有 sendBroadcast 块，无接收 | 广播接收 |
| **Service 生命周期** | 仅有 startService/stopService 块 | 后台服务（无 onStartCommand 等） |

---

## 五、代码生成架构瓶颈

### 5.1 单文件单类生成

ActivityCodeGenerator.generateCode() 将整个 Activity 生成为单个 Java 文件：
- **所有逻辑在一个类里** — 无法拆分为多个类/文件
- **Activity 数量有上限** — 受项目文件结构限制

### 5.2 变量全部为实例字段

所有用户变量声明为 `private` 类字段：
```java
private double myNumber = 0;
private String myString = "";
```
- **无局部变量** — 每个变量都占用实例内存
- **无常量** — 核心变量系统无 final/static（类型 5 `addCustomVariable` 和类型 6 自定义变量均可声明，但失去块编程优势）
- **命名冲突风险** — 所有事件共享同一命名空间

### 5.3 仅生成 Java

BlockInterpreter 和 BlockCodeRegistry 100% 生成 Java 代码：
- **无 Kotlin 支持** — 不能使用协程、扩展函数、空安全等
- **无 Gradle 自定义** — build.gradle 模板固定（除了 Command Block）

### 5.4 字符串拼接式代码生成

所有代码通过 String.format / StringBuilder 拼接，无 AST 中间表示：
- **无类型检查** — 类型错误只能在 javac 编译时发现
- **无代码分析** — 无法检测未使用变量、死代码、空指针风险
- **调试困难** — 生成的代码无源码映射

---

## 六、Logic Editor UI 瓶颈

### 6.1 搜索能力受限

**已有：** 调色板类别搜索（PaletteSelector.showSearchDialog）— 可按名称过滤调色板类别（如“变量”“控制”“组件”等）

**仍缺失：**
- **无块级搜索** — 无法在已放置的块中搜索特定块/文本
- **无变量使用位置搜索** — 重命名/删除变量无法确认影响范围
- **无跨 Activity 搜索** — 每个 Activity 的逻辑独立查看

### 6.2 跨 Activity 复用受限

- **MoreBlock 不能跨 Activity 共享** — 每个 Activity 独立定义
  - 缓解：MoreBlock Collection 可以保存/导入，但需要手动操作
- **Block Collection 已有** — 可以保存/加载块组合，部分缓解了跨 Activity 复制的问题
- **无全局变量** — Activity 间数据传递仅通过 Intent putExtra / SharedPreferences

### 6.3 无块折叠/注释

**已有：** Undo/Redo（BlockHistoryManager）、查看生成源码、块复制、块收藏

**仍缺失：**
- **无法折叠嵌套块** — 深层嵌套导致可读性极差
- **无法给块添加注释** — 无法解释复杂逻辑的意图
- **无法暂时禁用块** — 调试时无法"注释掉"一段逻辑
- **无断点/单步调试** — 完全无调试能力

### 6.4 重构能力受限

**已有：** Activity 内变量/列表重命名（LogicClickListener.renameVariable → ProjectDataStore.renameVariableInBlocks 自动更新所有块引用）

**仍缺失：**
- **无跨 Activity 重命名** — 重命名只在当前 Activity 生效
- **无提取函数** — 无法将选中的块提取为 MoreBlock
- **无内联函数** — 无法将 MoreBlock 展开为内联代码

---

## 七、addSourceDirectly 的"逃生舱"问题

addSourceDirectly 块是突破块系统限制的主要手段，但它本身有问题：

- **单行文本框输入** — 写多行 Java 代码极其不便
- **无语法高亮/自动补全** — 纯文本输入
- **ViewBinding 不自动转换** — 必须手动写 `binding.viewId`
- **无实时验证** — 错误只在编译时发现

**缓解：** `asdBoolean`/`asdNumber`/`asdString` 提供类型化的 addSourceDirectly 变体，
允许原始代码返回特定类型值（boolean/number/string），可作为表达式嵌入其他块。

更大的问题是：**过度依赖 addSourceDirectly / addCustomVariable / customImport
会让块编程退化为不便的文本编辑器。** 项目已经为此提供了大量"逃生舱"块
（Command Block Java/XML、addPermission 等），说明现有块系统经常需要被绕过。

---

## 八、瓶颈影响矩阵（修正版）

排除已有功能后的**真正瓶颈**：

| 瓶颈 | 影响范围 | 用户痛感 | 改进难度 | 优先级 |
|------|---------|---------|---------|-------|
| 无 for-each (列表遍历) | 所有列表遍历场景 | 高 | **低** — 新增 1 个 C 形块 | **P0** |
| 无 else-if 链 | 多条件判断 | 高 | **中** — 需 subStack3 或新块 | **P0** |
| Map 值强转无安全检查 | Map 数据处理 | 中 | **低** — 添加 instanceof 检查 | **P2** |
| try-catch 不支持 finally / 多 catch | 资源清理、精细异常处理 | 中 | **中** — 需 subStack3 | **P1** |
| 变量全为 double 无 int | 位运算/精度 | 中 | **高** — 影响整个类型系统 | **P1** |
| 无块搜索 | 大型项目 | 极高 | **中** — UI 功能 | **P1** |
| MoreBlock 无自定义类型参数 | 函数封装 | 低 | **高** — 需动态类型系统 | **P2** |
| 无全局 MoreBlock（跨 Activity） | 代码复用 | 高 | **高** — 架构变更 | **P1** |
| 无块注释/折叠/禁用 | 调试、可读性 | 中 | **低** — UI + codegen | **P2** |
| 无局部变量 | 内存/命名冲突 | 中 | **高** — 架构变更 | **P2** |
| 未知类型自定义变量无块操作 | 非内置类型使用体验 | 低 | **高** — 需动态块生成 | **P3** |
| 无自定义类定义 | 数据建模 | 高 | **极高** — 架构革命 | **P3** |
| 无 Kotlin 生成 | 现代 Android 开发 | 中 | **极高** — 完全重写 codegen | **P3** |

---

## 九、推荐改进路径

### 阶段一：低成本高回报

1. **for-each 列表遍历块** — 类似 sqliteForEachRow 的 C 形块，遍历 ListStr/ListInt/ListMap
2. **块级搜索** — 在调色板中按块名搜索（已有类别搜索），在画布中搜索已放置的块

### 阶段二：中等投入

3. **else-if 链** — 需要扩展 BlockBean 支持或设计新的块连接模式
4. **块注释/折叠/禁用** — BlockBean 添加 `disabled` 字段，codegen 跳过
5. **try-catch-finally** — 需要 subStack3 或链式块设计

### 阶段三：架构级改进

6. **全局 MoreBlock** — 独立于 Activity 的共享函数定义
7. **int 变量类型** — 新增变量类型 ID，修改 ComponentTypeMapper
8. **局部变量** — MoreBlock/事件内部变量作用域

### 阶段四：长期愿景

9. **自定义类** — 数据类定义 + 构造函数
10. **Kotlin 代码生成** — 协程、空安全、扩展函数
11. **AST 中间表示** — 替代字符串拼接，支持代码分析/优化
12. **调试器** — 断点 + 变量监视
